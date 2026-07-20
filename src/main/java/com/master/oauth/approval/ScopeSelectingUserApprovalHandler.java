package com.master.oauth.approval;

import com.master.oauth.service.CustomUserDetailsService;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.provider.AuthorizationRequest;
import org.springframework.security.oauth2.provider.ClientDetails;
import org.springframework.security.oauth2.provider.ClientDetailsService;
import org.springframework.security.oauth2.provider.ClientRegistrationException;
import org.springframework.security.oauth2.provider.approval.ApprovalStoreUserApprovalHandler;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Restricts grantable scopes to the intersection of client scopes and user-allowed DB scopes,
 * and only includes scopes the user explicitly approved on the consent page.
 */
public class ScopeSelectingUserApprovalHandler extends ApprovalStoreUserApprovalHandler {

    private final CustomUserDetailsService userDetailsService;
    private ClientDetailsService clientDetailsService;

    public ScopeSelectingUserApprovalHandler(CustomUserDetailsService userDetailsService) {
        this.userDetailsService = userDetailsService;
    }

    @Override
    public void setClientDetailsService(ClientDetailsService clientDetailsService) {
        this.clientDetailsService = clientDetailsService;
        super.setClientDetailsService(clientDetailsService);
    }

    @Override
    public AuthorizationRequest checkForPreApproval(AuthorizationRequest authorizationRequest,
                                                    Authentication userAuthentication) {
        Set<String> requested = new LinkedHashSet<>(authorizationRequest.getScope());
        Set<String> allowed = filterByUserAndClient(userAuthentication.getName(),
                authorizationRequest.getClientId(), requested);
        authorizationRequest.setScope(allowed);
        // Always show consent so the user can pick scopes explicitly
        authorizationRequest.setApproved(false);
        return authorizationRequest;
    }

    @Override
    public boolean isApproved(AuthorizationRequest authorizationRequest, Authentication userAuthentication) {
        return authorizationRequest.isApproved();
    }

    @Override
    @SuppressWarnings("unchecked")
    public AuthorizationRequest updateAfterApproval(AuthorizationRequest authorizationRequest,
                                                    Authentication userAuthentication) {
        Map<String, String> approvalParameters = authorizationRequest.getApprovalParameters();
        Set<String> approved = new LinkedHashSet<>();

        Set<String> candidateScopes = filterByUserAndClient(
                userAuthentication.getName(),
                authorizationRequest.getClientId(),
                authorizationRequest.getScope());

        for (String scope : candidateScopes) {
            String key = "scope." + scope;
            if (approvalParameters.containsKey(key)
                    && "true".equalsIgnoreCase(approvalParameters.get(key))) {
                approved.add(scope);
            }
        }

        // Also accept a multi-value "scopes" parameter from REST consent API
        String scopesParam = approvalParameters.get("scopes");
        if (scopesParam != null && !scopesParam.trim().isEmpty()) {
            for (String part : scopesParam.split(",")) {
                String scope = part.trim();
                if (candidateScopes.contains(scope)) {
                    approved.add(scope);
                }
            }
        }

        authorizationRequest.setScope(approved);
        authorizationRequest.setApproved(!approved.isEmpty()
                && "true".equalsIgnoreCase(approvalParameters.get("user_oauth_approval")));

        return authorizationRequest;
    }

    private Set<String> filterByUserAndClient(String username, String clientId, Set<String> requested) {
        Set<String> userAllowed = userDetailsService.getAllowedScopeNames(username);
        Set<String> clientScopes = resolveClientScopes(clientId);

        Set<String> result = new HashSet<>();
        for (String scope : requested) {
            if (userAllowed.contains(scope) && clientScopes.contains(scope)) {
                result.add(scope);
            }
        }
        // If client did not request specific scopes, offer intersection of user + client
        if (requested.isEmpty()) {
            for (String scope : clientScopes) {
                if (userAllowed.contains(scope)) {
                    result.add(scope);
                }
            }
        }
        return result;
    }

    private Set<String> resolveClientScopes(String clientId) {
        try {
            ClientDetails client = clientDetailsService.loadClientByClientId(clientId);
            return client.getScope() != null ? client.getScope() : new HashSet<String>();
        } catch (ClientRegistrationException ex) {
            return new HashSet<String>();
        }
    }
}
