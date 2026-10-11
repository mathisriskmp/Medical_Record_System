package com.medchain.ui.controllers;

import com.medchain.app.Main;
import com.medchain.model.AccessPermission;
import com.medchain.model.MedicalRecord;
import com.medchain.model.User;
import com.medchain.service.AccessService;
import com.medchain.service.AuthService;
import com.medchain.service.EmergencyAccessService;
import com.medchain.service.MedicalRecordService;
import com.medchain.util.SessionManager;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * DoctorDashboardController — Doctor Dashboard Screen Controller
 *
 * Controls the DoctorDashboard.fxml screen.
 *
 * Features:
 * → View list of patients who granted access
 * → View selected patient's medical records
 * → Add new medical record (blockchain anchored)
 * → Emergency access to any patient
 *
 * @author  MedChain Team
 * @version 1.0
 */
public class DoctorDashboardController {

    private static final Logger LOGGER =
        Logger.getLogger(DoctorDashboardController.class.getName());

    // ── FXML Fields ───────────────────────────────────────────────────────────
    @FXML private Label welcomeLabel;
    @FXML private Label statusLabel;
    @FXML private Label totalPatientsLabel;
    @FXML private Label totalRecordsLabel;

    // Patients table
    @FXML private TableView<AccessPermission>           patientsTable;
    @FXML private TableColumn<AccessPermission, String> colPatientName;
    @FXML private TableColumn<AccessPermission, String> colGrantedAt;

    // Records table
    @FXML private TableView<MedicalRecord>           recordsTable;
    @FXML private TableColumn<MedicalRecord, String> colDate;
    @FXML private TableColumn<MedicalRecord, String> colDiagnosis;
    @FXML private TableColumn<MedicalRecord, String> colPrescription;
    @FXML private TableColumn<MedicalRecord, String> colNotes;

    // Add record form
    @FXML private TextArea diagnosisField;
    @FXML private TextArea prescriptionField;
    @FXML private TextArea notesField;

    // Emergency access
    @FXML private ComboBox<User> emergencyPatientCombo;

    // ── Services ──────────────────────────────────────────────────────────────
    private final MedicalRecordService  recordService   =
        new MedicalRecordService();
    private final AccessService         accessService   =
        new AccessService();
    private final EmergencyAccessService emergencyService =
        new EmergencyAccessService();
    private final AuthService           authService     =
        new AuthService();

    // ── State ─────────────────────────────────────────────────────────────────
    private User            currentUser;
    private AccessPermission selectedPatient;

    /**
     * Called automatically when DoctorDashboard.fxml loads.
     */
    @FXML
    public void initialize() {
        currentUser = SessionManager.getInstance().getCurrentUser();
        welcomeLabel.setText("Dr. " + currentUser.getName());

        setupTables();
        loadPatients();
        loadEmergencyPatients();

        LOGGER.info("✅ DoctorDashboard initialized for: " +
                    currentUser.getDisplayName());
    }

    // ── Table Setup ───────────────────────────────────────────────────────────

    private void setupTables() {
        // Patients table
        colPatientName.setCellValueFactory(
            new PropertyValueFactory<>("patientName"));
        colGrantedAt.setCellValueFactory(
            new PropertyValueFactory<>("grantedAt"));
        patientsTable.setPlaceholder(
            new Label("No patients have granted you access yet."));

        // Records table
        colDate.setCellValueFactory(
            new PropertyValueFactory<>("createdAt"));
        colDiagnosis.setCellValueFactory(
            new PropertyValueFactory<>("diagnosis"));
        colPrescription.setCellValueFactory(
            new PropertyValueFactory<>("prescription"));
        colNotes.setCellValueFactory(
            new PropertyValueFactory<>("notes"));
        recordsTable.setPlaceholder(
            new Label("Select a patient to view records."));
    }

    // ── Data Loading ──────────────────────────────────────────────────────────

