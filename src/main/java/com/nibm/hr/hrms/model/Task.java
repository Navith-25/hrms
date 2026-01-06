package com.nibm.hr.hrms.model;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "tasks")
public class Task {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    private String description;
    private LocalDate deadline;

    // ADDED PRIORITY FIELD
    private String priority;

    @Column(name = "assigned_date")
    private LocalDate assignedDate;

    @Enumerated(EnumType.STRING)
    private TaskStatus status;

    @Column(name = "manager_comments")
    private String managerComments;

    private Integer rating;

    @ManyToOne
    @JoinColumn(name = "assigned_by_id")
    private Employee manager;

    @ManyToOne
    @JoinColumn(name = "employee_id")
    private Employee employee;

    // --- Constructors ---
    public Task() {
        this.assignedDate = LocalDate.now();
        this.status = TaskStatus.PENDING;
    }

    // --- Getters and Setters ---

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public LocalDate getDeadline() { return deadline; }
    public void setDeadline(LocalDate deadline) { this.deadline = deadline; }

    // Added Getter/Setter for Priority
    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }

    public LocalDate getAssignedDate() { return assignedDate; }
    public void setAssignedDate(LocalDate assignedDate) { this.assignedDate = assignedDate; }

    public TaskStatus getStatus() { return status; }
    public void setStatus(TaskStatus status) { this.status = status; }

    public String getManagerComments() { return managerComments; }
    public void setManagerComments(String managerComments) { this.managerComments = managerComments; }

    public Integer getRating() { return rating; }
    public void setRating(Integer rating) { this.rating = rating; }

    public Employee getManager() { return manager; }
    public void setManager(Employee manager) { this.manager = manager; }

    public Employee getEmployee() { return employee; }
    public void setEmployee(Employee employee) { this.employee = employee; }
}