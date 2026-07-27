package com.health.health_system.repository;

import com.health.health_system.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Integer> {

    Page<Notification> findByUserIdOrderByCreateTimeDesc(Integer userId, Pageable pageable);

    int countByUserIdAndIsRead(Integer userId, Integer isRead);

    List<Notification> findByUserIdAndIsRead(Integer userId, Integer isRead);

    // 修改这个方法：使用 @Query 注解
    @Transactional
    @Modifying
    @Query("UPDATE Notification n SET n.isRead = :isRead WHERE n.userId = :userId")
    void updateIsReadByUserId(@Param("userId") Integer userId, @Param("isRead") Integer isRead);
}