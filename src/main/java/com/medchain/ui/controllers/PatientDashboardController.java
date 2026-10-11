package com.medchain.ui.controllers;

import com.medchain.app.Main;
import com.medchain.model.AccessPermission;
import com.medchain.model.MedicalRecord;
import com.medchain.model.User;
import com.medchain.service.AccessService;
import com.medchain.service.AuthService;
import com.medchain.service.MedicalRecordService;
import com.medchain.util.SessionManager;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * PatientDashboardController — Patient Dashboard Screen Controller
 *
 * Controls the PatientDashboard.fxml screen.
 *
 * Features:
 * → View all personal medical records
 * → Verify record integrity against blockchain
 * → Grant/Revoke doctor access permissions
 * → View blockchain chain status
 *
 * @author  MedChain Team
 * @version 1.0
 */
public class PatientDashboardController {

    private static final Logger LOGGER =
        Logger.getLogger(PatientDashboardController.class.getName());

    // ── FXML Fields — Records Table ───────────────────────────────────────────
    @FXML private Label   welcomeLabel;
    @FXML private Label   totalRecordsLabel;
    @FXML private Label   chainStatusLabel;
    @FXML private Label   doctorCountLabel;

    @FXML private TableView<MedicalRecord>           recordsTable;
    @FXML private TableColumn<MedicalRecord, String> colDoctor;
    @FXML private TableColumn<MedicalRecord, String> colDiagnosis;
    @FXML private TableColumn<MedicalRecord, String> colPrescription;
    @FXML private TableColumn<MedicalRecord, String> colDate;
    @FXML private TableColumn<MedicalRecord, String> colHash;

    // ── FXML Fields — Access Control ──────────────────────────────────────────
    @FXML private TableView<AccessPermission>              accessTable;
    @FXML private TableColumn<AccessPermission, String>    colPermDoctor;
    @FXML private TableColumn<AccessPermission, String>    colPermGranted;
    @FXML private TableColumn<AccessPermission, String>    colPermStatus;
    @FXML private ComboBox<User>                           doctorComboBox;

    // ── FXML Fields — Verify ──────────────────────────────────────────────────
    @FXML private Label  verifyStatusLabel;
    @FXML private Label  statusLabel;

    // ── Services ──────────────────────────────────────────────────────────────
    private final MedicalRecordService recordService =
        new MedicalRecordService();
    private final AccessService accessService =
        new AccessService();
    private final AuthService   authService   =
        new AuthService();

    // ── Current User ──────────────────────────────────────────────────────────
    private User currentUser;

    /**
     * Called automatically when PatientDashboard.fxml loads.
     * Initializes all tables, loads data, and sets up UI.
     */
    @FXML
    public void initialize() {
        currentUser = SessionManager.getInstance().getCurrentUser();

        // ── Welcome message ───────────────────────────────────────────────────
        welcomeLabel.setText("Welcome back, " + currentUser.getName() + "!");

        // ── Setup tables ──────────────────────────────────────────────────────
        setupRecordsTable();
        setupAccessTable();

        // ── Load data ─────────────────────────────────────────────────────────
        loadRecords();
        loadDoctors();
        loadPermissions();
        updateStats();

        LOGGER.info("✅ PatientDashboard initialized for: " +
                    currentUser.getName());
    }

    // ── Table Setup ───────────────────────────────────────────────────────────

    /**
     * Sets up the medical records table columns.
     * Maps column to MedicalRecord property using PropertyValueFactory.
     */
    private void setupRecordsTable() {
        colDoctor.setCellValueFactory(
            new PropertyValueFactory<>("doctorName"));
        colDiagnosis.setCellValueFactory(
            new PropertyValueFactory<>("diagnosis"));
        colPrescription.setCellValueFactory(
            new PropertyValueFactory<>("prescription"));
        colDate.setCellValueFactory(
            new PropertyValueFactory<>("createdAt"));
        colHash.setCellValueFactory(
            new PropertyValueFactory<>("recordHash"));

        // Show placeholder when no records
        recordsTable.setPlaceholder(
            new Label("No medical records found."));
    }

    /**
     * Sets up the access permissions table columns.
     */
    private void setupAccessTable() {
        colPermDoctor.setCellValueFactory(
            new PropertyValueFactory<>("doctorName"));
        colPermGranted.setCellValueFactory(
            new PropertyValueFactory<>("grantedAt"));
        colPermStatus.setCellValueFactory(
            new PropertyValueFactory<>("statusDisplay"));

        accessTable.setPlaceholder(
            new Label("No doctor permissions found."));
    }

    // ── Data Loading ──────────────────────────────────────────────────────────

