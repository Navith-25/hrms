package com.nibm.hr.hrms.controller;

import com.nibm.hr.hrms.model.Employee;
import com.nibm.hr.hrms.service.EmployeeService;
import com.nibm.hr.hrms.service.MessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.security.Principal;
import java.util.List;

@Controller
public class MessageController {

    @Autowired
    private MessageService messageService;

    @Autowired
    private EmployeeService employeeService;

    @GetMapping("/messages")
    public String showInbox(Model model, Principal principal) {
        model.addAttribute("inboxMessages", messageService.getInbox(principal));
        model.addAttribute("outboxMessages", messageService.getOutbox(principal));
        return "messages";
    }

    @GetMapping("/messages/new")
    public String showComposeForm(Model model, Principal principal) {
        List<Employee> employees = employeeService.getAllEmployees();
        model.addAttribute("employees", employees);
        return "message_form";
    }

    @PostMapping("/messages/send")
    public String sendMessage(@RequestParam(required = false) Long recipientId,
                              @RequestParam String subject,
                              @RequestParam String content,
                              @RequestParam(defaultValue = "false") boolean sendToAll,
                              @RequestParam(defaultValue = "false") boolean sendToDept,
                              Principal principal) {

        if (sendToAll) {
            messageService.sendNotificationToAll(principal, subject, content);
        } else if (sendToDept) {
            messageService.sendNotificationToDepartment(principal, subject, content);
        } else if (recipientId != null) {
            messageService.sendMessage(principal, recipientId, subject, content);
        }

        return "redirect:/messages";
    }
}