package com.aeroai.model;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.LocalDateTime;

@Entity @Table(name = "symptom_assessments")
public class SymptomAssessment {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable=false) private Long ownerId;
    @Column(nullable=false, length=1200) private String symptoms;
    @Column(nullable=false, length=32) private String urgency;
    @Column(nullable=false, length=1000) private String guidance;
    @Column(nullable=false) private LocalDateTime createdAt;
    protected SymptomAssessment() {}
    public SymptomAssessment(Long ownerId, String symptoms, String urgency, String guidance) {
        this.ownerId=ownerId; this.symptoms=symptoms; this.urgency=urgency; this.guidance=guidance; this.createdAt=LocalDateTime.now();
    }
    public Long getId(){return id;} @JsonIgnore public Long getOwnerId(){return ownerId;} public String getSymptoms(){return symptoms;}
    public String getUrgency(){return urgency;} public String getGuidance(){return guidance;} public LocalDateTime getCreatedAt(){return createdAt;}
}
