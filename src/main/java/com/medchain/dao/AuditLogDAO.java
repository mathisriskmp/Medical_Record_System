package com.medchain.dao;

import com.medchain.model.AuditLog;
import com.medchain.util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * AuditLogDAO — Data Access Object for Audit Logs
 *
 * Handles all database operations for the audit_logs table.
 * Every important action in the system gets permanently logged here.
 *
 * @author  MedChain Team
 * @version 1.0
 */
public class AuditLogDAO {

    private static final Logger LOGGER = Logger.getLogger(AuditLogDAO.class.getName());

    private static final String INSERT_LOG =
        "INSERT INTO audit_logs (user_id, action, details) VALUES (?, ?, ?)";

    private static final String SELECT_ALL =
        "SELECT al.*, u.name AS user_name FROM audit_logs al " +
        "JOIN users u ON al.user_id = u.id " +
        "ORDER BY al.timestamp DESC";

    private static final String SELECT_BY_USER =
        "SELECT al.*, u.name AS user_name FROM audit_logs al " +
        "JOIN users u ON al.user_id = u.id " +
        "WHERE al.user_id = ? ORDER BY al.timestamp DESC";

    /**
     * Saves a new audit log entry to the database.
     *
     * @param  userId   ID of the user who performed the action
     * @param  action   action constant from AuditLog class
     * @param  details  human-readable description of the action
     */
    public void log(int userId, String action, String details) {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(INSERT_LOG)) {

            ps.setInt(1,    userId);
            ps.setString(2, action);
            ps.setString(3, details);
            ps.executeUpdate();
            LOGGER.info("📋 Audit logged → User #" + userId + " | " + action);

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "❌ Error saving audit log.", e);
        }
    }

    /**
     * Returns all audit logs — used by Admin dashboard.
     *
     * @return  list of all AuditLog objects ordered by newest first
     */
    public List<AuditLog> findAll() {
        List<AuditLog> logs = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SELECT_ALL)) {

            ResultSet rs = ps.executeQuery();
            while (rs.next()) logs.add(mapRow(rs));

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "❌ Error fetching all audit logs.", e);
        }
        return logs;
    }

    /**
     * Returns audit logs for a specific user.
     *
     * @param  userId  user ID to filter by
     * @return         list of AuditLog objects for that user
     */
    public List<AuditLog> findByUserId(int userId) {
        List<AuditLog> logs = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SELECT_BY_USER)) {

            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) logs.add(mapRow(rs));

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "❌ Error fetching audit logs for user: " + userId, e);
        }
        return logs;
    }

    private AuditLog mapRow(ResultSet rs) throws SQLException {
        AuditLog log = new AuditLog();
        log.setId(rs.getInt("id"));
        log.setUserId(rs.getInt("user_id"));
        log.setUserName(rs.getString("user_name"));
        log.setAction(rs.getString("action"));
        log.setDetails(rs.getString("details"));
        Timestamp ts = rs.getTimestamp("timestamp");
        if (ts != null) log.setTimestamp(ts.toLocalDateTime());
        return log;
    }
}
