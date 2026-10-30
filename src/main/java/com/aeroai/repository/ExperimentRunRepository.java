package com.aeroai.repository;

import com.aeroai.model.ExperimentRun;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ExperimentRunRepository extends JpaRepository<ExperimentRun, Long> {
    List<ExperimentRun> findByOwnerIdOrderByCreatedAtDesc(Long ownerId);
    java.util.Optional<ExperimentRun> findByIdAndOwnerId(Long id, Long ownerId);
}
