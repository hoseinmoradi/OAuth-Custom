package com.master.oauth.auth;

import com.master.oauth.service.CustomUserDetailsService;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

/**
 * Pluggable authentication: verifies credentials via CredentialVerifier (local or remote),
 * then loads authorities/scopes from the local users table.
 */
@Component
public class PluggableAuthenticationProvider implements AuthenticationProvider {

    private final CredentialVerifier credentialVerifier;
    private final CustomUserDetailsService userDetailsService;

    public PluggableAuthenticationProvider(CredentialVerifier credentialVerifier,
                                           CustomUserDetailsService userDetailsService) {
        this.credentialVerifier = credentialVerifier;
        this.userDetailsService = userDetailsService;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String username = authentication.getName();
        String password = authentication.getCredentials() == null
                ? ""
                : authentication.getCredentials().toString();

        if (!credentialVerifier.verify(username, password)) {
            throw new BadCredentialsException("Invalid username or password");
        }

        UserDetails user = userDetailsService.loadUserByUsername(username);
        if (!user.isEnabled()) {
            throw new DisabledException("User is disabled");
        }

        return new UsernamePasswordAuthenticationToken(user, password, user.getAuthorities());
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }
}
