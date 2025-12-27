package com.nibm.hr.hrms.service;

import com.nibm.hr.hrms.model.Attendance;
import com.nibm.hr.hrms.model.Employee;
import com.nibm.hr.hrms.repository.AttendanceRepository;
import com.nibm.hr.hrms.repository.EmployeeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class AttendanceService {

    @Autowired
    private AttendanceRepository attendanceRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    public List<Employee> getAllEmployees() {
        return employeeRepository.findAll();
    }

    @Transactional
    public void saveAttendance(Long employeeId, LocalDate date, String status) {
        Employee employee = employeeRepository.findById(employeeId).orElse(null);
        if (employee != null) {
            Attendance existing = attendanceRepository.findByEmployeeAndDate(employee, date);
            if (existing != null) {
                existing.setStatus(status);
                attendanceRepository.save(existing);
            } else {
                Attendance attendance = new Attendance(employee, date, status);
                attendanceRepository.save(attendance);
            }
        }
    }

    @Transactional
    public void saveAttendance(Attendance attendance) {
        if (attendance.getEmployee() != null && attendance.getDate() != null) {
            Attendance existing = attendanceRepository.findByEmployeeAndDate(attendance.getEmployee(), attendance.getDate());
            if (existing != null) {
                existing.setStatus(attendance.getStatus());
                existing.setInTime(attendance.getInTime());
                existing.setOutTime(attendance.getOutTime());
                attendanceRepository.save(existing);
            } else {
                attendanceRepository.save(attendance);
            }
        }
    }

    public List<Attendance> getAttendanceForEmployee(Employee employee) {
        return attendanceRepository.findByEmployee(employee);
    }
}