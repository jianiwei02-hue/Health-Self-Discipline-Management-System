package com.health.health_system.repository;

import com.health.health_system.entity.Report;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ReportRepository extends JpaRepository<Report, Long> {

    // 获取所有举报（按状态升序、时间倒序）
    Page<Report> findAllByOrderByStatusAscCreateTimeDesc(Pageable pageable);

    // 按状态筛选举报
    Page<Report> findByStatus(Integer status, Pageable pageable);

    // 统计某个目标的待处理举报数量
    @Query("SELECT COUNT(r) FROM Report r WHERE r.targetType = :targetType AND r.targetId = :targetId AND r.status = 0")
    int countPendingByTarget(@Param("targetType") String targetType, @Param("targetId") Long targetId);

    // 检查用户是否已举报过该目标（待处理状态）
    @Query("SELECT COUNT(r) > 0 FROM Report r WHERE r.reporterId = :reporterId AND r.targetType = :targetType AND r.targetId = :targetId AND r.status = 0")
    boolean existsPendingByReporterAndTarget(@Param("reporterId") Integer reporterId,
                                             @Param("targetType") String targetType,
                                             @Param("targetId") Long targetId);
}