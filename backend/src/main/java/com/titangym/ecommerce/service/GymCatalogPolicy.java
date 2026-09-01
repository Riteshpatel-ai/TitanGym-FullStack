package com.titangym.ecommerce.service;

import com.titangym.ecommerce.model.ProductEntity;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

public final class GymCatalogPolicy {

    private static final Set<String> GYM_KEYWORDS = Set.of(
            "gym", "fitness", "workout", "training", "strength", "cardio",
            "recovery", "supplement", "nutrition", "protein", "creatine",
            "whey", "bcaa", "pre-workout", "apparel", "accessory", "equipment",
            "dumbbell", "barbell", "kettlebell", "bench", "rack", "mat", "rope",
            "strap", "belt", "glove", "shaker", "bottle", "roller", "resistance",
            "treadmill", "bike", "jump rope", "lifting", "lifting belt"
    );

    private GymCatalogPolicy() {
    }

    public static boolean isGymRelevant(ProductEntity product) {
        String text = searchableText(product);
        return GYM_KEYWORDS.stream().anyMatch(text::contains);
    }

    public static boolean matchesSearch(ProductEntity product, String search) {
        if (search == null || search.isBlank()) {
            return true;
        }
        String normalizedSearch = normalize(search);
        String text = searchableText(product);
        return text.contains(normalizedSearch);
    }

    public static List<ProductEntity> filterGymProducts(Collection<ProductEntity> products) {
        return products.stream()
                .filter(GymCatalogPolicy::isGymRelevant)
                .sorted(Comparator.comparing(
                        product -> normalize(product.getName()),
                        Comparator.naturalOrder()
                ))
                .collect(Collectors.toList());
    }

    public static List<ProductEntity> rankByQuery(Collection<ProductEntity> products, String query, int limit) {
        List<String> tokens = tokenize(query);
        return products.stream()
                .sorted(Comparator
                        .comparingInt((ProductEntity product) -> score(product, tokens))
                        .reversed()
                        .thenComparing(product -> normalize(product.getName())))
                .limit(Math.max(limit, 0))
                .collect(Collectors.toList());
    }

    public static String buildCatalogContext(List<ProductEntity> products) {
        if (products.isEmpty()) {
            return "No products are loaded yet.";
        }

        return products.stream()
                .limit(12)
                .map(product -> String.format(
                        Locale.US,
                        "- %s | %s | %s | stock:%d",
                        safeText(product.getName(), "Unnamed product"),
                        safeText(product.getCategory(), "gym gear"),
                        formatPrice(product.getPrice()),
                        product.getQuantity()
                ))
                .collect(Collectors.joining("\n"));
    }

    private static int score(ProductEntity product, List<String> tokens) {
        if (tokens.isEmpty()) {
            return 0;
        }

        String text = searchableText(product);
        int matches = 0;
        for (String token : tokens) {
            if (text.contains(token)) {
                matches += 2;
            }
        }
        return matches;
    }

    private static List<String> tokenize(String query) {
        if (query == null || query.isBlank()) {
            return List.of();
        }
        String[] parts = normalize(query).split("\\s+");
        List<String> tokens = new ArrayList<>();
        for (String part : parts) {
            if (part.length() > 2) {
                tokens.add(part);
            }
        }
        return tokens;
    }

    private static String searchableText(ProductEntity product) {
        return normalize(String.join(" ",
                safeText(product.getName(), ""),
                safeText(product.getDescription(), ""),
                safeText(product.getCategory(), "")));
    }

    private static String normalize(String value) {
        return safeText(value, "").toLowerCase(Locale.ROOT).replaceAll("\\s+", " ").trim();
    }

    private static String safeText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private static String formatPrice(BigDecimal price) {
        return price == null ? "price unavailable" : "$" + price.stripTrailingZeros().toPlainString();
    }
}
