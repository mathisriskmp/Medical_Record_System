package com.medchain.service;

import com.medchain.dao.AccessPermissionDAO;
import com.medchain.dao.AuditLogDAO;
import com.medchain.dao.DocumentDAO;
import com.medchain.model.AuditLog;
import com.medchain.model.MedicalDocument;
import com.medchain.model.User;
import com.medchain.util.HashUtil;
import com.medchain.util.SessionManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * DocumentService — Document Management Business Logic
 *
 * Handles uploading, viewing and downloading
 * medical documents for patients and doctors.
 *
 * Supported formats: PDF, JPG, PNG
 * Max file size: 10 MB
 *
 * @author  MedChain Team
 * @version 1.0
 */
public class DocumentService {

    private static final Logger LOGGER =
        Logger.getLogger(DocumentService.class.getName());

    // ── Constants ─────────────────────────────────────────────────────────
    private static final long   MAX_FILE_SIZE = 10 * 1024 * 1024; // 10 MB
    private static final String[] ALLOWED_TYPES =
        {"pdf", "jpg", "jpeg", "png"};

    // ── Dependencies ──────────────────────────────────────────────────────
    private final DocumentDAO         documentDAO = new DocumentDAO();
    private final AccessPermissionDAO accessDAO   = new AccessPermissionDAO();
    private final AuditLogDAO         auditDAO    = new AuditLogDAO();

    // ── Upload ────────────────────────────────────────────────────────────

    /**
     * Uploads a medical document to the database.
     *
     * Steps:
     * 1. Validate file type and size
     * 2. Read file bytes
     * 3. Generate SHA-256 hash of file
     * 4. Save to database
     * 5. Log the action
     *
     * @param  file          the file to upload
     * @param  patientId     patient this document belongs to
     * @param  documentType  type of document (Test Report etc)
     * @param  description   brief description
     * @return               saved MedicalDocument
     */
    public MedicalDocument uploadDocument(
            File file, int patientId,
            String documentType, String description) throws IOException {

        User uploader = SessionManager.getInstance().getCurrentUser();

        // ── Validate file ─────────────────────────────────────────────────
        if (file == null || !file.exists()) {
            throw new IllegalArgumentException("File not found.");
        }

        if (file.length() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException(
                "File too large! Maximum size is 10 MB. " +
                "Your file: " + formatSize(file.length()));
        }

        String extension = getExtension(file.getName()).toLowerCase();
        if (!isAllowedType(extension)) {
            throw new IllegalArgumentException(
                "File type not supported! " +
                "Allowed: PDF, JPG, PNG");
        }

        // ── Read file bytes ───────────────────────────────────────────────
        byte[] fileBytes = Files.readAllBytes(file.toPath());

        // ── Generate hash ─────────────────────────────────────────────────
        String fileHash = HashUtil.sha256(new String(fileBytes));

        // ── Build document object ─────────────────────────────────────────
        MedicalDocument doc = new MedicalDocument();
        doc.setPatientId(patientId);
        doc.setUploadedBy(uploader.getId());
        doc.setFileName(file.getName());
        doc.setFileType(extension.toUpperCase());
        doc.setFileSize(file.length());
        doc.setFileData(fileBytes);
        doc.setDocumentType(documentType);
        doc.setDescription(description);
        doc.setDocumentHash(fileHash);

        // Set doctor ID if uploader is a doctor
        if (uploader.getRole() == User.Role.DOCTOR) {
            doc.setDoctorId(uploader.getId());
        }

        // ── Save to database ──────────────────────────────────────────────
        int generatedId = documentDAO.save(doc);
        if (generatedId == -1) {
            throw new RuntimeException("Failed to save document.");
        }
        doc.setId(generatedId);

        // ── Log action ────────────────────────────────────────────────────
        auditDAO.log(
            uploader.getId(),
            "UPLOAD_DOCUMENT",
            uploader.getName() + " uploaded '" +
            file.getName() + "' for Patient #" + patientId
        );

        LOGGER.info("✅ Document uploaded: " + file.getName() +
                    " by " + uploader.getName());
        return doc;
    }

    // ── View / Download ───────────────────────────────────────────────────

