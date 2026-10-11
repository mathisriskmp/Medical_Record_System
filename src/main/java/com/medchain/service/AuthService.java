package com.medchain.service;

import com.medchain.dao.AuditLogDAO;
import com.medchain.dao.UserDAO;
import com.medchain.model.AuditLog;
import com.medchain.model.User;
import com.medchain.util.BCryptUtil;
import com.medchain.util.SessionManager;

import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * AuthService — Authentication & Authorization Service
 *
 * Handles all authentication operations:
 * → User login with BCrypt password verification
 * → New user registration with password hashing
 * → Session management (start/end)
 * → Password strength validation
 *
 * This is the FIRST service called when the app starts.
 * Every user must pass through AuthService before
 * accessing any other part of the system.
 *
 * @author  MedChain Team
 * @version 1.0
 */
public class AuthService {

    // ── Logger ────────────────────────────────────────────────────────────────
    private static final Logger LOGGER = Logger.getLogger(AuthService.class.getName());

    // ── Dependencies ──────────────────────────────────────────────────────────
    private final UserDAO     userDAO     = new UserDAO();
    private final AuditLogDAO auditLogDAO = new AuditLogDAO();

    // ── Login ─────────────────────────────────────────────────────────────────

    /**
     * Authenticates a user by email and password.
     *
     * Steps:
     * 1. Find user by email in database
     * 2. Verify password using BCrypt
     * 3. Start session if valid
     * 4. Log the login action
     *
     * @param  email     user's email address
     * @param  password  plain text password entered by user
     * @return           Optional containing authenticated User,
     *                   empty if credentials are invalid
     */
    public Optional<User> login(String email, String password) {
        // ── Input Validation ──────────────────────────────────────────────────
        if (email == null    || email.trim().isEmpty() ||
            password == null || password.isEmpty()) {
            LOGGER.warning("⚠️ Login attempt with empty email or password.");
            return Optional.empty();
        }

        try {
            // ── Step 1: Find user by email ────────────────────────────────────
            Optional<User> userOpt = userDAO.findByEmail(email.trim().toLowerCase());

            if (userOpt.isEmpty()) {
                LOGGER.warning("❌ Login failed — no user found for: " + email);
                return Optional.empty();
            }

            User user = userOpt.get();

            // ── Step 2: Verify password ───────────────────────────────────────
            if (!BCryptUtil.verifyPassword(password, user.getPasswordHash())) {
                LOGGER.warning("❌ Login failed — wrong password for: " + email);
                return Optional.empty();
            }

            // ── Step 3: Start session ─────────────────────────────────────────
            SessionManager.getInstance().setCurrentUser(user);

            // ── Step 4: Log the action ────────────────────────────────────────
            auditLogDAO.log(
                user.getId(),
                AuditLog.ACTION_LOGIN,
                user.getDisplayName() + " logged in as " + user.getRole().getDisplayName()
            );

            LOGGER.info("✅ Login successful: " + user.getDisplayName());
            return Optional.of(user);

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "❌ Unexpected error during login.", e);
            return Optional.empty();
        }
    }

    // ── Registration ──────────────────────────────────────────────────────────

    /**
     * Registers a new user in the system.
     *
     * Steps:
     * 1. Validate input fields
     * 2. Check email is not already registered
     * 3. Validate password strength
     * 4. Hash the password using BCrypt
     * 5. Save user to database
     *
     * @param  name      full name of the user
     * @param  email     email address (must be unique)
     * @param  password  plain text password (will be hashed)
     * @param  role      PATIENT, DOCTOR, or ADMIN
     * @return           registered User object with generated ID
     * @throws IllegalArgumentException if validation fails
     */
    public User register(String name, String email,
                         String password, User.Role role) {
        // ── Input Validation ──────────────────────────────────────────────────
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Name cannot be empty.");
        }
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("Email cannot be empty.");
        }
        if (password == null || password.isEmpty()) {
            throw new IllegalArgumentException("Password cannot be empty.");
        }

        // ── Check email uniqueness ────────────────────────────────────────────
        if (userDAO.emailExists(email.trim().toLowerCase())) {
            throw new IllegalArgumentException(
                "Email already registered: " + email);
        }

        // ── Validate password strength ────────────────────────────────────────
        if (!BCryptUtil.isPasswordStrong(password)) {
            throw new IllegalArgumentException(
                "Password must be at least 8 characters with " +
                "uppercase, lowercase, and a number.");
        }

        // ── Hash password ─────────────────────────────────────────────────────
        String hashedPassword = BCryptUtil.hashPassword(password);

        // ── Create and save user ──────────────────────────────────────────────
        User user = new User(
            name.trim(),
            email.trim().toLowerCase(),
            hashedPassword,
            role
        );

        int generatedId = userDAO.save(user);
        if (generatedId == -1) {
            throw new RuntimeException("Failed to save user to database.");
        }

        user.setId(generatedId);
        LOGGER.info("✅ User registered: " + user.getDisplayName() +
                    " [" + role + "]");
        return user;
    }

    // ── Logout ────────────────────────────────────────────────────────────────

    /**
     * Logs out the current user and clears the session.
     * Logs the logout action to audit trail.
     */
    public void logout() {
        User current = SessionManager.getInstance().getCurrentUser();
        if (current != null) {
            auditLogDAO.log(
                current.getId(),
                AuditLog.ACTION_LOGOUT,
                current.getDisplayName() + " logged out after " +
                SessionManager.getInstance().getSessionDurationMinutes() + " minutes"
            );
        }
        SessionManager.getInstance().clearSession();
        LOGGER.info("✅ User logged out successfully.");
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Returns the currently logged-in user.
     *
     * @return  current User or null if not logged in
     */
    public User getCurrentUser() {
        return SessionManager.getInstance().getCurrentUser();
    }

    /**
     * Checks if any user is currently logged in.
     *
     * @return  true if session is active
     */
    public boolean isLoggedIn() {
        return SessionManager.getInstance().isLoggedIn();
    }
}


