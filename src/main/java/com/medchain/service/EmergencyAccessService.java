package com.medchain.service;

import com.medchain.dao.AuditLogDAO;
import com.medchain.dao.RecordDAO;
import com.medchain.dao.UserDAO;
import com.medchain.model.AuditLog;
import com.medchain.model.MedicalRecord;
import com.medchain.model.User;
import com.medchain.util.DBConnection;
import com.medchain.util.SessionManager;

import java.sql.*;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * EmergencyAccessService — Emergency Access Business Logic
 *
 * Handles critical situations where a doctor needs immediate
 * access to a patient's records without normal permission.
 *
 * This is a BYPASS mechanism — it skips the normal
 * access_permissions check BUT:
 * → Requires a valid reason to be provided
 * → Permanently logs the emergency access
 * → Admin can audit all emergency accesses
 * → Doctor must be authenticated
 *
 * Use case: Patient is unconscious and doctor needs
 * medication history to avoid dangerous drug interactions.
 *
 * @author  MedChain Team
 * @version 1.0
 */
public class EmergencyAccessService {

    // ── Logger ────────────────────────────────────────────────────────────────
    private static final Logger LOGGER =
        Logger.getLogger(EmergencyAccessService.class.getName());

    // ── Dependencies ──────────────────────────────────────────────────────────
    private final RecordDAO   recordDAO   = new RecordDAO();
    private final UserDAO     userDAO     = new UserDAO();
    private final AuditLogDAO auditLogDAO = new AuditLogDAO();

    // ── Emergency Access ──────────────────────────────────────────────────────

    /**
     * Grants emergency access to a patient's records.
     *
     * Bypasses normal permission check but requires:
     * 1. Caller must be a DOCTOR
     * 2. Reason must be provided (min 10 characters)
     * 3. Patient must exist in system
     *
     * @param  patientId  ID of the patient
     * @param  reason     reason for emergency access
     * @return            list of patient's medical records
     * @throws SecurityException        if caller is not a doctor
     * @throws IllegalArgumentException if reason is empty or patient not found
     */
    public List<MedicalRecord> accessEmergency(int patientId, String reason) {
        // ── Verify caller is a doctor ─────────────────────────────────────────
        User doctor = SessionManager.getInstance().getCurrentUser();
        if (doctor == null || doctor.getRole() != User.Role.DOCTOR) {
            throw new SecurityException(
                "Only doctors can use emergency access.");
        }

        // ── Validate reason ───────────────────────────────────────────────────
        if (reason == null || reason.trim().length() < 10) {
            throw new IllegalArgumentException(
                "Please provide a valid reason (min 10 characters).");
        }

        // ── Verify patient exists ─────────────────────────────────────────────
        Optional<User> patientOpt = userDAO.findById(patientId);
        if (patientOpt.isEmpty()) {
            throw new IllegalArgumentException(
                "Patient #" + patientId + " not found.");
        }

        User patient = patientOpt.get();

        // ── Log to emergency_logs table ───────────────────────────────────────
        logEmergencyAccess(doctor.getId(), patientId, reason.trim());

        // ── Log to audit_logs table ───────────────────────────────────────────
        auditLogDAO.log(
            doctor.getId(),
            AuditLog.ACTION_EMERGENCY_ACCESS,
            doctor.getDisplayName() + " used emergency access for " +
            patient.getName() + " | Reason: " + reason.trim()
        );

        LOGGER.warning("🚨 EMERGENCY ACCESS: " + doctor.getDisplayName() +
                       " accessed records of " + patient.getName() +
                       " | Reason: " + reason);

        // ── Return patient records ────────────────────────────────────────────
        return recordDAO.findByPatientId(patientId);
    }

    /**
     * Saves emergency access event to emergency_logs table.
     *
     * @param  doctorId   doctor's user ID
     * @param  patientId  patient's user ID
     * @param  reason     reason for emergency access
     */
    private void logEmergencyAccess(int doctorId,
                                    int patientId, String reason) {
        String sql = "INSERT INTO emergency_logs " +
                     "(doctor_id, patient_id, reason) VALUES (?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1,    doctorId);
            ps.setInt(2,    patientId);
            ps.setString(3, reason);
            ps.executeUpdate();
            LOGGER.info("🚨 Emergency log saved for Doctor #" +
                        doctorId + " → Patient #" + patientId);

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "❌ Error saving emergency log.", e);
        }
    }

    /**
     * Returns all emergency access logs — for Admin dashboard.
     *
     * @return  list of emergency log entries
     */
    public List<EmergencyLog> getAllEmergencyLogs() {
        String sql =
            "SELECT el.*, ud.name AS doctor_name, up.name AS patient_name " +
            "FROM emergency_logs el " +
            "JOIN users ud ON el.doctor_id  = ud.id " +
            "JOIN users up ON el.patient_id = up.id " +
            "ORDER BY el.accessed_at DESC";

        List<EmergencyLog> logs = new java.util.ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                EmergencyLog log = new EmergencyLog();
                log.setId(rs.getInt("id"));
                log.setDoctorId(rs.getInt("doctor_id"));
                log.setPatientId(rs.getInt("patient_id"));
                log.setDoctorName(rs.getString("doctor_name"));
                log.setPatientName(rs.getString("patient_name"));
                log.setReason(rs.getString("reason"));
                Timestamp ts = rs.getTimestamp("accessed_at");
                if (ts != null) log.setAccessedAt(ts.toLocalDateTime());
                logs.add(log);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "❌ Error fetching emergency logs.", e);
        }
        return logs;
    }

    // ── Inner Class ───────────────────────────────────────────────────────────

    /**
     * EmergencyLog — Inner model class for emergency log entries.
     * Maps to the emergency_logs table.
     */
    public static class EmergencyLog {
        private int           id;
        private int           doctorId;
        private int           patientId;
        private String        doctorName;
        private String        patientName;
        private String        reason;
        private java.time.LocalDateTime accessedAt;

        public int    getId()                     { return id; }
        public void   setId(int id)               { this.id = id; }
        public int    getDoctorId()               { return doctorId; }
        public void   setDoctorId(int doctorId)   { this.doctorId = doctorId; }
        public int    getPatientId()              { return patientId; }
        public void   setPatientId(int patientId) { this.patientId = patientId; }
        public String getDoctorName()             { return doctorName; }
        public void   setDoctorName(String n)     { this.doctorName = n; }
        public String getPatientName()            { return patientName; }
        public void   setPatientName(String n)    { this.patientName = n; }
        public String getReason()                 { return reason; }
        public void   setReason(String reason)    { this.reason = reason; }
        public java.time.LocalDateTime getAccessedAt()    { return accessedAt; }
        public void setAccessedAt(java.time.LocalDateTime t) { this.accessedAt = t; }

        @Override
        public String toString() {
            return "EmergencyLog{doctor=" + doctorName +
                   ", patient=" + patientName +
                   ", reason=" + reason + "}";
        }
    }
}
