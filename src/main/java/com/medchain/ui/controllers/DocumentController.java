package com.medchain.ui.controllers;

import com.medchain.app.Main;
import com.medchain.model.MedicalDocument;
import com.medchain.model.User;
import com.medchain.service.DocumentService;
import com.medchain.service.UserService;
import com.medchain.util.SessionManager;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * DocumentController — Document Upload/View Screen Controller
 *
 * @author  MedChain Team
 * @version 2.0
 */
public class DocumentController {

    private static final Logger LOGGER =
        Logger.getLogger(DocumentController.class.getName());

    // ── FXML Fields ───────────────────────────────────────────────────────
    @FXML private Label    welcomeLabel;
    @FXML private Label    statusLabel;

    // Upload section
    @FXML private Label            selectedFileLabel;
    @FXML private ComboBox<String> documentTypeCombo;
    @FXML private TextField        descriptionField;
    @FXML private Button           uploadButton;

    // Patient combo — only shown for DOCTOR role
    @FXML private VBox             patientSelectBox;
    @FXML private ComboBox<User>   patientComboBox;

    // Documents table
    @FXML private TableView<MedicalDocument>           documentsTable;
    @FXML private TableColumn<MedicalDocument, String> colFileName;
    @FXML private TableColumn<MedicalDocument, String> colFileType;
    @FXML private TableColumn<MedicalDocument, String> colDocType;
    @FXML private TableColumn<MedicalDocument, String> colPatient;
    @FXML private TableColumn<MedicalDocument, String> colUploadedBy;
    @FXML private TableColumn<MedicalDocument, String> colSize;
    @FXML private TableColumn<MedicalDocument, String> colDate;

    // ── State ─────────────────────────────────────────────────────────────
    private File              selectedFile;
    private final DocumentService documentService = new DocumentService();
    private final UserService     userService     = new UserService();
    private User              currentUser;

