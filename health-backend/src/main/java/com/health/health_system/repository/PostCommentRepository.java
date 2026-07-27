package com.health.health_system.repository;

import com.health.health_system.entity.PostComment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

public interface PostCommentRepository extends JpaRepository<PostComment, Integer> {

    // 获取帖子的顶级评论列表（parent_id IS NULL 或 parent_id = 0），按时间倒序
    @Query("SELECT c FROM PostComment c WHERE c.postId = :postId " +
            "AND (c.parentId IS NULL OR c.parentId = 0) " +
            "ORDER BY c.createTime DESC")
    List<PostComment> findTopCommentsByPostId(@Param("postId") Integer postId);

    // 获取帖子的顶级评论，按置顶优先排序
    @Query("SELECT c FROM PostComment c WHERE c.postId = :postId " +
            "AND (c.parentId IS NULL OR c.parentId = 0) " +
            "ORDER BY c.isPinned DESC, c.createTime DESC")
    List<PostComment> findTopCommentsWithPinnedFirst(@Param("postId") Integer postId);

    // 获取帖子的所有回复（parent_id > 0），按时间正序
    @Query("SELECT c FROM PostComment c WHERE c.postId = :postId " +
            "AND c.parentId IS NOT NULL AND c.parentId > 0 " +
            "ORDER BY c.createTime ASC")
    List<PostComment> findRepliesByPostId(@Param("postId") Integer postId);

    // 获取某个父评论的所有子回复
    List<PostComment> findByParentIdOrderByCreateTimeAsc(Integer parentId);

    // 查询置顶评论
    List<PostComment> findByPostIdAndIsPinned(Integer postId, Integer isPinned);

    // ========== 原有的方法保留 ==========

    List<PostComment> findByPostIdOrderByCreateTimeAsc(Integer postId);

    @Transactional
    @Modifying
    void deleteByPostId(Integer postId);

    int countByPostId(Integer postId);

    // ========== 管理员专用方法 ==========

    /**
     * 获取所有评论（分页，按创建时间倒序）
     */
    @Query("SELECT c FROM PostComment c ORDER BY c.createTime DESC")
    Page<PostComment> findAllForAdmin(Pageable pageable);

    /**
     * 按关键字搜索评论（分页）
     */
    @Query("SELECT c FROM PostComment c WHERE c.content LIKE %:keyword% ORDER BY c.createTime DESC")
    Page<PostComment> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

    /**
     * 获取帖子关联的评论数量
     */
    int countByPostIdIn(List<Integer> postIds);

    /**
     * 批量删除评论
     */
    @Transactional
    @Modifying
    void deleteByIdIn(List<Integer> ids);

    /**
     * 获取父评论的所有子回复ID
     */
    @Query("SELECT c.id FROM PostComment c WHERE c.parentId = :parentId")
    List<Integer> findReplyIdsByParentId(@Param("parentId") Integer parentId);
}