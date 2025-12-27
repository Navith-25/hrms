package com.nibm.hr.hrms.controller;

import com.nibm.hr.hrms.model.*;
import com.nibm.hr.hrms.service.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Controller
public class AttendanceController {

    @Autowired
    private AttendanceService attendanceService;

    @Autowired
    private EmployeeService employeeService;

    @Autowired
    private LeaveRequestService leaveRequestService;

    @Autowired
    private PayrollService payrollService;

    @Autowired
    private TaskService taskService;

    @Autowired
    private TrainingService trainingService;

    @Autowired
    private LoanService loanService;

    @Autowired
    private PerformanceReviewService performanceReviewService;

    // --- 1. View Calendar Page ---
    @GetMapping("/my-calendar")
    public String showMyCalendar(Model model, Principal principal) {
        Employee employee = employeeService.getEmployeeByUsername(principal.getName());
        model.addAttribute("employee", employee);
        return "my_calendar";
    }

    // --- 2. API Endpoint for FullCalendar ---
    @GetMapping("/api/calendar/events")
    @ResponseBody
    public List<CalendarEvent> getCalendarEvents(Principal principal) {

        Employee employee = employeeService.getEmployeeByUsername(principal.getName());
        List<CalendarEvent> events = new ArrayList<>();
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");

        // A. Attendance (Green)
        List<Attendance> attendanceList = attendanceService.getAttendanceForEmployee(employee);
        for (Attendance att : attendanceList) {
            if (att.getDate() != null) {
                CalendarEvent event = new CalendarEvent();
                event.setId("att-" + att.getId());

                String title = "Present";
                StringBuilder desc = new StringBuilder("Status: " + att.getStatus());

                if (att.getInTime() != null) {
                    title += " (" + att.getInTime().format(timeFormatter) + ")";
                    desc.append("\nIn: ").append(att.getInTime().format(timeFormatter));
                }
                if (att.getOutTime() != null) {
                    desc.append("\nOut: ").append(att.getOutTime().format(timeFormatter));
                }

                event.setTitle(title);
                event.setStart(att.getDate().toString());
                event.setDescription(desc.toString());
                event.setColor("#28a745");
                event.setAllDay(true);
                events.add(event);
            }
        }

        // B. Leave Requests (Yellow/Red)
        List<LeaveRequest> leaveList = leaveRequestService.getLeavesForEmployee(employee.getId());
        for (LeaveRequest leave : leaveList) {
            CalendarEvent event = new CalendarEvent();
            event.setId("leave-" + leave.getId());
            event.setTitle("Leave: " + leave.getLeaveType());
            event.setStart(leave.getStartDate().toString());
            event.setEnd(leave.getEndDate().plusDays(1).toString());
            event.setDescription("Reason: " + leave.getReason() + "\nStatus: " + leave.getStatus());

            if (leave.getStatus() == LeaveStatus.APPROVED) {
                event.setColor("#ffc107");
                event.setTextColor("#000000");
            } else if (leave.getStatus() == LeaveStatus.PENDING) {
                event.setColor("#6c757d");
            } else {
                event.setColor("#dc3545");
            }
            event.setAllDay(true);
            events.add(event);
        }

        // C. Payslips (Blue)
        List<Payslip> payslips = payrollService.getPayslipsForEmployee(principal);
        for (Payslip slip : payslips) {
            CalendarEvent event = new CalendarEvent();
            event.setId("pay-" + slip.getId());
            event.setTitle("Salary Paid");
            event.setStart(slip.getPayDate().toString());
            event.setDescription("Net Pay: " + slip.getNetPay() + "\nBonus: " + slip.getBonus());
            event.setColor("#0d6efd");
            event.setAllDay(true);
            events.add(event);
        }

        // D. Tasks (Purple)
        if (taskService != null) {
            List<Task> tasks = taskService.getMyTasks(principal);
            for (Task task : tasks) {
                CalendarEvent event = new CalendarEvent();
                event.setId("task-" + task.getId());
                event.setTitle("DEADLINE: " + task.getTitle());
                event.setDescription("Desc: " + task.getDescription() + "\nStatus: " + task.getStatus());

                if (task.getDeadline() != null) {
                    event.setStart(task.getDeadline().toString());
                    event.setColor("#6f42c1");
                    event.setAllDay(true);
                    events.add(event);
                }
            }
        }

        // E. Training (Cyan)
        if (trainingService != null) {
            List<TrainingProgram> trainings = trainingService.getTrainingsForEmployee(employee);
            for (TrainingProgram training : trainings) {
                CalendarEvent event = new CalendarEvent();
                event.setId("train-" + training.getId());
                event.setTitle("Training: " + training.getTitle());
                event.setDescription("Details: " + training.getDescription());

                if (training.getStartDate() != null) {
                    event.setStart(training.getStartDate().toString());
                    if (training.getEndDate() != null) {
                        event.setEnd(training.getEndDate().plusDays(1).toString());
                    }
                    event.setColor("#0dcaf0");
                    event.setTextColor("#000000");
                    event.setAllDay(true);
                    events.add(event);
                }
            }
        }

        // F. Loans (Teal)
        if (loanService != null) {
            List<Loan> loans = loanService.getLoansForEmployee(employee);
            for (Loan loan : loans) {
                CalendarEvent event = new CalendarEvent();
                event.setId("loan-" + loan.getId());
                event.setTitle("Loan Request: " + loan.getTotalAmount());

                String desc = "Amount: " + loan.getTotalAmount() +
                        "\nRepayment: " + loan.getRepaymentMonths() + " Months" +
                        "\nReason: " + loan.getReason() +
                        "\nStatus: " + loan.getStatus();

                event.setDescription(desc);

                if (loan.getRequestDate() != null) {
                    event.setStart(loan.getRequestDate().toString());
                    event.setColor("#20c997");
                    event.setAllDay(true);
                    events.add(event);
                }
            }
        }

        // G. Performance Reviews (Orange)
        if (performanceReviewService != null) {
            List<PerformanceReview> reviews = performanceReviewService.getReviewsForEmployee(employee);
            for (PerformanceReview review : reviews) {
                CalendarEvent event = new CalendarEvent();
                event.setId("review-" + review.getId());
                event.setTitle("Performance Review");

                String desc = "Overall Rating: " + review.getOverallRating() + "/10" +
                        "\nQuality: " + review.getQualityOfWork() +
                        "\nCommunication: " + review.getCommunication() +
                        "\nComments: " + review.getManagerComments();

                event.setDescription(desc);

                if (review.getReviewDate() != null) {
                    event.setStart(review.getReviewDate().toString());
                    event.setColor("#fd7e14");
                    event.setAllDay(true);
                    events.add(event);
                }
            }
        }

        return events;
    }

