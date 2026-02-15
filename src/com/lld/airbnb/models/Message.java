package com.lld.airbnb.models;

import java.time.LocalDateTime;

public class Message {
    private String messageId;
    private String senderId;
    private String receiverId;
    private String bookingId;       // Optional: related booking
    private String content;
    private LocalDateTime sentAt;
    private boolean isRead;

    public Message(String messageId, String senderId, String receiverId,
                  String content, String bookingId) {
        this.messageId = messageId;
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.content = content;
        this.bookingId = bookingId;
        this.sentAt = LocalDateTime.now();
        this.isRead = false;
    }

    public void markAsRead() {
        this.isRead = true;
    }

    // Getters and Setters
    public String getMessageId() { return messageId; }
    public void setMessageId(String messageId) { this.messageId = messageId; }

    public String getSenderId() { return senderId; }
    public void setSenderId(String senderId) { this.senderId = senderId; }

    public String getReceiverId() { return receiverId; }
    public void setReceiverId(String receiverId) { this.receiverId = receiverId; }

    public String getBookingId() { return bookingId; }
    public void setBookingId(String bookingId) { this.bookingId = bookingId; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public LocalDateTime getSentAt() { return sentAt; }
    public void setSentAt(LocalDateTime sentAt) { this.sentAt = sentAt; }

    public boolean isRead() { return isRead; }
    public void setRead(boolean read) { isRead = read; }

    @Override
    public String toString() {
        return "Message{" +
                "from='" + senderId + '\'' +
                ", to='" + receiverId + '\'' +
                ", content='" + content + '\'' +
                ", read=" + isRead +
                ", sentAt=" + sentAt +
                '}';
    }
}
