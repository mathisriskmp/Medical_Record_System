package com.medchain.ui.controllers;

import com.medchain.app.Main;
import com.medchain.model.AuditLog;
import com.medchain.model.User;
import com.medchain.service.AuditService;
import com.medchain.service.AuthService;
import com.medchain.service.EmergencyAccessService;
import com.medchain.service.EmergencyAccessService.EmergencyLog;
import com.medchain.service.MedicalRecordService;
import com.medchain.service.UserService;
import com.medchain.util.SessionManager;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * AdminDashboardController — Admin Dashboard Screen Controller
 *
 * Controls the AdminDashboard.fxml screen.
 *
 * Features:
 * → View and manage all system users
 * → View complete audit log trail
 * → View emergency access logs
 * → System-wide statistics
 *
 * @author  MedChain Team
 * @version 1.0
 */
public class AdminDashboardController {

    private static final Logger LOGGER =
        Logger.getLogger(AdminDashboardController.class.getName());

    // ── FXML Fields ───────────────────────────────────────────────────────────
    @FXML private Label welcomeLabel;
    @FXML private Label statusLabel;

    // Stats
    @FXML private Label totalUsersLabel;
    @FXML private Label totalPatientsLabel;
    @FXML private Label totalDoctorsLabel;
    @FXML private Label totalRecordsLabel;
    @FXML private Label chainStatusLabel;

    // Users table
    @FXML private TableView<User>              usersTable;
    @FXML private TableColumn<User, Integer>   colUserId;
    @FXML private TableColumn<User, String>    colUserName;
    @FXML private TableColumn<User, String>    colUserEmail;
    @FXML private TableColumn<User, String>    colUserRole;
    @FXML private TableColumn<User, String>    colUserCreated;

    // Audit table
    @FXML private TableView<AuditLog>              auditTable;
    @FXML private TableColumn<AuditLog, String>    colAuditUser;
    @FXML private TableColumn<AuditLog, String>    colAuditAction;
    @FXML private TableColumn<AuditLog, String>    colAuditDetails;
    @FXML private TableColumn<AuditLog, String>    colAuditTime;

    // Emergency table
    @FXML private TableView<EmergencyLog>           emergencyTable;
    @FXML private TableColumn<EmergencyLog, String> colEmgDoctor;
    @FXML private TableColumn<EmergencyLog, String> colEmgPatient;
    @FXML private TableColumn<EmergencyLog, String> colEmgReason;
    @FXML private TableColumn<EmergencyLog, String> colEmgTime;


    // ── Services ──────────────────────────────────────────────────────────────
    private final UserService            userService      =
        new UserService();
    private final AuditService           auditService     =
        new AuditService();
    private final EmergencyAccessService emergencyService =
        new EmergencyAccessService();
    private final MedicalRecordService   recordService    =
        new MedicalRecordService();
    private final AuthService            authService      =
        new AuthService();

    private User currentUser;

    /**
     * Called automatically when AdminDashboard.fxml loads.
     */
    @FXML
    public void initialize() {
        currentUser = SessionManager.getInstance().getCurrentUser();
        welcomeLabel.setText("Admin: " + currentUser.getName());

        setupTables();
        loadAll();

        LOGGER.info("✅ AdminDashboard initialized.");
    }

    // ── Table Setup ───────────────────────────────────────────────────────────

    private void setupTables() {
        // Users table
        colUserId.setCellValueFactory(
            new PropertyValueFactory<>("id"));
        colUserName.setCellValueFactory(
            new PropertyValueFactory<>("name"));
        colUserEmail.setCellValueFactory(
            new PropertyValueFactory<>("email"));
        colUserRole.setCellValueFactory(
            new PropertyValueFactory<>("role"));
        colUserCreated.setCellValueFactory(
            new PropertyValueFactory<>("createdAt"));
        usersTable.setPlaceholder(new Label("No users found."));

        // Audit table
        colAuditUser.setCellValueFactory(
            new PropertyValueFactory<>("userName"));
        colAuditAction.setCellValueFactory(
            new PropertyValueFactory<>("action"));
        colAuditDetails.setCellValueFactory(
            new PropertyValueFactory<>("details"));
        colAuditTime.setCellValueFactory(
            new PropertyValueFactory<>("timestamp"));
        auditTable.setPlaceholder(new Label("No audit logs found."));

        // Emergency table
        colEmgDoctor.setCellValueFactory(
            new PropertyValueFactory<>("doctorName"));
        colEmgPatient.setCellValueFactory(
            new PropertyValueFactory<>("patientName"));
        colEmgReason.setCellValueFactory(
            new PropertyValueFactory<>("reason"));
        colEmgTime.setCellValueFactory(
            new PropertyValueFactory<>("accessedAt"));
        emergencyTable.setPlaceholder(
            new Label("No emergency access logs."));
    }

