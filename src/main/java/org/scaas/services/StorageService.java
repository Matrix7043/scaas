package org.scaas.services;

import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

public interface StorageService {
    File getFile(String storagePath);
    String upload(MultipartFile file, UUID functionId) throws IOException;
    void overwrite(String storagePath, MultipartFile file) throws IOException;
    void delete(String storagePath) throws IOException;
}
