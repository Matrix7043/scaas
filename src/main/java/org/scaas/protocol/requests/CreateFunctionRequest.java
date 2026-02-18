package org.scaas.protocol.requests;

public record CreateFunctionRequest(String name, String runtime, String entryPoint) {

}