    /**
     * Loads patients who granted access to this doctor.
     */
    private void loadPatients() {
        try {
            List<AccessPermission> patients =
                accessService.getMyPatients();
            patientsTable.setItems(
                FXCollections.observableArrayList(patients));
            totalPatientsLabel.setText(
                String.valueOf(patients.size()));
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "❌ Error loading patients.", e);
            showStatus("Error loading patients: " + e.getMessage(), true);
        }
    }

    /**
     * Loads all patients into emergency ComboBox.
     */
    private void loadEmergencyPatients() {
        try {
            List<User> allPatients =
                new com.medchain.dao.UserDAO()
                    .findByRole(User.Role.PATIENT);
            emergencyPatientCombo.setItems(
                FXCollections.observableArrayList(allPatients));
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE,
                "❌ Error loading emergency patients.", e);
        }
    }

    // ── Button Handlers ───────────────────────────────────────────────────────

    /**
     * Loads records for selected patient.
     * Called when "Load Records" button is clicked.
     */
    @FXML
    public void handleLoadPatientRecords() {
        selectedPatient =
            patientsTable.getSelectionModel().getSelectedItem();

        if (selectedPatient == null) {
            showStatus("Please select a patient first.", false);
            return;
        }

        try {
            List<MedicalRecord> records =
                recordService.getPatientRecords(
                    selectedPatient.getPatientId());
            recordsTable.setItems(
                FXCollections.observableArrayList(records));
            totalRecordsLabel.setText(String.valueOf(records.size()));
            showStatus("Showing records for: " +
                       selectedPatient.getPatientName(), false);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "❌ Error loading records.", e);
            showStatus("Error: " + e.getMessage(), true);
        }
    }

    /**
     * Adds a new medical record for selected patient.
     * Called when "Save + Add to Blockchain" button is clicked.
     *
     * Validates form → calls service → refreshes table → clears form
     */
    @FXML
    public void handleAddRecord() {
        // ── Check patient selected ────────────────────────────────────────────
        if (selectedPatient == null) {
            selectedPatient =
                patientsTable.getSelectionModel().getSelectedItem();
        }
        if (selectedPatient == null) {
            showStatus("Please select a patient first.", false);
            return;
        }

        // ── Get form values ───────────────────────────────────────────────────
        String diagnosis    = diagnosisField.getText().trim();
        String prescription = prescriptionField.getText().trim();
        String notes        = notesField.getText().trim();

        // ── Validate required fields ──────────────────────────────────────────
        if (diagnosis.isEmpty()) {
            showStatus("Diagnosis cannot be empty.", true);
            diagnosisField.requestFocus();
            return;
        }

        try {
            // ── Save record ───────────────────────────────────────────────────
            MedicalRecord saved = recordService.addRecord(
                selectedPatient.getPatientId(),
                currentUser.getId(),
                diagnosis,
                prescription,
                notes
            );

            showStatus("✅ Record #" + saved.getId() +
                       " saved and anchored to blockchain!", false);

            // ── Refresh & clear ───────────────────────────────────────────────
            handleLoadPatientRecords();
            clearForm();

        } catch (SecurityException e) {
            showStatus("Access denied: " + e.getMessage(), true);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "❌ Error adding record.", e);
            showStatus("Error saving record: " + e.getMessage(), true);
        }
    }

    /**
     * Handles Emergency Access button click.
     * Shows reason input dialog and bypasses normal permission.
     */
    @FXML
    public void handleEmergencyAccess() {
        User selectedPatient = emergencyPatientCombo.getValue();

        if (selectedPatient == null) {
            showStatus("Please select a patient for emergency access.", false);
            return;
        }

        // ── Ask for reason via dialog ─────────────────────────────────────────
        TextInputDialog reasonDialog = new TextInputDialog();
        reasonDialog.setTitle("🚨 Emergency Access");
        reasonDialog.setHeaderText(
            "Emergency access for: " + selectedPatient.getName());
        reasonDialog.setContentText(
            "Reason for emergency access:");

        Optional<String> result = reasonDialog.showAndWait();
        if (result.isEmpty() || result.get().trim().isEmpty()) {
            showStatus("Emergency access cancelled — reason required.", false);
            return;
        }

        try {
            List<MedicalRecord> records =
                emergencyService.accessEmergency(
                    selectedPatient.getId(),
                    result.get().trim()
                );

            // Show records in table
            recordsTable.setItems(
                FXCollections.observableArrayList(records));

            showStatus("🚨 Emergency access granted for " +
                       selectedPatient.getName() +
                       " — this access has been logged.", false);

        } catch (IllegalArgumentException e) {
            showStatus(e.getMessage(), true);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "❌ Emergency access error.", e);
            showStatus("Error: " + e.getMessage(), true);
        }
    }

    /**
     * Handles Sign Out button.
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
     * Refreshes patient list.
     */
    @FXML
    public void handleRefresh() {
        loadPatients();
        showStatus("Refreshed.", false);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void clearForm() {
        diagnosisField.clear();
        prescriptionField.clear();
        notesField.clear();
    }

    private void showStatus(String message, boolean isError) {
        statusLabel.setText(message);
        statusLabel.setStyle(isError
            ? "-fx-text-fill: #dc2626;"
            : "-fx-text-fill: #059669;");
    }
}
