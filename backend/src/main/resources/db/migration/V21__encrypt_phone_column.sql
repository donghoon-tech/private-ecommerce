-- V21: Expand representative_phone column to support AES-256-GCM encrypted values
--
-- Background:
--   Phone numbers were previously stored as plaintext (e.g., "01012345678", max 20 chars).
--   AES-256-GCM encryption + Base64 encoding produces ~64-80 chars per phone number.
--   The column size must be increased to accommodate encrypted values.
--
-- Note on existing data migration:
--   Existing plaintext phone numbers are migrated to encrypted form programmatically
--   by DataMigrationService on application startup (see @EventListener(ApplicationReadyEvent)).
--   SQL-level migration is intentionally avoided because:
--   1. AES-256 encryption logic must stay in Java (PhoneEncryptor).
--   2. PostgreSQL's pgcrypto extension is not assumed to be available.

ALTER TABLE users
    ALTER COLUMN representative_phone TYPE VARCHAR(500);
