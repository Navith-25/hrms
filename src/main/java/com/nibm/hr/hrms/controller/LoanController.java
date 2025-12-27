package com.nibm.hr.hrms.controller;


import com.nibm.hr.hrms.model.Loan;
import com.nibm.hr.hrms.model.User;
import com.nibm.hr.hrms.repository.UserRepository;
import com.nibm.hr.hrms.service.LoanService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;

@Controller
public class LoanController {

    @Autowired
    private LoanService loanService;
    @Autowired
    private UserRepository userRepository;

    @GetMapping("/loan/request")
    public String showLoanRequestForm(Model model, Principal principal) {
        User user = userRepository.findByUsername(principal.getName());
        model.addAttribute("loan", new Loan());
        model.addAttribute("myLoans", loanService.getMyLoans(user.getEmployee()));
        return "loan_request";
    }

    @PostMapping("/loan/save")
    public String submitLoanRequest(@ModelAttribute Loan loan, Principal principal) {
        User user = userRepository.findByUsername(principal.getName());
        loanService.requestLoan(loan, user.getEmployee());
        return "redirect:/loan/request?success";
    }

    @GetMapping("/finance/loans")
    public String showFinanceLoanPage(Model model, Principal principal) {
        User user = userRepository.findByUsername(principal.getName());
        model.addAttribute("pendingLoans", loanService.getPendingLoansForApprover(user));

        return "finance_loans";
    }

    @PostMapping("/finance/loan/action")
    public String approveOrRejectLoan(@RequestParam("loanId") Long loanId,
                                      @RequestParam("action") String action,
                                      Principal principal,
                                      RedirectAttributes redirectAttributes) {
        try {
            loanService.updateLoanStatus(loanId, action, principal.getName());
        } catch (AccessDeniedException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/finance/loans";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "An error occurred.");
            return "redirect:/finance/loans";
        }

        redirectAttributes.addFlashAttribute("success", "Loan " + action.toLowerCase() + "ed successfully.");
        return "redirect:/finance/loans";
    }
}