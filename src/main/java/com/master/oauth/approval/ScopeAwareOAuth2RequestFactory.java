package com.master.oauth.approval;

import com.master.oauth.service.CustomUserDetailsService;
import org.springframework.security.oauth2.common.exceptions.InvalidScopeException;
import org.springframework.security.oauth2.provider.AuthorizationRequest;
import org.springframework.security.oauth2.provider.ClientDetails;
import org.springframework.security.oauth2.provider.ClientDetailsService;
import org.springframework.security.oauth2.provider.TokenRequest;
import org.springframework.security.oauth2.provider.request.DefaultOAuth2RequestFactory;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Ensures token scopes never exceed the intersection of
 * requested scopes, client scopes, and user DB scopes.
 */
public class ScopeAwareOAuth2RequestFactory extends DefaultOAuth2RequestFactory {

    private final CustomUserDetailsService userDetailsService;
    private final ClientDetailsService clientDetailsService;

    public ScopeAwareOAuth2RequestFactory(ClientDetailsService clientDetailsService,
                                          CustomUserDetailsService userDetailsService) {
        super(clientDetailsService);
        this.clientDetailsService = clientDetailsService;
        this.userDetailsService = userDetailsService;
        setCheckUserScopes(false);
    }

    @Override
    public AuthorizationRequest createAuthorizationRequest(Map<String, String> authorizationParameters) {
        AuthorizationRequest request = super.createAuthorizationRequest(authorizationParameters);
        return request;
    }

    @Override
    public TokenRequest createTokenRequest(Map<String, String> requestParameters,
                                           ClientDetails authenticatedClient) {
        TokenRequest tokenRequest = super.createTokenRequest(requestParameters, authenticatedClient);
        String username = requestParameters.get("username");
        if (username != null && !username.trim().isEmpty()) {
            Set<String> filtered = filterScopes(
                    username,
                    authenticatedClient.getScope(),
                    tokenRequest.getScope());
            if (filtered.isEmpty()) {
                throw new InvalidScopeException(
                        "No allowed scopes for this user/client combination");
            }
            tokenRequest.setScope(filtered);
        }
        return tokenRequest;
    }

    private Set<String> filterScopes(String username, Set<String> clientScopes, Set<String> requested) {
        Set<String> userAllowed = userDetailsService.getAllowedScopeNames(username);
        Set<String> base = (requested == null || requested.isEmpty())
                ? clientScopes
                : requested;

        Set<String> result = new LinkedHashSet<String>();
        if (base == null) {
            return result;
        }
        for (String scope : base) {
            if (userAllowed.contains(scope) && (clientScopes == null || clientScopes.contains(scope))) {
                result.add(scope);
            }
        }
        return result;
    }
}
