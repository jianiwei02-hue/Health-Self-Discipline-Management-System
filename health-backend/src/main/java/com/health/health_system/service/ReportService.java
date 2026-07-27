package com.health.health_system.service;

import com.health.health_system.entity.Report;
import com.health.health_system.repository.ReportRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class ReportService {

    @Autowired
    private ReportRepository reportRepository;

    /**
     * 提交举报
     */
    @Transactional
    public Report submitReport(Integer reporterId, String targetType, Long targetId, String reason) {
        // 检查是否已举报过（待处理状态）
        boolean alreadyReported = reportRepository.existsPendingByReporterAndTarget(reporterId, targetType, targetId);
        if (alreadyReported) {
            throw new RuntimeException("您已举报过该内容，请等待管理员处理");
        }

        Report report = new Report();
        report.setReporterId(reporterId);
        report.setTargetType(targetType);
        report.setTargetId(targetId);
        report.setReason(reason);
        report.setStatus(0);
        report.setCreateTime(LocalDateTime.now());

        return reportRepository.save(report);
    }

    /**
     * 获取举报列表（管理员）
     */
    public Page<Report> getReports(Integer status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        if (status != null && status >= 0) {
            return reportRepository.findByStatus(status, pageable);
        }
        return reportRepository.findAllByOrderByStatusAscCreateTimeDesc(pageable);
    }

    /**
     * 处理举报
     * @param reportId 举报ID
     * @param handlerId 处理人ID
     * @param action 1:通过（删除内容）, 2:驳回
     * @param handleNote 处理备注
     */
    @Transactional
    public void handleReport(Long reportId, Integer handlerId, Integer action, String handleNote) {
        Report report = reportRepository.findById(reportId).orElse(null);
        if (report == null) {
            throw new RuntimeException("举报不存在");
        }

        if (action == 1) {
            report.setStatus(1);  // 已处理
        } else if (action == 2) {
            report.setStatus(2);  // 已驳回
        } else {
            throw new RuntimeException("无效的操作");
        }

        report.setHandlerId(handlerId);
        report.setHandleTime(LocalDateTime.now());
        report.setHandleNote(handleNote);
        reportRepository.save(report);
    }
}