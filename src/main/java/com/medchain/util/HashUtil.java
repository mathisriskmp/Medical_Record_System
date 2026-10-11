package com.medchain.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * HashUtil — Cryptographic Hashing Utility
 *
 * Provides SHA-256 hashing functionality for the MedChain blockchain system.
 * Every medical record gets a unique SHA-256 hash that acts as its fingerprint.
 * If even one character in the record changes, the hash completely changes —
 * making tampering instantly detectable.
 *
 * Algorithm : SHA-256
 * Output    : 64-character hexadecimal string
 *
 * @author  MedChain Team
 * @version 1.0
 */
public class HashUtil {

    // ── Logger ────────────────────────────────────────────────────────────────
    private static final Logger LOGGER = Logger.getLogger(HashUtil.class.getName());

    // ── Genesis Block Hash ────────────────────────────────────────────────────
    public static final String GENESIS_PREVIOUS_HASH = "0000000000000000000000000000000000000000000000000000000000000000";

    /**
     * Private constructor — prevents instantiation.
     * This is a utility class and should only be used via static methods.
     */
    private HashUtil() {
        throw new UnsupportedOperationException(
            "HashUtil is a utility class and cannot be instantiated."
        );
    }

    // ── Core Hashing ──────────────────────────────────────────────────────────

    /**
     * Generates a SHA-256 hash of any given input string.
     *
     * @param  input  the string to hash
     * @return        64-character hex SHA-256 hash string
     * @throws RuntimeException if SHA-256 algorithm is unavailable
     */
    public static String sha256(String input) {
        if (input == null || input.isEmpty()) {
            LOGGER.warning("⚠️ HashUtil.sha256() called with null or empty input.");
            return "";
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            LOGGER.log(Level.SEVERE, "❌ SHA-256 algorithm not available.", e);
            throw new RuntimeException("SHA-256 algorithm not available: " + e.getMessage());
        }
    }

    /**
     * Generates a SHA-256 hash specifically for a medical record block.
     *
     * Combines all critical fields of a medical record into one string
     * and hashes them together — ensuring any change to any field
     * produces a completely different hash.
     *
     * @param  blockIndex    position of the block in the chain
     * @param  previousHash  hash of the previous block in the chain
     * @param  patientId     ID of the patient
     * @param  doctorId      ID of the doctor
     * @param  diagnosis     diagnosis text
     * @param  prescription  prescription text
     * @param  notes         additional notes
     * @param  timestamp     record creation timestamp
     * @return               64-character hex SHA-256 hash
     */
    public static String hashRecord(int blockIndex,
            String previousHash,
            int patientId,
            int doctorId,
            String diagnosis,
            String prescription,
            String notes) {
// NO timestamp — so hash is always reproducible!
        String combined = blockIndex     + "|"
+ previousHash   + "|"
+ patientId      + "|"
+ doctorId       + "|"
+ diagnosis      + "|"
+ prescription   + "|"
+ notes;
return sha256(combined);
}

    /**
     * Verifies whether a given input string matches a previously stored hash.
     * Used for tamper detection — compares the recomputed hash with the stored one.
     *
     * @param  input          the original input string to verify
     * @param  expectedHash   the stored hash to compare against
     * @return                true if hashes match (record intact), false if tampered
     */
    public static boolean verifyHash(String input, String expectedHash) {
        if (input == null || expectedHash == null) {
            LOGGER.warning("⚠️ verifyHash() called with null input or expectedHash.");
            return false;
        }
        String computedHash = sha256(input);
        boolean isValid = computedHash.equals(expectedHash);

        if (isValid) {
            LOGGER.info("✅ Hash verification passed — record is intact.");
        } else {
            LOGGER.warning("❌ Hash verification FAILED — record may have been tampered!");
        }
        return isValid;
    }

    /**
     * Returns a short preview of a hash for display purposes.
     * e.g. "a3f8c2d1e5..." instead of the full 64-character string.
     *
     * @param  hash    the full hash string
     * @param  length  number of characters to show
     * @return         shortened hash preview
     */
    public static String shortHash(String hash, int length) {
        if (hash == null || hash.length() < length) return hash;
        return hash.substring(0, length) + "...";
    }

    // ── Private Helpers ───────────────────────────────────────────────────────

    /**
     * Converts a byte array to a hexadecimal string.
     *
     * @param  bytes  raw byte array from MessageDigest
     * @return        hex string representation
     */
    private static String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        return hexString.toString();
    }
}