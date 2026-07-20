package com.master.oauth.config;

import com.master.oauth.service.TokenRegistryService;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.common.exceptions.InvalidTokenException;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.token.DefaultTokenServices;
import org.springframework.security.oauth2.provider.token.TokenStore;

/**
 * Registers issued JWTs and rejects revoked ones on load.
 */
public class RevocableTokenServices extends DefaultTokenServices {

    private final TokenRegistryService tokenRegistryService;
    private TokenStore tokenStore;

    public RevocableTokenServices(TokenRegistryService tokenRegistryService) {
        this.tokenRegistryService = tokenRegistryService;
    }

    @Override
    public void setTokenStore(TokenStore tokenStore) {
        this.tokenStore = tokenStore;
        super.setTokenStore(tokenStore);
    }

    @Override
    public OAuth2AccessToken createAccessToken(OAuth2Authentication authentication) throws AuthenticationException {
        OAuth2AccessToken token = super.createAccessToken(authentication);
        tokenRegistryService.register(token, authentication);
        return token;
    }

    @Override
    public OAuth2Authentication loadAuthentication(String accessTokenValue) throws AuthenticationException, InvalidTokenException {
        OAuth2Authentication authentication = super.loadAuthentication(accessTokenValue);
        OAuth2AccessToken accessToken = tokenStore.readAccessToken(accessTokenValue);
        if (accessToken != null && accessToken.getAdditionalInformation() != null) {
            Object jti = accessToken.getAdditionalInformation().get("jti");
            if (jti != null && tokenRegistryService.isRevoked(String.valueOf(jti))) {
                throw new InvalidTokenException("Token has been revoked");
            }
        }
        return authentication;
    }

    @Override
    public boolean revokeToken(String tokenValue) {
        OAuth2AccessToken accessToken = tokenStore.readAccessToken(tokenValue);
        if (accessToken != null && accessToken.getAdditionalInformation() != null) {
            Object jti = accessToken.getAdditionalInformation().get("jti");
            if (jti != null) {
                tokenRegistryService.revokeByJti(String.valueOf(jti));
            }
        }
        return super.revokeToken(tokenValue);
    }
}
