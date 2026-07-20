package com.master.oauth.controller;

import com.master.oauth.entity.User;
import com.master.oauth.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public List<Map<String, Object>> list() {
        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        for (User user : userService.findAll()) {
            result.add(userService.toView(user));
        }
        return result;
    }

    @GetMapping("/{id}")
    public Map<String, Object> get(@PathVariable Long id) {
        return userService.toView(userService.findById(id));
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> create(@RequestBody Map<String, Object> request) {
        User created = userService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.toView(created));
    }

    @PutMapping("/{id}")
    public Map<String, Object> update(@PathVariable Long id, @RequestBody Map<String, Object> request) {
        return userService.toView(userService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        userService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/scopes")
    public Map<String, Object> replaceScopes(@PathVariable Long id,
                                             @RequestBody Map<String, Object> request) {
        @SuppressWarnings("unchecked")
        List<String> scopes = (List<String>) request.get("scopes");
        if (scopes == null) {
            scopes = new ArrayList<String>();
        }
        return userService.toView(userService.replaceScopes(id, scopes));
    }
}
