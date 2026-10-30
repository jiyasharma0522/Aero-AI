
package com.aeroai.repository;

import com.aeroai.model.RespiratoryReading;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface RespiratoryReadingRepository
        extends JpaRepository<RespiratoryReading, Long> {
    List<RespiratoryReading> findByOwnerIdOrderByRecordedAtDesc(Long ownerId);
    java.util.Optional<RespiratoryReading> findByIdAndOwnerId(Long id, Long ownerId);
}
