package org.scaas.services;

import org.scaas.exceptions.DeploymentServiceException;

import java.io.File;
import java.util.UUID;

public interface DeploymentService {
    String deploy(UUID id, String hashCode, File file, double cpuCores, int memory, int pids)
            throws DeploymentServiceException;
    void deleteDeployment(UUID id, String hashCode);
}
