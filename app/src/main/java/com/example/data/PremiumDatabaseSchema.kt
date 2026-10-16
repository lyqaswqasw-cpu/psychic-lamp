package com.example.data

/**
 * 1. Session Lock Entity: Enforces a strict single-device limit per activation code.
 * Ensures that once an activation code is bound to a specific hardware UUID (deviceId),
 * no secondary devices can use the same session concurrently.
 */
data class UserSession(
    val activationCode: String,
    val deviceId: String,
    val lastActiveTimestamp: Long,
    val sessionToken: String,
    val isLocked: Boolean = true,
    val leaseDurationMs: Long = 300000L // 5 minutes heartbeat lease
)

/**
 * 2. E2EE Chat Message Entity: Encrypted Chat Messages.
 * Uses AES-GCM (256-bit) encryption on the client device prior to dispatch.
 * Decryption can only be performed by holders of the corresponding private keys.
 */
data class EncryptedChatMessage(
    val messageId: String,
    val channelId: String,
    val senderId: String,
    val senderUsername: String,
    // Base64 encoded AES-GCM-256 payload
    val encryptedPayload: String,
    // Base64 encoded 12-byte initialization vector (IV) for AES-GCM
    val initializationVector: String,
    // Ephemeral ECDH Public Key used by the sender for the shared secret derivation
    val senderEphemeralKey: String,
    val timestamp: Long
)

/**
 * Premium App Database Schemas & DDL definitions
 */
object PremiumDatabaseSchema {
    /**
     * Raw SQL DDL Schemas to easily reproduce the exact tables in native SQLite/MySQL
     */
    const val SQL_CREATE_USER_SESSIONS_TABLE = """
        CREATE TABLE IF NOT EXISTS `user_sessions` (
            `activationCode` TEXT NOT NULL, 
            `device_id` TEXT NOT NULL, 
            `last_active_timestamp` INTEGER NOT NULL, 
            `session_token` TEXT NOT NULL, 
            `is_locked` INTEGER NOT NULL, 
            `lease_duration_ms` INTEGER NOT NULL, 
            PRIMARY KEY(`activationCode`)
        );
    """

    const val SQL_CREATE_ENCRYPTED_MESSAGES_TABLE = """
        CREATE TABLE IF NOT EXISTS `encrypted_messages` (
            `messageId` TEXT NOT NULL, 
            `channel_id` TEXT NOT NULL, 
            `sender_id` TEXT NOT NULL, 
            `sender_username` TEXT NOT NULL, 
            `encrypted_payload` TEXT NOT NULL, 
            `initialization_vector` TEXT NOT NULL, 
            `sender_ephemeral_key` TEXT NOT NULL, 
            `timestamp` INTEGER NOT NULL, 
            PRIMARY KEY(`messageId`)
        );
    """
}
