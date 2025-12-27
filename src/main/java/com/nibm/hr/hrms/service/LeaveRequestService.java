package com.nibm.hr.hrms.service;

import com.nibm.hr.hrms.model.LeaveRequest;

import java.security.Principal;
import java.util.List;

public interface LeaveRequestService {
    List<LeaveRequest> getLeavesForEmployee(Long employeeId);
    List<LeaveRequest> getMyLeaveRequests(Principal principal);
    List<LeaveRequest> getPendingRequestsForManager(Principal principal);
    List<LeaveRequest> getAllPendingRequestsForAdmin();

    void createLeaveRequest(LeaveRequest leaveRequest, Principal principal);
    void approveRequestAsManager(Long id, Principal principal);
    void rejectRequestAsManager(Long id, Principal principal);
    void approveRequestAsAdmin(Long id, Principal principal);
    void rejectRequestAsAdmin(Long id, Principal principal);
}