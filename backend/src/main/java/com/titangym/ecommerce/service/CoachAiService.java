package com.titangym.ecommerce.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.titangym.ecommerce.dto.CoachAiDTO.CoachChatRequestDTO;
import com.titangym.ecommerce.dto.CoachAiDTO.CoachChatResponseDTO;
import com.titangym.ecommerce.exception.BadRequestException;
import com.titangym.ecommerce.exception.ExternalServiceException;
import com.titangym.ecommerce.model.ProductEntity;
import com.titangym.ecommerce.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Locale;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CoachAiService {

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private final ProductRepository productRepository;
    private final ObjectMapper objectMapper;
    private final String groqApiKey;
    private final String groqModel;
    private final String groqApiUrl;

    public CoachAiService(
            ProductRepository productRepository,
            ObjectMapper objectMapper,
            @Value("${groq.api.key:}") String groqApiKey,
            @Value("${groq.api.model:llama-3.1-70b-versatile}") String groqModel,
            @Value("${groq.api.url:https://api.groq.com/openai/v1/chat/completions}") String groqApiUrl
    ) {
        this.productRepository = productRepository;
        this.objectMapper = objectMapper;
        this.groqApiKey = groqApiKey;
        this.groqModel = groqModel;
        this.groqApiUrl = groqApiUrl;
    }

    public CoachChatResponseDTO chat(CoachChatRequestDTO request) {
        List<ProductEntity> catalog = GymCatalogPolicy.filterGymProducts(productRepository.findAll());
        List<ProductEntity> recommendations = GymCatalogPolicy.rankByQuery(catalog, request.getMessage(), 3);

        List<String> recommendedNames = recommendations.stream()
                .map(ProductEntity::getName)
                .toList();

        if (groqApiKey == null || groqApiKey.isBlank()) {
            return new CoachChatResponseDTO(buildFallbackReply(request.getMessage(), recommendedNames), recommendedNames);
        }

        String systemPrompt = buildSystemPrompt(catalog);
        String reply = callGroq(request.getMessage(), systemPrompt);
        return new CoachChatResponseDTO(reply, recommendedNames);
    }

    private String callGroq(String userMessage, String systemPrompt) {
        try {
            var payload = objectMapper.createObjectNode();
            payload.put("model", groqModel);
            payload.put("temperature", 0.4);

            var messages = payload.putArray("messages");
            messages.addObject()
                    .put("role", "system")
                    .put("content", systemPrompt);
            messages.addObject()
                    .put("role", "user")
                    .put("content", userMessage);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(groqApiUrl))
                    .header("Authorization", "Bearer " + groqApiKey)
                    .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload)))
                    .timeout(Duration.ofSeconds(30))
                    .build();

            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new ExternalServiceException("Groq request failed with status " + response.statusCode());
            }

            JsonNode root = objectMapper.readTree(response.body());
            String reply = root.path("choices").path(0).path("message").path("content").asText("");
            if (reply.isBlank()) {
                throw new ExternalServiceException("Groq returned an empty coach response.");
            }
            return reply.trim();
        }
        catch (IOException ex) {
            throw new ExternalServiceException("Unable to read the Groq AI response.");
        }
        catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new ExternalServiceException("Groq request was interrupted.");
        }
    }

    private String buildSystemPrompt(List<ProductEntity> catalog) {
        String catalogContext = GymCatalogPolicy.buildCatalogContext(catalog);
        return """
                You are Coach AI for TitanGym, a gym-only e-commerce store.
                Be warm, concise, and practical.
                Only recommend gym, fitness, nutrition, training, recovery, or workout gear.
                When the user asks for product advice, use the catalog below and mention matching products by name when relevant.
                If something is not in the catalog, say so clearly and suggest the closest alternative.

                Catalog:
                %s
                """.formatted(catalogContext);
    }

    private String buildFallbackReply(String userMessage, List<String> recommendations) {
        String lowered = userMessage == null ? "" : userMessage.toLowerCase(Locale.ROOT);
        if (recommendations.isEmpty()) {
            return "I’m ready to help with strength, recovery, nutrition, or workout gear. Tell me your goal, budget, or equipment type and I’ll narrow it down.";
        }

        String recommendationText = recommendations.stream().collect(Collectors.joining(", "));
        if (lowered.contains("beginner")) {
            return "For a beginner setup, start with these TitanGym picks: " + recommendationText + ". Keep it simple and build consistency first.";
        }
        if (lowered.contains("recovery")) {
            return "For recovery, I’d look at: " + recommendationText + ". These are strong picks for cooldown and muscle recovery.";
        }
        return "Based on your request, I’d start with: " + recommendationText + ". If you want, I can narrow it down by goal, budget, or workout style.";
    }
}
