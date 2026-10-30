package com.aeroai.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.time.LocalDateTime;

@Entity
@Table(name = "respiratory_readings")
public class RespiratoryReading {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private int spo2;
    private int breathingRate;
    private int pulse;
    private LocalDateTime recordedAt;
    // Nullable additive column: historic file-backed rows remain readable and are not
    // mislabeled as measurements or simulated samples.
    private String dataOrigin;
    // Nullable so existing file-backed H2 rows remain valid after this additive column is created.
    private Long ownerId;

    public RespiratoryReading() {
    }

    public RespiratoryReading(int spo2, int breathingRate, int pulse) {
        this.spo2 = spo2;
        this.breathingRate = breathingRate;
        this.pulse = pulse;
        this.recordedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public int getSpo2() { return spo2; }
    public int getBreathingRate() { return breathingRate; }
    public int getPulse() { return pulse; }
    public LocalDateTime getRecordedAt() { return recordedAt; }
    public String getDataOrigin() { return dataOrigin == null ? "LEGACY_UNCLASSIFIED" : dataOrigin; }
    @JsonIgnore public Long getOwnerId() { return ownerId; }

    public void setId(Long id) { this.id = id; }
    public void setSpo2(int spo2) { this.spo2 = spo2; }
    public void setBreathingRate(int breathingRate) { this.breathingRate = breathingRate; }
    public void setPulse(int pulse) { this.pulse = pulse; }
    public void setDataOrigin(String dataOrigin) { this.dataOrigin = dataOrigin; }
    public void setRecordedAt(LocalDateTime recordedAt) {
        this.recordedAt = recordedAt;
    }
    public void setOwnerId(Long ownerId) { this.ownerId = ownerId; }
}
