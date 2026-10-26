# Offline decrypt for encrypted Yuyue backups

## YUY2（现行默认）

Encrypted files start with ASCII `YUY2`, then:

- 1-byte format version (`1`)
- 2-byte salt length (big-endian) + salt bytes
- 4-byte PBKDF2 iterations (big-endian, default `120000`)
- 2-byte key length in bits (big-endian, default `256`)
- 12-byte GCM IV
- AES-256-GCM ciphertext (128-bit tag)

Key = PBKDF2-HMAC-SHA256(passphrase, salt_from_header, iterations, keyLenBits/8).

Salt and KDF params live **in the file header**, so a YUY2 backup can be restored on another device with only the passphrase.

Prefer restoring inside 语曰（Vault → 备份与恢复）. This note exists so data remains yours if the app is gone.

## YUY1（旧格式，只读兼容）

Legacy files start with ASCII `YUY1`, then version byte `1`, 12-byte GCM IV, then AES-256-GCM ciphertext.

Key = PBKDF2-HMAC-SHA256(passphrase, **device salt**, 120000, 32 bytes). The salt was stored on the creating device (`yuyue_backup_secure`), not in the file. Cross-device restore of YUY1 needs that salt; prefer re-exporting as YUY2.

## Product note

Backup / restore does **not** depend on a cloud Agent. CaptureKernel writes locally first; optional model polish never replaces the encrypted archive path.
