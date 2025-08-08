package me.rightsflow.intersync.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.listener.CommonErrorHandler
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer
import org.springframework.kafka.listener.DefaultErrorHandler
import org.springframework.util.backoff.FixedBackOff

//@Configuration
class KafkaErrorHandlerConfig {

    /**
     * Создает и настраивает обработчик ошибок для Kafka-консьюмеров.
     * Этот обработчик будет использоваться для всех консьюмеров в приложении.
     *
     * @param kafkaTemplate KafkaTemplate для отправки сообщений в DLQ. Spring Boot сконфигурирует его автоматически.
     */
    @Bean
    fun commonErrorHandler(kafkaTemplate: KafkaTemplate<*, *>): CommonErrorHandler {

        // 1. Создаем "восстановитель", который будет отправлять сообщения в DLQ.
        // Он использует стандартный KafkaTemplate.
        val recoverer = DeadLetterPublishingRecoverer(kafkaTemplate)

        // 2. Это ключевая настройка!
        // Она говорит, что если "восстановление" (отправка в DLQ) завершилось с ошибкой,
        // то нужно перебросить исключение дальше. Это ПРЕДОТВРАТИТ коммит offset'а.
        // В Spring Kafka 3.x+ этот метод был заменен на `setThrowIfReSeeded(true)`,
        // но `setCommitRecovered(false)` все еще работает для обратной совместимости во многих случаях.
        // Для максимальной надежности можно использовать оба, но `setThrowIfReSeeded` имеет приоритет.
        // ВАЖНО: В Spring for Apache Kafka 3.0+ `setCommitRecovered` устарел.
        // Вместо него используйте конструктор `DefaultErrorHandler(recoverer, backOff)`.
        // Если `recoverer` выбрасывает исключение, `DefaultErrorHandler` его перехватит и не закоммитит offset.

        // 3. Создаем основной обработчик ошибок.
        // Конструктор принимает "восстановитель" и политику повторов (BackOff).
        // Мы используем FixedBackOff с 0 повторами на этом уровне,
        // т.к. повторы у нас уже настроены на уровне binder'а (`max-attempts`).
        // `DefaultErrorHandler` корректно обработает исключение от `recoverer` и не закоммитит offset.
        val errorHandler = DefaultErrorHandler(recoverer, FixedBackOff(0L, 0L))

        // 4. Говорим обработчику НЕ коммитить offset, если восстановление (отправка в DLQ) не удалось.
        // Это гарантирует, что мы не потеряем сообщение.
        errorHandler.setCommitRecovered(false)

        return errorHandler
    }
}
