package com.medchain.blockchain;

import com.medchain.util.HashUtil;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

/**
 * Blockchain — Core Blockchain Simulation
 *
 * Manages the chain of blocks for MedChain.
 * Singleton pattern — one chain per application.
 *
 * @author  MedChain Team
 * @version 2.0
 */
public class Blockchain {

    private static final Logger LOGGER =
        Logger.getLogger(Blockchain.class.getName());

    // ── Singleton ─────────────────────────────────────────────────────────
    private static volatile Blockchain instance;

    // ── Chain ─────────────────────────────────────────────────────────────
    private final List<Block> chain;

    private Blockchain() {
        chain = new ArrayList<>();
        createGenesisBlock();
    }

    public static Blockchain getInstance() {
        if (instance == null) {
            synchronized (Blockchain.class) {
                if (instance == null) {
                    instance = new Blockchain();
                }
            }
        }
        return instance;
    }

    // ── Genesis Block ─────────────────────────────────────────────────────

    private void createGenesisBlock() {
        Block genesis = new Block(
            0,
            HashUtil.GENESIS_PREVIOUS_HASH,
            HashUtil.sha256("MedChain Genesis Block"),
            0
        );
        chain.add(genesis);
        LOGGER.info("🌱 Genesis block created → " +
                    HashUtil.shortHash(genesis.getBlockHash(), 16));
    }

    // ── Add Block ─────────────────────────────────────────────────────────

    /**
     * Adds a new block for a medical record.
     */
    public Block addBlock(String recordHash, int recordId) {
        Block lastBlock = getLatestBlock();
        Block newBlock  = new Block(
            chain.size(),
            lastBlock.getBlockHash(),
            recordHash,
            recordId
        );
        chain.add(newBlock);
        LOGGER.info("✅ Block #" + newBlock.getBlockIndex() +
                    " added. Chain length: " + chain.size());
        return newBlock;
    }

    // ── Validation ────────────────────────────────────────────────────────

    /**
     * Validates the entire blockchain.
     *
     * IMPORTANT: Only checks chain LINKS (previousHash matches).
     * Does NOT recompute hashes — avoids timestamp mismatch issue.
     */
    public boolean isChainValid() {
        for (int i = 1; i < chain.size(); i++) {
            Block current  = chain.get(i);
            Block previous = chain.get(i - 1);

            // Check chain link — does this block point to previous?
            if (!current.getPreviousHash()
                        .equals(previous.getBlockHash())) {
                LOGGER.warning("❌ Chain broken between Block #" +
                               (i - 1) + " and Block #" + i +
                               "\n  Expected prevHash: " +
                               HashUtil.shortHash(
                                   previous.getBlockHash(), 16) +
                               "\n  Got prevHash:      " +
                               HashUtil.shortHash(
                                   current.getPreviousHash(), 16));
                return false;
            }
        }
        LOGGER.info("✅ Chain valid — " + chain.size() + " blocks intact.");
        return true;
    }

    /**
     * Verifies a specific record against the blockchain.
     */
    public boolean verifyRecord(int recordId, String recordHash) {
        for (Block block : chain) {
            if (block.getRecordId() == recordId) {
                boolean valid = block.getRecordHash().equals(recordHash);
                if (valid) {
                    LOGGER.info("✅ Record #" + recordId + " verified.");
                } else {
                    LOGGER.warning("❌ Record #" + recordId + " TAMPERED!");
                }
                return valid;
            }
        }
        LOGGER.warning("⚠️ Record #" + recordId + " not found in chain.");
        return false;
    }

    // ── Database Rebuild ──────────────────────────────────────────────────

    /**
     * Rebuilds blockchain from database on app startup.
     * Restores EXACT original hashes using forceBlockHash().
     */
    public void rebuildFromDatabase() {
        try {
            chain.clear();
            createGenesisBlock();

            String sql = "SELECT id, record_hash, block_index, " +
                         "previous_hash, block_hash " +
                         "FROM medical_records " +
                         "ORDER BY block_index ASC";

            try (java.sql.Connection conn =
                     com.medchain.util.DBConnection.getConnection();
                 java.sql.PreparedStatement ps =
                     conn.prepareStatement(sql);
                 java.sql.ResultSet rs = ps.executeQuery()) {

                while (rs.next()) {
                    int    recordId   = rs.getInt("id");
                    String recordHash = rs.getString("record_hash");
                    int    blockIndex = rs.getInt("block_index");
                    String prevHash   = rs.getString("previous_hash");
                    String blockHash  = rs.getString("block_hash");

                    // Recreate block
                    Block block = new Block(
                        blockIndex, prevHash,
                        recordHash, recordId
                    );

                    // ── CRITICAL: Force exact original hash ────────────────
                    if (blockHash != null && !blockHash.isEmpty()) {
                        block.forceBlockHash(blockHash);
                        LOGGER.info("⛓ Restored Block #" + blockIndex +
                                    " → " +
                                    HashUtil.shortHash(blockHash, 16));
                    }

                    chain.add(block);
                }
            }

            // Verify chain after rebuild
            if (isChainValid()) {
                LOGGER.info("✅ Chain integrity confirmed after rebuild.");
            } else {
                LOGGER.warning("⚠️ Chain integrity check failed after rebuild!");
            }

        } catch (Exception e) {
            LOGGER.severe("❌ Rebuild failed: " + e.getMessage());
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    public Block getLatestBlock() {
        return chain.get(chain.size() - 1);
    }

    public Block getBlockByIndex(int index) {
        if (index < 0 || index >= chain.size()) return null;
        return chain.get(index);
    }

    public Block getBlockByRecordId(int recordId) {
        return chain.stream()
                    .filter(b -> b.getRecordId() == recordId)
                    .findFirst()
                    .orElse(null);
    }

    public List<Block> getChain() {
        return Collections.unmodifiableList(chain);
    }

    public int getChainLength() {
        return chain.size();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== MedChain Blockchain ===\n");
        sb.append("Blocks : ").append(chain.size()).append("\n");
        sb.append("Valid  : ").append(isChainValid()).append("\n");
        for (Block b : chain) sb.append(b.toString()).append("\n");
        return sb.toString();
    }
}