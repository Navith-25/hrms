package com.nibm.hr.hrms.model;

import jakarta.persistence.*;
import org.springframework.data.domain.Persistable;

import java.math.BigDecimal;

@Entity
@Table(name = "payroll_settings")
public class Payroll implements Persistable<Long> {

    @Id
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "employee_id")
    private Employee employee;

    @Column(name = "base_salary", precision = 10, scale = 2)
    private BigDecimal baseSalary;

    @Column(precision = 10, scale = 2)
    private BigDecimal standardDeductions;

    @Transient
    private boolean isNew = true;

    public Payroll() {
        this.baseSalary = BigDecimal.ZERO;
        this.standardDeductions = BigDecimal.ZERO;
    }

    @Override
    public boolean isNew() {
        return this.isNew;
    }
    @PostLoad
    @PostPersist
    void markNotNew() {
        this.isNew = false;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Employee getEmployee() {
        return employee;
    }

    public void setEmployee(Employee employee) {
        this.employee = employee;
    }

    public BigDecimal getBaseSalary() {
        return baseSalary;
    }

    public void setBaseSalary(BigDecimal baseSalary) {
        this.baseSalary = baseSalary;
    }

    public BigDecimal getStandardDeductions() {
        return standardDeductions;
    }

    public void setStandardDeductions(BigDecimal standardDeductions) {
        this.standardDeductions = standardDeductions;
    }
}