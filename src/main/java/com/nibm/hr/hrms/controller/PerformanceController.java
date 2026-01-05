package com.nibm.hr.hrms.controller;

import com.nibm.hr.hrms.model.Employee;
import com.nibm.hr.hrms.model.PerformanceReview;
import com.nibm.hr.hrms.model.User;
import com.nibm.hr.hrms.repository.UserRepository;
import com.nibm.hr.hrms.service.EmployeeService;
import com.nibm.hr.hrms.service.PerformanceReviewService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import java.security.Principal;
import java.time.LocalDate;
import java.util.List;

@Controller
public class PerformanceController {

    @Autowired
    private PerformanceReviewService reviewService;

    @Autowired
    private EmployeeService employeeService;

    @Autowired
    private UserRepository userRepository;

    private Employee getEmployeeFromPrincipal(Principal principal) {
        if (principal == null) throw new AccessDeniedException("User not logged in");
        User user = userRepository.findByUsername(principal.getName());
        return user.getEmployee();
    }

    private void checkNotAdmin(Principal principal) {
        User user = userRepository.findByUsername(principal.getName());
        boolean isAdmin = user.getRoles().stream()
                .anyMatch(r -> r.getName().equals("ROLE_ADMIN"));
        if (isAdmin) {
            throw new AccessDeniedException("The Owner (Admin) does not conduct performance reviews.");
        }
    }

    @GetMapping("/admin/performance/list/{employeeId}")
    public String showEmployeeReviewList(@PathVariable Long employeeId, Model model) {
        Employee employee = employeeService.getEmployeeById(employeeId);
        List<PerformanceReview> reviews = reviewService.getReviewsForEmployee(employeeId);
        model.addAttribute("employee", employee);
        model.addAttribute("reviews", reviews);
        return "admin_performance_list";
    }

    @GetMapping("/admin/performance/new/{employeeId}")
    public String showNewReviewForm(@PathVariable Long employeeId, Model model, Principal principal) {
        // Block Admin from seeing the form
        checkNotAdmin(principal);

        Employee loggedIn = getEmployeeFromPrincipal(principal);
        if (loggedIn.getId().equals(employeeId)) {
            throw new AccessDeniedException("You cannot create a performance review for yourself.");
        }

        Employee employee = employeeService.getEmployeeById(employeeId);
        PerformanceReview review = new PerformanceReview();
        review.setEmployee(employee);
        review.setReviewDate(LocalDate.now());
        model.addAttribute("review", review);
        model.addAttribute("employeeName", employee.getFirstName() + " " + employee.getLastName());
        return "admin_performance_form";
    }

    @GetMapping("/admin/performance/edit/{reviewId}")
    public String showEditReviewForm(@PathVariable Long reviewId, Model model, Principal principal) {
        checkNotAdmin(principal);

        PerformanceReview review = reviewService.getReviewById(reviewId);
        model.addAttribute("review", review);
        model.addAttribute("employeeName", review.getEmployee().getFirstName() + " " + review.getEmployee().getLastName());
        return "admin_performance_form";
    }

    @PostMapping("/admin/performance/save")
    public String saveReview(@ModelAttribute("review") PerformanceReview review, Principal principal) {
        checkNotAdmin(principal);

        Employee loggedIn = getEmployeeFromPrincipal(principal);
        if (review.getEmployee() != null && loggedIn.getId().equals(review.getEmployee().getId())) {
            throw new AccessDeniedException("You cannot save a performance review for yourself.");
        }

        Employee employee = employeeService.getEmployeeById(review.getEmployee().getId());
        review.setEmployee(employee);

        // Pass principal to service for strict role hierarchy validation
        reviewService.saveReview(review, principal);

        return "redirect:/admin/performance/list/" + review.getEmployee().getId();
    }
}