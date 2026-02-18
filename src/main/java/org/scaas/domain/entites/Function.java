package org.scaas.domain.entites;

import jakarta.persistence.*;
import lombok.*;
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

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Runtime runtime;
    @Column(nullable = false)
    private String entryPoint;

    @Column(nullable = false)
    private LocalDateTime createdAt;
    @Column(nullable = false)
    private LocalDateTime updatedAt;
}
