package com.medchain.service;

import com.medchain.dao.AuditLogDAO;
import com.medchain.dao.UserDAO;
import com.medchain.model.AuditLog;
import com.medchain.model.User;
import com.medchain.util.SessionManager;

import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * UserService — User Management Business Logic
 *
 * Handles Admin-level user management operations:
 * → View all users in the system
 * → Delete users
 * → Fetch users by role
 *
 * Only accessible to ADMIN role users.
 *
 * @author  MedChain Team
 * @version 1.0
 */
public class UserService {

    // ── Logger ────────────────────────────────────────────────────────────────
    private static final Logger LOGGER =
        Logger.getLogger(UserService.class.getName());

    // ── Dependencies ──────────────────────────────────────────────────────────
    private final UserDAO     userDAO     = new UserDAO();
    private final AuditLogDAO auditLogDAO = new AuditLogDAO();

    /**
     * Returns all users in the system.
     * Used in Admin dashboard's user management table.
     *
     * @return  list of all User objects
     * @throws  SecurityException if caller is not Admin
     */
    public List<User> getAllUsers() {
        validateAdminAccess();
        return userDAO.findAll();
    }

    /**
     * Returns all doctors in the system.
     *
     * @return  list of Doctor users
     */
    public List<User> getAllDoctors() {
        return userDAO.findByRole(User.Role.DOCTOR);
    }

    /**
     * Returns all patients in the system.
     *
     * @return  list of Patient users
     */
    public List<User> getAllPatients() {
        return userDAO.findByRole(User.Role.PATIENT);
    }

    /**
     * Finds a user by their ID.
     *
     * @param  userId  user ID
     * @return         Optional containing User if found
     */
    public Optional<User> getUserById(int userId) {
        return userDAO.findById(userId);
    }

    /**
     * Deletes a user from the system.
     * Only Admin can delete users.
     * Admin cannot delete themselves.
     *
     * @param  userId  ID of user to delete
     * @throws SecurityException        if caller is not Admin
     * @throws IllegalArgumentException if trying to delete self
     * @throws RuntimeException         if deletion fails
     */
    public void deleteUser(int userId) {
        validateAdminAccess();

        // ── Admin cannot delete themselves ────────────────────────────────────
        int currentAdminId = SessionManager.getInstance().getCurrentUserId();
        if (userId == currentAdminId) {
            throw new IllegalArgumentException(
                "You cannot delete your own account.");
        }

        Optional<User> userOpt = userDAO.findById(userId);
        if (userOpt.isEmpty()) {
            throw new IllegalArgumentException(
                "User #" + userId + " not found.");
        }

        String userName = userOpt.get().getName();
        boolean success = userDAO.delete(userId);

        if (!success) {
            throw new RuntimeException("Failed to delete user #" + userId);
        }

        // ── Log action ────────────────────────────────────────────────────────
        auditLogDAO.log(
            currentAdminId,
            AuditLog.ACTION_DELETE_USER,
            "Admin deleted user: " + userName + " (ID: " + userId + ")"
        );

        LOGGER.info("✅ User deleted: " + userName + " (ID: " + userId + ")");
    }

    /**
     * Returns statistics for Admin dashboard.
     *
     * @return  int array: [totalUsers, totalPatients,
     *                      totalDoctors, totalAdmins]
     */
    public int[] getUserStats() {
        List<User> all      = userDAO.findAll();
        int patients = (int) all.stream()
            .filter(u -> u.getRole() == User.Role.PATIENT).count();
        int doctors  = (int) all.stream()
            .filter(u -> u.getRole() == User.Role.DOCTOR).count();
        int admins   = (int) all.stream()
            .filter(u -> u.getRole() == User.Role.ADMIN).count();
        return new int[]{all.size(), patients, doctors, admins};
    }

    // ── Private Helper ────────────────────────────────────────────────────────

    /**
     * Validates that the current user is an Admin.
     *
     * @throws SecurityException if current user is not Admin
     */
    private void validateAdminAccess() {
        if (!SessionManager.getInstance().isAdmin()) {
            throw new SecurityException(
                "Access denied — Admin privileges required.");
        }
    }
}
