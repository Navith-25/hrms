package com.nibm.hr.hrms.repository;

import com.nibm.hr.hrms.model.Department;
import com.nibm.hr.hrms.model.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DepartmentRepository extends JpaRepository<Department, Long> {
    Optional<Department> findByName(String name);
    Optional<Department> findByManager(Employee manager);
}