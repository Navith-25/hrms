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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class EmployeeServiceImpl implements EmployeeService {

    @Autowired private EmployeeRepository employeeRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private DepartmentRepository departmentRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    @Autowired private EmailService emailService;

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
    public List<Employee> getPendingEmployees() {
        return employeeRepository.findAll().stream()
                .filter(e -> e.getUser() != null && !e.getUser().isEnabled())
                .collect(Collectors.toList());
    }

    // =====================================
    // 100% UPDATED: APPROVE WITH AUTO PASSWORD & EMAIL
    // =====================================
    @Override
    @Transactional
    public void approveEmployee(Long id) {
        Employee employee = getEmployeeById(id);
        User user = employee.getUser();

        if (user != null) {
            String tempPassword = UUID.randomUUID().toString().substring(0, 8) + "-HRMS";

            user.setPassword(passwordEncoder.encode(tempPassword));
            user.setEnabled(true);
            userRepository.save(user);

            emailService.sendWelcomeEmail(employee.getEmail(), employee.getFirstName(), user.getUsername(), tempPassword);
        } else {
            throw new RuntimeException("Cannot approve: No user account found for this employee.");
        }
    }

    @Override
    @Transactional
    public void rejectEmployee(Long id) {
        Employee employee = getEmployeeById(id);
        User user = employee.getUser();
        if (user != null && !user.isEnabled()) {
            userRepository.delete(user);
        } else {
            throw new RuntimeException("Cannot reject: Employee is already active or has no account.");
        }
    }

    @Override
    @Transactional
    public void changePassword(String username, String oldPassword, String newPassword) {
        User user = userRepository.findByUsername(username);
        if (user == null) throw new RuntimeException("User account not found.");
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) throw new RuntimeException("Current password is incorrect.");
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    // =====================================
    // UPDATED: CREATE ACCOUNT WITHOUT PASSWORD
    // =====================================
    @Override
    @Transactional
    public Employee createNewEmployee(NewEmployeeRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isCurrentUserAdmin = auth != null && auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        if (request.isAdmin() && !isCurrentUserAdmin) throw new RuntimeException("Security Alert: Only existing Administrators can assign the Admin role.");
        if (userRepository.findByUsername(request.getUsername()) != null) throw new RuntimeException("Username '" + request.getUsername() + "' is already taken.");
        if (employeeRepository.findAll().stream().anyMatch(e -> e.getEmail().equalsIgnoreCase(request.getEmail()))) throw new RuntimeException("Email '" + request.getEmail() + "' is already registered in the system.");

        Department department = departmentRepository.findById(request.getDepartmentId()).orElseThrow(() -> new RuntimeException("Department not found"));

        Employee employee = new Employee();
        employee.setFirstName(request.getFirstName());
        employee.setLastName(request.getLastName());
        employee.setEmail(request.getEmail());
        employee.setPosition(request.getPosition());
        employee.setHireDate(request.getHireDate());
        employee.setDepartment(department);

        double initialAnnual = (request.isManager() || request.isHrManager() || request.isDirector()) ? 19.0 : 14.0;
        employee.setAnnualLeaveBalance(initialAnnual);
        employee.setCasualLeaveBalance(7.0);
        employee.setSickLeaveBalance(14.0);

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
        user.setEnabled(false);

        Set<Role> roles = new HashSet<>();
        roles.add(findRoleByName("ROLE_EMPLOYEE"));

        if (request.isManager()) { roles.add(findRoleByName("ROLE_MANAGER")); department.setManager(employee); }
        if (request.isHrManager()) roles.add(findRoleByName("ROLE_HR_MANAGER"));
        if (request.isHrStaff()) roles.add(findRoleByName("ROLE_HR_STAFF"));
        if (request.isFinance()) roles.add(findRoleByName("ROLE_FINANCE"));
        if (request.isDirector()) roles.add(findRoleByName("ROLE_DIRECTOR"));
        if (request.isAdmin()) roles.add(findRoleByName("ROLE_ADMIN"));

        user.setRoles(roles);
        user.setEmployee(employee);
        employee.setUser(user);

        userRepository.save(user);
        if (request.isManager()) departmentRepository.save(department);

        return employee;
    }

    @Override
    @Transactional
    public Employee updateEmployee(NewEmployeeRequest request) {
        Employee employee = employeeRepository.findById(request.getId()).orElseThrow(() -> new RuntimeException("Employee not found"));
        User user = employee.getUser();
        if (user == null) throw new RuntimeException("User not found");

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isCurrentUserAdmin = auth != null && auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        if (request.isAdmin() && !isCurrentUserAdmin) throw new RuntimeException("Security Alert: Only existing Administrators can assign the Admin role.");

        User existingUser = userRepository.findByUsername(request.getUsername());
        if (existingUser != null && !existingUser.getId().equals(user.getId())) throw new RuntimeException("Username '" + request.getUsername() + "' is already taken.");

        boolean emailExists = employeeRepository.findAll().stream().anyMatch(e -> e.getEmail().equalsIgnoreCase(request.getEmail()) && !e.getId().equals(employee.getId()));
        if (emailExists) throw new RuntimeException("Email '" + request.getEmail() + "' is already registered by another user.");

        Department department = departmentRepository.findById(request.getDepartmentId()).orElseThrow(() -> new RuntimeException("Department not found"));

        employee.setFirstName(request.getFirstName());
        employee.setLastName(request.getLastName());
        employee.setEmail(request.getEmail());
        employee.setPosition(request.getPosition());
        employee.setHireDate(request.getHireDate());

        if (!employee.getDepartment().getId().equals(department.getId())) {
            if (employee.getDepartment().getManager() != null && employee.getDepartment().getManager().getId().equals(employee.getId())) {
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

        if (request.isHrManager()) roles.add(findRoleByName("ROLE_HR_MANAGER"));
        if (request.isHrStaff()) roles.add(findRoleByName("ROLE_HR_STAFF"));
        if (request.isFinance()) roles.add(findRoleByName("ROLE_FINANCE"));
        if (request.isDirector()) roles.add(findRoleByName("ROLE_DIRECTOR"));
        if (request.isAdmin()) roles.add(findRoleByName("ROLE_ADMIN"));

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
        return employeeRepository.findById(id).orElseThrow(() -> new RuntimeException("Employee not found for id :: " + id));
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
            user.setEnabled(false);
            userRepository.save(user);
        }
    }

    @Override
    public Page<Employee> findPaginated(int pageNo, int pageSize, String sortField, String sortDir, String keyword) {
        Sort sort = sortDir.equalsIgnoreCase(Sort.Direction.ASC.name()) ? Sort.by(sortField).ascending() : Sort.by(sortField).descending();
        Pageable pageable = PageRequest.of(pageNo - 1, pageSize, sort);
        if (keyword != null && !keyword.isEmpty()) {
            return employeeRepository.searchEmployees(keyword, pageable);
        }
        return employeeRepository.findAll(pageable);
    }
}