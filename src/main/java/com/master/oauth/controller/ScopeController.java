package com.master.oauth.controller;

import com.master.oauth.entity.Scope;
import com.master.oauth.service.ScopeService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/scopes")
public class ScopeController {

    private final ScopeService scopeService;

    public ScopeController(ScopeService scopeService) {
        this.scopeService = scopeService;
    }

    @GetMapping
    public List<Map<String, Object>> list(
            @RequestParam(value = "activeOnly", defaultValue = "true") boolean activeOnly) {
        List<Scope> scopes = activeOnly ? scopeService.findAllActive() : scopeService.findAll();
        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        for (Scope scope : scopes) {
            result.add(scopeService.toView(scope));
        }
        return result;
    }

    @GetMapping("/{id}")
    public Map<String, Object> get(@PathVariable Long id) {
        return scopeService.toView(scopeService.findById(id));
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> create(@RequestBody Map<String, Object> request) {
        Scope created = scopeService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(scopeService.toView(created));
    }

    @PutMapping("/{id}")
    public Map<String, Object> update(@PathVariable Long id, @RequestBody Map<String, Object> request) {
        return scopeService.toView(scopeService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        scopeService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
