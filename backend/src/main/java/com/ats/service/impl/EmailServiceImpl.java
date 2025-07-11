package com.ats.service.impl;

import com.ats.model.EmailNotification;
import com.ats.model.User;
import com.ats.model.Application;
import com.ats.model.EmailEvent;
import com.ats.model.Interview;
import com.ats.model.RecipientType;
import com.ats.repository.EmailNotificationRepository;
import com.ats.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.HashMap;

@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {
    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;
    private final EmailNotificationRepository emailNotificationRepository;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    /**
     * Email event configuration
     */
    private static class EventConfig {
        final RecipientType recipientType;
        final String subjectTemplate;

        EventConfig(RecipientType recipientType, String subjectTemplate) {
            this.recipientType = recipientType;
            this.subjectTemplate = subjectTemplate;
        }
    }

    // Event configuration mapping
    private final Map<EmailEvent, EventConfig> eventConfigs = Map.of(
        EmailEvent.APPLICATION_RECEIVED, new EventConfig(RecipientType.CANDIDATE, "Application Received - %s"),
        EmailEvent.APPLICATION_REVIEWED, new EventConfig(RecipientType.CANDIDATE, "Application Status Update - %s"),
        EmailEvent.APPLICATION_SHORTLISTED, new EventConfig(RecipientType.CANDIDATE, "Congratulations! You've Been Shortlisted - %s"),
        EmailEvent.INTERVIEW_ASSIGNED_TO_INTERVIEWER, new EventConfig(RecipientType.INTERVIEWER, "New Interview Assignment - %s"),
        EmailEvent.INTERVIEW_ASSIGNED_TO_CANDIDATE, new EventConfig(RecipientType.CANDIDATE, "Interview Scheduled - %s"),
        EmailEvent.JOB_OFFER, new EventConfig(RecipientType.CANDIDATE, "Job Offer - %s")
    );

    /**
     * Generic method to send an email with a template and save notification
     * 
     * @param to Recipient email address
     * @param subject Email subject
     * @param templateName Template name to use
     * @param templateVariables Variables to pass to the template
     * @param user Related user (optional)
     * @return Created EmailNotification entity
     * @throws MessagingException If there's an error sending the email
     */
    @Transactional
    private EmailNotification sendTemplateEmail(String to, String subject, String templateName, 
            Map<String, Object> templateVariables, User user) throws MessagingException {
        
        // Create Thymeleaf context and add variables
        Context context = new Context();
        templateVariables.forEach(context::setVariable);
        
        // Process template
        String emailContent = templateEngine.process(templateName, context);
        
        // Create email notification record
        EmailNotification.EmailNotificationBuilder builder = EmailNotification.builder()
                .recipientEmail(to)
                .subject(subject)
                .body(emailContent)
                .templateName(templateName)
                .status(EmailNotification.EmailStatus.PENDING)
                .retryCount(0);
                
        if (user != null) {
            builder.relatedUser(user);
        }
        
        EmailNotification notification = builder.build();
        
        // Save notification first
        notification = emailNotificationRepository.save(notification);

        try {
            // Try to send the email
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(emailContent, true);
            
            mailSender.send(message);
            
            // Update status to SENT
            notification.setStatus(EmailNotification.EmailStatus.SENT);
            return emailNotificationRepository.save(notification);
        } catch (MessagingException e) {
            // Update status to FAILED and save error message
            notification.setStatus(EmailNotification.EmailStatus.FAILED);
            notification.setErrorMessage(e.getMessage());
            notification = emailNotificationRepository.save(notification);
            
            // Rethrow the exception for the caller to handle if needed
            throw e;
        } catch (Exception e) {
            // Update status to FAILED and save error message for non-MessagingException
            notification.setStatus(EmailNotification.EmailStatus.FAILED);
            notification.setErrorMessage(e.getMessage());
            notification = emailNotificationRepository.save(notification);
            
            // Convert to MessagingException to maintain compatibility with existing method signatures
            MessagingException messagingException = new MessagingException("Failed to send email", e);
            throw messagingException;
        }
    }

    @Override
    @Transactional
    public EmailNotification sendVerificationEmail(String to, String token) throws MessagingException {
        Map<String, Object> templateVars = Map.of(
            "verificationLink", frontendUrl + "/verify-email?token=" + token
        );
        
        return sendTemplateEmail(to, "Verify your email address", "verification-email", templateVars, null);
    }
    
    @Override
    @Transactional
    public EmailNotification sendPasswordResetEmail(String to, String token, User user) throws MessagingException {
        Map<String, Object> templateVars = Map.of(
            "resetLink", frontendUrl + "/reset-password?token=" + token
        );
        
        return sendTemplateEmail(to, "Reset Your Password", "password-reset-email", templateVars, user);
    }
    
    @Override
    @Transactional
    public EmailNotification sendNewUserVerificationEmail(User user, String token) throws MessagingException {
        Map<String, Object> templateVars = Map.of(
            "verificationLink", frontendUrl + "/verify-email?token=" + token,
            "userName", user.getFirstName()
        );
        
        return sendTemplateEmail(user.getEmail(), "Your account has been created", "new-user-email", templateVars, user);
    }

    @Override
    @Transactional
    public EmailNotification sendEmailFromNotification(EmailNotification notification) {
        try {
            // Create a MimeMessage from the notification
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setTo(notification.getRecipientEmail());
            helper.setSubject(notification.getSubject());
            helper.setText(notification.getBody(), true);
            
            // Try to send the email
            mailSender.send(message);
            
            // Update status to SENT
            notification.setStatus(EmailNotification.EmailStatus.SENT);
            return emailNotificationRepository.save(notification);
        } catch (Exception e) {
            // Update status to FAILED and save error message
            notification.setStatus(EmailNotification.EmailStatus.FAILED);
            notification.setErrorMessage(e.getMessage());
            return emailNotificationRepository.save(notification);
        }
    }

    @Override
    @Transactional
    public EmailNotification sendApplicationEmail(Application application, EmailEvent event) throws MessagingException {
        EventConfig config = eventConfigs.get(event);
        if (config == null) {
            throw new IllegalArgumentException("Unsupported email event: " + event);
        }

        // Determine recipient based on event configuration
        String recipientEmail = getRecipientEmail(application, null, config.recipientType);
        User relatedUser = getRelatedUser(application, null, config.recipientType);
        
        // Generate subject
        String subject = String.format(config.subjectTemplate, application.getJob().getTitle());
        
        // Generate template variables based on event
        Map<String, Object> templateVars = buildApplicationTemplateVariables(application, event);
        
        return sendTemplateEmail(recipientEmail, subject, event.getTemplateName(), templateVars, relatedUser);
    }

    @Override
    @Transactional
    public EmailNotification sendInterviewEmail(Interview interview, EmailEvent event) throws MessagingException {
        EventConfig config = eventConfigs.get(event);
        if (config == null) {
            throw new IllegalArgumentException("Unsupported email event: " + event);
        }

        // Determine recipient based on event configuration
        String recipientEmail = getRecipientEmail(interview.getApplication(), interview, config.recipientType);
        User relatedUser = getRelatedUser(interview.getApplication(), interview, config.recipientType);
        
        // Generate subject
        String subject = String.format(config.subjectTemplate, interview.getApplication().getJob().getTitle());
        
        // Generate template variables based on event
        Map<String, Object> templateVars = buildInterviewTemplateVariables(interview, event);
        
        return sendTemplateEmail(recipientEmail, subject, event.getTemplateName(), templateVars, relatedUser);
    }

    /**
     * Determines the recipient email based on the recipient type
     */
    private String getRecipientEmail(Application application, Interview interview, RecipientType recipientType) {
        switch (recipientType) {
            case CANDIDATE:
                return application.getCandidate().getEmail();
            case INTERVIEWER:
                if (interview == null) {
                    throw new IllegalArgumentException("Interview is required for INTERVIEWER recipient type");
                }
                return interview.getInterviewer().getEmail();
            case ADMIN:
                // For now, return the admin who shortlisted (if available)
                return application.getShortlistedBy() != null ? 
                       application.getShortlistedBy().getEmail() : 
                       application.getCandidate().getEmail(); // fallback
            default:
                throw new IllegalArgumentException("Unsupported recipient type: " + recipientType);
        }
    }

    /**
     * Determines the related user based on the recipient type
     */
    private User getRelatedUser(Application application, Interview interview, RecipientType recipientType) {
        switch (recipientType) {
            case CANDIDATE:
                return application.getCandidate();
            case INTERVIEWER:
                if (interview == null) {
                    throw new IllegalArgumentException("Interview is required for INTERVIEWER recipient type");
                }
                return interview.getInterviewer();
            case ADMIN:
                return application.getShortlistedBy();
            default:
                return null;
        }
    }

    /**
     * Builds template variables for application-related emails
     */
    private Map<String, Object> buildApplicationTemplateVariables(Application application, EmailEvent event) {
        Map<String, Object> templateVars = new HashMap<>();
        
        // Common variables for all application emails
        templateVars.put("candidateName", application.getCandidate().getFirstName() + " " + application.getCandidate().getLastName());
        templateVars.put("jobTitle", application.getJob().getTitle());
        templateVars.put("applicationId", application.getId().toString());
        
        // Event-specific variables
        switch (event) {
            case APPLICATION_RECEIVED:
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMMM dd, yyyy 'at' hh:mm a");
                templateVars.put("applicationDate", application.getCreatedAt().format(formatter));
                break;
            case APPLICATION_REVIEWED:
            case APPLICATION_SHORTLISTED:
                // No additional variables needed for these events
                break;
        }
        
        return templateVars;
    }

    /**
     * Builds template variables for interview-related emails
     */
    private Map<String, Object> buildInterviewTemplateVariables(Interview interview, EmailEvent event) {
        Map<String, Object> templateVars = new HashMap<>();
        Application application = interview.getApplication();
        
        // Common variables for all interview emails
        templateVars.put("candidateName", application.getCandidate().getFirstName() + " " + application.getCandidate().getLastName());
        templateVars.put("jobTitle", application.getJob().getTitle());
        templateVars.put("applicationId", application.getId().toString());
        
        // Add scheduled date if available
        if (interview.getScheduledAt() != null) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEEE, MMMM dd, yyyy 'at' hh:mm a");
            templateVars.put("scheduledDate", interview.getScheduledAt().format(formatter));
        }
        
        // Event-specific variables
        switch (event) {
            case INTERVIEW_ASSIGNED_TO_INTERVIEWER:
                templateVars.put("interviewerName", interview.getInterviewer().getFirstName() + " " + interview.getInterviewer().getLastName());
                templateVars.put("candidateEmail", application.getCandidate().getEmail());
                templateVars.put("interviewTemplate", interview.getSkeleton().getName());
                templateVars.put("interviewerPortalLink", frontendUrl + "/interviewer/dashboard");
                break;
            case INTERVIEW_ASSIGNED_TO_CANDIDATE:
                templateVars.put("candidatePortalLink", frontendUrl + "/candidate/dashboard");
                break;
        }
        
        return templateVars;
    }
} 