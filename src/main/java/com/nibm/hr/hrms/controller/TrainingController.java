package com.nibm.hr.hrms.controller;

import com.nibm.hr.hrms.model.*;
import com.nibm.hr.hrms.repository.DepartmentRepository;
import com.nibm.hr.hrms.repository.EmployeeRepository;
import com.nibm.hr.hrms.repository.UserRepository;
import com.nibm.hr.hrms.service.TrainingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.stream.Collectors;

@Controller
public class TrainingController {

    @Autowired
    private TrainingService trainingService;
    @Autowired
    private DepartmentRepository departmentRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private EmployeeRepository employeeRepository;

    // --- CEO: Create Training Program ---
    @GetMapping("/admin/training/create")
    public String showCreateTrainingForm(Model model) {
        model.addAttribute("training", new TrainingProgram());
        model.addAttribute("departments", departmentRepository.findAll());
        return "admin_training_form";
    }

    @PostMapping("/admin/training/save")
    public String saveTraining(@ModelAttribute TrainingProgram training, @RequestParam("departmentId") Long deptId) {
        trainingService.createTraining(training, deptId);
        return "redirect:/";
    }

    // --- MANAGER: List Trainings for their Department ---
    @GetMapping("/manager/training")
    public String showManagerTrainings(Model model, Principal principal) {
        User user = userRepository.findByUsername(principal.getName());
        Employee manager = user.getEmployee();

        Department department = departmentRepository.findByManager(manager)
                .orElseThrow(() -> new AccessDeniedException("You are not managing any department"));

        List<TrainingProgram> trainings = trainingService.getTrainingsForDepartment(department);
        model.addAttribute("trainings", trainings);
        return "manager_training_list";
    }

    // --- MANAGER: Assign Employees View ---
    @GetMapping("/manager/training/assign/{id}")
    public String showAssignForm(@PathVariable("id") Long trainingId, Model model, Principal principal) {
        User user = userRepository.findByUsername(principal.getName());
        Employee manager = user.getEmployee();
        Department department = departmentRepository.findByManager(manager).orElse(null);

        TrainingProgram training = trainingService.getTrainingById(trainingId);

        List<Employee> deptEmployees = employeeRepository.findByDepartment(department);

        deptEmployees = deptEmployees.stream()
                .filter(e -> !e.getId().equals(manager.getId()))
                .collect(Collectors.toList());

        deptEmployees = deptEmployees.stream()
                .filter(e -> e.getUser().getRoles().stream()
                        .noneMatch(r -> r.getName().equals("ROLE_ADMIN")))
                .collect(Collectors.toList());

        model.addAttribute("training", training);
        model.addAttribute("employees", deptEmployees);
        return "manager_training_assign";
    }

    @PostMapping("/manager/training/assign")
    public String saveAssignments(@RequestParam("trainingId") Long trainingId,
                                  @RequestParam(value = "employeeIds", required = false) List<Long> employeeIds) {
        if (employeeIds != null && !employeeIds.isEmpty()) {
            trainingService.assignEmployeesToTraining(trainingId, employeeIds);
        }
        return "redirect:/manager/training";
    }
}