    /**
     * Loads all medical records for current patient.
     */
    private void loadRecords() {
        try {
            List<MedicalRecord> records =
                recordService.getPatientRecords(currentUser.getId());
            recordsTable.setItems(
                FXCollections.observableArrayList(records));
            totalRecordsLabel.setText(String.valueOf(records.size()));
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "❌ Error loading records.", e);
            showStatus("Error loading records: " + e.getMessage(), true);
        }
    }

    /**
     * Loads all doctors into the ComboBox dropdown.
     */
    private void loadDoctors() {
        try {
            List<User> doctors = accessService.getAllDoctors();
            doctorComboBox.setItems(
                FXCollections.observableArrayList(doctors));
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "❌ Error loading doctors.", e);
        }
    }

    /**
     * Loads current patient's doctor permissions.
     */
    private void loadPermissions() {
        try {
            List<AccessPermission> perms =
                accessService.getMyPermissions();
            accessTable.setItems(
                FXCollections.observableArrayList(perms));

            long activeCount = perms.stream()
                .filter(AccessPermission::isActive).count();
            doctorCountLabel.setText(String.valueOf(activeCount));

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "❌ Error loading permissions.", e);
        }
    }

    /**
     * Updates the stats labels at top of dashboard.
     */
    private void updateStats() {
        try {
            boolean chainValid = recordService.validateBlockchain();
            chainStatusLabel.setText(chainValid ? "✅ Intact" : "❌ Tampered!");
            chainStatusLabel.setStyle(chainValid
                ? "-fx-text-fill: #059669;"
                : "-fx-text-fill: #dc2626;");
        } catch (Exception e) {
            chainStatusLabel.setText("Unknown");
        }
    }

    // ── Button Handlers ───────────────────────────────────────────────────────

    /**
     * Verifies the selected record's integrity against blockchain.
     * Called when "Verify Record" button is clicked.
     */
    @FXML
    public void handleVerifyRecord() {
        MedicalRecord selected =
            recordsTable.getSelectionModel().getSelectedItem();

        if (selected == null) {
            showStatus("Please select a record to verify.", false);
            return;
        }

        try {
            boolean isValid = recordService.verifyRecord(selected.getId());

            if (isValid) {
                verifyStatusLabel.setText(
                    "✅ Record is VALID — hash matches blockchain");
                verifyStatusLabel.setStyle("-fx-text-fill: #059669;");
            } else {
                verifyStatusLabel.setText(
                    "❌ Record may be TAMPERED — hash mismatch!");
                verifyStatusLabel.setStyle("-fx-text-fill: #dc2626;");
            }

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "❌ Verification error.", e);
            showStatus("Verification failed: " + e.getMessage(), true);
        }
    }

    /**
     * Grants selected doctor access to current patient's records.
     * Called when "Grant Access" button is clicked.
     */
    @FXML
    public void handleGrantAccess() {
        User selectedDoctor = doctorComboBox.getValue();

        if (selectedDoctor == null) {
            showStatus("Please select a doctor first.", false);
            return;
        }

        try {
            accessService.grantAccess(selectedDoctor.getId());
            loadPermissions();
            doctorComboBox.setValue(null);
            showStatus("✅ Access granted to " +
                       selectedDoctor.getDisplayName(), false);
        } catch (IllegalArgumentException e) {
            showStatus(e.getMessage(), true);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "❌ Grant access error.", e);
            showStatus("Error granting access: " + e.getMessage(), true);
        }
    }

    /**
     * Revokes selected permission from access table.
     * Called when "Revoke Access" button is clicked.
     */
    @FXML
    public void handleRevokeAccess() {
        AccessPermission selected =
            accessTable.getSelectionModel().getSelectedItem();

        if (selected == null) {
            showStatus("Please select a permission to revoke.", false);
            return;
        }

        // ── Confirmation dialog ───────────────────────────────────────────────
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Revoke Access");
        confirm.setHeaderText("Revoke access from " +
                              selected.getDoctorName() + "?");
        confirm.setContentText(
            "This doctor will no longer be able to view your records.");

        confirm.showAndWait().ifPresent(result -> {
            if (result == ButtonType.OK) {
                try {
                    accessService.revokeAccess(selected.getDoctorId());
                    loadPermissions();
                    showStatus("✅ Access revoked from " +
                               selected.getDoctorName(), false);
                } catch (Exception e) {
                    LOGGER.log(Level.SEVERE, "❌ Revoke error.", e);
                    showStatus("Error: " + e.getMessage(), true);
                }
            }
        });
    }

    /**
     * Handles Sign Out button click.
     */
    @FXML
    public void handleLogout() {
        authService.logout();
        Main.loadScene("/fxml/Login.fxml",
            Main.LOGIN_WIDTH, Main.LOGIN_HEIGHT);
    }
    @FXML
    public void handleOpenDocuments() {
        Main.loadScene("/fxml/DocumentViewer.fxml",
            Main.DASHBOARD_WIDTH,
            Main.DASHBOARD_HEIGHT);
    }
    /**
     * Refreshes all data on the dashboard.
     */
    @FXML
    public void handleRefresh() {
        loadRecords();
        loadPermissions();
        updateStats();
        showStatus("Dashboard refreshed.", false);
    }

    // ── UI Helper ─────────────────────────────────────────────────────────────

    private void showStatus(String message, boolean isError) {
        statusLabel.setText(message);
        statusLabel.setStyle(isError
            ? "-fx-text-fill: #dc2626;"
            : "-fx-text-fill: #059669;");
    }
}
