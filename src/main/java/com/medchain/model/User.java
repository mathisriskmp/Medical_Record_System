package com.medchain.model;

import java.time.LocalDateTime;

/**
 * User — Domain Model
 *
 * Represents a system user in the MedChain application.
 * Three roles exist: PATIENT, DOCTOR, and ADMIN — each with
 * different access levels and dashboard views.
 *
 * Maps to the 'users' table in medchain_db.
 *
 * @author  MedChain Team
 * @version 1.0
 */
public class User {

    // ── Role Enum ─────────────────────────────────────────────────────────────
    public enum Role {
        PATIENT, DOCTOR, ADMIN;

        /**
         * Returns a display-friendly version of the role name.
         */
        public String getDisplayName() {
            return switch (this) {
                case PATIENT -> "Patient";
                case DOCTOR  -> "Doctor";
                case ADMIN   -> "Admin";
            };
        }
    }

    // ── Fields ────────────────────────────────────────────────────────────────
    private int           id;
    private String        name;
    private String        email;
    private String        passwordHash;
    private Role          role;
    private LocalDateTime createdAt;

    // ── Constructors ──────────────────────────────────────────────────────────

    /** Default constructor — required for JDBC mapping */
    public User() {}

    /**
     * Full constructor — used when loading user from database.
     */
    public User(int id, String name, String email,
                String passwordHash, Role role, LocalDateTime createdAt) {
        this.id           = id;
        this.name         = name;
        this.email        = email;
        this.passwordHash = passwordHash;
        this.role         = role;
        this.createdAt    = createdAt;
    }

    /**
     * Registration constructor — used when creating a new user.
     */
    public User(String name, String email, String passwordHash, Role role) {
        this.name         = name;
        this.email        = email;
        this.passwordHash = passwordHash;
        this.role         = role;
    }

    // ── Getters & Setters ─────────────────────────────────────────────────────
    public int getId()                      { return id; }
    public void setId(int id)               { this.id = id; }

    public String getName()                 { return name; }
    public void setName(String name)        { this.name = name; }

    public String getEmail()                { return email; }
    public void setEmail(String email)      { this.email = email; }

    public String getPasswordHash()                     { return passwordHash; }
    public void setPasswordHash(String passwordHash)    { this.passwordHash = passwordHash; }

    public Role getRole()                   { return role; }
    public void setRole(Role role)          { this.role = role; }

    public LocalDateTime getCreatedAt()                 { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt)   { this.createdAt = createdAt; }

    // ── Utility Methods ───────────────────────────────────────────────────────

    /**
     * Returns a display name with role prefix.
     * e.g. "Dr. Ravi Kumar" for doctors, "Arun Sharma" for patients.
     */
    public String getDisplayName() {
        return role == Role.DOCTOR ? "Dr. " + name : name;
    }

    @Override
    public String toString() {
        return getDisplayName();
    }
}
