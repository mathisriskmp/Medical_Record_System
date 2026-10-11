package com.medchain.service;

import com.medchain.blockchain.Block;
import com.medchain.blockchain.Blockchain;
import com.medchain.dao.AccessPermissionDAO;
import com.medchain.dao.AuditLogDAO;
import com.medchain.dao.RecordDAO;
import com.medchain.model.AuditLog;
import com.medchain.model.MedicalRecord;
import com.medchain.util.HashUtil;
import com.medchain.util.SessionManager;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
//import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * MedicalRecordService — Medical Record Business Logic
 *
 * The most critical service in MedChain. Handles:
 * → Adding new medical records (with blockchain anchoring)
 * → Retrieving records with access control checks
 * → Verifying record integrity against blockchain
 * → Blockchain chain validation
 *
 * Every record addition triggers:
 * 1. Access permission check
 * 2. SHA-256 hash generation
 * 3. Blockchain block creation
 * 4. Database save
 * 5. Audit log entry
 *
 * @author  MedChain Team
 * @version 1.0
 */
public class MedicalRecordService {

    // ── Logger ────────────────────────────────────────────────────────────────
    private static final Logger LOGGER =
        Logger.getLogger(MedicalRecordService.class.getName());

    // ── Dependencies ──────────────────────────────────────────────────────────
    private final RecordDAO           recordDAO    = new RecordDAO();
    private final AccessPermissionDAO accessDAO    = new AccessPermissionDAO();
    private final AuditLogDAO         auditLogDAO  = new AuditLogDAO();
    private final Blockchain          blockchain   = Blockchain.getInstance();

    // ── Add Record ────────────────────────────────────────────────────────────

    /**
     * Adds a new medical record for a patient.
     *
     * This is the most important method in the entire system.
     * It coordinates 5 operations atomically:
     *
     * 1. Checks doctor has permission to add record for patient
     * 2. Generates SHA-256 hash of all record fields
     * 3. Creates a new block in the blockchain
     * 4. Saves the record + hash + block info to MySQL
     * 5. Logs the action to audit trail
     *
     * @param  patientId    ID of the patient
     * @param  doctorId     ID of the doctor adding the record
     * @param  diagnosis    diagnosis text
     * @param  prescription prescription details
     * @param  notes        additional clinical notes
     * @return              saved MedicalRecord with generated ID
     * @throws SecurityException        if doctor has no access
     * @throws IllegalArgumentException if required fields are empty
     * @throws RuntimeException         if save operation fails
     */
    public MedicalRecord addRecord(int patientId, int doctorId,
                                   String diagnosis, String prescription,
                                   String notes) {
        // ── Step 1: Check access permission ──────────────────────────────────
        if (!accessDAO.hasAccess(patientId, doctorId)) {
            LOGGER.warning("❌ Doctor #" + doctorId +
                           " attempted to add record without permission for Patient #"
                           + patientId);
            throw new SecurityException(
                "Access denied — patient has not granted you permission.");
        }

        // ── Input Validation ──────────────────────────────────────────────────
        if (diagnosis == null || diagnosis.trim().isEmpty()) {
            throw new IllegalArgumentException("Diagnosis cannot be empty.");
        }

        // ── Step 2: Generate SHA-256 hash ─────────────────────────────────────
        String timestamp = LocalDateTime.now()
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        Block  latestBlock = blockchain.getLatestBlock();
        int    blockIndex  = latestBlock.getBlockIndex() + 1;
        String prevHash    = latestBlock.getBlockHash();

     // REMOVE timestamp from hash generation
        String recordHash = HashUtil.hashRecord(
            blockIndex,
            prevHash,
            patientId,
            doctorId,
            diagnosis.trim(),
            prescription != null ? prescription.trim() : "",
            notes != null ? notes.trim() : ""
            // ← NO timestamp here!
        );

        // ── Step 3: Add block to blockchain ───────────────────────────────────
        Block newBlock = blockchain.addBlock(recordHash, 0);
        // recordId is 0 temporarily — updated after DB save

        // ── Step 4: Save to database ──────────────────────────────────────────
        MedicalRecord record = new MedicalRecord();
        record.setPatientId(patientId);
        record.setDoctorId(doctorId);
        record.setDiagnosis(diagnosis.trim());
        record.setPrescription(prescription != null ? prescription.trim() : "");
        record.setNotes(notes != null ? notes.trim() : "");
        record.setRecordHash(recordHash);
        record.setBlockIndex(newBlock.getBlockIndex());
        record.setBlockHash(newBlock.getBlockHash());
        record.setPreviousHash(newBlock.getPreviousHash());

        int generatedId = recordDAO.save(record);
        if (generatedId == -1) {
            throw new RuntimeException("Failed to save medical record to database.");
        }

        record.setId(generatedId);
        newBlock.setRecordId(generatedId);

        // ── Step 5: Log the action ────────────────────────────────────────────
        auditLogDAO.log(
            doctorId,
            AuditLog.ACTION_ADD_RECORD,
            "Dr. added record #" + generatedId +
            " for Patient #" + patientId +
            " | Block #" + newBlock.getBlockIndex()
        );

        LOGGER.info("✅ Record #" + generatedId + " added successfully " +
                    "| Block #" + newBlock.getBlockIndex());
        return record;
    }

