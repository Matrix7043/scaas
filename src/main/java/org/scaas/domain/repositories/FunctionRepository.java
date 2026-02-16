package org.scaas.domain.repositories;

import org.scaas.domain.entites.Function;
import org.scaas.domain.entites.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface FunctionRepository extends JpaRepository<Function, UUID> {

    List<Function> findByOwner(User owner);

}
