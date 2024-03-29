package com.eni.backend.room.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ChatDto {
    public enum MessageType{
        ENTER, TALK, LEAVE, EXPELLED;
    }

    private MessageType type;
    private String roomId;
    private String sender;
    private String message;
    private String roomType;
}
