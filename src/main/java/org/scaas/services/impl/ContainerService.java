package org.scaas.services.impl;

import org.scaas.services.DeploymentService;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class ContainerService implements DeploymentService {
    @Override
    public String deploy(UUID id, String hashCode, File file, double cpuCores, int memory, int pids) throws Exception {
        String containerName = "deployment_" + id + "_" + hashCode;
        try {
            TimeUnit.SECONDS.sleep(5);
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
        return containerName;
    }

    @Override
    public void deleteDeployment(UUID id, String hashCode) {
        try {
            TimeUnit.SECONDS.sleep(5);
        }
        catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }
}
