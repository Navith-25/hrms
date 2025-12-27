package com.nibm.hr.hrms.service;

import com.nibm.hr.hrms.model.Employee;
import com.nibm.hr.hrms.model.PerformanceReview;
import java.util.List;
import java.security.Principal;

public interface PerformanceReviewService {
    List<PerformanceReview> getReviewsForEmployee(Long employeeId);
    List<PerformanceReview> getReviewsForEmployeeByUsername(Principal principal);
    List<PerformanceReview> getReviewsForEmployee(Employee employee);
    List<PerformanceReview> getReviewsByManager(Employee manager);
    List<Employee> getEmployeesManagedBy(Principal principal);

    PerformanceReview saveReview(PerformanceReview review);
    PerformanceReview getReviewById(Long reviewId);
    void saveReviewAsManager(PerformanceReview review, Principal principal);


}