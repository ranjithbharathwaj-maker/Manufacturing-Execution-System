package com.mes.factory.service;

import com.mes.factory.model.AuditLog;
import com.mes.factory.repository.AuditLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
public class AuditLogService {

    @Autowired
    private AuditLogRepository auditLogRepository;

    public void log(String username, String action, String details) {
        AuditLog log = new AuditLog();
        log.setUsername(username != null ? username : "SYSTEM");
        log.setAction(action);
        log.setTimestamp(new Date());
        log.setDetails(details);
        auditLogRepository.save(log);
        System.out.println("[AUDIT LOG] " + username + " performed " + action + ": " + details);
    }

    public List<AuditLog> getRecentLogs() {
        return auditLogRepository.findFirst50ByOrderByTimestampDesc();
    }
}
