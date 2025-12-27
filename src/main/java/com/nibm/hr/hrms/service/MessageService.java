package com.nibm.hr.hrms.service;

import com.nibm.hr.hrms.model.Message;
import java.security.Principal;
import java.util.List;

public interface MessageService {
    List<Message> getInbox(Principal principal);
    List<Message> getOutbox(Principal principal);

    void sendMessage(Principal principal, Long recipientId, String subject, String content);
    void sendNotificationToAll(Principal principal, String subject, String content);
    void sendNotificationToDepartment(Principal principal, String subject, String content);
    void markAsRead(Long messageId);
}