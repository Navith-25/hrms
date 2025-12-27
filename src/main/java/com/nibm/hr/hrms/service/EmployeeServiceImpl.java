package com.nibm.hr.hrms.service;

import com.nibm.hr.hrms.dto.NewEmployeeRequest;
import com.nibm.hr.hrms.model.Department;
import com.nibm.hr.hrms.model.Employee;
import com.nibm.hr.hrms.model.Role;
import com.nibm.hr.hrms.model.User;
import com.nibm.hr.hrms.repository.DepartmentRepository;
import com.nibm.hr.hrms.repository.EmployeeRepository;
import com.nibm.hr.hrms.repository.RoleRepository;
import com.nibm.hr.hrms.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class EmployeeServiceImpl implements EmployeeService {

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public Employee getEmployeeByUsername(String username) {
        User user = userRepository.findByUsername(username);
        return user != null ? user.getEmployee() : null;
    }

    @Override
    public List<Employee> getAllEmployees() {
        return employeeRepository.findAll();
    }

    @Override
    @Transactional
    public Employee createNewEmployee(NewEmployeeRequest request) {
        Department department = departmentRepository.findById(request.getDepartmentId())
                .orElseThrow(() -> new RuntimeException("Department not found"));

        Employee employee = new Employee();
        employee.setFirstName(request.getFirstName());
        employee.setLastName(request.getLastName());
        employee.setEmail(request.getEmail());
        employee.setPosition(request.getPosition());
        employee.setHireDate(request.getHireDate());
        employee.setDepartment(department);

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setEnabled(true);

        Set<Role> roles = new HashSet<>();
        roles.add(findRoleByName("ROLE_EMPLOYEE"));

        if (request.isManager()) {
            roles.add(findRoleByName("ROLE_MANAGER"));
            department.setManager(employee);
        }
        if (request.isHrManager()) {
            roles.add(findRoleByName("ROLE_HR_MANAGER"));
        }
        if (request.isHrStaff()) {
            roles.add(findRoleByName("ROLE_HR_STAFF"));
        }
        if (request.isFinance()) {
            roles.add(findRoleByName("ROLE_FINANCE"));
        }
        if (request.isDirector()) {
            roles.add(findRoleByName("ROLE_DIRECTOR"));
        }
        if (request.isAdmin()) {
            roles.add(findRoleByName("ROLE_ADMIN"));
        }

        user.setRoles(roles);
        user.setEmployee(employee);
        employee.setUser(user);

        userRepository.save(user);

        if (request.isManager()) {
            departmentRepository.save(department);
        }

        return employee;
    }

    @Override
    @Transactional
    public Employee updateEmployee(NewEmployeeRequest request) {
        Employee employee = employeeRepository.findById(request.getId())
                .orElseThrow(() -> new RuntimeException("Employee not found for id :: " + request.getId()));
        User user = employee.getUser();
        if (user == null) {
            throw new RuntimeException("User not found for employee :: " + employee.getFirstName());
        }

        Department department = departmentRepository.findById(request.getDepartmentId())
                .orElseThrow(() -> new RuntimeException("Department not found"));

        employee.setFirstName(request.getFirstName());
        employee.setLastName(request.getLastName());
        employee.setEmail(request.getEmail());
        employee.setPosition(request.getPosition());
        employee.setHireDate(request.getHireDate());

        if (!employee.getDepartment().getId().equals(department.getId())) {
            if (employee.getDepartment().getManager() != null &&
                    employee.getDepartment().getManager().getId().equals(employee.getId())) {
                employee.getDepartment().setManager(null);
                departmentRepository.save(employee.getDepartment());
            }
            employee.setDepartment(department);
        }

        Set<Role> roles = new HashSet<>();
        roles.add(findRoleByName("ROLE_EMPLOYEE"));

        if (request.isManager()) {
            roles.add(findRoleByName("ROLE_MANAGER"));
            department.setManager(employee);
            departmentRepository.save(department);
        } else {
            if (department.getManager() != null && department.getManager().getId().equals(employee.getId())) {
                department.setManager(null);
                departmentRepository.save(department);
            }
        }

        if (request.isHrManager()) {
            roles.add(findRoleByName("ROLE_HR_MANAGER"));
        }
        if (request.isHrStaff()) {
            roles.add(findRoleByName("ROLE_HR_STAFF"));
        }
        if (request.isFinance()) {
            roles.add(findRoleByName("ROLE_FINANCE"));
        }
        if (request.isDirector()) {
            roles.add(findRoleByName("ROLE_DIRECTOR"));
        }
        if (request.isAdmin()) {
            roles.add(findRoleByName("ROLE_ADMIN"));
        }

        user.setRoles(roles);
        userRepository.save(user);
        return employee;
    }

    private Role findRoleByName(String name) {
        Role role = roleRepository.findByName(name);
        if (role == null) {
            role = new Role();
            role.setName(name);
            roleRepository.save(role);
        }
        return role;
    }

    @Override
    public Employee getEmployeeById(Long id) {
        Optional<Employee> optional = employeeRepository.findById(id);
        if (optional.isPresent()) {
            return optional.get();
        } else {
            throw new RuntimeException("Employee not found for id :: " + id);
        }
    }

    @Override
    @Transactional
    public void deleteEmployeeById(Long id) {
        Employee employee = getEmployeeById(id);

        Optional<Department> managedDept = departmentRepository.findByManager(employee);
        if (managedDept.isPresent()) {
            Department dept = managedDept.get();
            dept.setManager(null);
            departmentRepository.save(dept);
        }
        User user = employee.getUser();
        if (user != null) {
            userRepository.delete(user);
        } else {
            this.employeeRepository.delete(employee);
        }
    }
}