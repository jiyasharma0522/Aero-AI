package com.aeroai.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.LocalDateTime;

@Entity
@Table(name = "experiment_runs")
public class ExperimentRun {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    private int respiratoryRate;
    private int tidalVolumeMl;
    private int compliance;
    private double simulatedVentilation;
    private LocalDateTime createdAt;
    // Nullable to retain compatibility with historical experiment rows.
    private Long ownerId;

    protected ExperimentRun() { }
    public ExperimentRun(String name, int respiratoryRate, int tidalVolumeMl, int compliance) {
        this.name = name;
        this.respiratoryRate = respiratoryRate;
        this.tidalVolumeMl = tidalVolumeMl;
        this.compliance = compliance;
        this.simulatedVentilation = respiratoryRate * tidalVolumeMl / 1000.0;
    }
    @PrePersist void stamp() { if (createdAt == null) createdAt = LocalDateTime.now(); }
    public Long getId() { return id; }
    public String getName() { return name; }
    public int getRespiratoryRate() { return respiratoryRate; }
    public int getTidalVolumeMl() { return tidalVolumeMl; }
    public int getCompliance() { return compliance; }
    public double getSimulatedVentilation() { return simulatedVentilation; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    @JsonIgnore public Long getOwnerId() { return ownerId; }
    public void setOwnerId(Long ownerId) { this.ownerId = ownerId; }
}
