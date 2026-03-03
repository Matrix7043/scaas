package org.scaas.utils;

import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@Component
public class HashingUtil {

    public String hashFile(MultipartFile file) {
        try(InputStream is = file.getInputStream()) {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = new byte[1024*8];
            int readBytes;

            while((readBytes = is.read(bytes)) !=-1) {
                digest.update(bytes, 0, readBytes);
            }

            StringBuilder sb = new StringBuilder();
            byte[] hashedBytes = digest.digest();
            for(byte b : hashedBytes) {
                sb.append(String.format("%02x", b));
            }

            return sb.toString();
        } catch (IOException | NoSuchAlgorithmException e) {
            throw new RuntimeException("Hashing failed for the file", e);
        }
    }

    public String hashFile(File file) {
        try(InputStream is = new FileInputStream(file)) {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = new byte[1024*8];
            int readBytes;

            while((readBytes = is.read(bytes)) !=-1) {
                digest.update(bytes, 0, readBytes);
            }

            StringBuilder sb = new StringBuilder();
            byte[] hashedBytes = digest.digest();
            for(byte b : hashedBytes) {
                sb.append(String.format("%02x", b));
            }

            return sb.toString();
        } catch (IOException | NoSuchAlgorithmException e) {
            throw new RuntimeException("Hashing failed for the file", e);
        }
    }
}
