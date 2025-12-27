package com.nibm.hr.hrms.service;

import com.nibm.hr.hrms.dto.NewEmployeeRequest;
import com.nibm.hr.hrms.model.Employee;
import java.util.List;

public interface EmployeeService {
    List<Employee> getAllEmployees();
    Employee createNewEmployee(NewEmployeeRequest request);
    Employee updateEmployee(NewEmployeeRequest request);
    Employee getEmployeeById(Long id);
    void deleteEmployeeById(Long id);

    Employee getEmployeeByUsername(String username);
}