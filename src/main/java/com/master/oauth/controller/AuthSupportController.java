package com.master.oauth.controller;

import com.master.oauth.auth.CredentialVerifier;
import com.master.oauth.captcha.CaptchaService;
import com.master.oauth.service.CustomUserDetailsService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Auth-related HTTP endpoints: captcha image + optional API login probe.
 */
@RestController
public class AuthSupportController {

    private final CaptchaService captchaService;
    private final CredentialVerifier credentialVerifier;
    private final CustomUserDetailsService userDetailsService;

    @Value("${auth.login.mode:local}")
    private String loginMode;

    public AuthSupportController(CaptchaService captchaService,
                                 CredentialVerifier credentialVerifier,
                                 CustomUserDetailsService userDetailsService) {
        this.captchaService = captchaService;
        this.credentialVerifier = credentialVerifier;
        this.userDetailsService = userDetailsService;
    }

    @GetMapping(value = "/captcha", produces = MediaType.IMAGE_PNG_VALUE)
    public byte[] captchaImage(HttpSession session) throws Exception {
        String code = captchaService.createAndStore(session);
        return captchaService.renderImage(code);
    }

    @GetMapping("/api/public/auth-mode")
    public Map<String, Object> authMode() {
        Map<String, Object> body = new LinkedHashMap<String, Object>();
        body.put("mode", loginMode);
        body.put("captchaEnabled", captchaService.isEnabled());
        return body;
    }

    /**
     * Machine-friendly credential check (does not create a browser session).
     * Captcha is optional here so external services can integrate easily;
     * browser form login always requires captcha when enabled.
     */
    @PostMapping("/api/auth/verify")
    public ResponseEntity<?> verify(@RequestBody Map<String, String> request) {
        String username = request.get("username");
        String password = request.get("password");
        boolean ok = credentialVerifier.verify(username, password);

        Map<String, Object> body = new LinkedHashMap<String, Object>();
        body.put("authenticated", ok);
        body.put("mode", loginMode);
        if (ok) {
            try {
                UserDetails user = userDetailsService.loadUserByUsername(username);
                body.put("username", user.getUsername());
                body.put("enabled", user.isEnabled());
            } catch (Exception ignored) {
                // remote auth may succeed before local profile exists
            }
        }
        return ResponseEntity.ok(body);
    }
}
