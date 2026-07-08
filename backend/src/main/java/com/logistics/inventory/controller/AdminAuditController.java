package com.logistics.inventory.controller;

import com.logistics.inventory.entity.AuditLog;
import com.logistics.inventory.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

/** Read-only audit trail; under /api/admin so the ADMIN role rule applies. */
@RestController
@RequestMapping("/api/admin/audit")
@RequiredArgsConstructor
public class AdminAuditController {

    public record AuditEntry(Long id, String actor, String action, String entityType,
                             Long entityId, String details, Instant createdAt) {
        static AuditEntry from(AuditLog log) {
            return new AuditEntry(log.getId(), log.getActor(), log.getAction(), log.getEntityType(),
                    log.getEntityId(), log.getDetails(), log.getCreatedAt());
        }
    }

    private final AuditLogRepository auditLogRepository;

    @GetMapping
    public Page<AuditEntry> list(@PageableDefault(size = 25, sort = "createdAt",
            direction = Sort.Direction.DESC) Pageable pageable) {
        return auditLogRepository.findAll(pageable).map(AuditEntry::from);
    }
}
