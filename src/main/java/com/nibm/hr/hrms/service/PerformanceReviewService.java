package com.nibm.hr.hrms.service;

import com.nibm.hr.hrms.model.Employee;
import com.nibm.hr.hrms.model.PerformanceReview;

import java.security.Principal;
import java.util.List;

public interface PerformanceReviewService {

    // Updated signature to accept Principal for role validation
    PerformanceReview saveReview(PerformanceReview review, Principal principal);

    List<PerformanceReview> getReviewsForEmployee(Long employeeId);
    List<PerformanceReview> getReviewsForEmployeeByUsername(Principal principal);
    PerformanceReview getReviewById(Long reviewId);

    List<Employee> getEmployeesManagedBy(Principal principal);
    void saveReviewAsManager(PerformanceReview review, Principal principal);
    List<PerformanceReview> getReviewsForEmployee(Employee employee);
    List<PerformanceReview> getReviewsByManager(Employee manager);
}