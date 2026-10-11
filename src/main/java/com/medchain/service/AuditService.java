package com.medchain.service;

import com.medchain.dao.AuditLogDAO;
import com.medchain.model.AuditLog;
import com.medchain.util.SessionManager;

import java.util.List;
//import java.util.logging.Logger;

/**
 * AuditService — Audit Log Business Logic
 *
 * Provides Admin-level access to the complete audit trail.
 * Every action in MedChain is permanently logged and
 * viewable through this service.
 *
 * @author  MedChain Team
 * @version 1.0
 */
public class AuditService {

    // ── Logger ────────────────────────────────────────────────────────────────
    

    // ── Dependencies ──────────────────────────────────────────────────────────
    private final AuditLogDAO auditLogDAO = new AuditLogDAO();

    /**
     * Returns all audit logs — for Admin dashboard.
     *
     * @return  list of all AuditLog objects newest first
     * @throws  SecurityException if caller is not Admin
     */
    public List<AuditLog> getAllLogs() {
        if (!SessionManager.getInstance().isAdmin()) {
            throw new SecurityException(
                "Access denied — Admin privileges required.");
        }
        return auditLogDAO.findAll();
    }

    /**
     * Returns audit logs for a specific user.
     *
     * @param  userId  user ID to filter by
     * @return         list of AuditLog objects for that user
     */
    public List<AuditLog> getLogsByUser(int userId) {
        return auditLogDAO.findByUserId(userId);
    }

    /**
     * Manually logs an action.
     * Used when services need to record custom events.
     *
     * @param  userId   user who performed the action
     * @param  action   action constant from AuditLog
     * @param  details  human readable description
     */
    public void log(int userId, String action, String details) {
        auditLogDAO.log(userId, action, details);
    }
}
