-- Custom users table
CREATE TABLE IF NOT EXISTS users (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    username        VARCHAR(100) NOT NULL UNIQUE,
    password        VARCHAR(255) NOT NULL,
    email           VARCHAR(150),
    full_name       VARCHAR(200),
    enabled         BOOLEAN NOT NULL DEFAULT TRUE,
    account_non_expired BOOLEAN NOT NULL DEFAULT TRUE,
    account_non_locked  BOOLEAN NOT NULL DEFAULT TRUE,
    credentials_non_expired BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Scopes defined in database
CREATE TABLE IF NOT EXISTS scopes (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    name            VARCHAR(100) NOT NULL UNIQUE,
    display_name    VARCHAR(200) NOT NULL,
    description     VARCHAR(500),
    active          BOOLEAN NOT NULL DEFAULT TRUE
);

-- Which scopes each user is allowed to grant
CREATE TABLE IF NOT EXISTS user_scopes (
    user_id         BIGINT NOT NULL,
    scope_id        BIGINT NOT NULL,
    PRIMARY KEY (user_id, scope_id),
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (scope_id) REFERENCES scopes(id)
);

-- Spring Security OAuth2 client details
CREATE TABLE IF NOT EXISTS oauth_client_details (
    client_id               VARCHAR(255) PRIMARY KEY,
    resource_ids            VARCHAR(255),
    client_secret           VARCHAR(255),
    scope                   VARCHAR(255),
    authorized_grant_types  VARCHAR(255),
    web_server_redirect_uri VARCHAR(1024),
    authorities             VARCHAR(255),
    access_token_validity   INTEGER,
    refresh_token_validity  INTEGER,
    additional_information  VARCHAR(4096),
    autoapprove             VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS oauth_access_token (
    token_id          VARCHAR(255),
    token             BLOB,
    authentication_id VARCHAR(255) PRIMARY KEY,
    user_name         VARCHAR(255),
    client_id         VARCHAR(255),
    authentication    BLOB,
    refresh_token     VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS oauth_refresh_token (
    token_id        VARCHAR(255),
    token           BLOB,
    authentication  BLOB
);

CREATE TABLE IF NOT EXISTS oauth_code (
    code            VARCHAR(255),
    authentication  BLOB
);

CREATE TABLE IF NOT EXISTS oauth_approvals (
    userId         VARCHAR(255),
    clientId       VARCHAR(255),
    scope          VARCHAR(255),
    status         VARCHAR(10),
    expiresAt      TIMESTAMP,
    lastModifiedAt TIMESTAMP
);

-- Issued JWT registry (list + revoke support for JWT tokens)
CREATE TABLE IF NOT EXISTS oauth_token_registry (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    jti             VARCHAR(100) NOT NULL UNIQUE,
    token_type      VARCHAR(20) NOT NULL,
    username        VARCHAR(100),
    client_id       VARCHAR(255) NOT NULL,
    scopes          VARCHAR(500),
    issued_at       TIMESTAMP NOT NULL,
    expires_at      TIMESTAMP,
    revoked         BOOLEAN NOT NULL DEFAULT FALSE,
    revoked_at      TIMESTAMP,
    refresh_jti     VARCHAR(100)
);

CREATE INDEX idx_token_registry_username ON oauth_token_registry(username);
CREATE INDEX idx_token_registry_client ON oauth_token_registry(client_id);
