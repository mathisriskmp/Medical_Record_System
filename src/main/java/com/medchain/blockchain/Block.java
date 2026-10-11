package com.medchain.blockchain;

import com.medchain.util.HashUtil;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.logging.Logger;

/**
 * Block — Blockchain Block Model
 *
 * Represents a single block in the MedChain blockchain.
 * Each block contains a medical record hash linked to
 * the previous block — creating an immutable chain.
 *
 * @author  MedChain Team
 * @version 2.0
 */
public class Block {

    private static final Logger LOGGER =
        Logger.getLogger(Block.class.getName());

    // ── Fields ────────────────────────────────────────────────────────────
    private int    blockIndex;
    private String previousHash;
    private String recordHash;
    private String timestamp;
    private String blockHash;
    private int    recordId;

    // ── Constructor ───────────────────────────────────────────────────────

    /**
     * Creates a new Block and computes its hash.
     */
    public Block(int blockIndex, String previousHash,
                 String recordHash, int recordId) {
        this.blockIndex   = blockIndex;
        this.previousHash = previousHash;
        this.recordHash   = recordHash;
        this.recordId     = recordId;
        this.timestamp    = LocalDateTime.now()
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        // Compute hash WITHOUT timestamp so it's reproducible
        this.blockHash = computeHash();

        LOGGER.info("⛓ Block #" + blockIndex + " created → " +
                    HashUtil.shortHash(blockHash, 16));
    }

    // ── Core Methods ──────────────────────────────────────────────────────

    /**
     * Computes hash using fields WITHOUT timestamp.
     * This ensures hash is reproducible after app restarts.
     */
    public String computeHash() {
        String data = blockIndex
                    + previousHash
                    + recordHash
                    + recordId;
        return HashUtil.sha256(data);
    }

    /**
     * Forces a specific hash — used when restoring from database.
     * Do NOT use this when creating new blocks!
     */
    public void forceBlockHash(String hash) {
        this.blockHash = hash;
    }

    /**
     * Validates block integrity by recomputing hash.
     * Only valid for NEWLY created blocks.
     * For restored blocks use chain link check instead.
     */
    public boolean isValid() {
        String recomputed = computeHash();
        return recomputed.equals(this.blockHash);
    }

    // ── Getters & Setters ─────────────────────────────────────────────────
    public int    getBlockIndex()                        { return blockIndex; }
    public void   setBlockIndex(int blockIndex)          { this.blockIndex = blockIndex; }
    public String getPreviousHash()                      { return previousHash; }
    public void   setPreviousHash(String previousHash)   { this.previousHash = previousHash; }
    public String getRecordHash()                        { return recordHash; }
    public void   setRecordHash(String recordHash)       { this.recordHash = recordHash; }
    public String getTimestamp()                         { return timestamp; }
    public void   setTimestamp(String timestamp)         { this.timestamp = timestamp; }
    public String getBlockHash()                         { return blockHash; }
    public void   setBlockHash(String blockHash)         { this.blockHash = blockHash; }
    public int    getRecordId()                          { return recordId; }
    public void   setRecordId(int recordId)              { this.recordId = recordId; }

    @Override
    public String toString() {
        return "Block{index=" + blockIndex
             + ", hash="    + HashUtil.shortHash(blockHash, 16)
             + ", prevHash=" + HashUtil.shortHash(previousHash, 16)
             + ", record="  + recordId + "}";
    }
}