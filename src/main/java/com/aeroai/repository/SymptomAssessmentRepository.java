package com.aeroai.repository;
import com.aeroai.model.SymptomAssessment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface SymptomAssessmentRepository extends JpaRepository<SymptomAssessment,Long> {
    List<SymptomAssessment> findByOwnerIdOrderByCreatedAtDesc(Long ownerId);
}
