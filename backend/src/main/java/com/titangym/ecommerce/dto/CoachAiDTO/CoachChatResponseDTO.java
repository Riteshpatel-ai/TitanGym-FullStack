package com.titangym.ecommerce.dto.CoachAiDTO;

import java.util.List;

public class CoachChatResponseDTO {

    private String reply;
    private List<String> recommendations;

    public CoachChatResponseDTO() {
    }

    public CoachChatResponseDTO(String reply, List<String> recommendations) {
        this.reply = reply;
        this.recommendations = recommendations;
    }

    public String getReply() {
        return reply;
    }

    public void setReply(String reply) {
        this.reply = reply;
    }

    public List<String> getRecommendations() {
        return recommendations;
    }

    public void setRecommendations(List<String> recommendations) {
        this.recommendations = recommendations;
    }
}
