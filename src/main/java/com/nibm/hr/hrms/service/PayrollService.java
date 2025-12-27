package com.nibm.hr.hrms.service;

import com.nibm.hr.hrms.model.Payroll;
import com.nibm.hr.hrms.model.Payslip;
import java.math.BigDecimal;
import java.security.Principal;
import java.time.LocalDate;
import java.util.List;

public interface PayrollService {
    List<Payslip> getPayslipsForEmployee(Principal principal);

    Payroll savePayrollSettings(Payroll payroll);
    Payroll getPayrollSettings(Long employeeId);
    Payslip getPayslipById(Long id);

    void generatePayslip(Long employeeId, LocalDate payDate, BigDecimal bonus);
}