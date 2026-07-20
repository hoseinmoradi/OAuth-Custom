package com.master.oauth.controller;

import com.master.oauth.captcha.CaptchaService;
import com.master.oauth.entity.Scope;
import com.master.oauth.service.ScopeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.provider.AuthorizationRequest;
import org.springframework.security.oauth2.provider.ClientDetails;
import org.springframework.security.oauth2.provider.ClientDetailsService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.SessionAttribute;

import java.security.Principal;
import java.util.Set;

@Controller
public class ConsentController {

    private final ScopeService scopeService;
    private final CaptchaService captchaService;

    @Autowired
    private ClientDetailsService clientDetailsService;

    public ConsentController(ScopeService scopeService, CaptchaService captchaService) {
        this.scopeService = scopeService;
        this.captchaService = captchaService;
    }

    @GetMapping("/login")
    public String login(Model model) {
        model.addAttribute("captchaEnabled", captchaService.isEnabled());
        return "login";
    }

    @GetMapping({"/oauth/consent", "/oauth/confirm_access"})
    public String consent(Model model,
                          Principal principal,
                          @SessionAttribute(value = "authorizationRequest", required = false)
                          AuthorizationRequest authorizationRequest) {

        if (authorizationRequest == null) {
            model.addAttribute("error",
                    "No pending authorization request. Start from /oauth/authorize.");
            return "consent";
        }

        String clientId = authorizationRequest.getClientId();
        ClientDetails client = clientDetailsService.loadClientByClientId(clientId);
        Set<String> requested = authorizationRequest.getScope();
        if (requested == null || requested.isEmpty()) {
            requested = client.getScope();
        }

        Set<Scope> availableScopes = scopeService.resolveConsentScopes(principal.getName(), requested);

        model.addAttribute("clientId", clientId);
        model.addAttribute("username", principal.getName());
        model.addAttribute("scopes", availableScopes);
        model.addAttribute("authorizationRequest", authorizationRequest);
        return "consent";
    }
}
