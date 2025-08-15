package org.snomed.ims.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Service to compress and decompress access tokens to reduce cookie size
 * Uses GZIP compression to significantly reduce token size while maintaining security
 */
@Service
public class CompressedTokenService {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(CompressedTokenService.class);
    
    // Encryption configuration
    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int KEY_SIZE = 128; // Reduced from 256 for smaller size
    private static final int GCM_IV_LENGTH = 8; // Reduced from 12 for smaller size
    private static final int GCM_TAG_LENGTH = 16;
    
    @Value("${token.encryption.key:${TOKEN_ENCRYPTION_KEY:${systemProperties['token.encryption.key']:default-encryption-key-change-in-production}}}")
    private String encryptionKeyString;
    
    private SecretKey secretKey;
    private final SecureRandom secureRandom = new SecureRandom();
    
    /**
     * Initialize the encryption key from the configured property
     */
    private void initializeEncryptionKey() {
        if (secretKey == null) {
            try {
                // Use configured key or fallback to a default for testing
                String keyToUse = (encryptionKeyString != null) ? encryptionKeyString : "default-test-key-for-development";
                
                // Generate a deterministic key using SHA-256 hash of the string
                MessageDigest digest = MessageDigest.getInstance("SHA-256");
                byte[] hash = digest.digest(keyToUse.getBytes(StandardCharsets.UTF_8));
                
                // Take the first 16 bytes (128 bits) for AES-128
                byte[] keyBytes = new byte[16];
                System.arraycopy(hash, 0, keyBytes, 0, 16);
                
                secretKey = new SecretKeySpec(keyBytes, "AES");
                LOGGER.debug("Encryption key initialized successfully");
            } catch (Exception e) {
                LOGGER.error("Failed to initialize encryption key", e);
                throw new RuntimeException("Encryption key initialization failed", e);
            }
        }
    }
    
    /**
     * Compress and encrypt an access token using lightweight AES-128-GCM
     * @param accessToken the access token to compress and encrypt
     * @return the compressed and encrypted token as a Base64 string, or null if operation fails
     */
    public String compressToken(String accessToken) {
        if (accessToken == null || accessToken.isEmpty()) {
            return null;
        }
        
        try {
            // 1. Compress the plain token first (better compression ratio)
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try (GZIPOutputStream gzip = new GZIPOutputStream(baos)) {
                gzip.write(accessToken.getBytes(StandardCharsets.UTF_8));
            }
            
            byte[] compressed = baos.toByteArray();
            
            // 2. Always encrypt for security, but with lightweight settings
            initializeEncryptionKey();
            
            // Use smaller IV (8 bytes instead of 12) and AES-128
            byte[] iv = new byte[GCM_IV_LENGTH];
            secureRandom.nextBytes(iv);
            
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmSpec);
            
            byte[] encrypted = cipher.doFinal(compressed);
            
            // Combine IV + encrypted data
            byte[] combined = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(encrypted, 0, combined, iv.length, encrypted.length);
            
            // Base64 encode the result
            String result = Base64.getUrlEncoder().withoutPadding().encodeToString(combined);
            
            LOGGER.debug("Compressed and encrypted token from {} to {} characters ({}% change)", 
                accessToken.length(), result.length(), 
                Math.round(((double) result.length() / accessToken.length() - 1.0) * 100));
            
            return result;
        } catch (Exception e) {
            LOGGER.error("Failed to compress and encrypt token", e);
            return null;
        }
    }
    
    /**
     * Decompress and decrypt a token back to the original access token
     * @param encryptedCompressedToken the encrypted and compressed token to decrypt and decompress
     * @return the original access token, or null if operation fails
     */
    public String decompressToken(String encryptedCompressedToken) {
        if (encryptedCompressedToken == null || encryptedCompressedToken.isEmpty()) {
            return null;
        }
        
        try {
            initializeEncryptionKey();
            
            // 1. Base64 decode
            byte[] decoded = Base64.getUrlDecoder().decode(encryptedCompressedToken);
            
            // 2. Decrypt first (the data is encrypted, not compressed)
            if (decoded.length < GCM_IV_LENGTH + GCM_TAG_LENGTH) {
                LOGGER.error("Token too short to contain valid encrypted data");
                return null;
            }
            
            byte[] iv = new byte[GCM_IV_LENGTH];
            byte[] encrypted = new byte[decoded.length - GCM_IV_LENGTH];
            System.arraycopy(decoded, 0, iv, 0, GCM_IV_LENGTH);
            System.arraycopy(decoded, GCM_IV_LENGTH, encrypted, 0, encrypted.length);
            
            // 3. Decrypt
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmSpec);
            
            byte[] decrypted = cipher.doFinal(encrypted);
            
            // 4. Decompress the decrypted data
            ByteArrayInputStream bais = new ByteArrayInputStream(decrypted);
            try (GZIPInputStream gzip = new GZIPInputStream(bais)) {
                String result = new String(gzip.readAllBytes(), StandardCharsets.UTF_8);
                
                LOGGER.debug("Decrypted and decompressed token from {} to {} characters", 
                    encryptedCompressedToken.length(), result.length());
                
                return result;
            }
        } catch (Exception e) {
            LOGGER.error("Failed to decrypt and decompress token", e);
            return null;
        }
    }
    
    /**
     * Check if encryption is enabled
     * @return true if encryption is enabled
     */
    public boolean isEncryptionEnabled() {
        return secretKey != null && 
               encryptionKeyString != null && 
               !encryptionKeyString.equals("default-encryption-key-change-in-production");
    }
    
    /**
     * Get the current encryption algorithm
     * @return the encryption algorithm being used
     */
    public String getEncryptionAlgorithm() {
        return ALGORITHM;
    }
    
    /**
     * Get the current key size
     * @return the encryption key size in bits
     */
    public int getKeySize() {
        return KEY_SIZE;
    }
}
