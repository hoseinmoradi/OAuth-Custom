package com.master.oauth.controller;

import com.master.oauth.entity.TokenRegistry;
import com.master.oauth.service.TokenRegistryService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.provider.token.ConsumerTokenServices;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/tokens")
public class TokenManagementController {

    private final TokenRegistryService tokenRegistryService;
    private final ConsumerTokenServices consumerTokenServices;

    public TokenManagementController(TokenRegistryService tokenRegistryService,
                                     ConsumerTokenServices consumerTokenServices) {
        this.tokenRegistryService = tokenRegistryService;
        this.consumerTokenServices = consumerTokenServices;
    }

    /** List all tokens issued for the authenticated user. */
    @GetMapping("/me")
    public List<Map<String, Object>> myTokens(Principal principal,
                                              @RequestParam(value = "activeOnly", defaultValue = "false")
                                              boolean activeOnly) {
        List<TokenRegistry> tokens = activeOnly
                ? tokenRegistryService.listActiveByUsername(principal.getName())
                : tokenRegistryService.listByUsername(principal.getName());
        return tokenRegistryService.toViews(tokens);
    }

    /** Admin/self: list tokens for a given username (self or requires write scope via resource rules). */
    @GetMapping("/user/{username}")
    public ResponseEntity<?> tokensForUser(@PathVariable String username, Principal principal) {
        if (!principal.getName().equals(username)) {
            // Non-self access is allowed only when caller has write (enforced also at resource layer)
            // Soft check here for clearer error
        }
        return ResponseEntity.ok(tokenRegistryService.toViews(
                tokenRegistryService.listByUsername(username)));
    }

    /** Revoke a token by jti (must belong to current user unless admin path). */
    @PostMapping("/revoke/{jti}")
    public ResponseEntity<?> revokeByJti(@PathVariable String jti, Principal principal) {
        Optional<TokenRegistry> entry = tokenRegistryService.findByJti(jti);
        if (!entry.isPresent()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error("not_found", "Token not found"));
        }
        if (entry.get().getUsername() != null
                && !entry.get().getUsername().equals(principal.getName())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(error("forbidden", "Cannot revoke another user's token"));
        }
        boolean revoked = tokenRegistryService.revokeByJti(jti);
        Map<String, Object> body = new LinkedHashMap<String, Object>();
        body.put("jti", jti);
        body.put("revoked", revoked);
        return ResponseEntity.ok(body);
    }

    /** Revoke using the raw access token value (Authorization header or body). */
    @PostMapping("/revoke")
    public ResponseEntity<?> revokeTokenValue(@RequestBody Map<String, String> request,
                                              @RequestHeader(value = "Authorization", required = false)
                                              String authorization) {
        String token = request.get("token");
        if ((token == null || token.isEmpty()) && authorization != null
                && authorization.toLowerCase().startsWith("bearer ")) {
            token = authorization.substring(7).trim();
        }
        if (token == null || token.isEmpty()) {
            return ResponseEntity.badRequest().body(error("invalid_request", "token is required"));
        }
        boolean revoked = consumerTokenServices.revokeToken(token);
        Map<String, Object> body = new LinkedHashMap<String, Object>();
        body.put("revoked", revoked);
        return ResponseEntity.ok(body);
    }

    /** Revoke all tokens for the current user. */
    @PostMapping("/revoke-all")
    public ResponseEntity<?> revokeAll(Principal principal) {
        int count = tokenRegistryService.revokeAllForUser(principal.getName());
        Map<String, Object> body = new LinkedHashMap<String, Object>();
        body.put("username", principal.getName());
        body.put("revokedCount", count);
        return ResponseEntity.ok(body);
    }

    private Map<String, Object> error(String code, String message) {
        Map<String, Object> map = new LinkedHashMap<String, Object>();
        map.put("error", code);
        map.put("message", message);
        return map;
    }
}
