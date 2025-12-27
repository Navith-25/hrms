package com.nibm.hr.hrms.controller;

import com.nibm.hr.hrms.model.Employee;
import com.nibm.hr.hrms.model.PerformanceReview;
import com.nibm.hr.hrms.model.Task;
import com.nibm.hr.hrms.service.EmployeeService;
import com.nibm.hr.hrms.service.LeaveRequestService;
import com.nibm.hr.hrms.service.PerformanceReviewService;
import com.nibm.hr.hrms.service.TaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.LocalDate;
import java.util.List;

@Controller
public class ManagerController {

    @Autowired
    private LeaveRequestService leaveRequestService;

    @Autowired
    private PerformanceReviewService performanceReviewService;

    @Autowired
    private EmployeeService employeeService;

    @Autowired
    private TaskService taskService;

    @GetMapping("/manager/team")
    public String showManagerDashboard() {
        return "redirect:/manager/leave";
    }

    @GetMapping("/manager/leave")
    public String showManagerLeavePage(Model model, Principal principal) {
        model.addAttribute("pendingRequests", leaveRequestService.getPendingRequestsForManager(principal));
        return "manager_leave_approval";
    }

    @PostMapping("/manager/leave/approve/{id}")
    public String approveLeaveRequest(@PathVariable("id") Long id, Principal principal) {
        leaveRequestService.approveRequestAsManager(id, principal);
        return "redirect:/manager/leave";
    }

    @PostMapping("/manager/leave/reject/{id}")
    public String rejectLeaveRequest(@PathVariable("id") Long id, Principal principal) {
        leaveRequestService.rejectRequestAsManager(id, principal);
        return "redirect:/manager/leave";
    }

    @GetMapping("/manager/performance")
    public String showTeamPerformance(Model model, Principal principal) {
        List<Employee> team = performanceReviewService.getEmployeesManagedBy(principal);
        model.addAttribute("teamEmployees", team);
        return "manager_performance";
    }

    @GetMapping("/manager/performance/new/{employeeId}")
    public String showNewReviewForm(@PathVariable Long employeeId, Model model) {
        Employee employee = employeeService.getEmployeeById(employeeId);

        PerformanceReview review = new PerformanceReview();
        review.setEmployee(employee);
        review.setReviewDate(LocalDate.now());

        model.addAttribute("review", review);
        model.addAttribute("employeeName", employee.getFirstName() + " " + employee.getLastName());
        model.addAttribute("postUrl", "/manager/performance/save");
        model.addAttribute("backLink", "/manager/performance");

        return "admin_performance_form";
    }

    @PostMapping("/manager/performance/save")
    public String saveManagerReview(@ModelAttribute("review") PerformanceReview review, Principal principal) {
        performanceReviewService.saveReviewAsManager(review, principal);
        return "redirect:/manager/performance";
    }

    @GetMapping("/manager/tasks")
    public String showManagerTasks(Model model, Principal principal) {
        model.addAttribute("tasks", taskService.getTasksAssignedByManager(principal));
        return "manager_tasks";
    }

    @GetMapping("/manager/tasks/new")
    public String showAssignTaskForm(Model model, Principal principal) {
        model.addAttribute("task", new Task());
        model.addAttribute("teamMembers", taskService.getMyTeam(principal));
        return "manager_task_form";
    }

    @PostMapping("/manager/tasks/save")
    public String saveTask(@ModelAttribute("task") Task task,
                           @RequestParam("employeeId") Long employeeId,
                           Principal principal) {
        taskService.assignTask(task, employeeId, principal);
        return "redirect:/manager/tasks";
    }

    @GetMapping("/manager/tasks/review/{id}")
    public String showReviewTaskForm(@PathVariable("id") Long id, Model model) {
        model.addAttribute("task", taskService.getTaskById(id));
        return "manager_task_review";
    }

    @PostMapping("/manager/tasks/review")
    public String saveTaskReview(@RequestParam("taskId") Long taskId,
                                 @RequestParam("rating") Integer rating,
                                 @RequestParam("comments") String comments,
                                 Principal principal) {
        taskService.reviewTask(taskId, rating, comments, principal);
        return "redirect:/manager/tasks";
    }
}