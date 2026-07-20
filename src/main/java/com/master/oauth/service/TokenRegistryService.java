package com.master.oauth.service;

import com.master.oauth.entity.TokenRegistry;
import com.master.oauth.repository.TokenRegistryRepository;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.common.OAuth2RefreshToken;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class TokenRegistryService {

    private final TokenRegistryRepository repository;

    public TokenRegistryService(TokenRegistryRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public void register(OAuth2AccessToken accessToken, OAuth2Authentication authentication) {
        String jti = extractJti(accessToken);
        if (jti == null) {
            return;
        }

        if (repository.findByJti(jti).isPresent()) {
            return;
        }

        TokenRegistry entry = new TokenRegistry();
        entry.setJti(jti);
        entry.setTokenType("access");
        entry.setClientId(authentication.getOAuth2Request().getClientId());
        if (authentication.getUserAuthentication() != null) {
            entry.setUsername(authentication.getUserAuthentication().getName());
        }
        if (accessToken.getScope() != null) {
            entry.setScopes(String.join(" ", accessToken.getScope()));
        }
        entry.setIssuedAt(LocalDateTime.now());
        entry.setExpiresAt(toLocalDateTime(accessToken.getExpiration()));

        OAuth2RefreshToken refreshToken = accessToken.getRefreshToken();
        if (refreshToken != null) {
            String refreshJti = extractRefreshJti(refreshToken);
            entry.setRefreshJti(refreshJti);
        }

        repository.save(entry);
    }

    @Transactional(readOnly = true)
    public boolean isRevoked(String jti) {
        return jti != null && repository.existsByJtiAndRevokedTrue(jti);
    }

    @Transactional(readOnly = true)
    public List<TokenRegistry> listByUsername(String username) {
        return repository.findByUsernameOrderByIssuedAtDesc(username);
    }

    @Transactional(readOnly = true)
    public List<TokenRegistry> listActiveByUsername(String username) {
        return repository.findByUsernameAndRevokedFalseOrderByIssuedAtDesc(username);
    }

    @Transactional
    public boolean revokeByJti(String jti) {
        return repository.revokeByJti(jti, LocalDateTime.now()) > 0;
    }

    @Transactional
    public int revokeAllForUser(String username) {
        return repository.revokeAllByUsername(username, LocalDateTime.now());
    }

    @Transactional(readOnly = true)
    public Optional<TokenRegistry> findByJti(String jti) {
        return repository.findByJti(jti);
    }

    public Map<String, Object> toView(TokenRegistry token) {
        Map<String, Object> map = new java.util.LinkedHashMap<String, Object>();
        map.put("id", token.getId());
        map.put("jti", token.getJti());
        map.put("tokenType", token.getTokenType());
        map.put("username", token.getUsername());
        map.put("clientId", token.getClientId());
        map.put("scopes", token.getScopes());
        map.put("issuedAt", token.getIssuedAt());
        map.put("expiresAt", token.getExpiresAt());
        map.put("revoked", token.isRevoked());
        map.put("revokedAt", token.getRevokedAt());
        map.put("active", !token.isRevoked()
                && (token.getExpiresAt() == null || token.getExpiresAt().isAfter(LocalDateTime.now())));
        return map;
    }

    public List<Map<String, Object>> toViews(List<TokenRegistry> tokens) {
        return tokens.stream().map(this::toView).collect(Collectors.toList());
    }

    @SuppressWarnings("unchecked")
    private String extractJti(OAuth2AccessToken accessToken) {
        Map<String, Object> info = accessToken.getAdditionalInformation();
        if (info != null && info.get("jti") != null) {
            return String.valueOf(info.get("jti"));
        }
        return null;
    }

    private String extractRefreshJti(OAuth2RefreshToken refreshToken) {
        // JWT refresh tokens embed jti inside the value; store raw fingerprint fallback
        String value = refreshToken.getValue();
        if (value != null && value.length() > 36) {
            return value.substring(value.length() - 36);
        }
        return value;
    }

    private LocalDateTime toLocalDateTime(Date date) {
        if (date == null) {
            return null;
        }
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(date.getTime()), ZoneId.systemDefault());
    }
}
