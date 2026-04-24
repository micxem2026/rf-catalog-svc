package me.rightsflow.common.permission.config;

import lombok.extern.slf4j.Slf4j;
import me.rightsflow.common.permission.aspect.PermissionAspect;
import me.rightsflow.common.permission.cache.InMemoryPermissionCache;
import me.rightsflow.common.permission.client.PermissionClient;
import me.rightsflow.common.permission.client.PermissionClientException;
import me.rightsflow.common.permission.client.RestPermissionClient;
import me.rightsflow.common.permission.kafka.PermissionInvalidationListener;
import me.rightsflow.common.permission.event.PermissionInvalidatedEvent;
import me.rightsflow.common.permission.registration.PermissionRegistrar;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;

import java.util.HashMap;
import java.util.Map;

/**
 * Spring Boot AutoConfiguration для модуля проверки прав доступа.
 *
 * <p>Подключается автоматически через
 * {@code META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports}.</p>
 *
 * <p>Активируется только если в classpath есть spring-kafka
 * и задан {@code spring.kafka.bootstrap-servers}.</p>
 */
@Slf4j
@AutoConfiguration
@EnableScheduling
@EnableConfigurationProperties(PermissionProperties.class)
public class RfPermissionAutoConfiguration {

    @Value("${spring.application.name}")
    private String applicationName;

    @Value("${spring.kafka.bootstrap-servers:localhost:9092}")
    private String bootstrapServers;

    // ---- Core beans ----

    @Bean
    @ConditionalOnMissingBean(PermissionClient.class)
    public PermissionClient permissionClient(PermissionProperties properties) {
        return new RestPermissionClient(properties, applicationName);
    }

    @Bean
    @ConditionalOnMissingBean(InMemoryPermissionCache.class)
    public InMemoryPermissionCache permissionCache(PermissionClient permissionClient,
                                                   PermissionProperties properties) {

        InMemoryPermissionCache cache = new InMemoryPermissionCache(permissionClient);

        // Инициализируем кэш при старте (загружаем известные роли)
        // На старте кэш пустой — роли будут загружены lazy при первом запросе
        // Это нормально, т.к. мы не знаем какие роли будут запрошены заранее
        log.info("Permission cache initialized for service '{}'. " +
                "Permissions will be loaded lazily on first access.", applicationName);

        if (properties.getStartupMode() == PermissionProperties.StartupMode.FAIL) {
            // Пробуем сделать тестовый запрос к rf-auth-svc
            try {
                permissionClient.fetchPermissionsForRoles(java.util.Set.of());
                log.info("Connection to rf-auth-svc verified successfully");
            } catch (PermissionClientException e) {
                throw new IllegalStateException(
                        "Cannot connect to rf-auth-svc (startup-mode=FAIL). " +
                                "Check rightsflow.permissions.auth-service-url", e);
            }
        }

        return cache;
    }

    @Bean
    @ConditionalOnMissingBean(PermissionRegistrar.class)
    public PermissionRegistrar permissionRegistrar(ApplicationContext applicationContext,
                                                   PermissionClient permissionClient,
                                                   PermissionProperties properties) {
        return new PermissionRegistrar(
                applicationContext,
                permissionClient,
                properties,
                applicationName
        );
    }

    @Bean
    public PermissionAspect permissionAspect(InMemoryPermissionCache permissionCache) {
        return new PermissionAspect(permissionCache);
    }

    // ---- JWT Authentication Converter ----

