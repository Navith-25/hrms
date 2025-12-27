package com.nibm.hr.hrms.service;

import com.nibm.hr.hrms.model.Employee;
import com.nibm.hr.hrms.model.Loan;
import com.nibm.hr.hrms.model.Role;
import com.nibm.hr.hrms.model.User;
import com.nibm.hr.hrms.repository.LoanRepository;
import com.nibm.hr.hrms.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class LoanService {

    @Autowired
    private LoanRepository loanRepository;

    @Autowired
    private UserRepository userRepository;

    // --- 1. Request Loan ---
    public void requestLoan(Loan loan, Employee employee) {
        loan.setEmployee(employee);
        loan.setRequestDate(LocalDate.now());
        loan.setStatus(Loan.LoanStatus.PENDING);
        loan.setRemainingBalance(loan.getTotalAmount());

        if (loan.getRepaymentMonths() > 0) {
            BigDecimal installment = loan.getTotalAmount().divide(
                    BigDecimal.valueOf(loan.getRepaymentMonths()), 2, RoundingMode.HALF_UP);
            loan.setMonthlyInstallment(installment);
        }

        loanRepository.save(loan);
    }

    // --- 2. Get Pending Loans (Strict Hierarchy) ---
    public List<Loan> getPendingLoansForApprover(User approver) {
        List<Loan> allPending = loanRepository.findByStatus(Loan.LoanStatus.PENDING);

        Set<String> approverRoles = approver.getRoles().stream().map(Role::getName).collect(Collectors.toSet());
        boolean isDirector = approverRoles.contains("ROLE_DIRECTOR");
        boolean isHrManager = approverRoles.contains("ROLE_HR_MANAGER");
        boolean isManagerRole = approverRoles.contains("ROLE_MANAGER");
        boolean isFinanceRole = approverRoles.contains("ROLE_FINANCE");
        boolean isFinanceDept = approver.getEmployee() != null && approver.getEmployee().getDepartment() != null
                && "Finance".equalsIgnoreCase(approver.getEmployee().getDepartment().getName());
        boolean isFinanceManager = isManagerRole && (isFinanceRole || isFinanceDept);
        boolean isFinanceStaff = isFinanceRole && !isManagerRole;

        return allPending.stream()
                .filter(loan -> {
                    User requester = loan.getEmployee().getUser();
                    Employee reqEmployee = loan.getEmployee();
                    if (requester.getId().equals(approver.getId())) return false;

                    Set<String> reqRoles = requester.getRoles().stream().map(Role::getName).collect(Collectors.toSet());
                    boolean reqIsHrManager = reqRoles.contains("ROLE_HR_MANAGER");
                    boolean reqIsManager = reqRoles.contains("ROLE_MANAGER");
                    boolean reqInFinanceDept = reqEmployee.getDepartment() != null
                            && "Finance".equalsIgnoreCase(reqEmployee.getDepartment().getName());
                    boolean reqIsFinanceManager = reqIsManager && (reqRoles.contains("ROLE_FINANCE") || reqInFinanceDept);

                    if (reqIsHrManager) return isDirector || isFinanceManager;
                    if (reqIsFinanceManager) return isDirector || isHrManager;
                    if (isFinanceManager || isFinanceStaff) return true;

                    return false;
                })
                .collect(Collectors.toList());
    }

    // --- 3. Approve/Reject Logic ---
    @Transactional
    public void updateLoanStatus(Long loanId, String status, String approverUsername) {
        Loan loan = loanRepository.findById(loanId).orElseThrow(() -> new RuntimeException("Loan not found"));
        User approver = userRepository.findByUsername(approverUsername);
        User requester = loan.getEmployee().getUser();
        Employee reqEmployee = loan.getEmployee();

        if (approver.getId().equals(requester.getId())) {
            throw new AccessDeniedException("You cannot approve your own loan.");
        }

        Set<String> approverRoles = approver.getRoles().stream().map(Role::getName).collect(Collectors.toSet());
        boolean isDirector = approverRoles.contains("ROLE_DIRECTOR");
        boolean isHrManager = approverRoles.contains("ROLE_HR_MANAGER");
        boolean isAppManager = approverRoles.contains("ROLE_MANAGER");
        boolean isAppFinance = approverRoles.contains("ROLE_FINANCE");
        boolean isAppFinanceDept = approver.getEmployee() != null && approver.getEmployee().getDepartment() != null
                && "Finance".equalsIgnoreCase(approver.getEmployee().getDepartment().getName());
        boolean isFinanceManager = isAppManager && (isAppFinance || isAppFinanceDept);
        boolean isFinanceStaff = isAppFinance || isAppFinanceDept;

        Set<String> reqRoles = requester.getRoles().stream().map(Role::getName).collect(Collectors.toSet());
        boolean reqIsHrManager = reqRoles.contains("ROLE_HR_MANAGER");
        boolean reqIsManager = reqRoles.contains("ROLE_MANAGER");
        boolean reqInFinanceDept = reqEmployee.getDepartment() != null
                && "Finance".equalsIgnoreCase(reqEmployee.getDepartment().getName());
        boolean reqIsFinanceManager = reqIsManager && (reqRoles.contains("ROLE_FINANCE") || reqInFinanceDept);

        if (reqIsHrManager) {
            if (!isDirector && !isFinanceManager) throw new AccessDeniedException("Only CEO or Finance Manager can approve HR Manager.");
        } else if (reqIsFinanceManager) {
            if (!isDirector && !isHrManager) throw new AccessDeniedException("Only CEO or HR Manager can approve Finance Manager.");
        } else {
            if (!isFinanceStaff) throw new AccessDeniedException("Only Finance Dept can approve.");
        }

        if ("APPROVE".equalsIgnoreCase(status)) {
            loan.setStatus(Loan.LoanStatus.APPROVED);
            loan.setStartDate(LocalDate.now());
        } else if ("REJECT".equalsIgnoreCase(status)) {
            loan.setStatus(Loan.LoanStatus.REJECTED);
        }
        loanRepository.save(loan);
    }

    // --- 4. Methods for Calendar & History (REQUIRED FOR CALENDAR) ---
    public List<Loan> getLoansForEmployee(Employee employee) {
        return loanRepository.findByEmployee(employee);
    }

    public List<Loan> getMyLoans(Employee employee) {
        return loanRepository.findByEmployee(employee);
    }

    public List<Loan> getAllPendingLoans() {
        return loanRepository.findByStatus(Loan.LoanStatus.PENDING);
    }

    // --- 5. Payroll Deduction ---
    @Transactional
    public BigDecimal processMonthlyDeduction(Employee employee) {
        List<Loan> activeLoans = loanRepository.findByEmployeeAndStatus(employee, Loan.LoanStatus.APPROVED);
        BigDecimal totalDeduction = BigDecimal.ZERO;

        for (Loan loan : activeLoans) {
            BigDecimal installment = loan.getMonthlyInstallment();
            if (loan.getRemainingBalance().compareTo(installment) < 0) {
                installment = loan.getRemainingBalance();
            }
            loan.setRemainingBalance(loan.getRemainingBalance().subtract(installment));
            totalDeduction = totalDeduction.add(installment);

            if (loan.getRemainingBalance().compareTo(BigDecimal.ZERO) <= 0) {
                loan.setStatus(Loan.LoanStatus.PAID_OFF);
            }
            loanRepository.save(loan);
        }
        return totalDeduction;
    }
}