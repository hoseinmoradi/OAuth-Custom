package com.master.oauth.service;

import com.master.oauth.entity.Scope;
import com.master.oauth.entity.User;
import com.master.oauth.repository.ScopeRepository;
import com.master.oauth.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ScopeService {

    private final ScopeRepository scopeRepository;
    private final UserRepository userRepository;

    public ScopeService(ScopeRepository scopeRepository, UserRepository userRepository) {
        this.scopeRepository = scopeRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<Scope> findAll() {
        return scopeRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Scope> findAllActive() {
        return scopeRepository.findByActiveTrue();
    }

    @Transactional(readOnly = true)
    public Scope findById(Long id) {
        return scopeRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Scope not found"));
    }

    @Transactional(readOnly = true)
    public List<Scope> findAllowedScopesForUser(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));
        return user.getScopes().stream()
                .filter(Scope::isActive)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Set<Scope> resolveConsentScopes(String username, Set<String> clientRequestedScopes) {
        Set<String> allowed = findAllowedScopesForUser(username).stream()
                .map(Scope::getName)
                .collect(Collectors.toSet());

        Set<Scope> result = new LinkedHashSet<Scope>();
        for (Scope scope : findAllActive()) {
            if (clientRequestedScopes.contains(scope.getName()) && allowed.contains(scope.getName())) {
                result.add(scope);
            }
        }
        return result;
    }

    @Transactional
    public Scope create(Map<String, Object> request) {
        String name = stringVal(request.get("name"));
        String displayName = stringVal(request.get("displayName"));
        if (name == null || name.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "name is required");
        }
        if (displayName == null || displayName.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "displayName is required");
        }
        if (scopeRepository.findByName(name.trim()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "scope name already exists");
        }
        Scope scope = new Scope();
        scope.setName(name.trim());
        scope.setDisplayName(displayName.trim());
        scope.setDescription(stringVal(request.get("description")));
        if (request.containsKey("active") && request.get("active") != null) {
            scope.setActive(Boolean.parseBoolean(String.valueOf(request.get("active"))));
        } else {
            scope.setActive(true);
        }
        return scopeRepository.save(scope);
    }

    @Transactional
    public Scope update(Long id, Map<String, Object> request) {
        Scope scope = findById(id);
        if (request.containsKey("name")) {
            String name = stringVal(request.get("name"));
            if (name == null || name.trim().isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "name cannot be empty");
            }
            scopeRepository.findByName(name.trim()).ifPresent(existing -> {
                if (!existing.getId().equals(id)) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "scope name already exists");
                }
            });
            scope.setName(name.trim());
        }
        if (request.containsKey("displayName")) {
            String displayName = stringVal(request.get("displayName"));
            if (displayName == null || displayName.trim().isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "displayName cannot be empty");
            }
            scope.setDisplayName(displayName.trim());
        }
        if (request.containsKey("description")) {
            scope.setDescription(stringVal(request.get("description")));
        }
        if (request.containsKey("active") && request.get("active") != null) {
            scope.setActive(Boolean.parseBoolean(String.valueOf(request.get("active"))));
        }
        return scopeRepository.save(scope);
    }

    @Transactional
    public void delete(Long id) {
        Scope scope = findById(id);
        List<User> users = userRepository.findAll();
        for (User user : users) {
            if (user.getScopes().remove(scope)) {
                userRepository.save(user);
            }
        }
        scopeRepository.delete(scope);
    }

    public Map<String, Object> toView(Scope scope) {
        Map<String, Object> map = new LinkedHashMap<String, Object>();
        map.put("id", scope.getId());
        map.put("name", scope.getName());
        map.put("displayName", scope.getDisplayName());
        map.put("description", scope.getDescription());
        map.put("active", scope.isActive());
        return map;
    }

    private String stringVal(Object value) {
        return value == null ? null : String.valueOf(value);
    }
}
