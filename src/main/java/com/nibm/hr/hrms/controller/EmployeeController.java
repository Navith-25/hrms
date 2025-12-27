package com.nibm.hr.hrms.controller;

import com.nibm.hr.hrms.dto.NewEmployeeRequest;
import com.nibm.hr.hrms.model.*;
import com.nibm.hr.hrms.repository.DepartmentRepository;
import com.nibm.hr.hrms.repository.UserRepository;
import com.nibm.hr.hrms.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.Set;
import java.util.stream.Collectors;

@Controller
public class EmployeeController {

    @Autowired
    private LeaveRequestService leaveRequestService;

    @Autowired
    private PayrollService payrollService;

    @Autowired
    private PerformanceReviewService reviewService;

    @Autowired
    private TaskService taskService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EmployeeService employeeService;

    @Autowired
    private DepartmentRepository departmentRepository;

    private Employee getEmployeeFromPrincipal(Principal principal) {
        if (principal == null) throw new AccessDeniedException("User not logged in");
        User user = userRepository.findByUsername(principal.getName());
        return user.getEmployee();
    }

    // --- 1. Leave Management ---
    @GetMapping("/leave")
    public String showLeavePage(Model model, Principal principal) {
        model.addAttribute("myLeaveRequests", leaveRequestService.getMyLeaveRequests(principal));
        model.addAttribute("leaveRequest", new LeaveRequest());
        return "leave_request";
    }

    @PostMapping("/leave/request")
    public String submitLeaveRequest(@ModelAttribute("leaveRequest") LeaveRequest leaveRequest, Principal principal) {
        leaveRequestService.createLeaveRequest(leaveRequest, principal);
        return "redirect:/leave";
    }

    // --- 2. Payslips ---
    @GetMapping("/payslips")
    public String showMyPayslips(Model model, Principal principal) {
        model.addAttribute("myPayslips", payrollService.getPayslipsForEmployee(principal));
        return "my_payslips";
    }

    @GetMapping("/payslip/{id}")
    public String showPayslipDetail(@PathVariable Long id, Model model, Authentication authentication, Principal principal) {
        Payslip payslip = payrollService.getPayslipById(id);
        Employee loggedInEmployee = getEmployeeFromPrincipal(principal);

        boolean isOwner = payslip.getEmployee().equals(loggedInEmployee);
        boolean hasOverrideAccess = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") ||
                        a.getAuthority().equals("ROLE_HR_MANAGER") ||
                        a.getAuthority().equals("ROLE_DIRECTOR") ||
                        a.getAuthority().equals("ROLE_FINANCE"));

        if (!isOwner && !hasOverrideAccess) {
            return "redirect:/payslips";
        }

        model.addAttribute("payslip", payslip);
        return "payslip_detail";
    }

    // --- 3. Performance Reviews ---
    @GetMapping("/performance")
    public String showMyReviews(Model model, Principal principal) {
        model.addAttribute("myReviews", reviewService.getReviewsForEmployeeByUsername(principal));
        return "my_performance_reviews";
    }

    // --- 4. Task Management ---
    @GetMapping("/employee/tasks")
    public String showMyTasks(Model model, Principal principal) {
        model.addAttribute("tasks", taskService.getMyTasks(principal));
        return "employee_tasks";
    }

    @PostMapping("/employee/tasks/submit/{id}")
    public String submitTask(@PathVariable("id") Long id, Principal principal) {
        taskService.submitTask(id, principal);
        return "redirect:/employee/tasks";
    }

    @GetMapping("/employee/trainings")
    public String showMyTrainings(Model model, Principal principal) {
        Employee employee = getEmployeeFromPrincipal(principal);
        model.addAttribute("trainings", employee.getTrainingPrograms());
        return "my_trainings";
    }

    // --- Show Form for New Employee ---
    @GetMapping("/showNewEmployeeForm")
    public String showNewEmployeeForm(Model model) {
        model.addAttribute("employeeRequest", new NewEmployeeRequest());
        model.addAttribute("allDepartments", departmentRepository.findAll());
        return "new_employee";
    }

    // --- Save New Employee ---
    @PostMapping("/admin/employee/saveNew")
    public String saveNewEmployee(@ModelAttribute("employeeRequest") NewEmployeeRequest request) {
        employeeService.createNewEmployee(request);
        return "redirect:/";
    }

    // --- Show Form for Update ---
    @GetMapping("/showFormForUpdate/{id}")
    public String showFormForUpdate(@PathVariable(value = "id") Long id, Model model) {
        Employee employee = employeeService.getEmployeeById(id);
        User user = employee.getUser();

        NewEmployeeRequest dto = new NewEmployeeRequest();
        dto.setId(employee.getId());
        dto.setFirstName(employee.getFirstName());
        dto.setLastName(employee.getLastName());
        dto.setEmail(employee.getEmail());
        dto.setPosition(employee.getPosition());
        dto.setHireDate(employee.getHireDate());
        if (employee.getDepartment() != null) {
            dto.setDepartmentId(employee.getDepartment().getId());
        }

        if (user != null) {
            dto.setUsername(user.getUsername());
            dto.setPassword(user.getPassword());

            Set<String> roleNames = user.getRoles().stream()
                    .map(Role::getName)
                    .collect(Collectors.toSet());

            dto.setManager(roleNames.contains("ROLE_MANAGER"));
            dto.setHrManager(roleNames.contains("ROLE_HR_MANAGER"));
            dto.setHrStaff(roleNames.contains("ROLE_HR_STAFF"));
            dto.setFinance(roleNames.contains("ROLE_FINANCE"));
            dto.setDirector(roleNames.contains("ROLE_DIRECTOR"));
            dto.setAdmin(roleNames.contains("ROLE_ADMIN"));
        }

        model.addAttribute("employeeRequest", dto);
        model.addAttribute("allDepartments", departmentRepository.findAll());

        return "update_employee";
    }

    // --- Save Updated Employee ---
    @PostMapping("/admin/employee/update")
    public String updateEmployee(@ModelAttribute("employeeRequest") NewEmployeeRequest request) {
        employeeService.updateEmployee(request);
        return "redirect:/";
    }

    // --- Delete Employee ---
    @PostMapping("/deleteEmployee/{id}")
    public String deleteEmployee(@PathVariable(value = "id") Long id) {
        employeeService.deleteEmployeeById(id);
        return "redirect:/";
    }
}