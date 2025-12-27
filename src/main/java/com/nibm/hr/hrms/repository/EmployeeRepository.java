package com.nibm.hr.hrms.repository;

import com.nibm.hr.hrms.model.Department;
import com.nibm.hr.hrms.model.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    List<Employee> findByDepartment(Department department);
    List<Employee> findByUser_Roles_NameIn(Collection<String> names);
}