    /**
     * Регистрирует {@link JwtAuthenticationConverter} настроенный на чтение claim {@code roles}.
     *
     * <p><b>Почему именно так:</b> Spring Security при настройке
     * {@code oauth2ResourceServer().jwt()} ищет бин типа {@link JwtAuthenticationConverter}
     * через ApplicationContext и подставляет его автоматически. Возврат кастомного
     * типа (даже реализующего нужный интерфейс) не работает — тип должен совпадать точно.</p>
     *
     * <p>Внутрь стандартного {@link JwtAuthenticationConverter} передаём
     * {@link RfJwtAuthenticationConverter} как {@code grantedAuthoritiesConverter} —
     * он читает claim {@code roles} вместо {@code scope}.</p>
     *
     * <p>Claim {@code username} используется как principal name для обычных пользователей;
     * для SERVICE (client_credentials) principal — это {@code sub} (== clientId).</p>
     */
    @Bean
    @ConditionalOnMissingBean(JwtAuthenticationConverter.class)
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        // Читаем роли из claim "roles", а не из "scope"
        converter.setJwtGrantedAuthoritiesConverter(new RfJwtAuthenticationConverter());
        // Для обычных пользователей principal = username; для SERVICE — sub (== clientId)
        converter.setPrincipalClaimName("sub");
        log.info("Registered JwtAuthenticationConverter: JWT 'roles' claim -> ROLE_* authorities.");
        return converter;
    }

    // ---- Scheduled refresh ----

    @Bean
    public PermissionCacheRefreshTask permissionCacheRefreshTask(InMemoryPermissionCache permissionCache) {
        return new PermissionCacheRefreshTask(permissionCache);
    }

    // ---- Kafka ----

    @Bean
    @ConditionalOnMissingBean(name = "permissionKafkaListenerContainerFactory")
    public ConcurrentKafkaListenerContainerFactory<String, PermissionInvalidatedEvent>
    permissionKafkaListenerContainerFactory(PermissionProperties properties) {

        Map<String, Object> consumerProps = new HashMap<>();
        consumerProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        consumerProps.put(ConsumerConfig.GROUP_ID_CONFIG,
                resolveConsumerGroup(properties));
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
        consumerProps.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);

        // Создаём JsonDeserializer явно, а не через рефлексию из ConsumerConfig.
        // Это единственный надёжный способ передать useHeadersIfPresent(false) —
        // через Map-свойства этот флаг игнорируется при создании через рефлексию.
        JsonDeserializer<PermissionInvalidatedEvent> valueDeserializer =
                new JsonDeserializer<>(PermissionInvalidatedEvent.class);
        // Игнорируем заголовок __TypeId__ — продюсер (rf-auth-svc) его не добавляет,
        // но на случай если кто-то другой будет писать в этот топик.
        valueDeserializer.addTrustedPackages("me.rightsflow.*");
        valueDeserializer.setUseTypeHeaders(false);

        ConsumerFactory<String, PermissionInvalidatedEvent> consumerFactory =
                new DefaultKafkaConsumerFactory<>(
                        consumerProps,
                        new StringDeserializer(),
                        valueDeserializer);

        ConcurrentKafkaListenerContainerFactory<String, PermissionInvalidatedEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        // Manual ack чтобы подтверждать offset только после успешной обработки
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL);

        log.info("Kafka listener container factory for permissions configured. " +
                        "Topic: {}, Group: {}",
                properties.getInvalidationTopic(),
                resolveConsumerGroup(properties));

        return factory;
    }

    @Bean
    public PermissionInvalidationListener permissionInvalidationListener(
            InMemoryPermissionCache permissionCache) {
        return new PermissionInvalidationListener(permissionCache);
    }

    // ---- Helper ----

    private String resolveConsumerGroup(PermissionProperties properties) {
        if (properties.getConsumerGroup() != null && !properties.getConsumerGroup().isBlank()) {
            return properties.getConsumerGroup();
        }
        return applicationName + "-permissions-group";
    }

    /**
     * Внутренний класс для scheduled refresh — вынесен чтобы избежать
     * self-injection проблем с @Scheduled в том же классе.
     */
    static class PermissionCacheRefreshTask {
        private final InMemoryPermissionCache cache;

        PermissionCacheRefreshTask(InMemoryPermissionCache cache) {
            this.cache = cache;
        }

        @Scheduled(fixedDelayString = "${rightsflow.permissions.refresh-interval:PT5M}",
                   initialDelayString = "${rightsflow.permissions.refresh-interval:PT5M}")
        public void scheduledRefresh() {
            cache.refresh();
        }
    }
}
