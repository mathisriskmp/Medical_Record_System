package com.medchain.model;

import java.time.LocalDateTime;

/**
 * MedicalDocument — Domain Model
 *
 * Represents an uploaded medical document in MedChain.
 * Supports PDF and image files (JPG, PNG).
 * File content stored as byte array in MySQL LONGBLOB.
 *
 * Maps to the 'medical_documents' table in medchain_db.
 *
 * @author  MedChain Team
 * @version 1.0
 */
public class MedicalDocument {

    // ── Document Type Constants ───────────────────────────────────────────
    public static final String TYPE_TEST_REPORT  = "Test Report";
    public static final String TYPE_PRESCRIPTION = "Prescription";
    public static final String TYPE_SCAN         = "Scan / X-Ray";
    public static final String TYPE_DISCHARGE    = "Discharge Summary";
    public static final String TYPE_OTHER        = "Other";

    // ── File Type Constants ───────────────────────────────────────────────
    public static final String FILE_PDF  = "PDF";
    public static final String FILE_JPG  = "JPG";
    public static final String FILE_PNG  = "PNG";

    // ── Fields ────────────────────────────────────────────────────────────
    private int           id;
    private Integer       recordId;       // nullable
    private int           patientId;
    private Integer       doctorId;       // nullable
    private int           uploadedBy;
    private String        uploadedByName; // joined from users
    private String        patientName;   // joined from users
    private String        doctorName;    // joined from users
    private String        fileName;
    private String        fileType;
    private long          fileSize;
    private byte[]        fileData;      // actual file bytes
    private String        documentType;
    private String        description;
    private String        documentHash;  // SHA-256 for tamper detection
    private LocalDateTime uploadedAt;

    // ── Constructors ──────────────────────────────────────────────────────
    public MedicalDocument() {}

    // ── Getters & Setters ─────────────────────────────────────────────────
    public int     getId()                        { return id; }
    public void    setId(int id)                  { this.id = id; }

    public Integer getRecordId()                  { return recordId; }
    public void    setRecordId(Integer recordId)  { this.recordId = recordId; }

    public int     getPatientId()                 { return patientId; }
    public void    setPatientId(int patientId)    { this.patientId = patientId; }

    public Integer getDoctorId()                  { return doctorId; }
    public void    setDoctorId(Integer doctorId)  { this.doctorId = doctorId; }

    public int     getUploadedBy()                { return uploadedBy; }
    public void    setUploadedBy(int uploadedBy)  { this.uploadedBy = uploadedBy; }

    public String  getUploadedByName()                        { return uploadedByName; }
    public void    setUploadedByName(String uploadedByName)   { this.uploadedByName = uploadedByName; }

    public String  getPatientName()               { return patientName; }
    public void    setPatientName(String n)       { this.patientName = n; }

    public String  getDoctorName()                { return doctorName; }
    public void    setDoctorName(String n)        { this.doctorName = n; }

    public String  getFileName()                  { return fileName; }
    public void    setFileName(String fileName)   { this.fileName = fileName; }

    public String  getFileType()                  { return fileType; }
    public void    setFileType(String fileType)   { this.fileType = fileType; }

    public long    getFileSize()                  { return fileSize; }
    public void    setFileSize(long fileSize)     { this.fileSize = fileSize; }

    public byte[]  getFileData()                  { return fileData; }
    public void    setFileData(byte[] fileData)   { this.fileData = fileData; }

    public String  getDocumentType()                          { return documentType; }
    public void    setDocumentType(String documentType)       { this.documentType = documentType; }

    public String  getDescription()               { return description; }
    public void    setDescription(String d)       { this.description = d; }

    public String  getDocumentHash()                          { return documentHash; }
    public void    setDocumentHash(String documentHash)       { this.documentHash = documentHash; }

    public LocalDateTime getUploadedAt()                      { return uploadedAt; }
    public void          setUploadedAt(LocalDateTime t)       { this.uploadedAt = t; }

    // ── Utility ───────────────────────────────────────────────────────────

    /**
     * Returns file size in human-readable format.
     * e.g. "1.2 MB", "450 KB"
     */
    public String getFileSizeDisplay() {
        if (fileSize < 1024) return fileSize + " B";
        else if (fileSize < 1024 * 1024)
            return String.format("%.1f KB", fileSize / 1024.0);
        else
            return String.format("%.1f MB", fileSize / (1024.0 * 1024));
    }

    @Override
    public String toString() {
        return "MedicalDocument{id=" + id +
               ", file=" + fileName +
               ", type=" + documentType +
               ", patient=" + patientName + "}";
    }
}