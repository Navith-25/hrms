package com.nibm.hr.hrms.service;

import com.nibm.hr.hrms.model.*;
import com.nibm.hr.hrms.repository.DepartmentRepository;
import com.nibm.hr.hrms.repository.EmployeeRepository;
import com.nibm.hr.hrms.repository.LeaveRequestRepository;
import com.nibm.hr.hrms.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.security.Principal;
import java.util.List;

@Service
public class LeaveRequestServiceImpl implements LeaveRequestService {

    @Autowired
    private LeaveRequestRepository leaveRequestRepository;

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
    public List<LeaveRequest> getLeavesForEmployee(Long employeeId) {
        Employee employee = employeeRepository.findById(employeeId).orElse(null);
        if (employee == null) return java.util.Collections.emptyList();
        return leaveRequestRepository.findByEmployee(employee);
    }

    @Override
    public List<LeaveRequest> getMyLeaveRequests(Principal principal) {
        Employee employee = getEmployeeFromPrincipal(principal);
        return leaveRequestRepository.findByEmployee(employee);
    }

    @Override
    public void createLeaveRequest(LeaveRequest leaveRequest, Principal principal) {
        Employee employee = getEmployeeFromPrincipal(principal);
        leaveRequest.setEmployee(employee);
        leaveRequest.setStatus(LeaveStatus.PENDING);
        leaveRequestRepository.save(leaveRequest);
    }

    @Override
    public List<LeaveRequest> getPendingRequestsForManager(Principal principal) {
        Employee manager = getEmployeeFromPrincipal(principal);
        Department managedDepartment = departmentRepository.findByManager(manager).orElse(null);
        if (managedDepartment == null) {
            return List.of();
        }
        return leaveRequestRepository.findByStatusAndEmployee_Department(LeaveStatus.PENDING, managedDepartment);
    }

    @Override
    public void approveRequestAsManager(Long id, Principal principal) {
        Employee manager = getEmployeeFromPrincipal(principal);
        LeaveRequest request = leaveRequestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Leave request not found"));

        if (request.getEmployee().getId().equals(manager.getId())) {
            throw new AccessDeniedException("Managers cannot approve their own leave requests.");
        }

        if (!request.getEmployee().getDepartment().getManager().equals(manager)) {
            throw new AccessDeniedException("You are not authorized to approve this request.");
        }

        request.setStatus(LeaveStatus.APPROVED);
        leaveRequestRepository.save(request);
    }

    @Override
    public void rejectRequestAsManager(Long id, Principal principal) {
        Employee manager = getEmployeeFromPrincipal(principal);
        LeaveRequest request = leaveRequestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Leave request not found"));

        if (request.getEmployee().getId().equals(manager.getId())) {
            throw new AccessDeniedException("Managers cannot process their own leave requests.");
        }

        if (!request.getEmployee().getDepartment().getManager().equals(manager)) {
            throw new AccessDeniedException("You are not authorized to reject this request.");
        }

        request.setStatus(LeaveStatus.REJECTED);
        leaveRequestRepository.save(request);
    }

    @Override
    public List<LeaveRequest> getAllPendingRequestsForAdmin() {
        return leaveRequestRepository.findByStatus(LeaveStatus.PENDING);
    }

    @Override
    public void approveRequestAsAdmin(Long id, Principal principal) {
        Employee approver = getEmployeeFromPrincipal(principal);
        LeaveRequest request = leaveRequestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Leave request not found"));

        if (request.getEmployee().getId().equals(approver.getId())) {
            throw new AccessDeniedException("You cannot approve your own leave request.");
        }

        boolean isApproverStaff = approver.getUser().getRoles().stream()
                .anyMatch(r -> r.getName().equals("ROLE_HR_STAFF"));
        boolean isTargetManager = request.getEmployee().getUser().getRoles().stream()
                .anyMatch(r -> r.getName().equals("ROLE_HR_MANAGER"));

        if (isApproverStaff && isTargetManager) {
            throw new AccessDeniedException("HR Staff cannot approve leave requests for HR Managers.");
        }

        request.setStatus(LeaveStatus.APPROVED);
        leaveRequestRepository.save(request);
    }
    @Override
    public void rejectRequestAsAdmin(Long id, Principal principal) {
        Employee approver = getEmployeeFromPrincipal(principal);
        LeaveRequest request = leaveRequestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Leave request not found"));
        if (request.getEmployee().getId().equals(approver.getId())) {
            throw new AccessDeniedException("You cannot reject your own leave request.");
        }

        boolean isApproverStaff = approver.getUser().getRoles().stream()
                .anyMatch(r -> r.getName().equals("ROLE_HR_STAFF"));
        boolean isTargetManager = request.getEmployee().getUser().getRoles().stream()
                .anyMatch(r -> r.getName().equals("ROLE_HR_MANAGER"));

        if (isApproverStaff && isTargetManager) {
            throw new AccessDeniedException("HR Staff cannot reject leave requests for HR Managers.");
        }

        request.setStatus(LeaveStatus.REJECTED);
        leaveRequestRepository.save(request);
    }
}