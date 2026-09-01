package com.titangym.ecommerce.controller;

import com.titangym.ecommerce.dto.CoachAiDTO.CoachChatRequestDTO;
import com.titangym.ecommerce.dto.CoachAiDTO.CoachChatResponseDTO;
import com.titangym.ecommerce.service.CoachAiService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/coach")
public class CoachAiController {

    private final CoachAiService coachAiService;

    public CoachAiController(CoachAiService coachAiService) {
        this.coachAiService = coachAiService;
    }

    @PostMapping("/chat")
    public ResponseEntity<CoachChatResponseDTO> chat(@Valid @RequestBody CoachChatRequestDTO request) {
        return ResponseEntity.ok(coachAiService.chat(request));
    }
}
