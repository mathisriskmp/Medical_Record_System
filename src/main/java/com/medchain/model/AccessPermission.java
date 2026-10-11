package com.medchain.model;

import java.time.LocalDateTime;

/**
 * AccessPermission — Domain Model
 *
 * Represents a permission granted by a patient to a doctor
 * to access their medical records. Patients have full control
 * over who can view their data — they can grant or revoke
 * access at any time.
 *
 * Maps to the 'access_permissions' table in medchain_db.
 *
 * @author  MedChain Team
 * @version 1.0
 */
public class AccessPermission {

    // ── Fields ────────────────────────────────────────────────────────────────
    private int           id;
    private int           patientId;
    private int           doctorId;
    private String        patientName;   // joined from users table
    private String        doctorName;    // joined from users table
    private boolean       isActive;
    private LocalDateTime grantedAt;

    // ── Constructors ──────────────────────────────────────────────────────────

    /** Default constructor */
    public AccessPermission() {}

    /**
     * Full constructor
     */
    public AccessPermission(int id, int patientId, int doctorId,
                            boolean isActive, LocalDateTime grantedAt) {
        this.id        = id;
        this.patientId = patientId;
        this.doctorId  = doctorId;
        this.isActive  = isActive;
        this.grantedAt = grantedAt;
    }

    // ── Getters & Setters ─────────────────────────────────────────────────────
    public int getId()                          { return id; }
    public void setId(int id)                   { this.id = id; }

    public int getPatientId()                   { return patientId; }
    public void setPatientId(int patientId)     { this.patientId = patientId; }

    public int getDoctorId()                    { return doctorId; }
    public void setDoctorId(int doctorId)       { this.doctorId = doctorId; }

    public String getPatientName()                      { return patientName; }
    public void setPatientName(String patientName)      { this.patientName = patientName; }

    public String getDoctorName()                       { return doctorName; }
    public void setDoctorName(String doctorName)        { this.doctorName = doctorName; }

    public boolean isActive()                           { return isActive; }
    public void setActive(boolean active)               { isActive = active; }

    public LocalDateTime getGrantedAt()                 { return grantedAt; }
    public void setGrantedAt(LocalDateTime grantedAt)   { this.grantedAt = grantedAt; }

    // ── Utility ───────────────────────────────────────────────────────────────

    /**
     * Returns the access status as a display string.
     */
    public String getStatusDisplay() {
        return isActive ? "Active" : "Revoked";
    }

    @Override
    public String toString() {
        return "AccessPermission{patient=" + patientName
             + ", doctor=" + doctorName
             + ", status=" + getStatusDisplay() + "}";
    }
}
