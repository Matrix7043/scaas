package org.scaas.services;

import java.io.File;
import java.util.UUID;

public interface DeploymentService {
    String deploy(UUID id, String hashCode, File file, double cpuCores, int memory, int pids)
            throws Exception;
    void deleteDeployment(UUID id, String hashCode);
}
