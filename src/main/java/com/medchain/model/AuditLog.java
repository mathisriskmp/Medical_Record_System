package com.medchain.model;

import java.time.LocalDateTime;

/**
 * AuditLog — Domain Model
 *
 * Represents a single audit event in the MedChain system.
 * Every important action — login, record creation, access grant/revoke,
 * emergency access — is permanently logged for transparency and accountability.
 *
 * Maps to the 'audit_logs' table in medchain_db.
 *
 * @author  MedChain Team
 * @version 1.0
 */
public class AuditLog {

    // ── Action Constants ──────────────────────────────────────────────────────
    public static final String ACTION_LOGIN           = "LOGIN";
    public static final String ACTION_LOGOUT          = "LOGOUT";
    public static final String ACTION_ADD_RECORD      = "ADD_RECORD";
    public static final String ACTION_VIEW_RECORD     = "VIEW_RECORD";
    public static final String ACTION_VERIFY_RECORD   = "VERIFY_RECORD";
    public static final String ACTION_GRANT_ACCESS    = "GRANT_ACCESS";
    public static final String ACTION_REVOKE_ACCESS   = "REVOKE_ACCESS";
    public static final String ACTION_EMERGENCY_ACCESS = "EMERGENCY_ACCESS";
    public static final String ACTION_DELETE_USER     = "DELETE_USER";

    // ── Fields ────────────────────────────────────────────────────────────────
    private int           id;
    private int           userId;
    private String        userName;     // joined from users table
    private String        action;       // one of the ACTION_ constants above
    private String        details;      // human-readable description
    private LocalDateTime timestamp;

    // ── Constructors ──────────────────────────────────────────────────────────

    /** Default constructor */
    public AuditLog() {}

    /**
     * Full constructor
     */
    public AuditLog(int id, int userId, String userName,
                    String action, String details, LocalDateTime timestamp) {
        this.id        = id;
        this.userId    = userId;
        this.userName  = userName;
        this.action    = action;
        this.details   = details;
        this.timestamp = timestamp;
    }

    /**
     * Creation constructor — used when logging a new event.
     */
    public AuditLog(int userId, String action, String details) {
        this.userId  = userId;
        this.action  = action;
        this.details = details;
    }

    // ── Getters & Setters ─────────────────────────────────────────────────────
    public int getId()                          { return id; }
    public void setId(int id)                   { this.id = id; }

    public int getUserId()                      { return userId; }
    public void setUserId(int userId)           { this.userId = userId; }

    public String getUserName()                 { return userName; }
    public void setUserName(String userName)    { this.userName = userName; }

    public String getAction()                   { return action; }
    public void setAction(String action)        { this.action = action; }

    public String getDetails()                  { return details; }
    public void setDetails(String details)      { this.details = details; }

    public LocalDateTime getTimestamp()                 { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp)   { this.timestamp = timestamp; }

    // ── Utility ───────────────────────────────────────────────────────────────

    @Override
    public String toString() {
        return "AuditLog{user=" + userName
             + ", action=" + action
             + ", time="   + timestamp + "}";
    }
}
