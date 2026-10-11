package com.medchain.dao;

import com.medchain.model.User;
import com.medchain.util.DBConnection;

import java.sql.*;
//import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * UserDAO — Data Access Object for Users
 *
 * Handles all database operations related to the users table.
 * Uses JDBC PreparedStatements to prevent SQL injection.
 *
 * Operations : Create, Read, Update, Delete (CRUD)
 * Table      : users
 *
 * @author  MedChain Team
 * @version 1.0
 */
public class UserDAO {

    // ── Logger ────────────────────────────────────────────────────────────────
    private static final Logger LOGGER = Logger.getLogger(UserDAO.class.getName());

    // ── SQL Queries ───────────────────────────────────────────────────────────
    private static final String INSERT_USER =
        "INSERT INTO users (name, email, password_hash, role) VALUES (?, ?, ?, ?)";

    private static final String SELECT_BY_ID =
        "SELECT * FROM users WHERE id = ?";

    private static final String SELECT_BY_EMAIL =
        "SELECT * FROM users WHERE email = ?";

    private static final String SELECT_BY_ROLE =
        "SELECT * FROM users WHERE role = ?";

    private static final String SELECT_ALL =
        "SELECT * FROM users ORDER BY created_at DESC";

    private static final String UPDATE_USER =
        "UPDATE users SET name = ?, email = ? WHERE id = ?";

    private static final String DELETE_USER =
        "DELETE FROM users WHERE id = ?";

    private static final String CHECK_EMAIL_EXISTS =
        "SELECT COUNT(*) FROM users WHERE email = ?";

    // ── CRUD Operations ───────────────────────────────────────────────────────

    /**
     * Inserts a new user into the database.
     *
     * @param  user  User object to save
     * @return       generated user ID, or -1 if failed
     */
    public int save(User user) {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                 INSERT_USER, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, user.getName());
            ps.setString(2, user.getEmail());
            ps.setString(3, user.getPasswordHash());
            ps.setString(4, user.getRole().name());
            ps.executeUpdate();

            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) {
                int id = keys.getInt(1);
                LOGGER.info("✅ User saved → ID: " + id + ", Email: " + user.getEmail());
                return id;
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "❌ Error saving user: " + user.getEmail(), e);
        }
        return -1;
    }

    /**
     * Finds a user by their ID.
     *
     * @param  id  user ID
     * @return     Optional containing User if found
     */
    public Optional<User> findById(int id) {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SELECT_BY_ID)) {

            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return Optional.of(mapRow(rs));

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "❌ Error finding user by ID: " + id, e);
        }
        return Optional.empty();
    }

    /**
     * Finds a user by their email address.
     * Used during login authentication.
     *
     * @param  email  user's email address
     * @return        Optional containing User if found
     */
    public Optional<User> findByEmail(String email) {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SELECT_BY_EMAIL)) {

            ps.setString(1, email);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return Optional.of(mapRow(rs));

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "❌ Error finding user by email: " + email, e);
        }
        return Optional.empty();
    }

    /**
     * Returns all users with a specific role.
     *
     * @param  role  PATIENT, DOCTOR, or ADMIN
     * @return       list of matching users
     */
    public List<User> findByRole(User.Role role) {
        List<User> users = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SELECT_BY_ROLE)) {

            ps.setString(1, role.name());
            ResultSet rs = ps.executeQuery();
            while (rs.next()) users.add(mapRow(rs));

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "❌ Error finding users by role: " + role, e);
        }
        return users;
    }

    /**
     * Returns all users in the system.
     *
     * @return  list of all users
     */
    public List<User> findAll() {
        List<User> users = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SELECT_ALL)) {

            ResultSet rs = ps.executeQuery();
            while (rs.next()) users.add(mapRow(rs));

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "❌ Error fetching all users.", e);
        }
        return users;
    }

    /**
     * Updates a user's name and email.
     *
     * @param  user  User object with updated fields
     * @return       true if update was successful
     */
    public boolean update(User user) {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(UPDATE_USER)) {

            ps.setString(1, user.getName());
            ps.setString(2, user.getEmail());
            ps.setInt(3, user.getId());
            boolean success = ps.executeUpdate() > 0;
            if (success) LOGGER.info("✅ User updated → ID: " + user.getId());
            return success;

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "❌ Error updating user: " + user.getId(), e);
        }
        return false;
    }

    /**
     * Deletes a user from the database by ID.
     *
     * @param  userId  ID of the user to delete
     * @return         true if deletion was successful
     */
    public boolean delete(int userId) {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(DELETE_USER)) {

            ps.setInt(1, userId);
            boolean success = ps.executeUpdate() > 0;
            if (success) LOGGER.info("✅ User deleted → ID: " + userId);
            return success;

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "❌ Error deleting user: " + userId, e);
        }
        return false;
    }

    /**
     * Checks if an email address is already registered.
     *
     * @param  email  email to check
     * @return        true if email already exists
     */
    public boolean emailExists(String email) {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(CHECK_EMAIL_EXISTS)) {

            ps.setString(1, email);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1) > 0;

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "❌ Error checking email: " + email, e);
        }
        return false;
    }

    // ── Private Helper ────────────────────────────────────────────────────────

    /**
     * Maps a ResultSet row to a User object.
     *
     * @param  rs  ResultSet pointing to a user row
     * @return     populated User object
     */
    private User mapRow(ResultSet rs) throws SQLException {
        User user = new User();
        user.setId(rs.getInt("id"));
        user.setName(rs.getString("name"));
        user.setEmail(rs.getString("email"));
        user.setPasswordHash(rs.getString("password_hash"));
        user.setRole(User.Role.valueOf(rs.getString("role")));
        Timestamp ts = rs.getTimestamp("created_at");
        if (ts != null) user.setCreatedAt(ts.toLocalDateTime());
        return user;
    }
}
