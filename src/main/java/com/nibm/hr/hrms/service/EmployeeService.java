package com.nibm.hr.hrms.service;

import com.nibm.hr.hrms.dto.NewEmployeeRequest;
import com.nibm.hr.hrms.model.Employee;
import org.springframework.data.domain.Page;

import java.util.List;

public interface EmployeeService {
    List<Employee> getPendingEmployees();
    List<Employee> getAllEmployees();
    Page<Employee> findPaginated(int pageNo, int pageSize, String sortField, String sortDir, String keyword);

    Employee createNewEmployee(NewEmployeeRequest request);
    Employee updateEmployee(NewEmployeeRequest request);
    Employee getEmployeeById(Long id);
    Employee getEmployeeByUsername(String username);

    void deleteEmployeeById(Long id);
    void approveEmployee(Long id);
    void rejectEmployee(Long id);
    void changePassword(String username, String oldPassword, String newPassword);

}