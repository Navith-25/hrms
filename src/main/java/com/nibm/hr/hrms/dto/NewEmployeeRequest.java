package com.nibm.hr.hrms.dto;

import java.time.LocalDate;

public class NewEmployeeRequest {

    private Long id;

    private String firstName;
    private String lastName;
    private String email;
    private String position;
    private LocalDate hireDate;
    private Long departmentId;

    private String username;
    private String password;

    private boolean isManager;
    private boolean isHrManager;
    private boolean isHrStaff;
    private boolean isFinance;
    private boolean isDirector;
    private boolean isAdmin;

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

    public Long getDepartmentId() { return departmentId; }
    public void setDepartmentId(Long departmentId) { this.departmentId = departmentId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public boolean isManager() { return isManager; }
    public void setManager(boolean manager) { isManager = manager; }

    public boolean isHrManager() { return isHrManager; }
    public void setHrManager(boolean hrManager) { isHrManager = hrManager; }

    public boolean isHrStaff() { return isHrStaff; }
    public void setHrStaff(boolean hrStaff) { isHrStaff = hrStaff; }

    public boolean isFinance() { return isFinance; }
    public void setFinance(boolean finance) { isFinance = finance; }

    public boolean isDirector() { return isDirector; }
    public void setDirector(boolean director) { isDirector = director; }

    public boolean isAdmin() { return isAdmin; }
    public void setAdmin(boolean admin) { isAdmin = admin; }
}