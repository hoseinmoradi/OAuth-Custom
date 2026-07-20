package com.master.oauth.config;

import com.master.oauth.entity.User;
import com.master.oauth.service.CustomUserDetailsService;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.common.DefaultOAuth2AccessToken;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.token.TokenEnhancer;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class CustomTokenEnhancer implements TokenEnhancer {

    private final CustomUserDetailsService userDetailsService;

    public CustomTokenEnhancer(CustomUserDetailsService userDetailsService) {
        this.userDetailsService = userDetailsService;
    }

    @Override
    public OAuth2AccessToken enhance(OAuth2AccessToken accessToken, OAuth2Authentication authentication) {
        if (authentication.getUserAuthentication() == null) {
            return accessToken;
        }

        Map<String, Object> additionalInfo = new HashMap<String, Object>();
        Object principal = authentication.getUserAuthentication().getPrincipal();
        String username;
        if (principal instanceof UserDetails) {
            username = ((UserDetails) principal).getUsername();
        } else {
            username = principal.toString();
        }

        try {
            User user = userDetailsService.findEntityByUsername(username);
            additionalInfo.put("user_id", user.getId());
            additionalInfo.put("full_name", user.getFullName());
            additionalInfo.put("email", user.getEmail());
        } catch (Exception ignored) {
            additionalInfo.put("user", username);
        }

        ((DefaultOAuth2AccessToken) accessToken).setAdditionalInformation(additionalInfo);
        return accessToken;
    }
}
