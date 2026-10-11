package com.medchain.dao;

import com.medchain.model.MedicalDocument;
import com.medchain.util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * DocumentDAO — Data Access Object for Medical Documents
 *
 * Handles all database operations for medical_documents table.
 * Stores and retrieves file data as byte arrays (LONGBLOB).
 *
 * @author  MedChain Team
 * @version 1.0
 */
public class DocumentDAO {

    private static final Logger LOGGER =
        Logger.getLogger(DocumentDAO.class.getName());

    // ── SQL Queries ───────────────────────────────────────────────────────
    private static final String INSERT_DOCUMENT =
        "INSERT INTO medical_documents " +
        "(record_id, patient_id, doctor_id, uploaded_by, " +
        "file_name, file_type, file_size, file_data, " +
        "document_type, description, document_hash) " +
        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

    private static final String SELECT_BY_PATIENT =
        "SELECT md.*, " +
        "up.name AS patient_name, " +
        "ud.name AS doctor_name, " +
        "ub.name AS uploaded_by_name " +
        "FROM medical_documents md " +
        "JOIN users up ON md.patient_id  = up.id " +
        "LEFT JOIN users ud ON md.doctor_id   = ud.id " +
        "JOIN users ub ON md.uploaded_by = ub.id " +
        "WHERE md.patient_id = ? " +
        "ORDER BY md.uploaded_at DESC";

    private static final String SELECT_BY_DOCTOR =
        "SELECT md.*, " +
        "up.name AS patient_name, " +
        "ud.name AS doctor_name, " +
        "ub.name AS uploaded_by_name " +
        "FROM medical_documents md " +
        "JOIN users up ON md.patient_id  = up.id " +
        "LEFT JOIN users ud ON md.doctor_id   = ud.id " +
        "JOIN users ub ON md.uploaded_by = ub.id " +
        "WHERE md.doctor_id = ? OR md.patient_id IN " +
        "(SELECT patient_id FROM access_permissions " +
        "WHERE doctor_id = ? AND is_active = TRUE) " +
        "ORDER BY md.uploaded_at DESC";

    private static final String SELECT_BY_ID =
        "SELECT md.*, " +
        "up.name AS patient_name, " +
        "ud.name AS doctor_name, " +
        "ub.name AS uploaded_by_name " +
        "FROM medical_documents md " +
        "JOIN users up ON md.patient_id  = up.id " +
        "LEFT JOIN users ud ON md.doctor_id   = ud.id " +
        "JOIN users ub ON md.uploaded_by = ub.id " +
        "WHERE md.id = ?";

    private static final String SELECT_ALL =
        "SELECT md.*, " +
        "up.name AS patient_name, " +
        "ud.name AS doctor_name, " +
        "ub.name AS uploaded_by_name " +
        "FROM medical_documents md " +
        "JOIN users up ON md.patient_id  = up.id " +
        "LEFT JOIN users ud ON md.doctor_id   = ud.id " +
        "JOIN users ub ON md.uploaded_by = ub.id " +
        "ORDER BY md.uploaded_at DESC";

    private static final String DELETE_DOCUMENT =
        "DELETE FROM medical_documents WHERE id = ?";

    // ── CRUD Operations ───────────────────────────────────────────────────

