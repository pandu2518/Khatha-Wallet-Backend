package com.khathabook.service;

import ai.onnxruntime.*;
import com.khathabook.dto.AiProductResponse;
import com.khathabook.model.Product;
import com.khathabook.repository.ProductRepository;
import com.khathabook.ai.utils.ImagePreprocessor;
import com.khathabook.ai.utils.LabelMapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.nio.FloatBuffer;
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
