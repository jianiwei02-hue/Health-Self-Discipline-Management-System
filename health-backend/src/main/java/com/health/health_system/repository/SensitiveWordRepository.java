package com.health.health_system.repository;

import com.health.health_system.entity.SensitiveWord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SensitiveWordRepository extends JpaRepository<SensitiveWord, Long> {
    List<SensitiveWord> findAll();
}