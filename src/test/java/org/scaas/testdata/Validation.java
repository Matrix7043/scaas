package org.scaas.testdata;

import org.junit.jupiter.params.provider.Arguments;
import org.scaas.domain.enumerations.DeploymentStatus;

import java.util.stream.Stream;

public class Validation {

    private static Stream<Arguments> deploymentStatusForUpdate() {
        return Stream.of(
                Arguments.of(DeploymentStatus.DEPLOYED, DeploymentStatus.OUTDATED),
                Arguments.of(DeploymentStatus.OUTDATED, DeploymentStatus.OUTDATED),
                Arguments.of(DeploymentStatus.FAILED, DeploymentStatus.OUTDATED),
                Arguments.of(DeploymentStatus.NOT_DEPLOYED, DeploymentStatus.NOT_DEPLOYED)
        );
    }

    private static Stream<Arguments> deploymentStatusForReplaceArtifact() {
        return Stream.of(
                Arguments.of(DeploymentStatus.DEPLOYED, DeploymentStatus.OUTDATED),
                Arguments.of(DeploymentStatus.OUTDATED, DeploymentStatus.OUTDATED),
                Arguments.of(DeploymentStatus.FAILED, DeploymentStatus.OUTDATED),
                Arguments.of(DeploymentStatus.NOT_DEPLOYED, DeploymentStatus.OUTDATED)
        );
    }

    private static Stream<Arguments> invalidRegisterRequests() {
        return Stream.of(

                Arguments.of("""
                    {
                        "username": "",
                        "firstName": "Test",
                        "lastName": "User",
                        "email": "test@test.com",
                        "password": "password123"
                    }
                    """
                ),

                Arguments.of("""
                    {
                        "username": "test",
                        "firstName": "Test",
                        "lastName": "User",
                        "email": "invalid-email",
                        "password": "password123"
                    }
                    """
                ),

                Arguments.of("""
                    {
                        "username": "test",
                        "firstName": "Test",
                        "lastName": "User",
                        "email": "test@test.com",
                        "password": "123"
                    }
                    """
                ),

                Arguments.of("""
                    {
                        "username": "test",
                        "firstName": "",
                        "lastName": "User",
                        "email": "test@test.com",
                        "password": "password123"
                    }
                    """
                )
        );
    }

    private static Stream<Arguments> invalidLoginRequests() {
        return Stream.of(

                Arguments.of("""
                    {
                        "email": "",
                        "password": "password123"
                    }
                    """
                ),

                Arguments.of("""
                    {
                        "email": "invalid-email",
                        "password": "password123"
                    }
                    """
                ),

                Arguments.of("""
                    {
                        "email": "test@test.com",
                        "password": ""
                    }
                    """
                )
        );
    }

    private static Stream<Arguments> invalidCreateFunctionRequests() {
        return Stream.of(

                Arguments.of("""
                    {
                        "name": "",
                        "runtime": "PYTHON",
                        "entryPoint": "handler"
                    }
                    """
                ),

                Arguments.of("""
                    {
                        "name": "valid",
                        "runtime": null,
                        "entryPoint": "handler"
                    }
                    """
                ),

                Arguments.of("""
                    {
                        "name": "valid",
                        "runtime": "INVALID_RUNTIME",
                        "entryPoint": "handler"
                    }
                    """
                )
        );
    }

    private static Stream<Arguments> invalidPatchRequests() {
        return Stream.of(

                Arguments.of("""
                    {
                        "name": "%s"
                    }
                    """.formatted("a".repeat(101))
                ),

                Arguments.of("""
                    {
                        "entryPoint": "%s"
                    }
                    """.formatted("a".repeat(101))
                ),
                Arguments.of("""
                    {
                        "cpuCores": "0.0"
                    }
                    """
                ),
                Arguments.of("""
                    {
                        "mem": "0"
                    }
                    """
                ),
                Arguments.of("""
                    {
                        "pids": "0"
                    }
                    """
                )
        );
    }



}
