package com.khathabook.service;

import com.khathabook.dto.AiProductResponse;
import com.khathabook.model.Product;
import com.khathabook.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Optional;

@Service
public class AiProductService {

    private final ProductRepository productRepository;

    public AiProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public AiProductResponse detectProduct(MultipartFile image) {
        System.out.println("📸 [AI-SCAN] AI Features are currently disabled for stability.");
        return new AiProductResponse(null, "AI Disabled", null, 0.0, 0.0f, "OCR Disabled", null);
    }
}
