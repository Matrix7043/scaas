package org.scaas.domain.repositories;

import org.scaas.domain.entites.Function;
import org.scaas.domain.entites.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface FunctionRepository extends JpaRepository<Function, UUID> {

    Page<Function> findByOwnerAndDeletedAtIsNull(User owner, Pageable pageable);
    Optional<Function> findByIdAndOwnerAndDeletedAtIsNull(UUID id, User owner);

}
