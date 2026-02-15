package com.lld.airbnb.services;

import com.lld.airbnb.models.Message;
import java.util.*;
import java.util.stream.Collectors;

public class MessagingService {
    private Map<String, Message> messages;
    private Map<String, List<Message>> conversationsByUser;  // userId -> all conversations

    public MessagingService() {
        this.messages = new HashMap<>();
        this.conversationsByUser = new HashMap<>();
    }

    public Message sendMessage(String messageId, String senderId, String receiverId,
                              String content, String bookingId) {
        Message message = new Message(messageId, senderId, receiverId, content, bookingId);
        messages.put(messageId, message);

        // Add to sender's conversations
        conversationsByUser.computeIfAbsent(senderId, k -> new ArrayList<>()).add(message);

        // Add to receiver's conversations
        conversationsByUser.computeIfAbsent(receiverId, k -> new ArrayList<>()).add(message);

        System.out.println("Message sent from " + senderId + " to " + receiverId);
        return message;
    }

    public List<Message> getConversation(String user1Id, String user2Id) {
        return messages.values().stream()
                .filter(m -> (m.getSenderId().equals(user1Id) && m.getReceiverId().equals(user2Id)) ||
                            (m.getSenderId().equals(user2Id) && m.getReceiverId().equals(user1Id)))
                .sorted(Comparator.comparing(Message::getSentAt))
                .collect(Collectors.toList());
    }

    public List<Message> getMessagesForUser(String userId) {
        return conversationsByUser.getOrDefault(userId, new ArrayList<>()).stream()
                .sorted(Comparator.comparing(Message::getSentAt).reversed())
                .collect(Collectors.toList());
    }

    public List<Message> getUnreadMessages(String userId) {
        return messages.values().stream()
                .filter(m -> m.getReceiverId().equals(userId) && !m.isRead())
                .sorted(Comparator.comparing(Message::getSentAt).reversed())
                .collect(Collectors.toList());
    }

    public void markMessageAsRead(String messageId) {
        Message message = messages.get(messageId);
        if (message != null) {
            message.markAsRead();
            System.out.println("Message marked as read: " + messageId);
        }
    }

    public void displayConversation(String user1Id, String user2Id) {
        List<Message> conversation = getConversation(user1Id, user2Id);
        System.out.println("\n=== Conversation between " + user1Id + " and " + user2Id + " ===");
        System.out.println("Total Messages: " + conversation.size());
        System.out.println();
        for (Message msg : conversation) {
            System.out.println("[" + msg.getSentAt() + "] " +
                             msg.getSenderId() + " → " + msg.getReceiverId() + ": " +
                             msg.getContent() +
                             (msg.isRead() ? " ✓✓" : " ✓"));
        }
    }
}
