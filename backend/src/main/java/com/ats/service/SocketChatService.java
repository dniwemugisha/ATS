package com.ats.service;

import com.ats.dto.ChatMessageDTO;
import com.ats.dto.ConversationDTO;
import com.ats.model.Chat;
import com.ats.model.Conversation;
import com.ats.model.ConversationStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class SocketChatService {

    private final ChatService chatService;
    private final ConversationService conversationService;
    private static final Logger log = LoggerFactory.getLogger(SocketChatService.class);

    @Autowired
    public SocketChatService(ChatService chatService, ConversationService conversationService) {
        this.chatService = chatService;
        this.conversationService = conversationService;
    }

    public ConversationJoinResult joinChat(Long userId) {
        // Check if there's an existing active conversation first
        List<Conversation> activeConversations = conversationService.getConversationsByCandidate(userId)
            .stream()
            .filter(conv -> conv.getStatus() == ConversationStatus.ACTIVE)
            .toList();
        
        boolean isNewConversation = activeConversations.isEmpty();
        
        Conversation conversation = conversationService.getOrCreateConversationForCandidate(userId);
        ConversationDTO conversationDTO = mapToConversationDTO(conversation);
        
        List<Chat> messages = chatService.getMessagesByConversation(conversation.getId());
        List<ChatMessageDTO> messageDTOs = messages.stream()
            .map(this::mapToChatMessageDTO)
            .toList();
            
        return new ConversationJoinResult(conversationDTO, messageDTOs, isNewConversation);
    }

    public ConversationDTO takeConversation(Long adminId, Long conversationId) {
        Optional<Conversation> conversationOpt = conversationService.getConversationById(conversationId);
        if (conversationOpt.isEmpty()) {
            throw new RuntimeException("Conversation not found");
        }
        
        Conversation conversation = conversationOpt.get();
        if (conversation.getAdmin() != null) {
            throw new RuntimeException("Conversation already assigned to another admin");
        }
        
        Conversation updatedConversation = conversationService.assignAdminToConversation(conversationId, adminId);
        return mapToConversationDTO(updatedConversation);
    }

    public ChatMessageDTO sendMessage(Long conversationId, Long userId, String content) {
        log.info("🔄 SocketChatService: Processing message from user {} to conversation {}", userId, conversationId);
        
        // Validate that the conversation is still active
        Optional<Conversation> conversationOpt = conversationService.getConversationById(conversationId);
        if (conversationOpt.isEmpty()) {
            log.error("❌ Conversation not found: {}", conversationId);
            throw new RuntimeException("Conversation not found");
        }
        
        Conversation conversation = conversationOpt.get();
        log.info("📋 Conversation details: ID={}, Status={}, Candidate={}, Admin={}", 
                conversation.getId(), conversation.getStatus(), 
                conversation.getCandidate().getId(), 
                conversation.getAdmin() != null ? conversation.getAdmin().getId() : "null");
        
        if (conversation.getStatus() == ConversationStatus.CLOSED) {
            log.error("❌ Cannot send message: Conversation {} is closed", conversationId);
            throw new RuntimeException("Cannot send message: Conversation has been closed");
        }
        
        // Save the message
        Chat savedMessage = chatService.sendMessage(conversationId, userId, content);
        log.info("💾 Message saved successfully: ID={}, Content='{}'", savedMessage.getId(), savedMessage.getContent());
        
        ChatMessageDTO messageDTO = mapToChatMessageDTO(savedMessage);
        log.info("📤 Message DTO created: ID={}, ConversationId={}, SenderId={}", 
                messageDTO.getId(), messageDTO.getConversationId(), messageDTO.getSenderId());
        
        return messageDTO;
    }

    public ConversationDTO closeConversation(Long conversationId) {
        Conversation closedConversation = conversationService.closeConversation(conversationId);
        return mapToConversationDTO(closedConversation);
    }

    public List<ConversationDTO> getUnassignedConversations() {
        List<Conversation> unassignedConversations = conversationService.getUnassignedConversations();
        
        // Return all unassigned conversations (no message filtering)
        return unassignedConversations.stream()
            .map(this::mapToConversationDTO)
            .toList();
    }

    public List<ConversationDTO> getActiveConversationsByAdmin(Long adminId) {
        List<Conversation> activeConversations = conversationService.getActiveConversationsByAdmin(adminId);
        return activeConversations.stream()
            .map(this::mapToConversationDTO)
            .toList();
    }

    public List<ConversationDTO> closeAllAdminConversations(Long adminId) {
        List<Conversation> activeConversations = conversationService.getActiveConversationsByAdmin(adminId);
        List<ConversationDTO> closedConversations = new ArrayList<>();
        
        for (Conversation conversation : activeConversations) {
            try {
                Conversation closedConversation = conversationService.closeConversation(conversation.getId());
                closedConversations.add(mapToConversationDTO(closedConversation));
            } catch (Exception e) {
                // Log error but continue with other conversations
                System.err.println("Error closing conversation " + conversation.getId() + ": " + e.getMessage());
            }
        }
        
        return closedConversations;
    }

    public ConversationDTO getConversationDTO(Long conversationId) {
        Optional<Conversation> conversationOpt = conversationService.getConversationById(conversationId);
        if (conversationOpt.isEmpty()) {
            return null;
        }
        
        return mapToConversationDTO(conversationOpt.get());
    }

    private ChatMessageDTO mapToChatMessageDTO(Chat chat) {
        ChatMessageDTO dto = new ChatMessageDTO();
        dto.setId(chat.getId());
        dto.setConversationId(chat.getConversation().getId());
        dto.setSenderId(chat.getSender().getId());
        dto.setSenderName(chat.getSender().getFirstName() + " " + chat.getSender().getLastName());
        dto.setSenderRole(chat.getSender().getRole().toString());
        dto.setContent(chat.getContent());
        dto.setCreatedAt(chat.getCreatedAt().atZone(ZoneId.systemDefault()).toString());
        dto.setMessageType("text");
        return dto;
    }

    private ConversationDTO mapToConversationDTO(Conversation conversation) {
        ConversationDTO dto = new ConversationDTO();
        dto.setId(conversation.getId());
        dto.setCandidateId(conversation.getCandidate().getId());
        dto.setCandidateName(conversation.getCandidate().getFirstName() + " " + conversation.getCandidate().getLastName());
        
        if (conversation.getAdmin() != null) {
            dto.setAdminId(conversation.getAdmin().getId());
            dto.setAdminName(conversation.getAdmin().getFirstName() + " " + conversation.getAdmin().getLastName());
        }
        
        dto.setStatus(conversation.getStatus());
        dto.setCreatedAt(conversation.getCreatedAt().atZone(ZoneId.systemDefault()).toString());
        dto.setUpdatedAt(conversation.getUpdatedAt().atZone(ZoneId.systemDefault()).toString());
        return dto;
    }

    public static class ConversationJoinResult {
        private final ConversationDTO conversation;
        private final List<ChatMessageDTO> messages;
        private final boolean isNewConversation;

        public ConversationJoinResult(ConversationDTO conversation, List<ChatMessageDTO> messages, boolean isNewConversation) {
            this.conversation = conversation;
            this.messages = messages;
            this.isNewConversation = isNewConversation;
        }

        public ConversationDTO getConversation() { return conversation; }
        public List<ChatMessageDTO> getMessages() { return messages; }
        public boolean isNewConversation() { return isNewConversation; }
    }
} 