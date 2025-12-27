package com.nibm.hr.hrms.service;

import com.nibm.hr.hrms.model.*;
import com.nibm.hr.hrms.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.security.Principal;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PerformanceReviewServiceImpl implements PerformanceReviewService {

    @Autowired
    private PerformanceReviewRepository reviewRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    private Employee getEmployeeFromPrincipal(Principal principal) {
        if (principal == null) {
            throw new AccessDeniedException("User is not logged in");
        }
        User user = userRepository.findByUsername(principal.getName());
        if (user == null || user.getEmployee() == null) {
            throw new RuntimeException("Logged-in user does not have a linked Employee profile.");
        }
        return user.getEmployee();
    }

    @Override
    public List<PerformanceReview> getReviewsForEmployee(Long employeeId) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new RuntimeException("Employee not found"));
        return reviewRepository.findByEmployee(employee);
    }

    @Override
    public List<PerformanceReview> getReviewsForEmployeeByUsername(Principal principal) {
        Employee employee = getEmployeeFromPrincipal(principal);
        return reviewRepository.findByEmployee(employee);
    }

    @Override
    public PerformanceReview saveReview(PerformanceReview review) {
        return reviewRepository.save(review);
    }

    @Override
    public PerformanceReview getReviewById(Long reviewId) {
        return reviewRepository.findById(reviewId)
                .orElseThrow(() -> new RuntimeException("Review not found"));
    }

    @Override
    public List<Employee> getEmployeesManagedBy(Principal principal) {
        Employee manager = getEmployeeFromPrincipal(principal);
        Department department = departmentRepository.findByManager(manager).orElse(null);

        if (department == null) {
            return List.of();
        }

        List<Employee> employees = employeeRepository.findByDepartment(department);

        return employees.stream()
                .filter(e -> !e.equals(manager))
                .filter(e -> e.getUser().getRoles().stream()
                        .noneMatch(r -> r.getName().equals("ROLE_ADMIN")))
                .collect(Collectors.toList());
    }

    @Override
    public void saveReviewAsManager(PerformanceReview review, Principal principal) {
        Employee manager = getEmployeeFromPrincipal(principal);

        Employee employeeToReview = employeeRepository.findById(review.getEmployee().getId())
                .orElseThrow(() -> new RuntimeException("Employee not found"));

        // Rule 1: Must be in manager's department
        if (employeeToReview.getDepartment() == null ||
                !employeeToReview.getDepartment().getManager().equals(manager)) {
            throw new AccessDeniedException("You are not authorized to review this employee.");
        }

        // Rule 2: Cannot review self
        if (employeeToReview.equals(manager)) {
            throw new AccessDeniedException("Managers cannot review themselves.");
        }

        // Rule 3: Cannot review Admins
        boolean isTargetAdmin = employeeToReview.getUser().getRoles().stream()
                .anyMatch(r -> r.getName().equals("ROLE_ADMIN"));

        if (isTargetAdmin) {
            throw new AccessDeniedException("Managers are not allowed to review Administrators.");
        }

        review.setEmployee(employeeToReview);
        review.setReviewer(manager);
        reviewRepository.save(review);
    }

    @Override
    public List<PerformanceReview> getReviewsForEmployee(Employee employee) {
        return reviewRepository.findByEmployee(employee);
    }

    @Override
    public List<PerformanceReview> getReviewsByManager(Employee manager) {
        return reviewRepository.findByReviewer(manager);
    }
}