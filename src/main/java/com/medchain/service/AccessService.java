package com.medchain.service;

import com.medchain.dao.AccessPermissionDAO;
import com.medchain.dao.AuditLogDAO;
import com.medchain.dao.UserDAO;
import com.medchain.model.AccessPermission;
import com.medchain.model.AuditLog;
import com.medchain.model.User;
import com.medchain.util.SessionManager;

import java.util.List;
import java.util.Optional;
//import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * AccessService — Access Control Business Logic
 *
 * Manages the patient-controlled access permission system.
 * Patients are the DATA OWNERS — they decide which doctors
 * can view and add to their medical records.
 *
 * Operations:
 * → grantAccess()  — patient gives doctor permission
 * → revokeAccess() — patient removes doctor's permission
 * → hasAccess()    — check if doctor has permission
 * → getPatientPermissions() — list all doctor permissions
 * → getDoctorPatients()     — list all accessible patients
 *
 * @author  MedChain Team
 * @version 1.0
 */
public class AccessService {

    // ── Logger ────────────────────────────────────────────────────────────────
    private static final Logger LOGGER =
        Logger.getLogger(AccessService.class.getName());

    // ── Dependencies ──────────────────────────────────────────────────────────
    private final AccessPermissionDAO accessDAO   = new AccessPermissionDAO();
    private final AuditLogDAO         auditLogDAO = new AuditLogDAO();
    private final UserDAO             userDAO     = new UserDAO();

    // ── Grant Access ──────────────────────────────────────────────────────────

    /**
     * Grants a doctor access to the current patient's records.
     *
     * Validates:
     * → Patient cannot grant access to themselves
     * → Target user must actually be a doctor
     * → Access not already active
     *
     * @param  doctorId  ID of the doctor to grant access to
     * @throws IllegalArgumentException if validation fails
     * @throws SecurityException        if caller is not a patient
     */
    public void grantAccess(int doctorId) {
        // ── Get current patient ───────────────────────────────────────────────
        User patient = SessionManager.getInstance().getCurrentUser();
        if (patient == null || patient.getRole() != User.Role.PATIENT) {
            throw new SecurityException("Only patients can grant access.");
        }

        int patientId = patient.getId();

        // ── Validate doctor exists and is a doctor ────────────────────────────
        Optional<User> doctorOpt = userDAO.findById(doctorId);
        if (doctorOpt.isEmpty()) {
            throw new IllegalArgumentException("Doctor not found.");
        }
        if (doctorOpt.get().getRole() != User.Role.DOCTOR) {
            throw new IllegalArgumentException(
                "Selected user is not a doctor.");
        }

        // ── Check not already granted ─────────────────────────────────────────
        if (accessDAO.hasAccess(patientId, doctorId)) {
            throw new IllegalArgumentException(
                "Access already granted to " +
                doctorOpt.get().getDisplayName());
        }

        // ── Grant access ──────────────────────────────────────────────────────
        boolean success = accessDAO.grant(patientId, doctorId);
        if (!success) {
            throw new RuntimeException("Failed to grant access.");
        }

        // ── Log action ────────────────────────────────────────────────────────
        auditLogDAO.log(
            patientId,
            AuditLog.ACTION_GRANT_ACCESS,
            patient.getName() + " granted access to " +
            doctorOpt.get().getDisplayName()
        );

        LOGGER.info("✅ Access granted: " + patient.getName() +
                    " → " + doctorOpt.get().getDisplayName());
    }

    // ── Revoke Access ─────────────────────────────────────────────────────────

    /**
     * Revokes a doctor's access to the current patient's records.
     *
     * @param  doctorId  ID of the doctor to revoke access from
     * @throws SecurityException if caller is not a patient
     * @throws IllegalArgumentException if no active permission exists
     */
    public void revokeAccess(int doctorId) {
        User patient = SessionManager.getInstance().getCurrentUser();
        if (patient == null || patient.getRole() != User.Role.PATIENT) {
            throw new SecurityException("Only patients can revoke access.");
        }

        int patientId = patient.getId();

        // ── Check permission exists ───────────────────────────────────────────
        if (!accessDAO.hasAccess(patientId, doctorId)) {
            throw new IllegalArgumentException(
                "No active permission found for this doctor.");
        }

        Optional<User> doctorOpt = userDAO.findById(doctorId);
        String doctorName = doctorOpt.map(User::getDisplayName)
                                     .orElse("Doctor #" + doctorId);

        // ── Revoke access ─────────────────────────────────────────────────────
        boolean success = accessDAO.revoke(patientId, doctorId);
        if (!success) {
            throw new RuntimeException("Failed to revoke access.");
        }

        // ── Log action ────────────────────────────────────────────────────────
        auditLogDAO.log(
            patientId,
            AuditLog.ACTION_REVOKE_ACCESS,
            patient.getName() + " revoked access from " + doctorName
        );

        LOGGER.info("✅ Access revoked: " + patient.getName() +
                    " → " + doctorName);
    }

    // ── Query Methods ─────────────────────────────────────────────────────────

    /**
     * Checks if a doctor has active access to a patient's records.
     *
     * @param  patientId  patient's user ID
     * @param  doctorId   doctor's user ID
     * @return            true if access is active
     */
    public boolean hasAccess(int patientId, int doctorId) {
        return accessDAO.hasAccess(patientId, doctorId);
    }

    /**
     * Returns all permissions for the current patient.
     * Shows which doctors have access and their status.
     *
     * @return  list of AccessPermission objects
     */
    public List<AccessPermission> getMyPermissions() {
        int patientId = SessionManager.getInstance().getCurrentUserId();
        return accessDAO.findByPatientId(patientId);
    }

    /**
     * Returns all patients who have given access to
     * the current doctor.
     *
     * @return  list of AccessPermission objects
     */
    public List<AccessPermission> getMyPatients() {
        int doctorId = SessionManager.getInstance().getCurrentUserId();
        return accessDAO.findByDoctorId(doctorId);
    }

    /**
     * Returns all doctors in the system.
     * Used to populate the doctor dropdown on Patient dashboard.
     *
     * @return  list of all doctors
     */
    public List<User> getAllDoctors() {
        return userDAO.findByRole(User.Role.DOCTOR);
    }
}
