package com.nibm.hr.hrms.controller;

import com.nibm.hr.hrms.model.Employee;
import com.nibm.hr.hrms.model.Payroll;
import com.nibm.hr.hrms.model.Role;
import com.nibm.hr.hrms.model.User;
import com.nibm.hr.hrms.repository.UserRepository;
import com.nibm.hr.hrms.service.EmployeeService;
import com.nibm.hr.hrms.service.PayrollService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.security.Principal;
import java.time.LocalDate;
import java.util.Set;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin/payroll")
public class PayrollController {

    @Autowired
    private PayrollService payrollService;

    @Autowired
    private EmployeeService employeeService;

    @Autowired
    private UserRepository userRepository;

    private void checkPayrollAccess(Long targetEmployeeId, Principal principal) {
        User currentUser = userRepository.findByUsername(principal.getName());
        Employee targetEmployee = employeeService.getEmployeeById(targetEmployeeId);
        User targetUser = targetEmployee.getUser();

        // 1. Conflict of Interest: Prevent Self-Management
        if (currentUser.getId().equals(targetUser.getId())) {
            throw new AccessDeniedException("SECURITY ALERT: You cannot manage your own payroll settings.");
        }

        // 2. Identify Current User Roles
        Set<String> currentRoles = currentUser.getRoles().stream()
                .map(Role::getName)
                .collect(Collectors.toSet());

        boolean isDirector = currentRoles.contains("ROLE_DIRECTOR");
        boolean isAdmin = currentRoles.contains("ROLE_ADMIN");
        boolean isHrManager = currentRoles.contains("ROLE_HR_MANAGER");
        boolean isManagerRole = currentRoles.contains("ROLE_MANAGER");
        boolean isFinanceRole = currentRoles.contains("ROLE_FINANCE");

        // Check Department (for Finance Staff/Managers)
        boolean isFinanceDept = currentUser.getEmployee() != null
                && currentUser.getEmployee().getDepartment() != null
                && "Finance".equalsIgnoreCase(currentUser.getEmployee().getDepartment().getName());

        boolean isFinanceManager = isManagerRole && (isFinanceRole || isFinanceDept);
        boolean isFinanceStaff = isFinanceRole || isFinanceDept;

        // 3. Identify Target User Roles
        Set<String> targetRoles = targetUser.getRoles().stream()
                .map(Role::getName)
                .collect(Collectors.toSet());

        boolean targetIsDirector = targetRoles.contains("ROLE_DIRECTOR");
        boolean targetIsHrManager = targetRoles.contains("ROLE_HR_MANAGER");
        boolean targetIsManager = targetRoles.contains("ROLE_MANAGER");

        boolean targetInFinanceDept = targetEmployee.getDepartment() != null
                && "Finance".equalsIgnoreCase(targetEmployee.getDepartment().getName());
        boolean targetIsFinanceManager = targetIsManager && (targetRoles.contains("ROLE_FINANCE") || targetInFinanceDept);

        // --- 4. HIERARCHY RULES ---

        // Rule A: Director (CEO) Payroll
        // Only an ADMIN can manage the Director's payroll.
        if (targetIsDirector) {
            if (!isAdmin) {
                throw new AccessDeniedException("Only an Administrator can manage the Director's payroll.");
            }
            return;
        }

        // Rule B: HR Manager Payroll
        // Visible to: Director OR Finance Manager
        if (targetIsHrManager) {
            if (!isDirector && !isFinanceManager && !isAdmin) {
                throw new AccessDeniedException("Only the Director or Finance Manager can manage HR Payroll.");
            }
            return;
        }

        // Rule C: Finance Manager Payroll
        // Visible to: Director OR HR Manager
        if (targetIsFinanceManager) {
            if (!isDirector && !isHrManager && !isAdmin) {
                throw new AccessDeniedException("Only the Director or HR Manager can manage Finance Payroll.");
            }
            return;
        }

        // Rule D: General Employees (Standard Staff, Finance Staff, Other Managers)
        // Visible to: Finance Department (Manager & Staff) OR Admin/Director
        if (!isFinanceStaff && !isAdmin && !isDirector) {
            throw new AccessDeniedException("Only the Finance Department can manage employee payroll.");
        }
    }

    // --- 1. Manage Salary Settings (View) ---
    @GetMapping("/manage/{employeeId}")
    public String showManagePayroll(@PathVariable Long employeeId, Model model, Principal principal) {
        checkPayrollAccess(employeeId, principal);

        Employee employee = employeeService.getEmployeeById(employeeId);
        Payroll payroll = payrollService.getPayrollSettings(employeeId);

        if (payroll == null) {
            payroll = new Payroll();
            payroll.setEmployee(employee);
        }

        model.addAttribute("employee", employee);
        model.addAttribute("payroll", payroll);
        return "admin_payroll_manage";
    }

    // --- 2. Save Salary Settings ---
    @PostMapping("/save")
    public String savePayroll(@ModelAttribute("payroll") Payroll formPayroll,
                              @RequestParam("employeeId") Long employeeId,
                              Principal principal) {
        checkPayrollAccess(employeeId, principal);

        Payroll existingPayroll = payrollService.getPayrollSettings(employeeId);

        if (existingPayroll != null) {
            existingPayroll.setBaseSalary(formPayroll.getBaseSalary());
            existingPayroll.setStandardDeductions(formPayroll.getStandardDeductions());
            payrollService.savePayrollSettings(existingPayroll);
        } else {
            Employee employee = employeeService.getEmployeeById(employeeId);
            formPayroll.setEmployee(employee);
            payrollService.savePayrollSettings(formPayroll);
        }

        return "redirect:/admin/payroll/manage/" + employeeId + "?success";
    }

    // --- 3. Run Payroll (View Form) ---
    @GetMapping("/run/{employeeId}")
    public String showRunPayrollForm(@PathVariable Long employeeId, Model model, Principal principal) {
        checkPayrollAccess(employeeId, principal);

        Employee employee = employeeService.getEmployeeById(employeeId);
        model.addAttribute("employee", employee);
        model.addAttribute("today", LocalDate.now());
        return "admin_payroll_run";
    }

    // --- 4. Process Payroll ---
    @PostMapping("/run")
    public String runPayroll(@RequestParam("employeeId") Long employeeId,
                             @RequestParam("payDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate payDate,
                             @RequestParam(value = "bonus", required = false) BigDecimal bonus,
                             Principal principal) {
        checkPayrollAccess(employeeId, principal);

        payrollService.generatePayslip(employeeId, payDate, bonus);

        return "redirect:/admin/payroll/run/" + employeeId + "?success";
    }
}