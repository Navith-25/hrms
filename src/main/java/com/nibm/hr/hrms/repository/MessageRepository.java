package com.nibm.hr.hrms.repository;

import com.nibm.hr.hrms.model.Employee;
import com.nibm.hr.hrms.model.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {
    List<Message> findByRecipientOrderByTimestampDesc(Employee recipient);
    List<Message> findBySenderOrderByTimestampDesc(Employee sender);
}