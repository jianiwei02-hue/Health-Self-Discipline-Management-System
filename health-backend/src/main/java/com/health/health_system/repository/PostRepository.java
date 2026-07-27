package com.health.health_system.repository;

import com.health.health_system.entity.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

public interface PostRepository extends JpaRepository<Post, Integer> {

    List<Post> findByUserIdOrderByCreateTimeDesc(Integer userId);

    @Query("SELECT p FROM Post p ORDER BY p.createTime DESC")
    List<Post> findAllOrderByCreateTimeDesc(Pageable pageable);

    @Query("SELECT p FROM Post p ORDER BY p.likeCount DESC, p.createTime DESC")
    List<Post> findHotPosts(Pageable pageable);

    // 统计用户发帖数量
    int countByUserId(Integer userId);

    // ========== 管理员专用方法 ==========

    /**
     * 获取所有帖子（分页，按创建时间倒序）
     */
    @Query("SELECT p FROM Post p ORDER BY p.isPinned DESC, p.createTime DESC")
    Page<Post> findAllForAdmin(Pageable pageable);

    /**
     * 按关键字搜索帖子（分页）
     */
    @Query("SELECT p FROM Post p WHERE p.content LIKE %:keyword% ORDER BY p.createTime DESC")
    Page<Post> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

    /**
     * 置顶/取消置顶
     */
    @Modifying
    @Transactional
    @Query("UPDATE Post p SET p.isPinned = :isPinned WHERE p.id = :postId")
    void updatePinStatus(@Param("postId") Integer postId, @Param("isPinned") Integer isPinned);

    /**
     * 加精/取消加精
     */
    @Modifying
    @Transactional
    @Query("UPDATE Post p SET p.isFeatured = :isFeatured WHERE p.id = :postId")
    void updateFeaturedStatus(@Param("postId") Integer postId, @Param("isFeatured") Integer isFeatured);

    // ========== 新增：根据内容关键词搜索（返回列表，用于排序后分页） ==========

    /**
     * 根据内容关键词搜索帖子（返回列表，用于排序后手动分页）
     */
    @Query("SELECT p FROM Post p WHERE p.content LIKE %:keyword% ORDER BY p.createTime DESC")
    List<Post> findByContentContaining(@Param("keyword") String keyword);
}