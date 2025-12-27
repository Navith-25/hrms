package com.nibm.hr.hrms;

import com.nibm.hr.hrms.model.Department;
import com.nibm.hr.hrms.model.Employee;
import com.nibm.hr.hrms.model.Role;
import com.nibm.hr.hrms.model.User;
import com.nibm.hr.hrms.repository.DepartmentRepository;
import com.nibm.hr.hrms.repository.EmployeeRepository;
import com.nibm.hr.hrms.repository.RoleRepository;
import com.nibm.hr.hrms.repository.UserRepository;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.Set;

@SpringBootApplication
public class HrmsApplication {

    public static void main(String[] args) {
        SpringApplication.run(HrmsApplication.class, args);
    }

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Bean
    public CommandLineRunner initialData(RoleRepository roleRepository,
                                         UserRepository userRepository,
                                         EmployeeRepository employeeRepository,
                                         DepartmentRepository departmentRepository) {
        return args -> {

            Role adminRole = findOrCreateRole(roleRepository, "ROLE_ADMIN");
            Role employeeRole = findOrCreateRole(roleRepository, "ROLE_EMPLOYEE");
            Role managerRole = findOrCreateRole(roleRepository, "ROLE_MANAGER");
            Role hrManagerRole = findOrCreateRole(roleRepository, "ROLE_HR_MANAGER");
            Role hrStaffRole = findOrCreateRole(roleRepository, "ROLE_HR_STAFF");
            Role financeRole = findOrCreateRole(roleRepository, "ROLE_FINANCE");
            Role directorRole = findOrCreateRole(roleRepository, "ROLE_DIRECTOR");


            Department hrDept = findOrCreateDept(departmentRepository, "Human Resources");
            Department engDept = findOrCreateDept(departmentRepository, "Engineering");
            Department itDept = findOrCreateDept(departmentRepository, "IT");
            Department financeDept = findOrCreateDept(departmentRepository, "Finance");
            Department execDept = findOrCreateDept(departmentRepository, "Executive");

            if (userRepository.findByUsername("navith") == null) {
                Employee adminEmployee = new Employee();
                adminEmployee.setFirstName("Navith");
                adminEmployee.setLastName("Fernando");
                adminEmployee.setEmail("navith@gmail.com");
                adminEmployee.setPosition("System Admin");
                adminEmployee.setDepartment(itDept);
                adminEmployee.setHireDate(LocalDate.now());

                User adminUser = new User();
                adminUser.setUsername("navith");
                adminUser.setPassword(passwordEncoder.encode("navith123"));
                adminUser.setRoles(Set.of(adminRole, employeeRole));

                adminUser.setEmployee(adminEmployee);
                adminEmployee.setUser(adminUser);
                userRepository.save(adminUser);
            }

            if (userRepository.findByUsername("ashen") == null) {
                Employee hrEmployee = new Employee();
                hrEmployee.setFirstName("Ashen");
                hrEmployee.setLastName("Fernando");
                hrEmployee.setEmail("ashen@gmail.com");
                hrEmployee.setPosition("HR Manager");
                hrEmployee.setDepartment(hrDept);
                hrEmployee.setHireDate(LocalDate.now());

                User hrUser = new User();
                hrUser.setUsername("ashen");
                hrUser.setPassword(passwordEncoder.encode("ashen123"));
                hrUser.setRoles(Set.of(hrManagerRole, managerRole, employeeRole));

                hrUser.setEmployee(hrEmployee);
                hrEmployee.setUser(hrUser);
                userRepository.save(hrUser);

                hrDept.setManager(hrEmployee);
                departmentRepository.save(hrDept);
            }

            if (userRepository.findByUsername("lihini") == null) {
                Employee staffEmployee = new Employee();
                staffEmployee.setFirstName("Lihini");
                staffEmployee.setLastName("Pathirana");
                staffEmployee.setEmail("lihini@gmail.com");
                staffEmployee.setPosition("HR Coordinator");
                staffEmployee.setDepartment(hrDept);
                staffEmployee.setHireDate(LocalDate.now());

                User staffUser = new User();
                staffUser.setUsername("lihini");
                staffUser.setPassword(passwordEncoder.encode("lihini123"));
                staffUser.setRoles(Set.of(hrStaffRole, employeeRole));

                staffUser.setEmployee(staffEmployee);
                staffEmployee.setUser(staffUser);
                userRepository.save(staffUser);
            }

            if (userRepository.findByUsername("hiran") == null) {
                Employee engManager = new Employee();
                engManager.setFirstName("Hiran");
                engManager.setLastName("Aloka");
                engManager.setEmail("hiran@gmail.com");
                engManager.setPosition("Engineering Manager");
                engManager.setDepartment(engDept);
                engManager.setHireDate(LocalDate.now());

                User engUser = new User();
                engUser.setUsername("hiran");
                engUser.setPassword(passwordEncoder.encode("hiran123"));
                engUser.setRoles(Set.of(managerRole, employeeRole));

                engUser.setEmployee(engManager);
                engManager.setUser(engUser);
                userRepository.save(engUser);
                engDept.setManager(engManager);
                departmentRepository.save(engDept);
            }

            if (userRepository.findByUsername("roshan") == null) {
                Employee empEmployee = new Employee();
                empEmployee.setFirstName("Roshan");
                empEmployee.setLastName("Nimantha");
                empEmployee.setEmail("roshan@gmail.com");
                empEmployee.setPosition("Software Engineer");
                empEmployee.setDepartment(itDept);
                empEmployee.setHireDate(LocalDate.now());

                User empUser = new User();
                empUser.setUsername("roshan");
                empUser.setPassword(passwordEncoder.encode("roshan123"));
                empUser.setRoles(Set.of(employeeRole));

                empUser.setEmployee(empEmployee);
                empEmployee.setUser(empUser);
                userRepository.save(empUser);
            }

            if (userRepository.findByUsername("harith") == null) {
                Employee finEmployee = new Employee();
                finEmployee.setFirstName("Harith");
                finEmployee.setLastName("Yasantha");
                finEmployee.setEmail("harith@gmail.com");
                finEmployee.setPosition("Finance Manager");
                finEmployee.setDepartment(financeDept);
                finEmployee.setHireDate(LocalDate.now());

                User finUser = new User();
                finUser.setUsername("harith");
                finUser.setPassword(passwordEncoder.encode("harith123"));
                finUser.setRoles(Set.of(managerRole, financeRole, employeeRole));

                finUser.setEmployee(finEmployee);
                finEmployee.setUser(finUser);
                userRepository.save(finUser);
            }

            if (userRepository.findByUsername("vimukthi") == null) {
                Employee ceoEmployee = new Employee();
                ceoEmployee.setFirstName("Vimukthi");
                ceoEmployee.setLastName("Ediriveera");
                ceoEmployee.setEmail("vimukthi@gmail.com");
                ceoEmployee.setPosition("CEO");
                ceoEmployee.setDepartment(execDept);
                ceoEmployee.setHireDate(LocalDate.now());

                User ceoUser = new User();
                ceoUser.setUsername("vimukthi");
                ceoUser.setPassword(passwordEncoder.encode("vimukthi123"));
                ceoUser.setRoles(Set.of(directorRole, employeeRole));

                ceoUser.setEmployee(ceoEmployee);
                ceoEmployee.setUser(ceoUser);
                userRepository.save(ceoUser);
            }
        };
    }

    private Role findOrCreateRole(RoleRepository repo, String roleName) {
        Role role = repo.findByName(roleName);
        if (role == null) {
            role = new Role();
            role.setName(roleName);
            repo.save(role);
        }
        return role;
    }

    private Department findOrCreateDept(DepartmentRepository repo, String deptName) {
        return repo.findByName(deptName).orElseGet(() -> {
            Department dept = new Department();
            dept.setName(deptName);
            return repo.save(dept);
        });
    }
}