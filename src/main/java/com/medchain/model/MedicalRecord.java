package com.medchain.model;

import java.time.LocalDateTime;

/**
 * MedicalRecord — Domain Model
 *
 * Represents a single medical record in the MedChain system.
 * Each record is linked to a patient and a doctor, contains
 * medical data, and is secured by a SHA-256 hash stored in
 * the blockchain simulation.
 *
 * Maps to the 'medical_records' table in medchain_db.
 *
 * @author  MedChain Team
 * @version 1.0
 */
public class MedicalRecord {

    // ── Fields ────────────────────────────────────────────────────────────────
    private int           id;
    private int           patientId;
    private int           doctorId;
    private String        patientName;    // joined from users table
    private String        doctorName;     // joined from users table
    private String        diagnosis;
    private String        prescription;
    private String        notes;
    private String        recordHash;     // SHA-256 hash → tamper detection
    private int           blockIndex;     // position in blockchain
    private String        previousHash;// hash of previous block
    private String blockHash;
    private LocalDateTime createdAt;

    // ── Constructors ──────────────────────────────────────────────────────────

    /** Default constructor */
    public MedicalRecord() {}

    /**
     * Full constructor — used when loading from database.
     */
    public MedicalRecord(int id, int patientId, int doctorId,
                         String diagnosis, String prescription, String notes,
                         String recordHash, int blockIndex, String previousHash,
                         LocalDateTime createdAt) {
        this.id           = id;
        this.patientId    = patientId;
        this.doctorId     = doctorId;
        this.diagnosis    = diagnosis;
        this.prescription = prescription;
        this.notes        = notes;
        this.recordHash   = recordHash;
        this.blockIndex   = blockIndex;
        this.previousHash = previousHash;
        this.createdAt    = createdAt;
    }

    // ── Getters & Setters ─────────────────────────────────────────────────────
    public int getId()                          { return id; }
    public void setId(int id)                   { this.id = id; }

    public int getPatientId()                   { return patientId; }
    public void setPatientId(int patientId)     { this.patientId = patientId; }

    public int getDoctorId()                    { return doctorId; }
    public void setDoctorId(int doctorId)       { this.doctorId = doctorId; }

    public String getPatientName()                      { return patientName; }
    public void setPatientName(String patientName)      { this.patientName = patientName; }

    public String getDoctorName()                       { return doctorName; }
    public void setDoctorName(String doctorName)        { this.doctorName = doctorName; }

    public String getDiagnosis()                        { return diagnosis; }
    public void setDiagnosis(String diagnosis)          { this.diagnosis = diagnosis; }
    public String getBlockHash() { return blockHash; }
    public void setBlockHash(String blockHash) { this.blockHash = blockHash; }
    public String getPrescription()                     { return prescription; }
    public void setPrescription(String prescription)    { this.prescription = prescription; }

    public String getNotes()                            { return notes; }
    public void setNotes(String notes)                  { this.notes = notes; }

    public String getRecordHash()                       { return recordHash; }
    public void setRecordHash(String recordHash)        { this.recordHash = recordHash; }

    public int getBlockIndex()                          { return blockIndex; }
    public void setBlockIndex(int blockIndex)           { this.blockIndex = blockIndex; }

    public String getPreviousHash()                     { return previousHash; }
    public void setPreviousHash(String previousHash)    { this.previousHash = previousHash; }

    public LocalDateTime getCreatedAt()                 { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt)   { this.createdAt = createdAt; }

    // ── Utility ───────────────────────────────────────────────────────────────

    @Override
    public String toString() {
        return "MedicalRecord{id=" + id
             + ", patient=" + patientName
             + ", doctor="  + doctorName
             + ", hash="    + (recordHash != null ? recordHash.substring(0, 16) + "..." : "null")
             + "}";
    }
}
