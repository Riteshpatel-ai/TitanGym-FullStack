package com.titangym.ecommerce.mapper;

import com.titangym.ecommerce.dto.CartDTO.CartResponseDTO;
import com.titangym.ecommerce.model.CartItemEntity;
import org.springframework.stereotype.Component;

@Component
public class CartMapper {

    private final ProductMapper productMapper;

    public CartMapper(ProductMapper productMapper) {
        this.productMapper = productMapper;
    }

    public CartResponseDTO mapToDTO(CartItemEntity entity) {
        return new CartResponseDTO(
                entity.getId(),
                productMapper.mapToDTO(entity.getProduct()),
                entity.getQuantity()
        );
    }
}
