package com.health.health_system.service;

import com.health.health_system.entity.Announcement;
import com.health.health_system.repository.AnnouncementRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AnnouncementService {

    @Autowired
    private AnnouncementRepository announcementRepository;

    /**
     * 获取启用的公告（前端社区页面展示）
     */
    public List<Announcement> getActiveAnnouncements() {
        return announcementRepository.findByIsActiveOrderBySortOrderAsc(1);
    }

    /**
     * 获取所有公告（管理员）
     */
    public Page<Announcement> getAllAnnouncements(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return announcementRepository.findAllByOrderBySortOrderAscCreateTimeDesc(pageable);
    }

    /**
     * 创建公告
     */
    @Transactional
    public Announcement createAnnouncement(Announcement announcement) {
        announcement.setCreateTime(LocalDateTime.now());
        announcement.setUpdateTime(LocalDateTime.now());
        return announcementRepository.save(announcement);
    }

    /**
     * 更新公告
     */
    @Transactional
    public Announcement updateAnnouncement(Long id, Announcement announcement) {
        Announcement existing = announcementRepository.findById(id).orElse(null);
        if (existing == null) {
            throw new RuntimeException("公告不存在");
        }
        existing.setTitle(announcement.getTitle());
        existing.setContent(announcement.getContent());
        existing.setType(announcement.getType());
        existing.setSortOrder(announcement.getSortOrder());
        existing.setUpdateTime(LocalDateTime.now());
        return announcementRepository.save(existing);
    }

    /**
     * 删除公告
     */
    @Transactional
    public void deleteAnnouncement(Long id) {
        if (!announcementRepository.existsById(id)) {
            throw new RuntimeException("公告不存在");
        }
        announcementRepository.deleteById(id);
    }

    /**
     * 启用/禁用公告
     */
    @Transactional
    public void toggleAnnouncement(Long id) {
        Announcement announcement = announcementRepository.findById(id).orElse(null);
        if (announcement == null) {
            throw new RuntimeException("公告不存在");
        }
        announcement.setIsActive(announcement.getIsActive() == 1 ? 0 : 1);
        announcement.setUpdateTime(LocalDateTime.now());
        announcementRepository.save(announcement);
    }
}