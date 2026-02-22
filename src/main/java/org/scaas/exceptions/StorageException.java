package org.scaas.exceptions;


public class StorageException extends RuntimeException{
    public StorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
