package com.master.oauth.auth;

import com.master.oauth.service.CustomUserDetailsService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "auth.login.mode", havingValue = "local", matchIfMissing = true)
public class LocalCredentialVerifier implements CredentialVerifier {

    private final CustomUserDetailsService userDetailsService;
    private final PasswordEncoder passwordEncoder;

    public LocalCredentialVerifier(CustomUserDetailsService userDetailsService,
                                   PasswordEncoder passwordEncoder) {
        this.userDetailsService = userDetailsService;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public boolean verify(String username, String password) {
        try {
            org.springframework.security.core.userdetails.UserDetails user =
                    userDetailsService.loadUserByUsername(username);
            return user.isEnabled()
                    && user.isAccountNonLocked()
                    && user.isAccountNonExpired()
                    && user.isCredentialsNonExpired()
                    && passwordEncoder.matches(password, user.getPassword());
        } catch (Exception ex) {
            return false;
        }
    }
}
