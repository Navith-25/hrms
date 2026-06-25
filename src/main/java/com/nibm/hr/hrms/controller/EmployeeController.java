package com.nibm.hr.hrms.controller;

import com.nibm.hr.hrms.dto.NewEmployeeRequest;
import com.nibm.hr.hrms.model.*;
import com.nibm.hr.hrms.repository.DepartmentRepository;
import com.nibm.hr.hrms.repository.EmployeeRepository;
import com.nibm.hr.hrms.repository.UserRepository;
import com.nibm.hr.hrms.service.*;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.Principal;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Controller
public class EmployeeController {

    @Autowired private LeaveRequestService leaveRequestService;
    @Autowired private PayrollService payrollService;
    @Autowired private PerformanceReviewService reviewService;
    @Autowired private TaskService taskService;
    @Autowired private UserRepository userRepository;
    @Autowired private EmployeeService employeeService;
    @Autowired private EmployeeRepository employeeRepository;
    @Autowired private DepartmentRepository departmentRepository;

    private final String PROFILE_UPLOAD_DIR = "uploads/profile_pics/";

    private Employee getEmployeeFromPrincipal(Principal principal) {
        if (principal == null) throw new AccessDeniedException("User not logged in");
        User user = userRepository.findByUsername(principal.getName());
        return user.getEmployee();
    }

    @GetMapping("/leave")
    public String showLeavePage(Model model, Principal principal) {
        model.addAttribute("employee", getEmployeeFromPrincipal(principal));
        model.addAttribute("myLeaveRequests", leaveRequestService.getMyLeaveRequests(principal));
        model.addAttribute("leaveRequest", new LeaveRequest());
        return "leave_request";
    }

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
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_HR_MANAGER") || a.getAuthority().equals("ROLE_DIRECTOR") || a.getAuthority().equals("ROLE_FINANCE"));

        if (!isOwner && !hasOverrideAccess) return "redirect:/payslips";