    @FXML
    public void initialize() {
        currentUser = SessionManager.getInstance().getCurrentUser();
        welcomeLabel.setText("Documents — " + currentUser.getName());

        setupTable();
        setupDocumentTypes();
        setupRoleBasedUI();
        loadDocuments();

        // ── Double click to open document ─────────────────────────────────
        documentsTable.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                handleViewDocument();
            }
        });

        LOGGER.info("✅ DocumentController initialized.");
    }

    // ── Role Based UI ─────────────────────────────────────────────────────

    /**
     * Shows or hides patient selector based on role.
     * Patient → hide patient selector (they upload for themselves)
     * Doctor  → show patient selector
     * Admin   → hide patient selector
     */
    private void setupRoleBasedUI() {
        boolean isDoctor = currentUser.getRole() == User.Role.DOCTOR;

        // Show patient combobox ONLY for doctors
        patientSelectBox.setVisible(isDoctor);
        patientSelectBox.setManaged(isDoctor);

        // Load patients into combobox if doctor
        if (isDoctor) {
            try {
                List<User> patients = userService.getAllPatients();
                patientComboBox.setItems(
                    FXCollections.observableArrayList(patients));
            } catch (Exception e) {
                LOGGER.log(Level.WARNING,
                    "⚠️ Could not load patients.", e);
            }
        }
    }

    // ── Table Setup ───────────────────────────────────────────────────────

    private void setupTable() {
        colFileName.setCellValueFactory(
            new PropertyValueFactory<>("fileName"));
        colFileType.setCellValueFactory(
            new PropertyValueFactory<>("fileType"));
        colDocType.setCellValueFactory(
            new PropertyValueFactory<>("documentType"));
        colPatient.setCellValueFactory(
            new PropertyValueFactory<>("patientName"));
        colUploadedBy.setCellValueFactory(
            new PropertyValueFactory<>("uploadedByName"));
        colSize.setCellValueFactory(
            new PropertyValueFactory<>("fileSizeDisplay"));
        colDate.setCellValueFactory(
            new PropertyValueFactory<>("uploadedAt"));

        documentsTable.setPlaceholder(
            new Label("No documents uploaded yet."));
    }

    private void setupDocumentTypes() {
        documentTypeCombo.setItems(
            FXCollections.observableArrayList(
                MedicalDocument.TYPE_TEST_REPORT,
                MedicalDocument.TYPE_PRESCRIPTION,
                MedicalDocument.TYPE_SCAN,
                MedicalDocument.TYPE_DISCHARGE,
                MedicalDocument.TYPE_OTHER
            )
        );
    }

    // ── Data Loading ──────────────────────────────────────────────────────

    private void loadDocuments() {
        try {
            List<MedicalDocument> docs;

            switch (currentUser.getRole()) {
                case PATIENT ->
                    docs = documentService.getPatientDocuments(
                        currentUser.getId());
                case DOCTOR ->
                    docs = documentService.getDoctorDocuments(
                        currentUser.getId());
                default ->
                    docs = documentService.getAllDocuments();
            }

            documentsTable.setItems(
                FXCollections.observableArrayList(docs));

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "❌ Error loading docs.", e);
            showStatus("Error loading: " + e.getMessage(), true);
        }
    }

    // ── Button Handlers ───────────────────────────────────────────────────

    /**
     * Opens file chooser dialog.
     */
    @FXML
    public void handleSelectFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Medical Document");
        fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter(
                "Medical Documents",
                "*.pdf", "*.jpg", "*.jpeg", "*.png"),
            new FileChooser.ExtensionFilter("PDF Files", "*.pdf"),
            new FileChooser.ExtensionFilter(
                "Image Files", "*.jpg", "*.jpeg", "*.png")
        );

        Stage stage = (Stage) uploadButton.getScene().getWindow();
        selectedFile = fileChooser.showOpenDialog(stage);

        if (selectedFile != null) {
            selectedFileLabel.setText(
                "📄 " + selectedFile.getName() +
                " (" + formatSize(selectedFile.length()) + ")");
            showStatus("File selected: " +
                       selectedFile.getName(), false);
        }
    }

    /**
     * Uploads selected document to database.
     */
    @FXML
    public void handleUpload() {
        // ── Validate file selected ────────────────────────────────────────
        if (selectedFile == null) {
            showStatus("Please select a file first!", true);
            return;
        }

        // ── Validate document type ────────────────────────────────────────
        String docType = documentTypeCombo.getValue();
        if (docType == null) {
            showStatus("Please select document type!", true);
            return;
        }

        // ── Get patient ID based on role ──────────────────────────────────
        int patientId;
        if (currentUser.getRole() == User.Role.PATIENT) {
            // Patient uploads for themselves
            patientId = currentUser.getId();

        } else if (currentUser.getRole() == User.Role.DOCTOR) {
            // Doctor must select a patient
            User selectedPatient = patientComboBox.getValue();
            if (selectedPatient == null) {
                showStatus("Please select a patient!", true);
                return;
            }
            patientId = selectedPatient.getId();

        } else {
            showStatus("Only patients and doctors can upload.", true);
            return;
        }

        // ── Upload ────────────────────────────────────────────────────────
        try {
            uploadButton.setDisable(true);
            uploadButton.setText("Uploading...");

            MedicalDocument saved = documentService.uploadDocument(
                selectedFile,
                patientId,
                docType,
                descriptionField.getText().trim()
            );

            showStatus("✅ Uploaded: " + saved.getFileName() +
                       " (ID: " + saved.getId() + ")", false);

            // Reset form
            selectedFile = null;
            selectedFileLabel.setText("No file selected");
            documentTypeCombo.setValue(null);
            descriptionField.clear();
            if (currentUser.getRole() == User.Role.DOCTOR) {
                patientComboBox.setValue(null);
            }

            loadDocuments();

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "❌ Upload error.", e);
            showStatus("Upload failed: " + e.getMessage(), true);
        } finally {
            uploadButton.setDisable(false);
            uploadButton.setText("⬆ Upload Document");
        }
    }

    /**
     * Opens selected document.
     * Works on both button click and double click!
     */
    @FXML
    public void handleViewDocument() {
        MedicalDocument selected =
            documentsTable.getSelectionModel().getSelectedItem();

        if (selected == null) {
            showStatus("Select a document or double-click to open.", false);
            return;
        }

        try {
            showStatus("Opening " + selected.getFileName() + "...", false);
            documentService.openDocument(selected.getId());
            showStatus("✅ Opened: " + selected.getFileName(), false);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "❌ View error.", e);
            showStatus("Failed to open: " + e.getMessage(), true);
        }
    }

    /**
     * Deletes selected document with confirmation.
     */
    @FXML
    public void handleDeleteDocument() {
        MedicalDocument selected =
            documentsTable.getSelectionModel().getSelectedItem();

        if (selected == null) {
            showStatus("Please select a document to delete.", true);
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete Document");
        confirm.setHeaderText("Delete " + selected.getFileName() + "?");
        confirm.setContentText(
            "This action cannot be undone!");

        confirm.showAndWait().ifPresent(result -> {
            if (result == ButtonType.OK) {
                try {
                    documentService.deleteDocument(selected.getId());
                    loadDocuments();
                    showStatus("✅ Document deleted.", false);
                } catch (Exception e) {
                    showStatus("Error: " + e.getMessage(), true);
                }
            }
        });
    }

    /**
     * Refreshes document list.
     */
    @FXML
    public void handleRefresh() {
        loadDocuments();
        showStatus("Refreshed.", false);
    }

    /**
     * Goes back to the correct dashboard based on role.
     */
    @FXML
    public void handleBack() {
        switch (currentUser.getRole()) {
            case PATIENT ->
                Main.loadScene("/fxml/PatientDashboard.fxml",
                    Main.DASHBOARD_WIDTH, Main.DASHBOARD_HEIGHT);
            case DOCTOR ->
                Main.loadScene("/fxml/DoctorDashboard.fxml",
                    Main.DASHBOARD_WIDTH, Main.DASHBOARD_HEIGHT);
            case ADMIN ->
                Main.loadScene("/fxml/AdminDashboard.fxml",
                    Main.DASHBOARD_WIDTH, Main.DASHBOARD_HEIGHT);
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private void showStatus(String message, boolean isError) {
        statusLabel.setText(message);
        statusLabel.setStyle(isError
            ? "-fx-text-fill: #dc2626;"
            : "-fx-text-fill: #059669;");
    }

    private String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        else if (bytes < 1024 * 1024)
            return String.format("%.1f KB", bytes / 1024.0);
        else
            return String.format("%.1f MB",
                bytes / (1024.0 * 1024));
    }
}