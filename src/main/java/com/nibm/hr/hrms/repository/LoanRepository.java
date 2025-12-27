package com.nibm.hr.hrms.repository;

import com.nibm.hr.hrms.model.Employee;
import com.nibm.hr.hrms.model.Loan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LoanRepository extends JpaRepository<Loan, Long> {
    List<Loan> findByEmployee(Employee employee);
    List<Loan> findByEmployeeAndStatus(Employee employee, Loan.LoanStatus status);
    List<Loan> findByStatus(Loan.LoanStatus status);
}