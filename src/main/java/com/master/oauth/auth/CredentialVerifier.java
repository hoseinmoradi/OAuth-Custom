package com.master.oauth.auth;

/**
 * Strategy for verifying username/password credentials.
 * Implementations: local DB or remote web service.
 */
public interface CredentialVerifier {

    /**
     * @return true when credentials are valid
     */
    boolean verify(String username, String password);
}
