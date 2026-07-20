package com.master.oauth.controller;

import com.master.oauth.entity.Scope;
import com.master.oauth.service.ScopeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.provider.AuthorizationRequest;
import org.springframework.security.oauth2.provider.ClientDetails;
import org.springframework.security.oauth2.provider.ClientDetailsService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;

import java.security.Principal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * REST alternative to the HTML consent page.
 * SPA / mobile clients can GET available scopes and POST the selected ones.
 */
@RestController
@RequestMapping("/api/consent")
public class ConsentApiController {

    private final ScopeService scopeService;

    @Autowired
    private ClientDetailsService clientDetailsService;

    public ConsentApiController(ScopeService scopeService) {
        this.scopeService = scopeService;
    }

    @GetMapping
    public ResponseEntity<?> pendingConsent(Principal principal,
                                            @SessionAttribute(value = "authorizationRequest", required = false)
                                            AuthorizationRequest authorizationRequest) {
        if (authorizationRequest == null) {
            Map<String, Object> error = new LinkedHashMap<String, Object>();
            error.put("error", "no_pending_request");
            error.put("message", "Start the authorization_code flow via /oauth/authorize first.");
            return ResponseEntity.badRequest().body(error);
        }

        ClientDetails client = clientDetailsService.loadClientByClientId(authorizationRequest.getClientId());
        Set<String> requested = authorizationRequest.getScope();
        if (requested == null || requested.isEmpty()) {
            requested = client.getScope();
        }

        Set<Scope> available = scopeService.resolveConsentScopes(principal.getName(), requested);
        List<Map<String, Object>> scopes = new ArrayList<Map<String, Object>>();
        for (Scope scope : available) {
            Map<String, Object> item = new LinkedHashMap<String, Object>();
            item.put("name", scope.getName());
            item.put("displayName", scope.getDisplayName());
            item.put("description", scope.getDescription());
            scopes.add(item);
        }

        Map<String, Object> body = new LinkedHashMap<String, Object>();
        body.put("clientId", authorizationRequest.getClientId());
        body.put("username", principal.getName());
        body.put("scopes", scopes);
        body.put("approveUrl", "/oauth/authorize");
        return ResponseEntity.ok(body);
    }

    /**
     * Approves selected scopes and continues the OAuth authorize endpoint.
     * Body example: { "scopes": ["read","profile"], "approved": true }
     */
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public RedirectView approveJson(@RequestBody Map<String, Object> body) {
        @SuppressWarnings("unchecked")
        List<String> scopes = (List<String>) body.get("scopes");
        boolean approved = body.get("approved") == null || Boolean.TRUE.equals(body.get("approved"));

        StringBuilder url = new StringBuilder("/oauth/authorize?user_oauth_approval=")
                .append(approved);
        if (scopes != null) {
            for (String scope : scopes) {
                url.append("&scope.").append(scope).append("=true");
            }
        }
        return new RedirectView(url.toString());
    }
}
