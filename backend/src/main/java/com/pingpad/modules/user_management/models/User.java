package com.pingpad.modules.user_management.models;

import jakarta.persistence.*;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String githubLogin;
    private String email;
    private String password; // Hashed password

    @DateTimeFormat
    private LocalDateTime createdAt;
    @DateTimeFormat
    private LocalDateTime updatedAt;

    // Constructors
    public User() {}

    public User(String name, String githubLogin, String email) {
        this.name = name;
        this.githubLogin = githubLogin;
        this.email = email;
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getGithubLogin() { return githubLogin; }
    public void setGithubLogin(String githubLogin) { this.githubLogin = githubLogin; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}
