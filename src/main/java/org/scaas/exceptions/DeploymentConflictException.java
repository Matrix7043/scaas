package org.scaas.exceptions;

public class DeploymentConflictException extends RuntimeException {
    public DeploymentConflictException(String message) {
        super(message);
    }
}