    // --- DTO Class ---
    public static class CalendarEvent {
        private String id;
        private String title;
        private String start;
        private String end;
        private String description;
        private String color;
        private String textColor;
        private boolean allDay;

        // Getters and Setters
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getStart() { return start; }
        public void setStart(String start) { this.start = start; }
        public String getEnd() { return end; }
        public void setEnd(String end) { this.end = end; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public String getColor() { return color; }
        public void setColor(String color) { this.color = color; }
        public String getTextColor() { return textColor; }
        public void setTextColor(String textColor) { this.textColor = textColor; }
        public boolean isAllDay() { return allDay; }
        public void setAllDay(boolean allDay) { this.allDay = allDay; }
    }

    // --- 3. Admin Endpoints ---

    @GetMapping("/admin/attendance")
    public String showMarkAttendanceForm(Model model) {
        List<Employee> employees = employeeService.getAllEmployees();
        model.addAttribute("employees", employees);
        // Added 'today' attribute so the form has a default date value
        model.addAttribute("today", LocalDate.now());
        return "mark_attendance";
    }

    // --- UPDATED METHOD: Handles bulk attendance submission ---
    @PostMapping("/admin/attendance/save")
    public String saveAttendance(@RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                                 HttpServletRequest request) {

        List<Employee> employees = employeeService.getAllEmployees();

        for (Employee emp : employees) {
            // Retrieve the radio button value for this specific employee
            // The name attribute in HTML is 'status_{id}'
            String statusParam = "status_" + emp.getId();
            String statusValue = request.getParameter(statusParam);

            if (statusValue != null && !statusValue.isEmpty()) {
                // Save using the service's helper method
                attendanceService.saveAttendance(emp.getId(), date, statusValue);
            }
        }

        return "redirect:/admin/attendance?success";
    }
}