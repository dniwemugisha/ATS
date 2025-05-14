package com.ats.repository;

import com.ats.model.EmailNotification;
import com.ats.model.EmailNotification.EmailStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EmailNotificationRepository extends JpaRepository<EmailNotification, Long> {
    
    List<EmailNotification> findByStatus(EmailStatus status);
    
    List<EmailNotification> findByRelatedUserId(Long userId);
    
    List<EmailNotification> findByRecipientEmail(String email);
    
    List<EmailNotification> findByTemplateName(String templateName);
    
    @Query("SELECT e FROM EmailNotification e WHERE e.status = 'FAILED' ORDER BY e.createdAt DESC")
    List<EmailNotification> findAllFailedEmails();
    
    @Query("SELECT COUNT(e) FROM EmailNotification e WHERE e.status = ?1")
    long countByStatus(EmailStatus status);
} 