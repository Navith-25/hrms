package com.nibm.hr.hrms.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "payslips")
public class Payslip {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Column(nullable = false)
    private LocalDate payDate;

    @Column(nullable = false)
    private BigDecimal basePay;

    private BigDecimal bonus = BigDecimal.ZERO;

    @Column(nullable = false)
    private BigDecimal tax;

    @Column(name = "loan_deduction")
    private BigDecimal loanDeduction = BigDecimal.ZERO;

    @Column(nullable = false)
    private BigDecimal totalDeductions;

    @Column(nullable = false)
    private BigDecimal netPay;

    public Payslip() {
    }

    public Payslip(Employee employee, LocalDate payDate, BigDecimal basePay, BigDecimal netPay) {
        this.employee = employee;
        this.payDate = payDate;
        this.basePay = basePay;
        this.netPay = netPay;
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

    public LocalDate getPayDate() {
        return payDate;
    }

    public void setPayDate(LocalDate payDate) {
        this.payDate = payDate;
    }

    public BigDecimal getBasePay() {
        return basePay;
    }

    public void setBasePay(BigDecimal basePay) {
        this.basePay = basePay;
    }

    public BigDecimal getBonus() {
        return bonus;
    }

    public void setBonus(BigDecimal bonus) {
        this.bonus = (bonus != null) ? bonus : BigDecimal.ZERO;
    }

    public BigDecimal getTax() {
        return tax;
    }

    public void setTax(BigDecimal tax) {
        this.tax = tax;
    }

    public BigDecimal getLoanDeduction() {
        return loanDeduction;
    }

    public void setLoanDeduction(BigDecimal loanDeduction) {
        this.loanDeduction = (loanDeduction != null) ? loanDeduction : BigDecimal.ZERO;
    }

    public BigDecimal getTotalDeductions() {
        return totalDeductions;
    }

    public void setTotalDeductions(BigDecimal totalDeductions) {
        this.totalDeductions = totalDeductions;
    }

    public BigDecimal getNetPay() {
        return netPay;
    }

    public void setNetPay(BigDecimal netPay) {
        this.netPay = netPay;
    }
}