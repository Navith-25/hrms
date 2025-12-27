package com.nibm.hr.hrms.service;

import com.nibm.hr.hrms.model.TrainingProgram;
import com.nibm.hr.hrms.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SystemService {

    @Autowired private LoanRepository loanRepository;
    @Autowired private PayslipRepository payslipRepository;
    @Autowired private PayrollRepository payrollRepository;
    @Autowired private LeaveRequestRepository leaveRequestRepository;
    @Autowired private AttendanceRepository attendanceRepository;
    @Autowired private PerformanceReviewRepository performanceReviewRepository;
    @Autowired private TaskRepository taskRepository;
    @Autowired private MessageRepository messageRepository;
    @Autowired private TrainingRepository trainingRepository;
    @Autowired private EmployeeRepository employeeRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private DepartmentRepository departmentRepository;

    @Transactional
    public void resetEntireSystem(String currentAdminUsername) {
        // 1. Delete Transactional Data (Child Records)
        loanRepository.deleteAll();
        payslipRepository.deleteAll();
        payrollRepository.deleteAll();
        leaveRequestRepository.deleteAll();
        attendanceRepository.deleteAll();
        performanceReviewRepository.deleteAll();
        taskRepository.deleteAll();
        messageRepository.deleteAll();

        // 2. Clear Training Program Associations and Data
        List<TrainingProgram> trainings = trainingRepository.findAll();
        for (TrainingProgram tp : trainings) {
            if (tp.getEmployees() != null) {
                tp.getEmployees().clear();
            }
            trainingRepository.save(tp);
        }
        trainingRepository.deleteAll();

    }
}