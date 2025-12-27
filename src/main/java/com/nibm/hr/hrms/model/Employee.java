package com.nibm.hr.hrms.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.Set;

@Entity
@Table(name = "employees")
public class Employee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String firstName;

    @Column(nullable = false)
    private String lastName;

    @Column(nullable = false, unique = true)
    private String email;

    private String position;

    private LocalDate hireDate;

    @OneToOne(mappedBy = "employee", cascade = CascadeType.ALL)
    private User user;

    @ManyToOne
    @JoinColumn(name = "department_id")
    private Department department;

    @ManyToMany(mappedBy = "employees")
    private Set<TrainingProgram> trainingPrograms;

    @OneToMany(mappedBy = "employee", cascade = CascadeType.ALL)
    private Set<LeaveRequest> leaveRequests;

    public Employee() {
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPosition() { return position; }
    public void setPosition(String position) { this.position = position; }

    public LocalDate getHireDate() { return hireDate; }
    public void setHireDate(LocalDate hireDate) { this.hireDate = hireDate; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public Department getDepartment() { return department; }
    public void setDepartment(Department department) { this.department = department; }

    public Set<TrainingProgram> getTrainingPrograms() { return trainingPrograms; }
    public void setTrainingPrograms(Set<TrainingProgram> trainingPrograms) { this.trainingPrograms = trainingPrograms; }

    public Set<LeaveRequest> getLeaveRequests() { return leaveRequests; }
    public void setLeaveRequests(Set<LeaveRequest> leaveRequests) { this.leaveRequests = leaveRequests; }
}