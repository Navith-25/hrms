package com.nibm.hr.hrms.service;

import com.nibm.hr.hrms.model.Employee;
import com.nibm.hr.hrms.model.Task;
import java.security.Principal;
import java.util.List;

public interface TaskService {
    List<Task> getTasksAssignedByManager(Principal principal);
    List<Task> getMyTasks(Principal principal);
    List<Employee> getMyTeam(Principal principal);

    void assignTask(Task task, Long employeeId, Principal principal);
    void submitTask(Long taskId, Principal principal);
    void reviewTask(Long taskId, Integer rating, String comments, Principal principal);

    Task getTaskById(Long id);
}