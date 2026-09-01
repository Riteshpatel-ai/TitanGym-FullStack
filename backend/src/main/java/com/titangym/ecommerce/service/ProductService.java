package com.titangym.ecommerce.service;

import com.titangym.ecommerce.dto.ProductDTO.ProductRequestDTO;
import com.titangym.ecommerce.dto.ProductDTO.ProductResponseDTO;
import com.titangym.ecommerce.mapper.ProductMapper;
import com.titangym.ecommerce.model.ProductEntity;
import com.titangym.ecommerce.repository.CartItemRepository;
import com.titangym.ecommerce.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class ProductService {
    /**
     * Product repository
     */
    private final ProductRepository productRepository;

    /**
     *
     */
    private final CartItemRepository cartItemRepository;

    /**
     * Product Mapper
     */
    private final ProductMapper productMapper;

    /**
     * Constructor
     */
    @Autowired
    public ProductService(ProductRepository productRepository, CartItemRepository cartItemRepository, ProductMapper productMapper) {
        this.productRepository = productRepository;
        this.cartItemRepository = cartItemRepository;
        this.productMapper = productMapper;
    }

    /**
     * Get all products
     */
    public Map<String, Object> getAllProducts(int page, int size, String search) {
        int pageSize = size <= 0 ? 10 : size;
        int requestedPage = Math.max(page, 0);
        String query = search == null ? "" : search.trim();

        List<ProductEntity> filteredProducts = productRepository.findAll().stream()
                .filter(GymCatalogPolicy::isGymRelevant)
                .filter(product -> GymCatalogPolicy.matchesSearch(product, query))
                .sorted(Comparator.comparing(product -> product.getName().toLowerCase()))
                .toList();

        int totalItems = filteredProducts.size();
        int totalPages = Math.max(1, (int) Math.ceil((double) totalItems / pageSize));
        int currentPage = Math.min(requestedPage, totalPages - 1);
        int fromIndex = Math.min(currentPage * pageSize, totalItems);
        int toIndex = Math.min(fromIndex + pageSize, totalItems);
        List<ProductEntity> pageContent = totalItems == 0
                ? new ArrayList<>()
                : filteredProducts.subList(fromIndex, toIndex);
        List<ProductResponseDTO> productResponseDTO = pageContent.stream()
                        .map(productMapper::mapToDTO)
                        .peek(dto -> dto.setPrice(convertToINR(dto.getPrice())))
                        .toList();

                Map<String, Object> response = new HashMap<>();
                response.put("products", productResponseDTO);
                response.put("currentPage", currentPage);
                response.put("totalItems", totalItems);
                response.put("totalPages", totalPages);

                return response;
    }

    /**
     * Add a product
     */
    public ProductResponseDTO addProduct(ProductRequestDTO product) {
        // Convert ProductRequestDTO to ProductEntity
        return productMapper.mapToDTO(productRepository.save(productMapper.mapToEntity(product)));
    }

    /**
     * Update a product
     */
    public ProductResponseDTO updateProduct(Long id, ProductRequestDTO product) {
        // Check if the product exists
        ProductEntity existing = productRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found"));

        // Update fields manually
        existing.setName(product.getName());
        existing.setDescription(product.getDescription());
        existing.setPrice(product.getPrice());
        existing.setQuantity(product.getQuantity());
        existing.setCategory(product.getCategory());
        existing.setImageUrl(product.getImageUrl());
        return productMapper.mapToDTO(productRepository.save(existing));
    }

    /**
     * Delete a product
     */
    @Transactional
    public void deleteProduct(Long id) {
        // Check if the product exists
        ProductEntity existing = productRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found"));
        // Check if the product is in any cart
        cartItemRepository.deleteByProductId(id);

        productRepository.delete(existing);

    }

    /**
     * Get a product by id
     */
    public ProductResponseDTO getProductById(Long id) {
        // Check if the product exists
        ProductEntity existing = productRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found"));
        if (!GymCatalogPolicy.isGymRelevant(existing)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found");
        }
            ProductResponseDTO dto = productMapper.mapToDTO(existing);
            dto.setPrice(convertToINR(dto.getPrice()));
            return dto;
        }

        /**
         * Convert a price to INR using a fixed exchange rate
         */
        private BigDecimal convertToINR(BigDecimal amount) {
            if (amount == null) return null;
            // Fixed exchange rate: 1 USD = 83 INR
            BigDecimal rate = BigDecimal.valueOf(83);
            return amount.multiply(rate).setScale(2, RoundingMode.HALF_UP);
        }

}