    /**
     * Saves a new document to the database.
     * Stores file content as LONGBLOB.
     *
     * @param  doc  MedicalDocument with file data
     * @return      generated document ID, or -1 if failed
     */
    public int save(MedicalDocument doc) {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                 INSERT_DOCUMENT, Statement.RETURN_GENERATED_KEYS)) {

            // Set nullable record_id
            if (doc.getRecordId() != null) {
                ps.setInt(1, doc.getRecordId());
            } else {
                ps.setNull(1, Types.INTEGER);
            }

            ps.setInt(2, doc.getPatientId());

            // Set nullable doctor_id
            if (doc.getDoctorId() != null) {
                ps.setInt(3, doc.getDoctorId());
            } else {
                ps.setNull(3, Types.INTEGER);
            }

            ps.setInt(4,    doc.getUploadedBy());
            ps.setString(5, doc.getFileName());
            ps.setString(6, doc.getFileType());
            ps.setLong(7,   doc.getFileSize());
            ps.setBytes(8,  doc.getFileData()); // ← file bytes
            ps.setString(9, doc.getDocumentType());
            ps.setString(10, doc.getDescription());
            ps.setString(11, doc.getDocumentHash());
            ps.executeUpdate();

            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) {
                int id = keys.getInt(1);
                LOGGER.info("✅ Document saved → ID: " + id +
                            " File: " + doc.getFileName());
                return id;
            }

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE,
                "❌ Error saving document: " + doc.getFileName(), e);
        }
        return -1;
    }

    /**
     * Returns all documents for a specific patient.
     *
     * @param  patientId  patient's user ID
     * @return            list of MedicalDocument (without file data)
     */
    public List<MedicalDocument> findByPatientId(int patientId) {
        return queryList(SELECT_BY_PATIENT, patientId, -1);
    }

    /**
     * Returns all documents accessible to a doctor.
     * Includes documents uploaded by doctor AND
     * documents of patients who granted access.
     *
     * @param  doctorId  doctor's user ID
     * @return           list of MedicalDocument
     */
    public List<MedicalDocument> findByDoctorId(int doctorId) {
        return queryList(SELECT_BY_DOCTOR, doctorId, doctorId);
    }

    /**
     * Returns all documents — for Admin.
     *
     * @return  list of all MedicalDocument objects
     */
    public List<MedicalDocument> findAll() {
        List<MedicalDocument> docs = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps =
                 conn.prepareStatement(SELECT_ALL)) {

            ResultSet rs = ps.executeQuery();
            while (rs.next()) docs.add(mapRow(rs, false));

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE,
                "❌ Error fetching all documents.", e);
        }
        return docs;
    }

    /**
     * Finds a document by ID including its file data.
     * Used when downloading or viewing a document.
     *
     * @param  id  document ID
     * @return     Optional containing MedicalDocument with file data
     */
    public Optional<MedicalDocument> findByIdWithData(int id) {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps =
                 conn.prepareStatement(SELECT_BY_ID)) {

            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return Optional.of(mapRow(rs, true)); // include file data
            }

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE,
                "❌ Error finding document by ID: " + id, e);
        }
        return Optional.empty();
    }

    /**
     * Deletes a document by ID.
     *
     * @param  docId  document ID
     * @return        true if deleted successfully
     */
    public boolean delete(int docId) {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps =
                 conn.prepareStatement(DELETE_DOCUMENT)) {

            ps.setInt(1, docId);
            boolean success = ps.executeUpdate() > 0;
            if (success)
                LOGGER.info("✅ Document deleted → ID: " + docId);
            return success;

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE,
                "❌ Error deleting document: " + docId, e);
        }
        return false;
    }

    // ── Private Helpers ───────────────────────────────────────────────────

    private List<MedicalDocument> queryList(
            String sql, int param1, int param2) {
        List<MedicalDocument> docs = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, param1);
            if (param2 >= 0) ps.setInt(2, param2);

            ResultSet rs = ps.executeQuery();
            while (rs.next())
                docs.add(mapRow(rs, false)); // no file data for list

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE,
                "❌ Error querying documents.", e);
        }
        return docs;
    }

    /**
     * Maps a ResultSet row to a MedicalDocument object.
     *
     * @param  rs           ResultSet row
     * @param  includeData  whether to include file bytes
     * @return              MedicalDocument object
     */
    private MedicalDocument mapRow(ResultSet rs,
                                    boolean includeData)
            throws SQLException {
        MedicalDocument doc = new MedicalDocument();
        doc.setId(rs.getInt("id"));

        int recordId = rs.getInt("record_id");
        if (!rs.wasNull()) doc.setRecordId(recordId);

        doc.setPatientId(rs.getInt("patient_id"));

        int doctorId = rs.getInt("doctor_id");
        if (!rs.wasNull()) doc.setDoctorId(doctorId);

        doc.setUploadedBy(rs.getInt("uploaded_by"));
        doc.setPatientName(rs.getString("patient_name"));
        doc.setDoctorName(rs.getString("doctor_name"));
        doc.setUploadedByName(rs.getString("uploaded_by_name"));
        doc.setFileName(rs.getString("file_name"));
        doc.setFileType(rs.getString("file_type"));
        doc.setFileSize(rs.getLong("file_size"));
        doc.setDocumentType(rs.getString("document_type"));
        doc.setDescription(rs.getString("description"));
        doc.setDocumentHash(rs.getString("document_hash"));

        // Only load file bytes when needed (downloading/viewing)
        if (includeData) {
            doc.setFileData(rs.getBytes("file_data"));
        }

        Timestamp ts = rs.getTimestamp("uploaded_at");
        if (ts != null) doc.setUploadedAt(ts.toLocalDateTime());

        return doc;
    }
}