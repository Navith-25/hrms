package com.nibm.hr.hrms.service;

import com.nibm.hr.hrms.model.*;
import com.nibm.hr.hrms.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.security.Principal;
import java.util.List;
import java.util.Set;
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
    public PerformanceReview getReviewById(Long reviewId) {
        return reviewRepository.findById(reviewId)
                .orElseThrow(() -> new RuntimeException("Review not found"));
    }

    // --- MAIN HIERARCHY LOGIC ---
    @Override
    public PerformanceReview saveReview(PerformanceReview review, Principal principal) {
        Employee reviewer = getEmployeeFromPrincipal(principal);
        User reviewerUser = reviewer.getUser();

        // 1. Identify Reviewer Roles
        Set<String> reviewerRoles = reviewerUser.getRoles().stream().map(Role::getName).collect(Collectors.toSet());
        boolean isRevAdmin = reviewerRoles.contains("ROLE_ADMIN");
        boolean isRevDirector = reviewerRoles.contains("ROLE_DIRECTOR");
        boolean isRevHrManager = reviewerRoles.contains("ROLE_HR_MANAGER");
        boolean isRevManager = reviewerRoles.contains("ROLE_MANAGER");
        boolean isRevFinance = reviewerRoles.contains("ROLE_FINANCE");

        // Check if Reviewer is a "Manager Level" (HR Manager, Finance Manager, or Dept Manager)
        boolean isReviewerAnyManager = isRevHrManager || isRevManager || (isRevFinance && isRevManager);

        // --- RULE: ADMIN (OWNER) DOES NOT DO REVIEWS ---
        if (isRevAdmin) {
            throw new AccessDeniedException("The Owner (Admin) does not conduct performance reviews.");
        }

        // --- RULE: REGULAR STAFF / HR STAFF CANNOT REVIEW ANYONE ---
        // If you are not a Director and not a Manager, you cannot review.
        if (!isRevDirector && !isReviewerAnyManager) {
            throw new AccessDeniedException("You do not have the authority to conduct performance reviews.");
        }

        // 2. Identify Target (Reviewee) Roles
        Employee reviewee = employeeRepository.findById(review.getEmployee().getId())
                .orElseThrow(() -> new RuntimeException("Employee to review not found"));
        User revieweeUser = reviewee.getUser();
        Set<String> targetRoles = revieweeUser.getRoles().stream().map(Role::getName).collect(Collectors.toSet());

        boolean isTargetAdmin = targetRoles.contains("ROLE_ADMIN");
        boolean isTargetDirector = targetRoles.contains("ROLE_DIRECTOR");
        boolean isTargetHrManager = targetRoles.contains("ROLE_HR_MANAGER");
        boolean isTargetManager = targetRoles.contains("ROLE_MANAGER");
        boolean isTargetFinance = targetRoles.contains("ROLE_FINANCE");

        boolean isTargetAnyManager = isTargetHrManager || isTargetManager || (isTargetFinance && isTargetManager);

        // --- HIERARCHY CHECKS ---

        // 1. No one reviews Admin or Director (except maybe Admin reviews Director, but Admin is disabled above)
        if (isTargetAdmin || isTargetDirector) {
            throw new AccessDeniedException("Performance reviews cannot be created for Top Management or the Owner.");
        }

        // 2. If Target is a Manager (HR Manager, Finance Manager, etc.) -> ONLY DIRECTOR can review
        if (isTargetAnyManager) {
            if (!isRevDirector) {
                throw new AccessDeniedException("Only the Director can review Managers.");
            }
        }

        // 3. If Target is Standard Staff -> DIRECTOR or DEPARTMENT MANAGER can review
        if (!isTargetAnyManager) {
            if (isRevDirector) {
                // Director can review anyone
            } else if (isReviewerAnyManager) {
                // Managers can only review their OWN department
                boolean sameDept = reviewee.getDepartment() != null &&
                        reviewer.getDepartment() != null &&
                        reviewee.getDepartment().getId().equals(reviewer.getDepartment().getId());

                if (!sameDept) {
                    throw new AccessDeniedException("Managers can only review employees within their own department.");
                }
            }
        }

        // 4. Self Review Block
        if (reviewer.getId().equals(reviewee.getId())) {
            throw new AccessDeniedException("You cannot review yourself.");
        }

        review.setReviewer(reviewer);
        return reviewRepository.save(review);
    }

    // --- Legacy / Helper Methods ---

    @Override
    public List<Employee> getEmployeesManagedBy(Principal principal) {
        Employee manager = getEmployeeFromPrincipal(principal);
        Department department = departmentRepository.findByManager(manager).orElse(null);

        if (department == null) {
            // If checking specifically for "My Team" list, use the department entity
            // But if generic list, maybe filter by department ID
            if (manager.getDepartment() != null) {
                return employeeRepository.findByDepartment(manager.getDepartment()).stream()
                        .filter(e -> !e.equals(manager))
                        .collect(Collectors.toList());
            }
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
        // Redirect to the main robust logic
        saveReview(review, principal);
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