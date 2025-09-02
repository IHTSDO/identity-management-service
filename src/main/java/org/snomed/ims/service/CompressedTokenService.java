package org.snomed.ims.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.charset.StandardCharsets;
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
    
    // No encryption - compression only for maximum size reduction
    

    
    /**
     * Compress an access token using GZIP for maximum size reduction
     * @param accessToken the access token to compress
     * @return the compressed token as a Base64 string, or null if operation fails
     */
    public String compressToken(String accessToken) {
        if (accessToken == null || accessToken.isEmpty()) {
            return null;
        }
        
        try {
            // Compress the token using GZIP for maximum size reduction
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try (GZIPOutputStream gzip = new GZIPOutputStream(baos)) {
                gzip.write(accessToken.getBytes(StandardCharsets.UTF_8));
            }
            
            byte[] compressed = baos.toByteArray();
            
            // Base64 encode the compressed result
            String result = Base64.getUrlEncoder().withoutPadding().encodeToString(compressed);
            
            LOGGER.debug("Compressed token from {} to {} characters ({}% reduction)", 
                accessToken.length(), result.length(), 
                Math.round((1.0 - (double) result.length() / accessToken.length()) * 100));
            
            return result;
        } catch (Exception e) {
            LOGGER.error("Failed to compress token", e);
            return null;
        }
    }
    
    /**
     * Decompress a token back to the original access token
     * @param compressedToken the compressed token to decompress
     * @return the original access token, or null if operation fails
     */
    public String decompressToken(String compressedToken) {
        if (compressedToken == null || compressedToken.isEmpty()) {
            return null;
        }
        
        try {
            // 1. Base64 decode
            byte[] decoded = Base64.getUrlDecoder().decode(compressedToken);
            
            // 2. Decompress using GZIP
            ByteArrayInputStream bais = new ByteArrayInputStream(decoded);
            try (GZIPInputStream gzip = new GZIPInputStream(bais)) {
                String result = new String(gzip.readAllBytes(), StandardCharsets.UTF_8);
                
                LOGGER.debug("Decompressed token from {} to {} characters", 
                    compressedToken.length(), result.length());
                
                return result;
            }
        } catch (Exception e) {
            LOGGER.error("Failed to decompress token", e);
            return null;
        }
    }
    

}
