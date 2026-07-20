package com.master.oauth.repository;

import com.master.oauth.entity.Scope;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ScopeRepository extends JpaRepository<Scope, Long> {

    Optional<Scope> findByName(String name);

    List<Scope> findByActiveTrue();
}
