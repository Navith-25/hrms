package com.nibm.hr.hrms.service;

import com.nibm.hr.hrms.model.*;
import com.nibm.hr.hrms.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class MessageServiceImpl implements MessageService {

    @Autowired
    private MessageRepository messageRepository;
    @Autowired
    private EmployeeRepository employeeRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private DepartmentRepository departmentRepository;

    private Employee getEmployeeFromPrincipal(Principal principal) {
        User user = userRepository.findByUsername(principal.getName());
        if (user == null || user.getEmployee() == null) {
            throw new RuntimeException("User invalid");
        }
        return user.getEmployee();
    }

    @Override
    public List<Message> getInbox(Principal principal) {
        return messageRepository.findByRecipientOrderByTimestampDesc(getEmployeeFromPrincipal(principal));
    }

    @Override
    public List<Message> getOutbox(Principal principal) {
        return messageRepository.findBySenderOrderByTimestampDesc(getEmployeeFromPrincipal(principal));
    }

    @Override
    @Transactional
    public void sendMessage(Principal principal, Long recipientId, String subject, String content) {
        Employee sender = getEmployeeFromPrincipal(principal);
        Employee recipient = employeeRepository.findById(recipientId)
                .orElseThrow(() -> new RuntimeException("Recipient not found"));

        Message msg = new Message();
        msg.setSender(sender);
        msg.setRecipient(recipient);
        msg.setSubject(subject);
        msg.setContent(content);
        msg.setTimestamp(LocalDateTime.now());
        messageRepository.save(msg);
    }

    @Override
    @Transactional
    public void sendNotificationToAll(Principal principal, String subject, String content) {
        Employee sender = getEmployeeFromPrincipal(principal);

        boolean isAuthorized = sender.getUser().getRoles().stream()
                .anyMatch(r -> r.getName().equals("ROLE_ADMIN") ||
                        r.getName().equals("ROLE_DIRECTOR") ||
                        r.getName().equals("ROLE_HR_MANAGER"));

        if (!isAuthorized) {
            throw new AccessDeniedException("Only HR and CEO can send broadcast notifications.");
        }

        List<Employee> allEmployees = employeeRepository.findAll();
        for (Employee emp : allEmployees) {
            if (emp.getId().equals(sender.getId())) continue;

            Message msg = new Message();
            msg.setSender(sender);
            msg.setRecipient(emp);
            msg.setSubject("[ANNOUNCEMENT] " + subject);
            msg.setContent(content);
            msg.setTimestamp(LocalDateTime.now());
            messageRepository.save(msg);
        }
    }

    @Override
    @Transactional
    public void sendNotificationToDepartment(Principal principal, String subject, String content) {
        Employee manager = getEmployeeFromPrincipal(principal);

        Department department = departmentRepository.findByManager(manager).orElse(null);
        if (department == null) {
            throw new AccessDeniedException("You are not managing a department.");
        }

        List<Employee> team = employeeRepository.findByDepartment(department);
        for (Employee emp : team) {
            if (emp.getId().equals(manager.getId())) continue;

            Message msg = new Message();
            msg.setSender(manager);
            msg.setRecipient(emp);
            msg.setSubject("[DEPT NOTICE] " + subject);
            msg.setContent(content);
            msg.setTimestamp(LocalDateTime.now());
            messageRepository.save(msg);
        }
    }

    @Override
    public void markAsRead(Long messageId) {
        Message msg = messageRepository.findById(messageId).orElse(null);
        if (msg != null) {
            msg.setRead(true);
            messageRepository.save(msg);
        }
    }
}