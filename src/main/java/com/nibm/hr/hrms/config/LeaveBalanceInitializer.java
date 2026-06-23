package com.nibm.hr.hrms.config;

import com.nibm.hr.hrms.model.Employee;
import com.nibm.hr.hrms.model.LeaveDuration;
import com.nibm.hr.hrms.model.LeaveRequest;
import com.nibm.hr.hrms.model.LeaveType;
import com.nibm.hr.hrms.repository.EmployeeRepository;
import com.nibm.hr.hrms.repository.LeaveRequestRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class LeaveBalanceInitializer implements CommandLineRunner {

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private LeaveRequestRepository leaveRequestRepository;

    @Override
    public void run(String... args) throws Exception {

        // --- 1. Fix Employee Leave Balances (0.0 Issue) ---
        List<Employee> employees = employeeRepository.findAll();
        boolean isEmpUpdated = false;

        for (Employee employee : employees) {
            // Check if the annual leave balance is 0.0 (needs initialization)
            if (employee.getAnnualLeaveBalance() == 0.0) {

                double annualBalance = 14.0;
                double casualBalance = 7.0;
                double sickBalance = 14.0;

                // Check roles to assign extra leaves for management
                if (employee.getUser() != null && employee.getUser().getRoles() != null) {
                    boolean isManagement = employee.getUser().getRoles().stream()
                            .anyMatch(r -> r.getName().equals("ROLE_MANAGER") ||
                                    r.getName().equals("ROLE_HR_MANAGER") ||
                                    r.getName().equals("ROLE_DIRECTOR"));

                    if (isManagement) {
                        annualBalance = 19.0; // 5 extra days for management
                    }
                }

                // Apply the new balances
                employee.setAnnualLeaveBalance(annualBalance);
                employee.setCasualLeaveBalance(casualBalance);
                employee.setSickLeaveBalance(sickBalance);

                // Save the updated employee record
                employeeRepository.save(employee);
                isEmpUpdated = true;
            }
        }

        if (isEmpUpdated) {
            System.out.println("SUCCESS: Existing employee leave balances were successfully updated to standard limits!");
        }

        // --- 2. Fix Old Leave Requests (NULL Reference Issue) ---
        List<LeaveRequest> leaveRequests = leaveRequestRepository.findAll();
        boolean isReqUpdated = false;

        for (LeaveRequest request : leaveRequests) {
            boolean needsUpdate = false;

            // If an old record has a null leaveType, set a default
            if (request.getLeaveType() == null) {
                request.setLeaveType(LeaveType.ANNUAL);
                needsUpdate = true;
            }

            // If an old record has a null duration, set a default
            if (request.getDuration() == null) {
                request.setDuration(LeaveDuration.FULL_DAY);
                needsUpdate = true;
            }

            // Save only if changes were made
            if (needsUpdate) {
                leaveRequestRepository.save(request);
                isReqUpdated = true;
            }
        }

        if (isReqUpdated) {
            System.out.println("SUCCESS: Old Leave Requests with NULL values were automatically fixed!");
        }
    }
}