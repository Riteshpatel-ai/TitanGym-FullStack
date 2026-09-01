package com.titangym.ecommerce.mapper;

import com.titangym.ecommerce.dto.ProductDTO.ProductRequestDTO;
import com.titangym.ecommerce.dto.ProductDTO.ProductResponseDTO;
import com.titangym.ecommerce.model.ProductEntity;
import org.springframework.stereotype.Component;

@Component
public class ProductMapper {
    /**
     * Map a ProductRequestDTO to a ProductEntity
     */
    public ProductEntity mapToEntity(ProductRequestDTO dto) {
        return new ProductEntity(
                dto.getName(),
                dto.getDescription(),
                dto.getPrice(),
                dto.getImageUrl(),
                dto.getCategory(),
                dto.getQuantity()
        );
    }

    /**
     * Map a ProductEntity to a ProductResponseDTO
     */
    public ProductResponseDTO mapToDTO(ProductEntity dto) {
        return new ProductResponseDTO(
                dto.getId(),
                dto.getName(),
                dto.getDescription(),
                dto.getPrice(),
                dto.getImageUrl(),
                dto.getCategory(),
                dto.getQuantity()
        );
    }
}
