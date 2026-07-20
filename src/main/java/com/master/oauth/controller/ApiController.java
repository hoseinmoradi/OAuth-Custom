package com.master.oauth.controller;

import com.master.oauth.entity.Scope;
import com.master.oauth.entity.User;
import com.master.oauth.service.CustomUserDetailsService;
import com.master.oauth.service.ScopeService;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class ApiController {

    private final ScopeService scopeService;
    private final CustomUserDetailsService userDetailsService;

    public ApiController(ScopeService scopeService, CustomUserDetailsService userDetailsService) {
        this.scopeService = scopeService;
        this.userDetailsService = userDetailsService;
    }

    @GetMapping("/public/health")
    public Map<String, Object> health() {
        Map<String, Object> body = new LinkedHashMap<String, Object>();
        body.put("status", "UP");
        body.put("service", "oauth-authorization-server");
        return body;
    }

    @GetMapping("/me")
    public Map<String, Object> me(Principal principal, Authentication authentication) {
        User user = userDetailsService.findEntityByUsername(principal.getName());
        Map<String, Object> body = new LinkedHashMap<String, Object>();
        body.put("id", user.getId());
        body.put("username", user.getUsername());
        body.put("email", user.getEmail());
        body.put("fullName", user.getFullName());
        body.put("allowedScopes", user.getScopes().stream()
                .filter(Scope::isActive)
                .map(Scope::getName)
                .collect(Collectors.toList()));

        if (authentication instanceof OAuth2Authentication) {
            OAuth2Authentication oauth = (OAuth2Authentication) authentication;
            body.put("tokenScopes", oauth.getOAuth2Request().getScope());
            body.put("clientId", oauth.getOAuth2Request().getClientId());
        }
        return body;
    }

    @GetMapping("/me/scopes")
    public List<Map<String, Object>> myScopes(Principal principal) {
        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        for (Scope scope : scopeService.findAllowedScopesForUser(principal.getName())) {
            result.add(scopeService.toView(scope));
        }
        return result;
    }
}
