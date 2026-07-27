package com.health.health_system.repository;

import com.health.health_system.entity.UserMedal;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface UserMedalRepository extends JpaRepository<UserMedal, Integer> {

    List<UserMedal> findByUserIdOrderByGetTimeDesc(Integer userId);

    boolean existsByUserIdAndMedalId(Integer userId, Integer medalId);
}