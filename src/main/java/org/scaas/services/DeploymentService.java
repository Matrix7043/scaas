package org.scaas.services;

import java.io.File;
import java.util.UUID;

public interface DeploymentService {
    String deploy(UUID id, String hashCode, File file);
    void deleteDeployment(UUID id, String hashCode);
}
