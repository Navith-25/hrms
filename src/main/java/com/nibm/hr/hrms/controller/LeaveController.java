package com.nibm.hr.hrms.controller;

import com.nibm.hr.hrms.model.LeaveRequest;
import com.nibm.hr.hrms.service.LeaveRequestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.Principal;

@Controller
public class LeaveController {

    @Autowired
    private LeaveRequestService leaveRequestService;

    // Upload folder path
    private final String UPLOAD_DIR = "uploads/medical_certs/";

    // ==========================================
    // 1. ADMIN LEAVE APPROVAL ENDPOINTS
    // ==========================================
    @GetMapping("/admin/leave")
    public String showAdminLeavePage(Model model, Principal principal) {
        model.addAttribute("pendingRequests", leaveRequestService.getAllPendingRequestsForAdmin(principal));
        model.addAttribute("postUrlPrefix", "/admin/leave");
        return "admin_leave_approval";
    }

    @PostMapping("/admin/leave/approve/{id}")
    public String approveLeaveRequestAdmin(@PathVariable("id") Long id, Principal principal) {
        leaveRequestService.approveRequestAsAdmin(id, principal);
        return "redirect:/admin/leave";
    }

    @PostMapping("/admin/leave/reject/{id}")
    public String rejectLeaveRequestAdmin(@PathVariable("id") Long id, Principal principal) {
        leaveRequestService.rejectRequestAsAdmin(id, principal);
        return "redirect:/admin/leave";
    }

    // ==========================================
    // 2. EMPLOYEE LEAVE ACTIONS
    // ==========================================
    @PostMapping("/leave/cancel/{id}")
    public String cancelLeaveRequest(@PathVariable("id") Long id, Principal principal, RedirectAttributes redirectAttributes) {
        try {
            leaveRequestService.cancelLeaveRequest(id, principal);
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/leave";
    }

    @PostMapping("/leave/request")
    public String submitLeaveRequest(@ModelAttribute("leaveRequest") LeaveRequest leaveRequest,
                                     @RequestParam(value = "medicalCert", required = false) MultipartFile medicalCert,
                                     Principal principal, RedirectAttributes redirectAttributes) {
        try {
            leaveRequestService.createLeaveRequest(leaveRequest, medicalCert, principal);
            redirectAttributes.addFlashAttribute("successMessage", "Leave request submitted successfully!");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/leave";
    }

    // ==========================================
    // 3. VIEW / DOWNLOAD MEDICAL CERTIFICATE
    // ==========================================
    // FIXED: Added :.+ to ensure the file extension (.pdf, .jpg) is NOT truncated by Spring Boot
    @GetMapping("/leave/cert/{fileName:.+}")
    public ResponseEntity<Resource> viewCertificate(@PathVariable("fileName") String fileName) {
        try {
            Path filePath = Paths.get(UPLOAD_DIR).resolve(fileName).normalize();
            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists() && resource.isReadable()) {
                String contentType = "application/pdf";
                if(fileName.toLowerCase().endsWith(".jpg") || fileName.toLowerCase().endsWith(".jpeg")) contentType = "image/jpeg";
                else if(fileName.toLowerCase().endsWith(".png")) contentType = "image/png";

                return ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(contentType))
                        .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                        .body(resource);
            } else {
                System.out.println("WARNING: Certificate file not found -> " + filePath.toAbsolutePath());
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }
}