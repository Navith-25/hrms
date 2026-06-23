package com.nibm.hr.hrms.service;

import com.nibm.hr.hrms.model.*;
import com.nibm.hr.hrms.repository.DepartmentRepository;
import com.nibm.hr.hrms.repository.EmployeeRepository;
import com.nibm.hr.hrms.repository.LeaveRequestRepository;
import com.nibm.hr.hrms.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.Principal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

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

    private final String UPLOAD_DIR = "uploads/medical_certs/";

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

    private void refundLeaveBalance(LeaveRequest request) {
        Employee employee = request.getEmployee();
        boolean isDirector = employee.getUser().getRoles().stream()
                .anyMatch(r -> r.getName().equals("ROLE_DIRECTOR"));

        if (isDirector || request.getLeaveType() == LeaveType.UNPAID) return;

        double daysToRefund = 0;
        if (request.getStartDate().isEqual(request.getEndDate())) {
            daysToRefund = (request.getDuration() == LeaveDuration.FULL_DAY) ? 1.0 : 0.5;
        } else {
            LocalDate date = request.getStartDate();
            while (!date.isAfter(request.getEndDate())) {
                DayOfWeek day = date.getDayOfWeek();
                if (day != DayOfWeek.SATURDAY && day != DayOfWeek.SUNDAY) {
                    daysToRefund += 1.0;
                }
                date = date.plusDays(1);
            }
        }

        switch (request.getLeaveType()) {
            case ANNUAL: employee.setAnnualLeaveBalance(employee.getAnnualLeaveBalance() + daysToRefund); break;
            case CASUAL: employee.setCasualLeaveBalance(employee.getCasualLeaveBalance() + daysToRefund); break;
            case SICK: employee.setSickLeaveBalance(employee.getSickLeaveBalance() + daysToRefund); break;
        }
        employeeRepository.save(employee);
    }

    @Scheduled(cron = "0 0 0 1 1 *")
    @Transactional
    public void resetAllLeaveBalances() {
        List<Employee> allEmployees = employeeRepository.findAll();
        for (Employee emp : allEmployees) {
            double standardAnnual = 14.0;

            if (emp.getUser() != null && emp.getUser().getRoles() != null) {
                boolean isManagement = emp.getUser().getRoles().stream()
                        .anyMatch(r -> r.getName().equals("ROLE_MANAGER") ||
                                r.getName().equals("ROLE_HR_MANAGER") ||
                                r.getName().equals("ROLE_DIRECTOR"));
                if (isManagement) {
                    standardAnnual = 19.0;
                }
            }

            double carryForward = Math.min(5.0, emp.getAnnualLeaveBalance());

            emp.setAnnualLeaveBalance(standardAnnual + carryForward);
            emp.setCasualLeaveBalance(7.0);
            emp.setSickLeaveBalance(14.0);
        }
        employeeRepository.saveAll(allEmployees);
        System.out.println("SYSTEM: All employee leave balances successfully reset with Carry-Forward.");
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
    @Transactional
    public void createLeaveRequest(LeaveRequest leaveRequest, MultipartFile medicalCert, Principal principal) {
        Employee employee = getEmployeeFromPrincipal(principal);

        if (leaveRequest.getStartDate().isBefore(LocalDate.now().minusDays(3))) {
            throw new RuntimeException("Action Denied: You cannot apply for a leave more than 3 days in the past.");
        }

        List<LeaveRequest> existingLeaves = leaveRequestRepository.findByEmployee(employee);
        boolean overlaps = existingLeaves.stream()
                .filter(req -> req.getStatus() != LeaveStatus.REJECTED)
                .anyMatch(req ->
                        !leaveRequest.getStartDate().isAfter(req.getEndDate()) &&
                                !leaveRequest.getEndDate().isBefore(req.getStartDate())
                );
        if (overlaps) {
            throw new RuntimeException("Leave dates overlap with an existing request.");
        }

        double daysRequested = 0;
        if (leaveRequest.getStartDate().isEqual(leaveRequest.getEndDate())) {
            DayOfWeek day = leaveRequest.getStartDate().getDayOfWeek();
            if (day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY) {
                throw new RuntimeException("Leave cannot be applied on a weekend.");
            }
            daysRequested = (leaveRequest.getDuration() == LeaveDuration.FULL_DAY) ? 1.0 : 0.5;
        } else {
            if (leaveRequest.getDuration() != LeaveDuration.FULL_DAY) {
                throw new RuntimeException("Half-day leaves can only be applied for a single date.");
            }
            LocalDate date = leaveRequest.getStartDate();
            while (!date.isAfter(leaveRequest.getEndDate())) {
                DayOfWeek day = date.getDayOfWeek();
                if (day != DayOfWeek.SATURDAY && day != DayOfWeek.SUNDAY) {
                    daysRequested += 1.0;
                }
                date = date.plusDays(1);
            }
            if (daysRequested == 0) {
                throw new RuntimeException("The selected date range only contains weekends.");
            }
        }

        if (leaveRequest.getLeaveType() == LeaveType.SICK && daysRequested > 2) {
            if (medicalCert == null || medicalCert.isEmpty()) {
                throw new RuntimeException("A Medical Certificate must be uploaded for sick leaves exceeding 2 days.");
            }
            try {
                Path uploadPath = Paths.get(UPLOAD_DIR);
                if (!Files.exists(uploadPath)) {
                    Files.createDirectories(uploadPath);
                }
                String originalFilename = medicalCert.getOriginalFilename();
                String fileExtension = originalFilename != null ? originalFilename.substring(originalFilename.lastIndexOf(".")) : ".pdf";
                String newFileName = UUID.randomUUID().toString() + fileExtension;

                Files.copy(medicalCert.getInputStream(), uploadPath.resolve(newFileName), StandardCopyOption.REPLACE_EXISTING);
                leaveRequest.setMedicalCertificatePath(newFileName);
            } catch (IOException e) {
                throw new RuntimeException("Failed to upload the medical certificate. Please try again.");
            }
        }

        boolean isDirector = employee.getUser().getRoles().stream()
                .anyMatch(r -> r.getName().equals("ROLE_DIRECTOR"));

        if (!isDirector && leaveRequest.getLeaveType() != LeaveType.UNPAID) {
            switch (leaveRequest.getLeaveType()) {
                case ANNUAL:
                    if (employee.getAnnualLeaveBalance() < daysRequested) throw new RuntimeException("Insufficient Annual Leave balance.");
                    employee.setAnnualLeaveBalance(employee.getAnnualLeaveBalance() - daysRequested);
                    break;
                case CASUAL:
                    if (employee.getCasualLeaveBalance() < daysRequested) throw new RuntimeException("Insufficient Casual Leave balance.");
                    employee.setCasualLeaveBalance(employee.getCasualLeaveBalance() - daysRequested);
                    break;
                case SICK:
                    if (employee.getSickLeaveBalance() < daysRequested) throw new RuntimeException("Insufficient Sick Leave balance.");
                    employee.setSickLeaveBalance(employee.getSickLeaveBalance() - daysRequested);
                    break;
            }
            employeeRepository.save(employee);
        }

        leaveRequest.setEmployee(employee);
        leaveRequest.setStatus(isDirector ? LeaveStatus.APPROVED : LeaveStatus.PENDING);
        leaveRequestRepository.save(leaveRequest);
    }

    @Override
    @Transactional
    public void cancelLeaveRequest(Long id, Principal principal) {
        Employee employee = getEmployeeFromPrincipal(principal);
        LeaveRequest request = leaveRequestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Leave request not found"));

        if (!request.getEmployee().getId().equals(employee.getId())) {
            throw new AccessDeniedException("You are not authorized to cancel this request.");
        }

        if (request.getStatus() != LeaveStatus.PENDING) {
            throw new RuntimeException("Only PENDING requests can be cancelled.");
        }

        refundLeaveBalance(request);
        leaveRequestRepository.delete(request);
    }

    @Override
    public List<LeaveRequest> getPendingRequestsForManager(Principal principal) {
        Employee manager = getEmployeeFromPrincipal(principal);

        Department managedDepartment = departmentRepository.findByManager(manager).orElse(null);

        // FIXED: Fallback logic ONLY for users who have BOTH ROLE_FINANCE AND ROLE_MANAGER
        if (managedDepartment == null) {
            boolean isFinance = manager.getUser().getRoles().stream()
                    .anyMatch(r -> r.getName().equals("ROLE_FINANCE"));
            boolean isManagerRole = manager.getUser().getRoles().stream()
                    .anyMatch(r -> r.getName().equals("ROLE_MANAGER") || r.getName().equals("ROLE_HR_MANAGER"));

            if (isFinance && isManagerRole) {
                return leaveRequestRepository.findByStatus(LeaveStatus.PENDING).stream()
                        .filter(req -> req.getEmployee().getDepartment() != null
                                && req.getEmployee().getDepartment().getName().equalsIgnoreCase("Finance"))
                        .filter(req -> !req.getEmployee().getId().equals(manager.getId()))
                        .collect(Collectors.toList());
            }
            return List.of();
        }

        return leaveRequestRepository.findByStatusAndEmployee_Department(LeaveStatus.PENDING, managedDepartment)
                .stream()
                .filter(req -> !req.getEmployee().getId().equals(manager.getId()))
                .collect(Collectors.toList());
    }

    @Override
    public void approveRequestAsManager(Long id, Principal principal) {
        Employee manager = getEmployeeFromPrincipal(principal);
        LeaveRequest request = leaveRequestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Leave request not found"));

        if (request.getEmployee().getId().equals(manager.getId())) {
            throw new AccessDeniedException("Managers cannot approve their own leave requests.");
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

        refundLeaveBalance(request);
        request.setStatus(LeaveStatus.REJECTED);
        leaveRequestRepository.save(request);
    }

    @Override
    public List<LeaveRequest> getAllPendingRequestsForAdmin(Principal principal) {
        Employee approver = getEmployeeFromPrincipal(principal);
        User approverUser = approver.getUser();
        List<LeaveRequest> allPending = leaveRequestRepository.findByStatus(LeaveStatus.PENDING);

        boolean isDirector = approverUser.getRoles().stream().anyMatch(r -> r.getName().equals("ROLE_DIRECTOR"));
        boolean isAdmin = approverUser.getRoles().stream().anyMatch(r -> r.getName().equals("ROLE_ADMIN"));

        if (isAdmin) {
            return java.util.Collections.emptyList();
        }

        return allPending.stream()
                .filter(req -> !req.getEmployee().getId().equals(approver.getId()))
                .filter(req -> {
                    if (isDirector) {
                        boolean targetIsManager = req.getEmployee().getUser().getRoles().stream()
                                .anyMatch(r -> r.getName().equals("ROLE_MANAGER"));
                        boolean targetIsHrManager = req.getEmployee().getUser().getRoles().stream()
                                .anyMatch(r -> r.getName().equals("ROLE_HR_MANAGER"));
                        return targetIsManager || targetIsHrManager;
                    }
                    return true;
                })
                .collect(Collectors.toList());
    }

    @Override
    public void approveRequestAsAdmin(Long id, Principal principal) {
        Employee approver = getEmployeeFromPrincipal(principal);
        LeaveRequest request = leaveRequestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Leave request not found"));

        boolean isAdmin = approver.getUser().getRoles().stream().anyMatch(r -> r.getName().equals("ROLE_ADMIN"));
        if (isAdmin) {
            throw new AccessDeniedException("Admin cannot approve leave requests.");
        }

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

        boolean isAdmin = approver.getUser().getRoles().stream().anyMatch(r -> r.getName().equals("ROLE_ADMIN"));
        if (isAdmin) {
            throw new AccessDeniedException("Admin cannot reject leave requests.");
        }

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

        refundLeaveBalance(request);
        request.setStatus(LeaveStatus.REJECTED);
        leaveRequestRepository.save(request);
    }
}