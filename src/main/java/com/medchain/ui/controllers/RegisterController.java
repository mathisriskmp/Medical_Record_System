package com.medchain.ui.controllers;

import com.medchain.app.Main;
import com.medchain.model.User;
import com.medchain.service.AuthService;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * RegisterController — Registration Screen Controller
 *
 * Handles new user registration for all 3 roles:
 * → PATIENT  — open registration
 * → DOCTOR   — open registration
 * → ADMIN    — requires secret admin code
 *
 * After successful registration → navigates back to Login.
 *
 * @author  MedChain Team
 * @version 1.0
 */
public class RegisterController {

    private static final Logger LOGGER =
        Logger.getLogger(RegisterController.class.getName());

    // ── Secret Admin Code ──────────────────────────────────────────────────────
    // Change this to your own secret code bruhh!
    private static final String ADMIN_SECRET_CODE = "MEDCHAIN@ADMIN2024";

    // ── FXML Fields ───────────────────────────────────────────────────────────
    @FXML private TextField     nameField;
    @FXML private TextField     emailField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private ComboBox<String> roleComboBox;
    @FXML private VBox          adminCodeBox;
    @FXML private PasswordField adminCodeField;
    @FXML private Label         errorLabel;
    @FXML private Label         successLabel;
    @FXML private Button        registerButton;

    // ── Service ───────────────────────────────────────────────────────────────
    private final AuthService authService = new AuthService();

    /**
     * Called automatically when Register.fxml loads.
     * Populates role dropdown and sets up role change listener.
     */
    @FXML
    public void initialize() {
        // ── Populate role dropdown ────────────────────────────────────────────
        roleComboBox.setItems(FXCollections.observableArrayList(
            "Patient", "Doctor", "Admin"
        ));

        // ── Show/hide admin code field based on role ───────────────────────────
        roleComboBox.setOnAction(_ -> {
            String selected = roleComboBox.getValue();
            boolean isAdmin = "Admin".equals(selected);
            adminCodeBox.setVisible(isAdmin);
            adminCodeBox.setManaged(isAdmin);
        });

        // ── Clear errors on typing ────────────────────────────────────────────
        nameField.setOnKeyTyped(_ -> clearMessages());
        emailField.setOnKeyTyped(_ -> clearMessages());
        passwordField.setOnKeyTyped(_ -> clearMessages());
        confirmPasswordField.setOnKeyTyped(_ -> clearMessages());

        LOGGER.info("✅ RegisterController initialized.");
    }

    // ── Register Handler ──────────────────────────────────────────────────────

    /**
     * Handles "Create Account" button click.
     *
     * Validates all fields → checks role → registers user →
     * shows success → navigates to login.
     */
    @FXML
    public void handleRegister() {
        // ── Get all field values ──────────────────────────────────────────────
        String name            = nameField.getText().trim();
        String email           = emailField.getText().trim();
        String password        = passwordField.getText();
        String confirmPassword = confirmPasswordField.getText();
        String roleStr         = roleComboBox.getValue();
        String adminCode       = adminCodeField.getText();

        // ── Validate all fields filled ────────────────────────────────────────
        if (name.isEmpty()) {
            showError("Please enter your full name.");
            nameField.requestFocus();
            return;
        }

        if (email.isEmpty()) {
            showError("Please enter your email address.");
            emailField.requestFocus();
            return;
        }

        if (!isValidEmail(email)) {
            showError("Please enter a valid email address.");
            emailField.requestFocus();
            return;
        }

        if (password.isEmpty()) {
            showError("Please enter a password.");
            passwordField.requestFocus();
            return;
        }

        if (!password.equals(confirmPassword)) {
            showError("Passwords do not match. Please try again.");
            confirmPasswordField.requestFocus();
            return;
        }

        if (roleStr == null || roleStr.isEmpty()) {
            showError("Please select your role.");
            roleComboBox.requestFocus();
            return;
        }

        // ── Admin code check ──────────────────────────────────────────────────
        if ("Admin".equals(roleStr)) {
            if (adminCode.isEmpty()) {
                showError("Admin secret code is required.");
                adminCodeField.requestFocus();
                return;
            }
            if (!adminCode.equals(ADMIN_SECRET_CODE)) {
                showError("Invalid admin secret code. Contact your system administrator.");
                adminCodeField.clear();
                adminCodeField.requestFocus();
                return;
            }
        }

        // ── Convert role string to enum ───────────────────────────────────────
        User.Role role = switch (roleStr) {
            case "Patient" -> User.Role.PATIENT;
            case "Doctor"  -> User.Role.DOCTOR;
            case "Admin"   -> User.Role.ADMIN;
            default        -> null;
        };

        if (role == null) {
            showError("Invalid role selected.");
            return;
        }

        // ── Disable button during registration ────────────────────────────────
        registerButton.setDisable(true);
        registerButton.setText("Creating account...");

        // ── Register user ─────────────────────────────────────────────────────
        try {
            User newUser = authService.register(name, email, password, role);

            // ── Success! ──────────────────────────────────────────────────────
            showSuccess("✅ Account created successfully! " +
                        "Welcome, " + newUser.getDisplayName() + "!");

            LOGGER.info("✅ New user registered: " +
                        newUser.getDisplayName() + " [" + role + "]");

            // ── Navigate to login after 2 seconds ─────────────────────────────
            new Thread(() -> {
                try {
                    Thread.sleep(2000);
                    javafx.application.Platform.runLater(() ->
                        Main.loadScene("/fxml/Login.fxml",
                            Main.LOGIN_WIDTH,
                            Main.LOGIN_HEIGHT)
                    );
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }).start();

        } catch (IllegalArgumentException e) {
            // Validation error from service
            showError(e.getMessage());
            registerButton.setDisable(false);
            registerButton.setText("Create Account");

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "❌ Registration error.", e);
            showError("Registration failed. Please check your connection.");
            registerButton.setDisable(false);
            registerButton.setText("Create Account");
        }
    }

    // ── Navigation ────────────────────────────────────────────────────────────

    /**
     * Navigates back to Login screen.
     */
    @FXML
    public void handleBackToLogin() {
        Main.loadScene("/fxml/Login.fxml",
            Main.LOGIN_WIDTH,
            Main.LOGIN_HEIGHT);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Basic email format validation.
     */
    private boolean isValidEmail(String email) {
        return email.contains("@") && email.contains(".");
    }

    private void showError(String message) {
        errorLabel.setText("⚠ " + message);
        errorLabel.setVisible(true);
        successLabel.setText("");
        successLabel.setVisible(false);
    }

    private void showSuccess(String message) {
        successLabel.setText(message);
        successLabel.setVisible(true);
        errorLabel.setText("");
        errorLabel.setVisible(false);
    }

    private void clearMessages() {
        errorLabel.setText("");
        errorLabel.setVisible(false);
        successLabel.setText("");
        successLabel.setVisible(false);
    }
}