        model.addAttribute("payslip", payslip);
        return "payslip_detail";
    }

    @GetMapping("/performance")
    public String showMyReviews(Model model, Principal principal) {
        model.addAttribute("myReviews", reviewService.getReviewsForEmployeeByUsername(principal));
        return "my_performance_reviews";
    }

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

    @GetMapping("/showNewEmployeeForm")
    public String showNewEmployeeForm(Model model) {
        model.addAttribute("employeeRequest", new NewEmployeeRequest());
        model.addAttribute("allDepartments", departmentRepository.findAll());
        return "new_employee";
    }

    // ==========================================
    // FIXED: SAVE EMPLOYEE WITH VALIDATION
    // ==========================================
    @PostMapping("/admin/employee/saveNew")
    public String saveNewEmployee(@Valid @ModelAttribute("employeeRequest") NewEmployeeRequest request,
                                  BindingResult bindingResult, Model model, RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("allDepartments", departmentRepository.findAll());
            return "new_employee";
        }
        try {
            employeeService.createNewEmployee(request);
            redirectAttributes.addFlashAttribute("successMessage", "Employee created successfully and is Pending Approval.");
            return "redirect:/";
        } catch (RuntimeException e) {
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("allDepartments", departmentRepository.findAll());
            return "new_employee";
        }
    }

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
        if (employee.getDepartment() != null) dto.setDepartmentId(employee.getDepartment().getId());

        if (user != null) {
            dto.setUsername(user.getUsername());
            Set<String> roleNames = user.getRoles().stream().map(Role::getName).collect(Collectors.toSet());
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

    // ==========================================
    // FIXED: UPDATE EMPLOYEE WITH VALIDATION
    // ==========================================
    @PostMapping("/admin/employee/update")
    public String updateEmployee(@Valid @ModelAttribute("employeeRequest") NewEmployeeRequest request,
                                 BindingResult bindingResult, Model model, RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("allDepartments", departmentRepository.findAll());
            return "update_employee";
        }
        try {
            employeeService.updateEmployee(request);
            redirectAttributes.addFlashAttribute("successMessage", "Employee details updated successfully.");
            return "redirect:/";
        } catch (RuntimeException e) {
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("allDepartments", departmentRepository.findAll());
            return "update_employee";
        }
    }

    @PostMapping("/deleteEmployee/{id}")
    public String deleteEmployee(@PathVariable(value = "id") Long id, RedirectAttributes redirectAttributes) {
        employeeService.deleteEmployeeById(id);
        redirectAttributes.addFlashAttribute("successMessage", "Employee account has been deactivated successfully.");
        return "redirect:/";
    }

    @PostMapping("/admin/employee/approve/{id}")
    public String approveEmployee(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        try {
            employeeService.approveEmployee(id);
            redirectAttributes.addFlashAttribute("successMessage", "Employee account approved and activated successfully!");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/";
    }

    @PostMapping("/admin/employee/reject/{id}")
    public String rejectEmployee(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        try {
            employeeService.rejectEmployee(id);
            redirectAttributes.addFlashAttribute("successMessage", "Pending employee account has been rejected and permanently removed.");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/";
    }

    @GetMapping("/my-profile")
    public String showMyProfile(Model model, Principal principal) {
        model.addAttribute("employee", getEmployeeFromPrincipal(principal));
        return "my_profile";
    }

    @PostMapping("/my-profile/update")
    public String updateMyProfile(@RequestParam(value = "phone", required = false) String phone,
                                  @RequestParam(value = "address", required = false) String address,
                                  @RequestParam(value = "dob", required = false) String dob,
                                  @RequestParam(value = "profileImage", required = false) MultipartFile profileImage,
                                  Principal principal, RedirectAttributes redirectAttributes) {
        try {
            Employee employee = getEmployeeFromPrincipal(principal);
            if (phone != null) employee.setPhone(phone);
            if (address != null) employee.setAddress(address);
            if (dob != null && !dob.isEmpty()) employee.setDob(LocalDate.parse(dob));
            if (profileImage != null && !profileImage.isEmpty()) {
                Path uploadPath = Paths.get(PROFILE_UPLOAD_DIR);
                if (!Files.exists(uploadPath)) Files.createDirectories(uploadPath);
                String originalFilename = profileImage.getOriginalFilename();
                String fileExtension = originalFilename != null ? originalFilename.substring(originalFilename.lastIndexOf(".")) : ".jpg";
                String newFileName = UUID.randomUUID().toString() + fileExtension;
                Files.copy(profileImage.getInputStream(), uploadPath.resolve(newFileName), StandardCopyOption.REPLACE_EXISTING);
                employee.setProfilePhoto(newFileName);
            }
            employeeRepository.save(employee);
            redirectAttributes.addFlashAttribute("successMessage", "Profile updated successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error updating profile: " + e.getMessage());
        }
        return "redirect:/my-profile";
    }

    @PostMapping("/my-profile/change-password")
    public String changePassword(@RequestParam("oldPassword") String oldPassword,
                                 @RequestParam("newPassword") String newPassword,
                                 @RequestParam("confirmPassword") String confirmPassword,
                                 Principal principal, RedirectAttributes redirectAttributes) {
        try {
            if (!newPassword.equals(confirmPassword)) throw new RuntimeException("New password and confirmation password do not match.");
            employeeService.changePassword(principal.getName(), oldPassword, newPassword);
            redirectAttributes.addFlashAttribute("successMessage", "Your password has been changed successfully. Please use the new password next time you log in.");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/my-profile";
    }

    @GetMapping("/profile-pics/{fileName:.+}")
    public ResponseEntity<Resource> viewProfilePhoto(@PathVariable("fileName") String fileName) {
        try {
            Path filePath = Paths.get(PROFILE_UPLOAD_DIR).resolve(fileName).normalize();
            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists() && resource.isReadable()) {
                String contentType = fileName.toLowerCase().endsWith(".png") ? "image/png" : "image/jpeg";
                return ResponseEntity.ok().contentType(MediaType.parseMediaType(contentType)).body(resource);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/admin/audit-logs")
    public String showAuditLogs(Model model) {
        model.addAttribute("employees", employeeRepository.findAll());
        return "audit_logs";
    }
}