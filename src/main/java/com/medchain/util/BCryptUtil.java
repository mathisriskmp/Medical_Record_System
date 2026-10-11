package com.medchain.util;

import org.mindrot.jbcrypt.BCrypt;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * BCryptUtil — Password Hashing Utility
 *
 * Provides secure password hashing and verification using the BCrypt algorithm.
 * Passwords are NEVER stored as plain text in the database — they are always
 * hashed before saving and verified during login.
 *
 * Algorithm  : BCrypt (adaptive hash function)
 * Work Factor: 12 (industry standard — balances security and performance)
 *
 * Why BCrypt over SHA-256 for passwords?
 * → BCrypt is specifically designed for passwords — it is intentionally slow
 *   and includes a salt automatically, making brute-force attacks impractical.
 * → SHA-256 is used for data integrity (record hashing), not passwords.
 *
 * @author  MedChain Team
 * @version 1.0
 */
public class BCryptUtil {

    // ── Logger ────────────────────────────────────────────────────────────────
    private static final Logger LOGGER = Logger.getLogger(BCryptUtil.class.getName());

    // ── BCrypt Work Factor ────────────────────────────────────────────────────
    // Higher = more secure but slower. 12 is the industry standard.
    private static final int WORK_FACTOR = 12;

    /**
     * Private constructor — prevents instantiation.
     * This is a utility class and should only be used via static methods.
     */
    private BCryptUtil() {
        throw new UnsupportedOperationException(
            "BCryptUtil is a utility class and cannot be instantiated."
        );
    }

    // ── Public Methods ────────────────────────────────────────────────────────

    /**
     * Hashes a plain text password using BCrypt.
     * A unique salt is automatically generated and embedded in the hash.
     *
     * @param  plainPassword  the raw password entered by the user
     * @return                BCrypt hashed password string (60 characters)
     * @throws IllegalArgumentException if password is null or empty
     */
    public static String hashPassword(String plainPassword) {
        if (plainPassword == null || plainPassword.isEmpty()) {
            throw new IllegalArgumentException("Password cannot be null or empty.");
        }
        try {
            String hashed = BCrypt.hashpw(plainPassword, BCrypt.gensalt(WORK_FACTOR));
            LOGGER.info("✅ Password hashed successfully.");
            return hashed;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "❌ Error hashing password.", e);
            throw new RuntimeException("Password hashing failed: " + e.getMessage());
        }
    }

    /**
     * Verifies a plain text password against a stored BCrypt hash.
     * Used during login to authenticate users.
     *
     * @param  plainPassword   the raw password entered during login
     * @param  hashedPassword  the stored BCrypt hash from the database
     * @return                 true if password matches, false otherwise
     */
    public static boolean verifyPassword(String plainPassword, String hashedPassword) {
        if (plainPassword == null || hashedPassword == null) {
            LOGGER.warning("⚠️ verifyPassword() called with null input.");
            return false;
        }
        try {
            boolean isMatch = BCrypt.checkpw(plainPassword, hashedPassword);
            if (isMatch) {
                LOGGER.info("✅ Password verification successful.");
            } else {
                LOGGER.warning("❌ Password verification failed — incorrect password.");
            }
            return isMatch;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "❌ Error verifying password.", e);
            return false;
        }
    }

    /**
     * Validates password strength before hashing.
     * Ensures password meets minimum security requirements.
     *
     * Requirements:
     * → Minimum 8 characters
     * → At least one uppercase letter
     * → At least one lowercase letter
     * → At least one digit
     *
     * @param  password  the password to validate
     * @return           true if password is strong enough, false otherwise
     */
    public static boolean isPasswordStrong(String password) {
        if (password == null || password.length() < 8) return false;
        boolean hasUpper   = password.chars().anyMatch(Character::isUpperCase);
        boolean hasLower   = password.chars().anyMatch(Character::isLowerCase);
        boolean hasDigit   = password.chars().anyMatch(Character::isDigit);
        return hasUpper && hasLower && hasDigit;
    }
}
