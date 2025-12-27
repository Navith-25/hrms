package com.nibm.hr.hrms.controller;

import com.nibm.hr.hrms.service.SystemService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;

@Controller
@RequestMapping("/admin/system")
public class SystemController {

    @Autowired
    private SystemService systemService;

    @GetMapping("/reset")
    public String showResetPage() {
        return "system_reset";
    }

    @PostMapping("/reset")
    public String performSystemReset(Principal principal, RedirectAttributes redirectAttributes) {
        try {
            systemService.resetEntireSystem(principal.getName());
            redirectAttributes.addFlashAttribute("success", "System has been successfully reset. All data (except your account) is deleted.");
        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "Error resetting system: " + e.getMessage());
        }
        return "redirect:/";
    }
}