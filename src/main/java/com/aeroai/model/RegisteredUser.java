package com.aeroai.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDateTime;

@Entity
@Table(name = "registered_users", uniqueConstraints = {
        @UniqueConstraint(name = "uk_registered_users_email", columnNames = "email")
})
public class RegisteredUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "first_name", nullable = false, length = 80)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 80)
    private String lastName;

    @Column(nullable = false, length = 254)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    @Column(nullable = false, length = 60)
    private String role;

    @Column(name = "field_of_interest", length = 80)
    private String fieldOfInterest;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected RegisteredUser() {
    }

    public RegisteredUser(String firstName, String lastName, String email, String passwordHash,
                          String role, String fieldOfInterest) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = role;
        this.fieldOfInterest = fieldOfInterest;
    }

    @PrePersist
    void setCreatedAt() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public Long getId() { return id; }
    public String getFirstName() { return firstName; }
    public void setFirstName(String value) { this.firstName = value; }
    public String getLastName() { return lastName; }
    public void setLastName(String value) { this.lastName = value; }
    public String getEmail() { return email; }
    public String getPasswordHash() { return passwordHash; }
    public String getRole() { return role; }
    public void setRole(String value) { this.role = value; }
    public String getFieldOfInterest() { return fieldOfInterest; }
    public void setFieldOfInterest(String value) { this.fieldOfInterest = value; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
