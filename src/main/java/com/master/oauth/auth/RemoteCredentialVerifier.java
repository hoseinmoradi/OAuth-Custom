package com.master.oauth.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * Calls an external auth web service.
 * Expected request JSON:  { "username": "...", "password": "..." }
 * Expected success: HTTP 200 and body containing "authenticated": true
 *                   OR HTTP 200 with any body (if auth.login.remote.accept-any-200=true default)
 */
@Component
@ConditionalOnProperty(name = "auth.login.mode", havingValue = "remote")
public class RemoteCredentialVerifier implements CredentialVerifier {

    private static final Logger log = LoggerFactory.getLogger(RemoteCredentialVerifier.class);

    private final RestTemplate restTemplate;

    @Value("${auth.login.remote.url}")
    private String remoteUrl;

    public RemoteCredentialVerifier(
            @Value("${auth.login.remote.connect-timeout-ms:3000}") int connectTimeout,
            @Value("${auth.login.remote.read-timeout-ms:5000}") int readTimeout) {
        org.springframework.http.client.SimpleClientHttpRequestFactory factory =
                new org.springframework.http.client.SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(connectTimeout);
        factory.setReadTimeout(readTimeout);
        this.restTemplate = new RestTemplate(factory);
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean verify(String username, String password) {
        try {
            Map<String, String> body = new HashMap<String, String>();
            body.put("username", username);
            body.put("password", password);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, String>> entity = new HttpEntity<Map<String, String>>(body, headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    remoteUrl, HttpMethod.POST, entity, Map.class);

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                return false;
            }
            Object authenticated = response.getBody().get("authenticated");
            if (authenticated instanceof Boolean) {
                return (Boolean) authenticated;
            }
            Object success = response.getBody().get("success");
            if (success instanceof Boolean) {
                return (Boolean) success;
            }
            // Fallback: 2xx with body treated as success
            return true;
        } catch (Exception ex) {
            log.warn("Remote login failed for user {}: {}", username, ex.getMessage());
            return false;
        }
    }
}
