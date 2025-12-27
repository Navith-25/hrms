package com.nibm.hr.hrms.controller;

import com.nibm.hr.hrms.model.*;
import com.nibm.hr.hrms.service.EmployeeService;
import com.nibm.hr.hrms.repository.LeaveRequestRepository;
import com.nibm.hr.hrms.service.PerformanceReviewService;
import com.nibm.hr.hrms.service.PayrollService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.io.IOException;
import java.io.PrintWriter;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Controller
public class ReportController {

    @Autowired
    private EmployeeService employeeService;

    @Autowired
    private PayrollService payrollService;

    @Autowired
    private PerformanceReviewService performanceReviewService;

    @Autowired
    private LeaveRequestRepository leaveRequestRepository;

    // --- 1. Individual Employee Report (TXT) ---
    @GetMapping("/admin/report/employee/{id}")
    public void downloadIndividualReport(@PathVariable("id") Long id, HttpServletResponse response) throws IOException {
        Employee employee = employeeService.getEmployeeById(id);

        Payroll payroll = null;
        try {
            payroll = payrollService.getPayrollSettings(id);
        } catch (Exception e) {
        }

        List<PerformanceReview> reviews = performanceReviewService.getReviewsForEmployee(id);
        List<LeaveRequest> leaveRequests = leaveRequestRepository.findByEmployee(employee);

        String filename = "Employee_Report_" + employee.getFirstName() + "_" + employee.getLastName() + ".txt";
        response.setContentType("text/plain");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");

        try (PrintWriter writer = response.getWriter()) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

            // --- HEADER ---
            writer.println("===================================================================");
            writer.println("                     EMPLOYEE CONFIDENTIAL REPORT                  ");
            writer.println("===================================================================");
            writer.println("");

            // --- SECTION 1: PERSONAL DETAILS ---
            writer.println("1. PERSONAL DETAILS");
            writer.println("-------------------");
            writer.println("Employee ID   : " + employee.getId());
            writer.println("Full Name     : " + employee.getFirstName() + " " + employee.getLastName());
            writer.println("Email         : " + employee.getEmail());
            writer.println("Department    : " + (employee.getDepartment() != null ? employee.getDepartment().getName() : "N/A"));
            writer.println("Position      : " + employee.getPosition());
            writer.println("Hire Date     : " + (employee.getHireDate() != null ? employee.getHireDate() : "N/A"));
            writer.println("");

            // --- SECTION 2: PAYROLL INFORMATION ---
            writer.println("2. FINANCIAL DETAILS");
            writer.println("--------------------");
            if (payroll != null) {
                writer.println("Base Salary         : " + payroll.getBaseSalary());
                writer.println("Standard Deductions : " + payroll.getStandardDeductions());
                if (payroll.getBaseSalary() != null && payroll.getStandardDeductions() != null) {
                    writer.println("Est. Net Salary     : " + payroll.getBaseSalary().subtract(payroll.getStandardDeductions()));
                }
            } else {
                writer.println("No payroll information configured.");
            }
            writer.println("");

            // --- SECTION 3: PERFORMANCE REVIEWS ---
            writer.println("3. PERFORMANCE HISTORY");
            writer.println("----------------------");
            if (reviews.isEmpty()) {
                writer.println("No performance reviews found.");
            } else {
                for (PerformanceReview review : reviews) {
                    writer.println("Review Date: " + review.getReviewDate());
                    writer.println("  - Overall Rating : " + review.getOverallRating() + "/10");
                    writer.println("  - Quality of Work: " + review.getQualityOfWork());
                    writer.println("  - Productivity   : " + review.getProductivity());
                    writer.println("  - Manager Comment: " + (review.getManagerComments() != null ? review.getManagerComments().replace("\n", " ") : "N/A"));
                    writer.println("-------------------------------------------------------------------");
                }
            }
            writer.println("");

            // --- SECTION 4: LEAVE HISTORY ---
            writer.println("4. LEAVE HISTORY");
            writer.println("----------------");
            if (leaveRequests == null || leaveRequests.isEmpty()) {
                writer.println("No leave records found.");
            } else {
                writer.printf("%-15s %-15s %-15s %-20s%n", "Start Date", "End Date", "Status", "Reason");
                writer.println("-------------------------------------------------------------------");
                for (LeaveRequest leave : leaveRequests) {
                    writer.printf("%-15s %-15s %-15s %-20s%n",
                            leave.getStartDate(),
                            leave.getEndDate(),
                            leave.getStatus(),
                            leave.getReason());
                }
            }
            writer.println("");

            // --- SECTION 5: TRAINING HISTORY (NEW) ---
            writer.println("5. TRAINING HISTORY");
            writer.println("-------------------");

            if (employee.getTrainingPrograms() == null || employee.getTrainingPrograms().isEmpty()) {
                writer.println("No training programs assigned.");
            } else {
                writer.printf("%-30s %-15s %-15s%n", "Program Title", "Start Date", "End Date");
                writer.println("--------------------------------------------------------------");
                for (TrainingProgram tp : employee.getTrainingPrograms()) {
                    writer.printf("%-30s %-15s %-15s%n",
                            tp.getTitle(),
                            tp.getStartDate(),
                            tp.getEndDate());
                }
            }

            writer.println("");
            writer.println("===================================================================");
            writer.println("Generated by HR Management System");
        }
    }

    // --- 2. All Employees List (CSV) ---
    @GetMapping("/admin/reports/employees")
    public void downloadEmployeeListReport(HttpServletResponse response) throws IOException {
        List<Employee> employees = employeeService.getAllEmployees();

        // Set content type to CSV
        response.setContentType("text/csv");
        response.setHeader("Content-Disposition", "attachment; filename=\"Employee_List_Report.csv\"");

        try (PrintWriter writer = response.getWriter()) {
            // Write CSV Header
            writer.println("Employee ID,First Name,Last Name,Email,Department,Position,Hire Date");

            // Write Data Rows
            for (Employee employee : employees) {
                writer.println(String.format("%d,%s,%s,%s,%s,%s,%s",
                        employee.getId(),
                        escapeCsv(employee.getFirstName()),
                        escapeCsv(employee.getLastName()),
                        escapeCsv(employee.getEmail()),
                        escapeCsv(employee.getDepartment() != null ? employee.getDepartment().getName() : "N/A"),
                        escapeCsv(employee.getPosition()),
                        employee.getHireDate()
                ));
            }
        }
    }

    private String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(",")) {
            return "\"" + value + "\"";
        }
        return value;
    }
}