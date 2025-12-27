package com.nibm.hr.hrms.repository;

import com.nibm.hr.hrms.model.Attendance;
import com.nibm.hr.hrms.model.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;

public interface AttendanceRepository extends JpaRepository<Attendance, Long> {
    List<Attendance> findByEmployee(Employee employee);
    Attendance findByEmployeeAndDate(Employee employee, LocalDate date);
}