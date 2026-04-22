package me.rightsflow.common.permission.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.extern.slf4j.Slf4j;
import me.rightsflow.common.permission.config.PermissionProperties;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

/**
 * Реализация {@link PermissionClient} через {@link RestClient}.
 *
 * <p>Аутентификация к rf-auth-svc выполняется через OAuth2 Client Credentials
 * с использованием настроек {@code rightsflow.oauth2.client.system}.</p>
 *
 * <p>Токен кэшируется до истечения срока действия и обновляется автоматически.</p>
 */
@Slf4j
public class RestPermissionClient implements PermissionClient {

    private final RestClient restClient;
    private final RestClient authClient;
    private final PermissionProperties properties;
    private final String applicationName;

    /** Кэшированный access token и время его истечения */
    private final AtomicReference<CachedToken> cachedToken = new AtomicReference<>();

    public RestPermissionClient(PermissionProperties properties, String applicationName) {
        this.properties = properties;
        this.applicationName = applicationName;
        this.restClient = RestClient.builder()
                .baseUrl(properties.getAuthServiceUrl())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
        this.authClient = RestClient.builder()
                .baseUrl(properties.getAuthServiceUrl())
                .build();
    }

    @Override
    public Map<String, Set<String>> fetchPermissionsForRoles(Set<String> roleNames) {
        if (roleNames == null || roleNames.isEmpty()) {
            return Collections.emptyMap();
        }

        String token = getOrRefreshToken();
        String rolesParam = String.join(",", roleNames);

        log.debug("Fetching permissions from rf-auth-svc for roles: {}, service: {}",
                roleNames, applicationName);

        try {
            PermissionsResponse response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/permissions/by-roles")
                            .queryParam("roles", rolesParam)
                            .queryParam("service", applicationName)
                            .build())
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .retrieve()
                    .body(PermissionsResponse.class);

            if (response == null || response.permissions() == null) {
                log.warn("Empty response from rf-auth-svc for roles: {}", roleNames);
                return buildEmptyMap(roleNames);
            }

            // Конвертируем ответ: { roleName -> [permissions] }
            Map<String, Set<String>> result = new HashMap<>();
            response.permissions().forEach((role, perms) ->
                    result.put(role, new HashSet<>(perms)));

            // Гарантируем что все запрошенные роли присутствуют в результате
            // (даже если у роли нет прав — кладём пустой Set, чтобы не запрашивать повторно)
            roleNames.forEach(role -> result.putIfAbsent(role, Collections.emptySet()));

            log.info("Loaded permissions for {} roles from rf-auth-svc. Total permissions: {}",
                    result.size(),
                    result.values().stream().mapToInt(Set::size).sum());

            return result;

        } catch (RestClientException e) {
            log.error("Failed to fetch permissions from rf-auth-svc: {}", e.getMessage());
            throw new PermissionClientException("Cannot load permissions from rf-auth-svc", e);
        }
    }

    /**
     * Возвращает действующий access token, при необходимости обновляет его.
     */
    private String getOrRefreshToken() {
        CachedToken current = cachedToken.get();
        if (current != null && !current.isExpired()) {
            return current.token();
        }
        return refreshToken();
    }

    private synchronized String refreshToken() {
        // Double-checked locking
        CachedToken current = cachedToken.get();
        if (current != null && !current.isExpired()) {
            return current.token();
        }

        log.debug("Requesting new OAuth2 token from rf-auth-svc");
        log.debug("System client secret: {}", properties.getSystemClientSecret());
        log.debug("System client ID: {}", properties.getSystemClientId());

        MultiValueMap<String, String> formParams = new LinkedMultiValueMap<>();
        formParams.add("grant_type", "client_credentials");
        formParams.add("scope", properties.getSystemClientScope());

        try {
            TokenResponse tokenResponse = authClient.post()
                    .uri("/oauth2/token")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .header(HttpHeaders.AUTHORIZATION,
                            buildBasicAuth(
                                    properties.getSystemClientId(),
                                    properties.getSystemClientSecret()))
                    .body(formParams)
                    .retrieve()
                    .body(TokenResponse.class);

            if (tokenResponse == null || tokenResponse.accessToken() == null) {
                throw new PermissionClientException("Empty token response from rf-auth-svc");
            } else {
                log.debug("Token received: accessToken={}, expiresIn={}",
                        tokenResponse.accessToken(),
                        tokenResponse.expiresIn());
            }

            // Вычитаем 30 секунд для запаса (expires_in приходит в секундах)
            long expiresInMs = (tokenResponse.expiresIn() - 30) * 1000L;
            CachedToken newToken = new CachedToken(
                    tokenResponse.accessToken(),
                    System.currentTimeMillis() + expiresInMs);
            cachedToken.set(newToken);

            log.debug("OAuth2 token refreshed, expires in {} seconds", tokenResponse.expiresIn());
            return newToken.token();

        } catch (RestClientException e) {
            throw new PermissionClientException("Failed to obtain OAuth2 token", e);
        }
    }

    private String buildBasicAuth(String clientId, String clientSecret) {
        String credentials = clientId + ":" + clientSecret;
        return "Basic " + Base64.getEncoder().encodeToString(credentials.getBytes());
    }

    private Map<String, Set<String>> buildEmptyMap(Set<String> roleNames) {
        return roleNames.stream()
                .collect(Collectors.toMap(r -> r, r -> Collections.emptySet()));
    }

    // ---- Internal DTOs ----

    record PermissionsResponse(
            String service,
            Map<String, List<String>> permissions
    ) {}

    record TokenResponse(
            @JsonProperty("access_token") String accessToken,
            @JsonProperty("expires_in") long expiresIn,
            @JsonProperty("token_type") String tokenType,
            @JsonProperty("scope") String scope
    ) {}

    record CachedToken(String token, long expiresAtMs) {
        boolean isExpired() {
            return System.currentTimeMillis() >= expiresAtMs;
        }
    }
}
