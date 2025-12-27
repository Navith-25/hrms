package com.nibm.hr.hrms.service;

import com.nibm.hr.hrms.model.Department;
import com.nibm.hr.hrms.model.Employee;
import com.nibm.hr.hrms.model.TrainingProgram;
import com.nibm.hr.hrms.repository.DepartmentRepository;
import com.nibm.hr.hrms.repository.EmployeeRepository;
import com.nibm.hr.hrms.repository.TrainingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TrainingService {

    @Autowired
    private TrainingRepository trainingRepository;
    @Autowired
    private DepartmentRepository departmentRepository;
    @Autowired
    private EmployeeRepository employeeRepository;

    // CEO: Create Training
    public void createTraining(TrainingProgram training, Long departmentId) {
        Department dept = departmentRepository.findById(departmentId)
                .orElseThrow(() -> new RuntimeException("Department not found"));
        training.setDepartment(dept);
        trainingRepository.save(training);
    }

    // Manager: Get Trainings for their Dept
    public List<TrainingProgram> getTrainingsForDepartment(Department department) {
        return trainingRepository.findByDepartment(department);
    }

    public TrainingProgram getTrainingById(Long id) {
        return trainingRepository.findById(id).orElseThrow(() -> new RuntimeException("Training not found"));
    }

    // Manager: Assign Employees
    @Transactional
    public void assignEmployeesToTraining(Long trainingId, List<Long> employeeIds) {
        TrainingProgram training = getTrainingById(trainingId);
        List<Employee> employees = employeeRepository.findAllById(employeeIds);

        training.getAssignedEmployees().addAll(employees);
        trainingRepository.save(training);
    }

    public List<TrainingProgram> getTrainingsForEmployee(Employee employee) {
        return trainingRepository.findByEmployeesContaining(employee);
    }
}