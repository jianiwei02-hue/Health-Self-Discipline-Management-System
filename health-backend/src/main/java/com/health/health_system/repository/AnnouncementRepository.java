package com.health.health_system.repository;

import com.health.health_system.entity.Announcement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AnnouncementRepository extends JpaRepository<Announcement, Long> {

    // 获取所有公告（按排序升序、创建时间倒序）
    Page<Announcement> findAllByOrderBySortOrderAscCreateTimeDesc(Pageable pageable);

    // 获取启用的公告（按排序升序）
    List<Announcement> findByIsActiveOrderBySortOrderAsc(Integer isActive);
}