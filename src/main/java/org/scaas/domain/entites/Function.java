package org.scaas.domain.entites;

import jakarta.persistence.*;
import lombok.*;
import org.scaas.domain.enumerations.DeploymentStatus;
import org.scaas.domain.enumerations.Runtime;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "functions")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class Function {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @Version
    private long version;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Runtime runtime;
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private DeploymentStatus deploymentStatus;
    private String deployedHashcode;
    private String currentHashCode;
    @Column(nullable = false)
    private String entryPoint;
    private String storagePath;
    private String invocationURL;

    @Column(nullable = false)
    private double cpuCores;
    @Column(nullable = false)
    private int memory;
    @Column(nullable = false)
    private int pidCount;

    @Column(nullable = false)
    private LocalDateTime createdAt;
    @Column(nullable = false)
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;
    private LocalDateTime deployedAt;
}