    /**
     * Downloads a document and saves it to a local file.
     * Used when user clicks "View" or "Download".
     *
     * @param  documentId   ID of document to download
     * @param  saveLocation folder to save the file
     * @return              the saved File
     */
    public File downloadDocument(int documentId,
                                  String saveLocation) {
        Optional<MedicalDocument> docOpt =
            documentDAO.findByIdWithData(documentId);

        if (docOpt.isEmpty()) {
            throw new IllegalArgumentException(
                "Document not found.");
        }

        MedicalDocument doc = docOpt.get();

        try {
            // Save to temp location for viewing
            File outputFile = new File(
                saveLocation + File.separator + doc.getFileName());

            try (FileOutputStream fos =
                     new FileOutputStream(outputFile)) {
                fos.write(doc.getFileData());
            }

            LOGGER.info("✅ Document saved to: " +
                        outputFile.getAbsolutePath());

            // Log the view action
            int userId = SessionManager.getInstance().getCurrentUserId();
            auditDAO.log(
                userId,
                "VIEW_DOCUMENT",
                "Viewed document: " + doc.getFileName() +
                " (ID: " + documentId + ")"
            );

            return outputFile;

        } catch (IOException e) {
            LOGGER.log(Level.SEVERE,
                "❌ Error saving document file.", e);
            throw new RuntimeException(
                "Failed to save document: " + e.getMessage());
        }
    }

    /**
     * Opens a document using the system's default application.
     * PDF opens in PDF viewer, images in image viewer.
     *
     * @param  documentId  ID of document to open
     */
    public void openDocument(int documentId) {
        try {
            // Save to temp folder
            String tempDir = System.getProperty("java.io.tmpdir");
            File file = downloadDocument(documentId, tempDir);
            String filePath = file.getAbsolutePath();

            LOGGER.info("📄 Opening file: " + filePath);

            // ── Try multiple methods ──────────────────────────────────────────

            String fileType = file.getName()
                .substring(file.getName().lastIndexOf('.') + 1)
                .toLowerCase();

            if (fileType.equals("pdf")) {
                // ── Open PDF using Windows start command ──────────────────────
                Runtime.getRuntime().exec(new String[]{
                    "cmd", "/c", "start", "", "\"" + filePath + "\""
                });

            } else if (fileType.equals("jpg") ||
                       fileType.equals("jpeg") ||
                       fileType.equals("png")) {
                // ── Open Image ────────────────────────────────────────────────
                Runtime.getRuntime().exec(new String[]{
                    "cmd", "/c", "start", "", "\"" + filePath + "\""
                });

            } else {
                // ── Fallback ──────────────────────────────────────────────────
                ProcessBuilder pb = new ProcessBuilder(
                    "rundll32",
                    "url.dll,FileProtocolHandler",
                    filePath
                );
                pb.start();
            }

            LOGGER.info("✅ Opened: " + file.getName());

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "❌ Error opening document.", e);
            throw new RuntimeException(
                "Failed to open: " + e.getMessage());
        }
    }

    // ── Fetch Documents ───────────────────────────────────────────────────

    /**
     * Returns all documents for a patient.
     */
    public List<MedicalDocument> getPatientDocuments(int patientId) {
        return documentDAO.findByPatientId(patientId);
    }

    /**
     * Returns all documents accessible to a doctor.
     */
    public List<MedicalDocument> getDoctorDocuments(int doctorId) {
        return documentDAO.findByDoctorId(doctorId);
    }

    /**
     * Returns all documents — Admin only.
     */
    public List<MedicalDocument> getAllDocuments() {
        if (!SessionManager.getInstance().isAdmin()) {
            throw new SecurityException(
                "Admin access required.");
        }
        return documentDAO.findAll();
    }

    /**
     * Deletes a document.
     * Only uploader or Admin can delete.
     */
    public void deleteDocument(int documentId) {
        int currentUserId =
            SessionManager.getInstance().getCurrentUserId();
        boolean success = documentDAO.delete(documentId);
        if (success) {
            auditDAO.log(
                currentUserId,
                "DELETE_DOCUMENT",
                "Deleted document ID: " + documentId
            );
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private String getExtension(String fileName) {
        int lastDot = fileName.lastIndexOf('.');
        if (lastDot == -1) return "";
        return fileName.substring(lastDot + 1);
    }

    private boolean isAllowedType(String extension) {
        for (String type : ALLOWED_TYPES) {
            if (type.equals(extension)) return true;
        }
        return false;
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