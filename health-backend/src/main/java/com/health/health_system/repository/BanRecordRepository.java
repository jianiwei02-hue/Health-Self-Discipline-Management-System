package com.health.health_system.repository;

import com.health.health_system.entity.BanRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface BanRecordRepository extends JpaRepository<BanRecord, Integer> {

    List<BanRecord> findByUserIdOrderByCreateTimeDesc(Integer userId);
}