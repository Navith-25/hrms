package com.nibm.hr.hrms.service;

import com.nibm.hr.hrms.model.Employee;
import com.nibm.hr.hrms.model.Payroll;
import com.nibm.hr.hrms.model.Payslip;
import com.nibm.hr.hrms.model.User;
import com.nibm.hr.hrms.repository.EmployeeRepository;
import com.nibm.hr.hrms.repository.PayrollRepository;
import com.nibm.hr.hrms.repository.PayslipRepository;
import com.nibm.hr.hrms.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.Principal;
import java.time.LocalDate;
import java.util.List;

@Service
public class PayrollServiceImpl implements PayrollService {

    @Autowired
    private PayrollRepository payrollRepository;

    @Autowired
    private PayslipRepository payslipRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private LoanService loanService;

    @Override
    @Transactional
    public Payroll savePayrollSettings(Payroll payroll) {
        if (payroll.getEmployee() != null && payroll.getEmployee().getId() != null) {
            Employee emp = employeeRepository.findById(payroll.getEmployee().getId())
                    .orElseThrow(() -> new RuntimeException("Employee not found"));
            payroll.setEmployee(emp);
        }
        return payrollRepository.save(payroll);
    }

    @Override
    public Payroll getPayrollSettings(Long employeeId) {
        return payrollRepository.findByEmployeeId(employeeId);
    }

    @Override
    @Transactional
    public void generatePayslip(Long employeeId, LocalDate payDate, BigDecimal bonus) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new RuntimeException("Employee not found"));

        Payroll payroll = payrollRepository.findByEmployeeId(employeeId);
        if (payroll == null) {
            throw new RuntimeException("Payroll settings not found. Please configure salary first.");
        }

        Payslip payslip = new Payslip();
        payslip.setEmployee(employee);
        payslip.setPayDate(payDate);
        payslip.setBasePay(payroll.getBaseSalary());

        // 1. Calculate Tax (Example: 5% flat rate)
        BigDecimal taxRate = new BigDecimal("0.05");
        BigDecimal tax = payroll.getBaseSalary().multiply(taxRate);
        payslip.setTax(tax);

        // 2. Process Loan Deduction
        BigDecimal loanDeduction = loanService.processMonthlyDeduction(employee);
        payslip.setLoanDeduction(loanDeduction);

        // 3. Calculate Total Deductions
        BigDecimal stdDeductions = (payroll.getStandardDeductions() != null)
                ? payroll.getStandardDeductions()
                : BigDecimal.ZERO;

        BigDecimal totalDeductions = stdDeductions.add(tax).add(loanDeduction);
        payslip.setTotalDeductions(totalDeductions);

        // 4. Calculate Net Pay
        BigDecimal finalBonus = (bonus != null) ? bonus : BigDecimal.ZERO;
        payslip.setBonus(finalBonus);

        BigDecimal netPay = payroll.getBaseSalary().add(finalBonus).subtract(totalDeductions);
        payslip.setNetPay(netPay);

        payslipRepository.save(payslip);
    }

    @Override
    public List<Payslip> getPayslipsForEmployee(Principal principal) {
        User user = userRepository.findByUsername(principal.getName());
        return payslipRepository.findByEmployeeOrderByPayDateDesc(user.getEmployee());
    }

    @Override
    public Payslip getPayslipById(Long id) {
        return payslipRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Payslip not found"));
    }
}