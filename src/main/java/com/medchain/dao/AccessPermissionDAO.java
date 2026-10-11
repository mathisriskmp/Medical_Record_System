package com.medchain.dao;

import com.medchain.model.AccessPermission;
import com.medchain.util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * AccessPermissionDAO — Data Access Object for Access Permissions
 *
 * Handles all database operations for access_permissions table.
 * Controls which doctors can access which patients' records.
 *
 * @author  MedChain Team
 * @version 1.0
 */
public class AccessPermissionDAO {

    private static final Logger LOGGER = Logger.getLogger(AccessPermissionDAO.class.getName());

    private static final String INSERT_PERMISSION =
        "INSERT INTO access_permissions (patient_id, doctor_id, is_active) " +
        "VALUES (?, ?, TRUE) ON DUPLICATE KEY UPDATE is_active = TRUE";

    private static final String REVOKE_PERMISSION =
        "UPDATE access_permissions SET is_active = FALSE " +
        "WHERE patient_id = ? AND doctor_id = ?";

    private static final String CHECK_ACCESS =
        "SELECT is_active FROM access_permissions " +
        "WHERE patient_id = ? AND doctor_id = ? AND is_active = TRUE";

    private static final String SELECT_BY_PATIENT =
        "SELECT ap.*, up.name AS patient_name, ud.name AS doctor_name " +
        "FROM access_permissions ap " +
        "JOIN users up ON ap.patient_id = up.id " +
        "JOIN users ud ON ap.doctor_id  = ud.id " +
        "WHERE ap.patient_id = ?";

    private static final String SELECT_BY_DOCTOR =
        "SELECT ap.*, up.name AS patient_name, ud.name AS doctor_name " +
        "FROM access_permissions ap " +
        "JOIN users up ON ap.patient_id = up.id " +
        "JOIN users ud ON ap.doctor_id  = ud.id " +
        "WHERE ap.doctor_id = ? AND ap.is_active = TRUE";

    /**
     * Grants a doctor access to a patient's records.
     * If permission already exists, reactivates it.
     *
     * @param  patientId  patient's user ID
     * @param  doctorId   doctor's user ID
     * @return            true if successful
     */
    public boolean grant(int patientId, int doctorId) {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(INSERT_PERMISSION)) {

            ps.setInt(1, patientId);
            ps.setInt(2, doctorId);
            boolean success = ps.executeUpdate() > 0;
            if (success) LOGGER.info("✅ Access granted: Patient #" +
                                     patientId + " → Doctor #" + doctorId);
            return success;

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "❌ Error granting access.", e);
        }
        return false;
    }

    /**
     * Revokes a doctor's access to a patient's records.
     *
     * @param  patientId  patient's user ID
     * @param  doctorId   doctor's user ID
     * @return            true if successful
     */
    public boolean revoke(int patientId, int doctorId) {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(REVOKE_PERMISSION)) {

            ps.setInt(1, patientId);
            ps.setInt(2, doctorId);
            boolean success = ps.executeUpdate() > 0;
            if (success) LOGGER.info("✅ Access revoked: Patient #" +
                                     patientId + " → Doctor #" + doctorId);
            return success;

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "❌ Error revoking access.", e);
        }
        return false;
    }

    /**
     * Checks if a doctor has active access to a patient's records.
     *
     * @param  patientId  patient's user ID
     * @param  doctorId   doctor's user ID
     * @return            true if access is active
     */
    public boolean hasAccess(int patientId, int doctorId) {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(CHECK_ACCESS)) {

            ps.setInt(1, patientId);
            ps.setInt(2, doctorId);
            return ps.executeQuery().next();

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "❌ Error checking access.", e);
        }
        return false;
    }

    /**
     * Returns all permissions for a patient.
     *
     * @param  patientId  patient's user ID
     * @return            list of AccessPermission objects
     */
    public List<AccessPermission> findByPatientId(int patientId) {
        return queryList(SELECT_BY_PATIENT, patientId);
    }

    /**
     * Returns all active patients for a doctor.
     *
     * @param  doctorId  doctor's user ID
     * @return           list of AccessPermission objects
     */
    public List<AccessPermission> findByDoctorId(int doctorId) {
        return queryList(SELECT_BY_DOCTOR, doctorId);
    }

    private List<AccessPermission> queryList(String sql, int param) {
        List<AccessPermission> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, param);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                AccessPermission ap = new AccessPermission();
                ap.setId(rs.getInt("id"));
                ap.setPatientId(rs.getInt("patient_id"));
                ap.setDoctorId(rs.getInt("doctor_id"));
                ap.setPatientName(rs.getString("patient_name"));
                ap.setDoctorName(rs.getString("doctor_name"));
                ap.setActive(rs.getBoolean("is_active"));
                Timestamp ts = rs.getTimestamp("granted_at");
                if (ts != null) ap.setGrantedAt(ts.toLocalDateTime());
                list.add(ap);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "❌ Error querying permissions.", e);
        }
        return list;
    }
}


