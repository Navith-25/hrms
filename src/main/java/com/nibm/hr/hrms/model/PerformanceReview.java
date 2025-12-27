package com.nibm.hr.hrms.model;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "performance_reviews")
public class PerformanceReview {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    // --- ADDED THIS FIELD ---
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewer_id")
    private Employee reviewer;

    @Column(name = "review_date")
    private LocalDate reviewDate;

    @Column(name = "quality_of_work")
    private Integer qualityOfWork;

    private Integer communication;
    private Integer productivity;
    private Integer reliability;

    @Column(name = "overall_rating")
    private Double overallRating;

    @Column(name = "manager_comments", length = 1000)
    private String managerComments;

    // --- Constructors ---
    public PerformanceReview() {}

    // --- Getters and Setters ---

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Employee getEmployee() { return employee; }
    public void setEmployee(Employee employee) { this.employee = employee; }

    // --- NEW GETTER/SETTER FOR REVIEWER ---
    public Employee getReviewer() { return reviewer; }
    public void setReviewer(Employee reviewer) { this.reviewer = reviewer; }

    public LocalDate getReviewDate() { return reviewDate; }
    public void setReviewDate(LocalDate reviewDate) { this.reviewDate = reviewDate; }

    public Integer getQualityOfWork() { return qualityOfWork; }
    public void setQualityOfWork(Integer qualityOfWork) { this.qualityOfWork = qualityOfWork; }

    public Integer getCommunication() { return communication; }
    public void setCommunication(Integer communication) { this.communication = communication; }

    public Integer getProductivity() { return productivity; }
    public void setProductivity(Integer productivity) { this.productivity = productivity; }

    public Integer getReliability() { return reliability; }
    public void setReliability(Integer reliability) { this.reliability = reliability; }

    public Double getOverallRating() { return overallRating; }
    public void setOverallRating(Double overallRating) { this.overallRating = overallRating; }

    public String getManagerComments() { return managerComments; }
    public void setManagerComments(String managerComments) { this.managerComments = managerComments; }
}