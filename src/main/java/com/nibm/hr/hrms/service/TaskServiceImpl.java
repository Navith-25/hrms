package com.nibm.hr.hrms.service;

import com.nibm.hr.hrms.model.*;
import com.nibm.hr.hrms.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class TaskServiceImpl implements TaskService {

    @Autowired
    private TaskRepository taskRepository;
    @Autowired
    private EmployeeRepository employeeRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private DepartmentRepository departmentRepository;

    private Employee getEmployeeFromPrincipal(Principal principal) {
        User user = userRepository.findByUsername(principal.getName());
        return user.getEmployee();
    }

    private User getUserFromPrincipal(Principal principal) {
        return userRepository.findByUsername(principal.getName());
    }


    @Override
    public List<Task> getTasksAssignedByManager(Principal principal) {
        User user = userRepository.findByUsername(principal.getName());
        if (user.getEmployee() == null) return new ArrayList<>();
        return taskRepository.findTasksByManagerId(user.getEmployee().getId());
    }

    @Override
    public List<Task> getMyTasks(Principal principal) {
        return taskRepository.findByEmployeeOrderByDeadlineAsc(getEmployeeFromPrincipal(principal));
    }

    @Override
    public List<Employee> getMyTeam(Principal principal) {
        User currentUser = getUserFromPrincipal(principal);
        Set<String> roles = currentUser.getRoles().stream()
                .map(Role::getName)
                .collect(Collectors.toSet());

        if (roles.contains("ROLE_ADMIN") || roles.contains("ROLE_DIRECTOR")) {
            return employeeRepository.findByUser_Roles_NameIn(List.of("ROLE_MANAGER", "ROLE_HR_MANAGER"));
        }

        if (roles.contains("ROLE_MANAGER") || roles.contains("ROLE_HR_MANAGER")) {
            Employee manager = currentUser.getEmployee();
            Department department = departmentRepository.findByManager(manager).orElse(null);
            if (department == null) return List.of();

            return employeeRepository.findByDepartment(department).stream()
                    .filter(e -> !e.equals(manager))
                    .collect(Collectors.toList());
        }

        return List.of();
    }

    @Override
    @Transactional
    public void assignTask(Task task, Long employeeId, Principal principal) {
        User currentUser = getUserFromPrincipal(principal);
        Employee assigner = currentUser.getEmployee();

        Employee assignee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new RuntimeException("Employee not found"));

        Set<String> assignerRoles = currentUser.getRoles().stream()
                .map(Role::getName)
                .collect(Collectors.toSet());

        Set<String> assigneeRoles = assignee.getUser().getRoles().stream()
                .map(Role::getName)
                .collect(Collectors.toSet());

        boolean isAllowed = false;

        if (assignerRoles.contains("ROLE_ADMIN") || assignerRoles.contains("ROLE_DIRECTOR")) {
            if (assigneeRoles.contains("ROLE_MANAGER") || assigneeRoles.contains("ROLE_HR_MANAGER")) {
                isAllowed = true;
            } else {
                throw new AccessDeniedException("Admins/Directors can only assign tasks to Managers.");
            }
        }
        else if (assignerRoles.contains("ROLE_MANAGER") || assignerRoles.contains("ROLE_HR_MANAGER")) {
            if (assignee.getDepartment() != null && assignee.getDepartment().getManager().equals(assigner)) {
                isAllowed = true;
            } else {
                throw new AccessDeniedException("You can only assign tasks to your department members.");
            }
        }

        if (!isAllowed) {
            throw new AccessDeniedException("You do not have permission to assign tasks to this user.");
        }

        task.setManager(assigner);
        task.setEmployee(assignee);
        task.setStatus(TaskStatus.PENDING);
        taskRepository.save(task);
    }

    @Override
    @Transactional
    public void submitTask(Long taskId, Principal principal) {
        Task task = taskRepository.findById(taskId).orElseThrow(() -> new RuntimeException("Task not found"));
        Employee employee = getEmployeeFromPrincipal(principal);

        if (!task.getEmployee().equals(employee)) {
            throw new AccessDeniedException("You cannot submit a task assigned to someone else.");
        }

        task.setStatus(TaskStatus.SUBMITTED);
        taskRepository.save(task);
    }

    @Override
    @Transactional
    public void reviewTask(Long taskId, Integer rating, String comments, Principal principal) {
        Task task = taskRepository.findById(taskId).orElseThrow(() -> new RuntimeException("Task not found"));
        Employee manager = getEmployeeFromPrincipal(principal);

        if (!task.getManager().equals(manager)) {
            throw new AccessDeniedException("You cannot review a task you didn't assign.");
        }

        task.setRating(rating);
        task.setManagerComments(comments);
        task.setStatus(TaskStatus.COMPLETED);
        taskRepository.save(task);
    }

    @Override
    public Task getTaskById(Long id) {
        return taskRepository.findById(id).orElseThrow(() -> new RuntimeException("Task not found"));
    }
}