package org.scaas.domain.entites;

import jakarta.persistence.*;
import lombok.*;

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

    @ManyToOne
    private User user;
}
