package com.medchain.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * DBConnection — Database Connection Utility
 *
 * Provides a singleton JDBC connection to the MySQL database.
 * All DAO classes use this class to obtain a database connection.
 *
 * Pattern  : Singleton
 * Database : MySQL
 * Driver   : com.mysql.cj.jdbc.Driver
 *
 * @author  MedChain Team
 * @version 1.0
 */
public class DBConnection {

    // ── Logger ────────────────────────────────────────────────────────────────
    private static final Logger LOGGER = Logger.getLogger(DBConnection.class.getName());

    // ── Database Configuration ────────────────────────────────────────────────
    private static final String DB_HOST     = "localhost";
    private static final String DB_PORT     = "3306";
    private static final String DB_NAME     = "medchain_db";
    private static final String DB_USER     = "root";
    private static final String DB_PASSWORD = "d@2006haranI";  // ← put your MySQL password here

    // ── Full JDBC URL ─────────────────────────────────────────────────────────
    private static final String DB_URL = "jdbc:mysql://"
            + DB_HOST + ":"
            + DB_PORT + "/"
            + DB_NAME
            + "?useSSL=false"
            + "&serverTimezone=Asia/Kolkata"
            + "&allowPublicKeyRetrieval=true"
            + "&autoReconnect=true";

    // ── Singleton Connection Instance ─────────────────────────────────────────
    private static Connection connection = null;

    /**
     * Private constructor — prevents instantiation.
     * This class should only be used via static methods.
     */
    private DBConnection() {
        throw new UnsupportedOperationException(
            "DBConnection is a utility class and cannot be instantiated."
        );
    }

    // ── Public Methods ────────────────────────────────────────────────────────

    /**
     * Returns the singleton database connection.
     * Creates a new connection if one does not exist or has been closed.
     *
     * @return  active {@link Connection} object
     * @throws  SQLException if connection cannot be established
     */
    public static Connection getConnection() throws SQLException {
        try {
            if (connection == null || connection.isClosed()) {
                // Load MySQL JDBC driver explicitly
                Class.forName("com.mysql.cj.jdbc.Driver");

                // Establish connection
                connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
                LOGGER.info("✅ Database connection established successfully.");
            }
        } catch (ClassNotFoundException e) {
            LOGGER.log(Level.SEVERE, " MySQL JDBC Driver not found.", e);
            throw new SQLException("MySQL JDBC Driver not found: " + e.getMessage());
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, " Failed to connect to database.", e);
            throw new SQLException("Database connection failed: " + e.getMessage());
        }
        return connection;
    }

    /**
     * Checks whether the database connection is currently active.
     *
     * @return  true if connected, false otherwise
     */
    public static boolean isConnected() {
        try {
            return connection != null && !connection.isClosed();
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "⚠️ Error checking connection status.", e);
            return false;
        }
    }

    /**
     * Closes the active database connection safely.
     * Should be called when the application is shutting down.
     */
    public static void closeConnection() {
        if (isConnected()) {
            try {
                connection.close();
                connection = null;
                LOGGER.info("✅ Database connection closed successfully.");
            } catch (SQLException e) {
                LOGGER.log(Level.WARNING, "⚠️ Error while closing database connection.", e);
            }
        }
    }

    /**
     * Returns the database URL being used for the connection.
     * Useful for debugging connection issues.
     *
     * @return  DB URL string
     */
    public static String getDbUrl() {
        return DB_URL;
    }
}