-- password for both users: password (BCrypt)
INSERT INTO users (username, password, email, full_name, enabled)
VALUES
('admin', '{bcrypt}$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', 'admin@example.com', 'System Admin', TRUE),
('alice', '{bcrypt}$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', 'alice@example.com', 'Alice User', TRUE),
('bob',   '{bcrypt}$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', 'bob@example.com', 'Bob User', TRUE);

INSERT INTO scopes (name, display_name, description, active) VALUES
('read',   'Read Access',   'Read resources and profile information', TRUE),
('write',  'Write Access',  'Create and update resources', TRUE),
('delete', 'Delete Access', 'Delete resources', TRUE),
('profile','Profile',       'Access user profile details', TRUE),
('email',  'Email',         'Access user email address', TRUE);

-- admin: all scopes
INSERT INTO user_scopes (user_id, scope_id)
SELECT u.id, s.id FROM users u CROSS JOIN scopes s WHERE u.username = 'admin';

-- alice: read, write, profile, email
INSERT INTO user_scopes (user_id, scope_id)
SELECT u.id, s.id FROM users u, scopes s
WHERE u.username = 'alice' AND s.name IN ('read', 'write', 'profile', 'email');

-- bob: read, profile
INSERT INTO user_scopes (user_id, scope_id)
SELECT u.id, s.id FROM users u, scopes s
WHERE u.username = 'bob' AND s.name IN ('read', 'profile');

-- client_secret: secret (BCrypt with {bcrypt} prefix for DelegatingPasswordEncoder)
INSERT INTO oauth_client_details (
    client_id, client_secret, scope, authorized_grant_types,
    web_server_redirect_uri, authorities, access_token_validity,
    refresh_token_validity, autoapprove
) VALUES (
    'web-client',
    '{bcrypt}$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG',
    'read,write,delete,profile,email',
    'authorization_code,refresh_token,password,client_credentials',
    'http://localhost:8080/login/oauth2/code/web-client,http://localhost:8081/callback,http://127.0.0.1:8081/callback',
    'ROLE_CLIENT',
    3600,
    86400,
    'false'
);

INSERT INTO oauth_client_details (
    client_id, client_secret, scope, authorized_grant_types,
    web_server_redirect_uri, authorities, access_token_validity,
    refresh_token_validity, autoapprove
) VALUES (
    'api-client',
    '{bcrypt}$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG',
    'read,write,profile',
    'password,refresh_token,client_credentials',
    NULL,
    'ROLE_CLIENT',
    3600,
    86400,
    'false'
);