    // ── Retrieve Records ──────────────────────────────────────────────────────

    /**
     * Returns all medical records for a patient.
     * Logs the view action to audit trail.
     *
     * @param  patientId  patient's user ID
     * @return            list of MedicalRecord objects
     */
    public List<MedicalRecord> getPatientRecords(int patientId) {
        int currentUserId = SessionManager.getInstance().getCurrentUserId();
        auditLogDAO.log(
            currentUserId,
            AuditLog.ACTION_VIEW_RECORD,
            "Viewed all records for Patient #" + patientId
        );
        return recordDAO.findByPatientId(patientId);
    }

    /**
     * Returns all records added by a specific doctor.
     *
     * @param  doctorId  doctor's user ID
     * @return           list of MedicalRecord objects
     */
    public List<MedicalRecord> getDoctorRecords(int doctorId) {
        return recordDAO.findByDoctorId(doctorId);
    }

    /**
     * Returns all records — used by Admin dashboard.
     *
     * @return  list of all MedicalRecord objects
     */
    public List<MedicalRecord> getAllRecords() {
        return recordDAO.findAll();
    }

    /**
     * Finds a single record by its ID.
     *
     * @param  recordId  record ID
     * @return           Optional containing MedicalRecord if found
     */
    public Optional<MedicalRecord> getRecordById(int recordId) {
        return recordDAO.findById(recordId);
    }

    // ── Verification ──────────────────────────────────────────────────────────

    /**
     * Verifies a medical record's integrity against the blockchain.
     *
     * How it works:
     * 1. Loads record from database
     * 2. Finds matching block in blockchain by record ID
     * 3. Compares record's stored hash with block's stored hash
     * 4. If they match → record is intact
     * 5. If they don't match → record was tampered
     *
     * @param  recordId  ID of the record to verify
     * @return           true if record is intact, false if tampered
     * @throws IllegalArgumentException if record not found
     */
    public boolean verifyRecord(int recordId) {
        Optional<MedicalRecord> recordOpt = recordDAO.findById(recordId);
        if (recordOpt.isEmpty()) {
            throw new IllegalArgumentException(
                "Record #" + recordId + " not found.");
        }

        MedicalRecord record = recordOpt.get();
        boolean isValid = blockchain.verifyRecord(
            recordId,
            record.getRecordHash()
        );

        int currentUserId = SessionManager.getInstance().getCurrentUserId();
        auditLogDAO.log(
            currentUserId,
            AuditLog.ACTION_VERIFY_RECORD,
            "Verified Record #" + recordId +
            " → " + (isValid ? "VALID ✅" : "TAMPERED ❌")
        );

        return isValid;
    }

    /**
     * Validates the entire blockchain integrity.
     * Checks all blocks from genesis to latest.
     *
     * @return  true if entire chain is valid
     */
    public boolean validateBlockchain() {
        return blockchain.isChainValid();
    }

    /**
     * Returns total number of blocks in the blockchain.
     *
     * @return  blockchain length
     */
    public int getBlockchainLength() {
        return blockchain.getChainLength();
    }
}
