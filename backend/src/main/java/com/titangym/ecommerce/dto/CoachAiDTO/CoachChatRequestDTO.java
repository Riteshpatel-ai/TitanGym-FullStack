package com.titangym.ecommerce.dto.CoachAiDTO;

import jakarta.validation.constraints.NotBlank;

public class CoachChatRequestDTO {

    @NotBlank(message = "Message is required")
    private String message;

    public CoachChatRequestDTO() {
    }

    public CoachChatRequestDTO(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
