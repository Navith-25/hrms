package com.nibm.hr.hrms.repository;

import com.nibm.hr.hrms.model.Employee;
import com.nibm.hr.hrms.model.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {

    List<Task> findByEmployeeOrderByDeadlineAsc(Employee employee);
    @Query("SELECT t FROM Task t WHERE t.employee.department.manager.id = :managerId")
    List<Task> findTasksByManagerId(@Param("managerId") Long managerId);
}