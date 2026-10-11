package com.medchain.dao;

import com.medchain.model.MedicalRecord;
import com.medchain.util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * RecordDAO — Data Access Object for Medical Records
 *
 * Handles all database operations for medical_records table.
 * Works alongside the blockchain layer — every record saved
 * here also gets a block added to the blockchain.
 *
 * @author  MedChain Team
 * @version 1.0
 */
public class RecordDAO {

    private static final Logger LOGGER = Logger.getLogger(RecordDAO.class.getName());

    // ── SQL Queries ───────────────────────────────────────────────────────────
    private static final String INSERT_RECORD =
        "INSERT INTO medical_records " +
        "(patient_id, doctor_id, diagnosis, prescription, notes, " +
        "record_hash, block_index, previous_hash, block_hash) " +
        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

    private static final String SELECT_BY_ID =
        "SELECT mr.*, up.name AS patient_name, ud.name AS doctor_name " +
        "FROM medical_records mr " +
        "JOIN users up ON mr.patient_id = up.id " +
        "JOIN users ud ON mr.doctor_id  = ud.id " +
        "WHERE mr.id = ?";

    private static final String SELECT_BY_PATIENT =
        "SELECT mr.*, up.name AS patient_name, ud.name AS doctor_name " +
        "FROM medical_records mr " +
        "JOIN users up ON mr.patient_id = up.id " +
        "JOIN users ud ON mr.doctor_id  = ud.id " +
        "WHERE mr.patient_id = ? ORDER BY mr.created_at DESC";

    private static final String SELECT_BY_DOCTOR =
        "SELECT mr.*, up.name AS patient_name, ud.name AS doctor_name " +
        "FROM medical_records mr " +
        "JOIN users up ON mr.patient_id = up.id " +
        "JOIN users ud ON mr.doctor_id  = ud.id " +
        "WHERE mr.doctor_id = ? ORDER BY mr.created_at DESC";

    private static final String SELECT_ALL =
        "SELECT mr.*, up.name AS patient_name, ud.name AS doctor_name " +
        "FROM medical_records mr " +
        "JOIN users up ON mr.patient_id = up.id " +
        "JOIN users ud ON mr.doctor_id  = ud.id " +
        "ORDER BY mr.created_at DESC";

    private static final String COUNT_ALL =
        "SELECT COUNT(*) FROM medical_records";

    // ── CRUD Operations ───────────────────────────────────────────────────────

    /**
     * Saves a new medical record to the database.
     *
     * @param  record  MedicalRecord to save
     * @return         generated record ID, or -1 if failed
     */
    public int save(MedicalRecord record) {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                 INSERT_RECORD, Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1,    record.getPatientId());
            ps.setInt(2,    record.getDoctorId());
            ps.setString(3, record.getDiagnosis());
            ps.setString(4, record.getPrescription());
            ps.setString(5, record.getNotes());
            ps.setString(6, record.getRecordHash());
            ps.setInt(7,    record.getBlockIndex());
            ps.setString(8, record.getPreviousHash());
            ps.setString(9, record.getBlockHash());
            ps.executeUpdate();

            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) {
                int id = keys.getInt(1);
                LOGGER.info("✅ Medical record saved → ID: " + id);
                return id;
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "❌ Error saving medical record.", e);
        }
        return -1;
    }

    /**
     * Finds a medical record by ID.
     *
     * @param  id  record ID
     * @return     Optional containing MedicalRecord if found
     */
    public Optional<MedicalRecord> findById(int id) {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SELECT_BY_ID)) {

            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return Optional.of(mapRow(rs));

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "❌ Error finding record by ID: " + id, e);
        }
        return Optional.empty();
    }

    /**
     * Returns all medical records for a specific patient.
     *
     * @param  patientId  patient's user ID
     * @return            list of records ordered by date descending
     */
    public List<MedicalRecord> findByPatientId(int patientId) {
        return queryList(SELECT_BY_PATIENT, patientId);
    }

    /**
     * Returns all medical records added by a specific doctor.
     *
     * @param  doctorId  doctor's user ID
     * @return           list of records ordered by date descending
     */
    public List<MedicalRecord> findByDoctorId(int doctorId) {
        return queryList(SELECT_BY_DOCTOR, doctorId);
    }

    /**
     * Returns all medical records in the system.
     * Used by Admin dashboard.
     *
     * @return  list of all records
     */
    public List<MedicalRecord> findAll() {
        List<MedicalRecord> records = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SELECT_ALL)) {

            ResultSet rs = ps.executeQuery();
            while (rs.next()) records.add(mapRow(rs));

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "❌ Error fetching all records.", e);
        }
        return records;
    }

    /**
     * Returns total count of medical records.
     *
     * @return  total record count
     */
    public int getTotalCount() {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(COUNT_ALL)) {

            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1);

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "❌ Error counting records.", e);
        }
        return 0;
    }

    // ── Private Helper ────────────────────────────────────────────────────────

    private List<MedicalRecord> queryList(String sql, int param) {
        List<MedicalRecord> records = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, param);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) records.add(mapRow(rs));

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "❌ Error querying records.", e);
        }
        return records;
    }

    private MedicalRecord mapRow(ResultSet rs) throws SQLException {
        MedicalRecord r = new MedicalRecord();
        r.setId(rs.getInt("id"));
        r.setPatientId(rs.getInt("patient_id"));
        r.setDoctorId(rs.getInt("doctor_id"));
        r.setPatientName(rs.getString("patient_name"));
        r.setDoctorName(rs.getString("doctor_name"));
        r.setDiagnosis(rs.getString("diagnosis"));
        r.setPrescription(rs.getString("prescription"));
        r.setNotes(rs.getString("notes"));
        r.setRecordHash(rs.getString("record_hash"));
        r.setBlockIndex(rs.getInt("block_index"));
        r.setPreviousHash(rs.getString("previous_hash"));
        r.setBlockHash(rs.getString("block_hash"));
        Timestamp ts = rs.getTimestamp("created_at");
        if (ts != null) r.setCreatedAt(ts.toLocalDateTime());
        return r;
    }
}
