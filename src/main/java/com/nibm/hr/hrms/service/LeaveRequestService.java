package com.nibm.hr.hrms.service;

import com.nibm.hr.hrms.model.LeaveRequest;
import org.springframework.web.multipart.MultipartFile;
import java.security.Principal;
import java.util.List;

public interface LeaveRequestService {
    List<LeaveRequest> getLeavesForEmployee(Long employeeId);
    List<LeaveRequest> getMyLeaveRequests(Principal principal);
    List<LeaveRequest> getPendingRequestsForManager(Principal principal);
    List<LeaveRequest> getAllPendingRequestsForAdmin(Principal principal);

    // UPDATED: Added MultipartFile for Medical Certificate
    void createLeaveRequest(LeaveRequest leaveRequest, MultipartFile medicalCert, Principal principal);
    void approveRequestAsManager(Long id, Principal principal);
    void rejectRequestAsManager(Long id, Principal principal);
    void approveRequestAsAdmin(Long id, Principal principal);
    void rejectRequestAsAdmin(Long id, Principal principal);
    void cancelLeaveRequest(Long id, Principal principal);
}