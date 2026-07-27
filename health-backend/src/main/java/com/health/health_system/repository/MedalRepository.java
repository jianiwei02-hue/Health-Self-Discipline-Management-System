package com.health.health_system.repository;

import com.health.health_system.entity.Medal;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface MedalRepository extends JpaRepository<Medal, Integer> {

    List<Medal> findByUserIdOrderByGetTimeDesc(Integer userId);

    boolean existsByUserIdAndMedalName(Integer userId, String medalName);
}