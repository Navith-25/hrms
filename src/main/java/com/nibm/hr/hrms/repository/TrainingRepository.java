package com.nibm.hr.hrms.repository;

import com.nibm.hr.hrms.model.Department;
import com.nibm.hr.hrms.model.Employee;
import com.nibm.hr.hrms.model.TrainingProgram;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TrainingRepository extends JpaRepository<TrainingProgram, Long> {
    List<TrainingProgram> findByDepartment(Department department);
    List<TrainingProgram> findByEmployeesContaining(Employee employee);
}