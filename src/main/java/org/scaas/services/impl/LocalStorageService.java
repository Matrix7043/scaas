package org.scaas.services.impl;

import org.scaas.services.StorageService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class LocalStorageService implements StorageService {

    @Value("${app.storage.base-path}")
    private String basePath;

    @Override
    public String upload(MultipartFile file, UUID functionId) throws IOException {

        Path functionDir = Paths.get(basePath, "functions", functionId.toString());
        if (!Files.exists(functionDir)) {
            Files.createDirectories(functionDir);
        }

        String fileName = UUID.randomUUID() + ".py";
        Path targetPath = functionDir.resolve(fileName);

        Files.copy(
                file.getInputStream(),
                targetPath,
                StandardCopyOption.REPLACE_EXISTING
        );

        return targetPath.toString();
    }

    @Override
    public void overwrite(String storagePath, MultipartFile file) throws IOException {
        Files.copy(file.getInputStream(),
                Path.of(storagePath),
                StandardCopyOption.REPLACE_EXISTING);
    }

    @Override
    public void delete(String storagePath) throws IOException {
        if(storagePath == null || storagePath.isBlank()) {
            return;
        }

        Path path = Paths.get(storagePath);

        Files.deleteIfExists(path);
    }
}
