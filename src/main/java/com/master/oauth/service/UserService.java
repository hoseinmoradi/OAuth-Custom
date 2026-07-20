package com.master.oauth.service;

import com.master.oauth.entity.Scope;
import com.master.oauth.entity.User;
import com.master.oauth.repository.ScopeRepository;
import com.master.oauth.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final ScopeRepository scopeRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository,
                       ScopeRepository scopeRepository,
                       PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.scopeRepository = scopeRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public List<User> findAll() {
        return userRepository.findAll();
    }

    @Transactional(readOnly = true)
    public User findById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    @Transactional(readOnly = true)
    public User findByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    @Transactional
    public User create(Map<String, Object> request) {
        String username = stringVal(request.get("username"));
        String password = stringVal(request.get("password"));
        if (username == null || username.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "username is required");
        }
        if (password == null || password.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "password is required");
        }
        if (userRepository.existsByUsername(username)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "username already exists");
        }

        User user = new User();
        user.setUsername(username.trim());
        user.setPassword(passwordEncoder.encode(password));
        applyProfile(user, request);
        user.setScopes(resolveScopes(request.get("scopes")));
        return userRepository.save(user);
    }

    @Transactional
    public User update(Long id, Map<String, Object> request) {
        User user = findById(id);
        if (request.containsKey("username")) {
            String username = stringVal(request.get("username"));
            if (username == null || username.trim().isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "username cannot be empty");
            }
            if (!user.getUsername().equals(username) && userRepository.existsByUsername(username)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "username already exists");
            }
            user.setUsername(username.trim());
        }
        if (request.containsKey("password") && request.get("password") != null
                && !stringVal(request.get("password")).isEmpty()) {
            user.setPassword(passwordEncoder.encode(stringVal(request.get("password"))));
        }
        applyProfile(user, request);
        if (request.containsKey("scopes")) {
            user.setScopes(resolveScopes(request.get("scopes")));
        }
        return userRepository.save(user);
    }

    @Transactional
    public void delete(Long id) {
        if (!userRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
        }
        userRepository.deleteById(id);
    }

    @Transactional
    public User replaceScopes(Long id, List<String> scopeNames) {
        User user = findById(id);
        user.setScopes(resolveScopes(scopeNames));
        return userRepository.save(user);
    }

    public Map<String, Object> toView(User user) {
        Map<String, Object> map = new LinkedHashMap<String, Object>();
        map.put("id", user.getId());
        map.put("username", user.getUsername());
        map.put("email", user.getEmail());
        map.put("fullName", user.getFullName());
        map.put("enabled", user.isEnabled());
        map.put("accountNonExpired", user.isAccountNonExpired());
        map.put("accountNonLocked", user.isAccountNonLocked());
        map.put("credentialsNonExpired", user.isCredentialsNonExpired());
        map.put("createdAt", user.getCreatedAt());
        map.put("scopes", user.getScopes().stream()
                .map(Scope::getName)
                .sorted()
                .collect(Collectors.toList()));
        return map;
    }

    private void applyProfile(User user, Map<String, Object> request) {
        if (request.containsKey("email")) {
            user.setEmail(stringVal(request.get("email")));
        }
        if (request.containsKey("fullName")) {
            user.setFullName(stringVal(request.get("fullName")));
        }
        if (request.containsKey("enabled") && request.get("enabled") != null) {
            user.setEnabled(Boolean.parseBoolean(String.valueOf(request.get("enabled"))));
        }
        if (request.containsKey("accountNonLocked") && request.get("accountNonLocked") != null) {
            user.setAccountNonLocked(Boolean.parseBoolean(String.valueOf(request.get("accountNonLocked"))));
        }
        if (request.containsKey("accountNonExpired") && request.get("accountNonExpired") != null) {
            user.setAccountNonExpired(Boolean.parseBoolean(String.valueOf(request.get("accountNonExpired"))));
        }
        if (request.containsKey("credentialsNonExpired") && request.get("credentialsNonExpired") != null) {
            user.setCredentialsNonExpired(
                    Boolean.parseBoolean(String.valueOf(request.get("credentialsNonExpired"))));
        }
    }

    @SuppressWarnings("unchecked")
    private Set<Scope> resolveScopes(Object scopesObj) {
        Set<Scope> result = new HashSet<Scope>();
        if (scopesObj == null) {
            return result;
        }
        List<String> names;
        if (scopesObj instanceof List) {
            names = new ArrayList<String>();
            for (Object item : (List<?>) scopesObj) {
                if (item != null) {
                    names.add(String.valueOf(item));
                }
            }
        } else {
            names = Arrays.asList(String.valueOf(scopesObj).split("[,\\s]+"));
        }
        for (String name : names) {
            if (name == null || name.trim().isEmpty()) {
                continue;
            }
            Scope scope = scopeRepository.findByName(name.trim())
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.BAD_REQUEST, "Unknown scope: " + name));
            result.add(scope);
        }
        return result;
    }

    private String stringVal(Object value) {
        return value == null ? null : String.valueOf(value);
    }
}
