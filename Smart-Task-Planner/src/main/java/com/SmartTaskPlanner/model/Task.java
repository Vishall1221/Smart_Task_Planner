package com.SmartTaskPlanner.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

@Entity
public class Task {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String description;
    private String duration;
    private String dependencies;

    @ManyToOne
    @JoinColumn(name = "plan_id")
    @JsonIgnore
    private Plan plan;

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getDuration() { return duration; }
    public void setDuration(String duration) { this.duration = duration; }

    public String getDependencies() { return dependencies; }
    public void setDependencies(String dependencies) { this.dependencies = dependencies; }

    public Plan getPlan() { return plan; }
    public void setPlan(Plan plan) { this.plan = plan; }
}
