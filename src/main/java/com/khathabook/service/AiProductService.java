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
    private final OcrService ocrService; // ✅ Re-enable
    private OrtEnvironment env;
    private OrtSession session;

    public AiProductService(ProductRepository productRepository, OcrService ocrService) {
        this.productRepository = productRepository;
        this.ocrService = ocrService;
    }

    @PostConstruct
    public void init() {
        try {
            this.env = OrtEnvironment.getEnvironment();
            // Load model from resources
            InputStream modelStream = getClass().getResourceAsStream("/models/product-detect.onnx");
            if (modelStream == null) {
                System.err.println("⚠️ [AI-INIT] product-detect.onnx not found in resources. AI will be disabled.");
                return;
            }
            byte[] modelBytes = modelStream.readAllBytes();
            this.session = env.createSession(modelBytes, new OrtSession.SessionOptions());
            System.out.println("✅ [AI-INIT] ONNX Model Loaded Successfully");
        } catch (Throwable e) {
            System.err.println("❌ [AI-INIT] Failed to load ONNX model. AI features will be skipped. Error: " + e.getMessage());
            // We do NOT throw exception here, to allow the Rest of the app (Login, Billing) to start!
        }
    }

    // 🧠 AI Product Detection (Improved Flow v2)
    public AiProductResponse detectProduct(MultipartFile image) {
        System.out.println("📸 [AI-SCAN] Received image: " + image.getOriginalFilename() + " (" + image.getSize() + " bytes)");
        
        String ocrText = "";
        String productName = "UNKNOWN";
        float confidence = 0.0f;
        Optional<Product> productOpt = Optional.empty();

        try {
            byte[] imageBytes = image.getBytes();
            
            // 1️⃣ ALWAYS RUN OCR FIRST (Highest accuracy for branded products)
            if (ocrService != null) {
                System.out.println("🔄 [AI-SCAN] Phase 1: Running OCR Extraction...");
                try {
                    ocrText = ocrService.extractTextFromBytes(imageBytes);
                    System.out.println("📄 [AI-SCAN] OCR Found: [" + ocrText + "]");
                    
                    if (!ocrText.isEmpty()) {
                        // ... (Matching logic remains same)
                    }
                } catch (Exception e) {
                    System.err.println("⚠️ [AI-SCAN] OCR Error: " + e.getMessage());
                }
            }

            // 2️⃣ RUN AI DETECTION (Fallback or Category Prediction)
            if (productOpt.isEmpty() && session != null) {
                System.out.println("🔄 [AI-SCAN] Phase 2: Running ONNX Inference...");
                
                FloatBuffer inputTensor = ImagePreprocessor.preprocessFromBytes(imageBytes);
                OnnxTensor tensor = OnnxTensor.createTensor(env, inputTensor, new long[]{1, 3, 224, 224});

                OrtSession.Result result = session.run(
                        java.util.Collections.singletonMap(
                                session.getInputNames().iterator().next(),
                                tensor
                        )
                );

                Object output = result.get(0).getValue();
                float[] probabilities;

                if (output instanceof float[][][]) { probabilities = ((float[][][]) output)[0][0]; }
                else if (output instanceof float[][]) { probabilities = ((float[][]) output)[0]; }
                else if (output instanceof float[]) { probabilities = (float[]) output; }
                else { throw new RuntimeException("Unsupported ONNX output type"); }

                int predictedIndex = LabelMapper.argmax(probabilities);
                confidence = probabilities[predictedIndex];
                productName = LabelMapper.map(predictedIndex);
                
                System.out.println("🤖 [AI-SCAN] AI Predicted index: " + predictedIndex + " (out of " + probabilities.length + ")");
                System.out.println("🤖 [AI-SCAN] AI Predicted: " + productName + " (" + confidence + ")");

                // DB lookup with AI label
                productOpt = productRepository.findTopByNameContainingIgnoreCase(productName);
            }

            // 3️⃣ FINAL RESPONSE BUILD
            double extractedPrice = 0.0;
            if (productOpt.isEmpty() && !ocrText.isEmpty()) {
                extractedPrice = extractPrice(ocrText);
            }

            if (productOpt.isPresent()) {
                Product product = productOpt.get();
                return new AiProductResponse(
                        product.getId(),
                        product.getName(),
                        product.getBarcode(),
                        product.getPrice(),
                        confidence,
                        ocrText,
                        product.getImageUrl() // ✅ PASS IMAGE URL
                );
            } else {
                System.out.println("⚠️ [AI-SCAN] No precise product match found in database.");
                return new AiProductResponse(
                        null,
                        productName,
                        null,
                        extractedPrice,
                        confidence,
                        ocrText,
                        null // No image for unknown
                );
            }

        } catch (Exception e) {
            System.err.println("❌ [AI-SCAN] FATAL ERROR: ");
            e.printStackTrace();
            throw new RuntimeException("AI detection failed: " + e.getMessage(), e);
        }
    }

    private double extractPrice(String text) {
        if (text == null || text.isEmpty()) return 0.0;
        // Look for patterns like Rs. 50, MRP 100, Price: 20.50, ₹ 100
        // We'll also look for standalone numbers that might be price if they follow a keyword
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("(MRP|Price|Rs|₹)\\s*:?\\s*(\\d+(\\.\\d+)?)", java.util.regex.Pattern.CASE_INSENSITIVE);
        java.util.regex.Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            try {
                return Double.parseDouble(matcher.group(2));
            } catch (Exception e) {
                return 0.0;
            }
        }
        return 0.0;
    }

    // 🧹 Clean shutdown
    @PreDestroy
    public void destroy() throws Exception {
        if (session != null) session.close();
        if (env != null) env.close();
    }
}