    // ── Data Loading ──────────────────────────────────────────────────────────

    /**
     * Loads all data for admin dashboard.
     */
    private void loadAll() {
        loadUsers();
        loadAuditLogs();
        loadEmergencyLogs();
        updateStats();
    }

    private void loadUsers() {
        try {
            List<User> users = userService.getAllUsers();
            usersTable.setItems(FXCollections.observableArrayList(users));
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "❌ Error loading users.", e);
            showStatus("Error loading users: " + e.getMessage(), true);
        }
    }

    private void loadAuditLogs() {
        try {
            List<AuditLog> logs = auditService.getAllLogs();
            auditTable.setItems(FXCollections.observableArrayList(logs));
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "❌ Error loading audit logs.", e);
        }
    }

    private void loadEmergencyLogs() {
        try {
            List<EmergencyLog> logs =
                emergencyService.getAllEmergencyLogs();
            emergencyTable.setItems(
                FXCollections.observableArrayList(logs));
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE,
                "❌ Error loading emergency logs.", e);
        }
    }

    private void updateStats() {
        try {
            int[] stats = userService.getUserStats();
            totalUsersLabel.setText(String.valueOf(stats[0]));
            totalPatientsLabel.setText(String.valueOf(stats[1]));
            totalDoctorsLabel.setText(String.valueOf(stats[2]));
            totalRecordsLabel.setText(
                String.valueOf(recordService.getAllRecords().size()));

            boolean chainValid = recordService.validateBlockchain();
            chainStatusLabel.setText(chainValid ? "✅ Valid" : "❌ Broken!");
            chainStatusLabel.setStyle(chainValid
                ? "-fx-text-fill: #059669;"
                : "-fx-text-fill: #dc2626;");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "❌ Error updating stats.", e);
        }
    }

    // ── Button Handlers ───────────────────────────────────────────────────────

    /**
     * Deletes selected user from system.
     * Shows confirmation dialog first.
     */
    @FXML
    public void handleDeleteUser() {
        User selected = usersTable.getSelectionModel().getSelectedItem();

        if (selected == null) {
            showStatus("Please select a user to delete.", false);
            return;
        }

        // ── Confirmation dialog ───────────────────────────────────────────────
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete User");
        confirm.setHeaderText("Delete " + selected.getName() + "?");
        confirm.setContentText(
            "This will permanently delete the user and all their data. " +
            "This cannot be undone!");

        confirm.showAndWait().ifPresent(result -> {
            if (result == ButtonType.OK) {
                try {
                    userService.deleteUser(selected.getId());
                    loadAll();
                    showStatus("✅ User " + selected.getName() +
                               " deleted successfully.", false);
                } catch (Exception e) {
                    LOGGER.log(Level.SEVERE, "❌ Delete error.", e);
                    showStatus("Error: " + e.getMessage(), true);
                }
            }
        });
    }

    /**
     * Refreshes all dashboard data.
     */
    @FXML
    public void handleRefreshAll() {
        loadAll();
        showStatus("✅ All data refreshed.", false);
    }

    /**
     * Handles Sign Out.
     */
    @FXML
    public void handleLogout() {
        authService.logout();
        Main.loadScene("/fxml/Login.fxml",
            Main.LOGIN_WIDTH, Main.LOGIN_HEIGHT);
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private void showStatus(String message, boolean isError) {
        statusLabel.setText(message);
        statusLabel.setStyle(isError
            ? "-fx-text-fill: #dc2626;"
            : "-fx-text-fill: #059669;");
    }
}
