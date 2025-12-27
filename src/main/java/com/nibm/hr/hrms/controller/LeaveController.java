package com.nibm.hr.hrms.controller;

import com.nibm.hr.hrms.service.LeaveRequestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import java.security.Principal;

@Controller
public class LeaveController {

    @Autowired
    private LeaveRequestService leaveRequestService;

    @GetMapping("/admin/leave")
    public String showAdminLeavePage(Model model) {
        model.addAttribute("pendingRequests", leaveRequestService.getAllPendingRequestsForAdmin());
        return "admin_leave_approval";
    }

    @PostMapping("/admin/leave/approve/{id}")
    public String approveLeaveRequest(@PathVariable("id") Long id, Principal principal) {
        leaveRequestService.approveRequestAsAdmin(id, principal);
        return "redirect:/admin/leave";
    }

    @PostMapping("/admin/leave/reject/{id}")
    public String rejectLeaveRequest(@PathVariable("id") Long id, Principal principal) {
        leaveRequestService.rejectRequestAsAdmin(id, principal);
        return "redirect:/admin/leave";
    }
}