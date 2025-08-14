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
    private static final int KEY_SIZE = 256;
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 16;
    
    @Value("${token.encryption.key:default-encryption-key-change-in-production}")
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
                
                // Generate a deterministic key from the string
                KeyGenerator keyGen = KeyGenerator.getInstance("AES");
                keyGen.init(KEY_SIZE, new SecureRandom(keyToUse.getBytes(StandardCharsets.UTF_8)));
                secretKey = keyGen.generateKey();
                LOGGER.debug("Encryption key initialized successfully");
            } catch (Exception e) {
                LOGGER.error("Failed to initialize encryption key", e);
                throw new RuntimeException("Encryption key initialization failed", e);
            }
        }
    }
    
    /**
     * Encrypt and compress an access token using AES-256-GCM + GZIP
     * @param accessToken the access token to encrypt and compress
     * @return the encrypted and compressed token as a Base64 string, or null if operation fails
     */
    public String compressToken(String accessToken) {
        if (accessToken == null || accessToken.isEmpty()) {
            return null;
        }
        
        try {
            initializeEncryptionKey();
            
            // 1. Encrypt the plain token
            byte[] iv = new byte[GCM_IV_LENGTH];
            secureRandom.nextBytes(iv);
            
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmSpec);
            
            byte[] encrypted = cipher.doFinal(accessToken.getBytes(StandardCharsets.UTF_8));
            
            // 2. Combine IV + encrypted data
            byte[] combined = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(encrypted, 0, combined, iv.length, encrypted.length);
            
            // 3. Compress the encrypted data
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try (GZIPOutputStream gzip = new GZIPOutputStream(baos)) {
                gzip.write(combined);
            }
            
            // 4. Base64 encode
            String result = Base64.getUrlEncoder().withoutPadding().encodeToString(baos.toByteArray());
            
            LOGGER.debug("Encrypted and compressed token from {} to {} characters ({}% reduction)", 
                accessToken.length(), result.length(), 
                Math.round((1.0 - (double) result.length() / accessToken.length()) * 100));
            
            return result;
        } catch (Exception e) {
            LOGGER.error("Failed to encrypt and compress token", e);
            return null;
        }
    }
    
    /**
     * Decrypt and decompress a token back to the original access token
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
            byte[] compressed = Base64.getUrlDecoder().decode(encryptedCompressedToken);
            
            // 2. Decompress
            ByteArrayInputStream bais = new ByteArrayInputStream(compressed);
            byte[] combined;
            try (GZIPInputStream gzip = new GZIPInputStream(bais)) {
                combined = gzip.readAllBytes();
            }
            
            // 3. Extract IV and encrypted data
            if (combined.length < GCM_IV_LENGTH + GCM_TAG_LENGTH) {
                LOGGER.error("Token too short to contain valid encrypted data");
                return null;
            }
            
            byte[] iv = new byte[GCM_IV_LENGTH];
            byte[] encrypted = new byte[combined.length - GCM_IV_LENGTH];
            System.arraycopy(combined, 0, iv, 0, GCM_IV_LENGTH);
            System.arraycopy(combined, GCM_IV_LENGTH, encrypted, 0, encrypted.length);
            
            // 4. Decrypt
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmSpec);
            
            byte[] decrypted = cipher.doFinal(encrypted);
            String result = new String(decrypted, StandardCharsets.UTF_8);
            
            LOGGER.debug("Decrypted and decompressed token from {} to {} characters", 
                encryptedCompressedToken.length(), result.length());
            
            return result;
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
