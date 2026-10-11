package com.medchain.util;

import com.medchain.model.User;
import java.util.logging.Logger;

/**
 * SessionManager — User Session Management
 *
 * Manages the currently logged-in user's session throughout the application.
 * Implemented as a thread-safe Singleton — only one session exists at a time.
 *
 * Responsibilities:
 * → Stores the currently logged-in User object
 * → Provides role-based access checks
 * → Clears session on logout
 *
 * Pattern : Singleton (Thread-Safe)
 *
 * @author  MedChain Team
 * @version 1.0
 */
public class SessionManager {

    // ── Logger ────────────────────────────────────────────────────────────────
    private static final Logger LOGGER = Logger.getLogger(SessionManager.class.getName());

    // ── Singleton Instance ────────────────────────────────────────────────────
    private static volatile SessionManager instance;

    // ── Session Data ──────────────────────────────────────────────────────────
    private User currentUser;
    private long loginTime;

    /**
     * Private constructor — prevents direct instantiation.
     */
    private SessionManager() {}

    /**
     * Returns the single instance of SessionManager.
     * Uses double-checked locking for thread safety.
     *
     * @return  the singleton SessionManager instance
     */
    public static SessionManager getInstance() {
        if (instance == null) {
            synchronized (SessionManager.class) {
                if (instance == null) {
                    instance = new SessionManager();
                }
            }
        }
        return instance;
    }

    // ── Session Control ───────────────────────────────────────────────────────

    /**
     * Sets the currently logged-in user and records login time.
     *
     * @param  user  the authenticated User object
     */
    public void setCurrentUser(User user) {
        this.currentUser = user;
        this.loginTime   = System.currentTimeMillis();
        LOGGER.info("✅ Session started for: " + user.getName() + " [" + user.getRole() + "]");
    }

    /**
     * Returns the currently logged-in user.
     *
     * @return  current User or null if no session is active
     */
    public User getCurrentUser() {
        return currentUser;
    }

    /**
     * Clears the current session — called on logout.
     */
    public void clearSession() {
        if (currentUser != null) {
            LOGGER.info("✅ Session cleared for: " + currentUser.getName());
        }
        this.currentUser = null;
        this.loginTime   = 0;
    }

    // ── Session Checks ────────────────────────────────────────────────────────

    /**
     * Checks if a user is currently logged in.
     *
     * @return  true if session is active, false otherwise
     */
    public boolean isLoggedIn() {
        return currentUser != null;
    }

    /**
     * Checks if the current user has the PATIENT role.
     *
     * @return  true if current user is a Patient
     */
    public boolean isPatient() {
        return isLoggedIn() && currentUser.getRole() == User.Role.PATIENT;
    }

    /**
     * Checks if the current user has the DOCTOR role.
     *
     * @return  true if current user is a Doctor
     */
    public boolean isDoctor() {
        return isLoggedIn() && currentUser.getRole() == User.Role.DOCTOR;
    }

    /**
     * Checks if the current user has the ADMIN role.
     *
     * @return  true if current user is an Admin
     */
    public boolean isAdmin() {
        return isLoggedIn() && currentUser.getRole() == User.Role.ADMIN;
    }

    /**
     * Returns how long the current user has been logged in (in minutes).
     *
     * @return  session duration in minutes
     */
    public long getSessionDurationMinutes() {
        if (!isLoggedIn()) return 0;
        return (System.currentTimeMillis() - loginTime) / 60000;
    }

    /**
     * Returns the current user's ID safely.
     *
     * @return  user ID or -1 if no session
     */
    public int getCurrentUserId() {
        return isLoggedIn() ? currentUser.getId() : -1;
    }

    /**
     * Returns the current user's name safely.
     *
     * @return  user name or "Guest" if no session
     */
    public String getCurrentUserName() {
        return isLoggedIn() ? currentUser.getName() : "Guest";
    }
}
