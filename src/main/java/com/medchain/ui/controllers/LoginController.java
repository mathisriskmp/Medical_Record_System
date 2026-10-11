package com.medchain.ui.controllers;

import com.medchain.app.Main;
import com.medchain.model.User;
import com.medchain.service.AuthService;
//import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * LoginController — Login Screen Controller
 *
 * Controls the Login.fxml screen.
 * Handles user authentication and routes to the
 * correct dashboard based on user role.
 *
 * Routing:
 * → PATIENT → PatientDashboard.fxml
 * → DOCTOR  → DoctorDashboard.fxml
 * → ADMIN   → AdminDashboard.fxml
 *
 * @author  MedChain Team
 * @version 1.0
 */
public class LoginController {

    private static final Logger LOGGER =
        Logger.getLogger(LoginController.class.getName());

    // ── FXML Injected Fields ──────────────────────────────────────────────────
    // These @FXML fields automatically connect to elements in Login.fxml
    @FXML private TextField     emailField;
    @FXML private PasswordField passwordField;
    @FXML private Button        loginButton;
    @FXML private Label         errorLabel;
    @FXML private Label         loadingLabel;

    // ── Service ───────────────────────────────────────────────────────────────
    private final AuthService authService = new AuthService();

    /**
     * Called automatically by JavaFX after FXML loads.
     * Sets up keyboard shortcuts and initial state.
     */
    @FXML
    public void initialize() {
        // Clear error label initially
        errorLabel.setText("");
        loadingLabel.setVisible(false);

        // Allow pressing Enter key to trigger login
        passwordField.setOnKeyPressed(this::handleEnterKey);
        emailField.setOnKeyPressed(this::handleEnterKey);

        LOGGER.info("✅ LoginController initialized.");
    }

    // ── Login Handler ─────────────────────────────────────────────────────────

    /**
     * Handles the Sign In button click.
     * Validates input, authenticates user,
     * and navigates to correct dashboard.
     */
    @FXML
    public void handleLogin() {
        // ── Get input values ──────────────────────────────────────────────────
        String email    = emailField.getText().trim();
        String password = passwordField.getText();

        // ── Validate fields not empty ─────────────────────────────────────────
        if (email.isEmpty() || password.isEmpty()) {
            showError("Please enter your email and password.");
            return;
        }

        // ── Show loading state ────────────────────────────────────────────────
        setLoading(true);
        clearError();

        // ── Authenticate ──────────────────────────────────────────────────────
        try {
            Optional<User> userOpt = authService.login(email, password);

            if (userOpt.isEmpty()) {
                showError("Invalid email or password. Please try again.");
                setLoading(false);
                return;
            }

            // ── Route to correct dashboard ────────────────────────────────────
            User user = userOpt.get();
            navigateToDashboard(user);

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "❌ Login error.", e);
            showError("Connection error. Please check your database.");
            setLoading(false);
        }
    }

    /**
     * Routes user to their role-specific dashboard.
     *
     * @param  user  authenticated user
     */
    private void navigateToDashboard(User user) {
        String fxmlPath = switch (user.getRole()) {
            case PATIENT -> "/fxml/PatientDashboard.fxml";
            case DOCTOR  -> "/fxml/DoctorDashboard.fxml";
            case ADMIN   -> "/fxml/AdminDashboard.fxml";
        };

        // Update window title
        Main.getPrimaryStage().setTitle(
            "MedChain — " + user.getDisplayName() +
            " [" + user.getRole().getDisplayName() + "]"
        );

        // Navigate to dashboard
        Main.loadScene(fxmlPath,
            Main.DASHBOARD_WIDTH,
            Main.DASHBOARD_HEIGHT);

        LOGGER.info("✅ Navigated to: " + fxmlPath);
    }

    // ── Keyboard Handler ──────────────────────────────────────────────────────

    /**
     * Handles Enter key press on email/password fields.
     */
    private void handleEnterKey(KeyEvent event) {
        if (event.getCode() == KeyCode.ENTER) {
            handleLogin();
        }
    }

    // ── UI Helpers ────────────────────────────────────────────────────────────
    @FXML
    public void handleGoToRegister() {
        Main.loadScene("/fxml/Register.fxml",
            Main.LOGIN_WIDTH,
            650);
    }
    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
    }

    private void clearError() {
        errorLabel.setText("");
        errorLabel.setVisible(false);
    }

    private void setLoading(boolean loading) {
        loginButton.setDisable(loading);
        loadingLabel.setVisible(loading);
        loadingLabel.setText(loading ? "Signing in..." : "");
    }
}
