package com.khathabook.service;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpMethod;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.io.IOException;
import org.springframework.web.multipart.MultipartFile;
import java.util.Optional;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;

import com.khathabook.model.Product;
import com.khathabook.model.Retailer;
import com.khathabook.repository.ProductRepository;
import com.khathabook.repository.RetailerRepository;

@Service
public class ProductService {

    private final ProductRepository productRepo;
    private final RetailerRepository retailerRepo;

    public ProductService(ProductRepository productRepo,
                          RetailerRepository retailerRepo) {
        this.productRepo = productRepo;
        this.retailerRepo = retailerRepo;
    }

    // =================================================
    // ✅ CREATE PRODUCT WITH INITIAL STOCK
    // =================================================
    public Product createProduct(Product product, int boxes, Long retailerId) {

        Retailer retailer = retailerRepo.findById(retailerId)
                .orElseThrow(() -> new RuntimeException("Retailer not found"));

        if (product.getBarcode() != null && !product.getBarcode().trim().isEmpty()) {
            productRepo.findByBarcodeAndRetailer_Id(product.getBarcode(), retailerId)
                .ifPresent(p -> {
                    throw new RuntimeException("You already have a product with this barcode: " + p.getName());
                });
        }

        if (product.getCategory() == null) {
            throw new RuntimeException("Category is required");
        }

        product.setRetailer(retailer);
        
        // Use provided barcode or generate one
        if (product.getBarcode() == null || product.getBarcode().trim().isEmpty()) {
            product.setBarcode("PRD-" + System.currentTimeMillis());
        }

        double stock;

        switch (product.getProductType()) {

            case "WEIGHT" -> {
                if (product.getBagSizeKg() == null)
                    throw new RuntimeException("Bag size missing");
                stock = boxes * product.getBagSizeKg();
            }

            case "LIQUID" -> {
                if (product.getPacketsPerBox() == null || product.getPacketSize() == null)
                    throw new RuntimeException("Liquid config missing");
                stock = boxes * product.getPacketsPerBox() * product.getPacketSize();
            }

            case "UNIT" -> {
                if (product.getUnitsPerBox() == null)
                    throw new RuntimeException("Units per box missing");
                stock = boxes * product.getUnitsPerBox();
            }

            default -> throw new RuntimeException("Invalid product type");
        }

        product.setQuantity(stock);
        return productRepo.save(product);
    }

    // =================================================
    // ✅ DELETE PRODUCT
    // =================================================
    public void deleteProduct(Long productId, Long retailerId) {

        Product product = productRepo.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        if (!product.getRetailer().getId().equals(retailerId)) {
            throw new RuntimeException("Unauthorized delete");
        }

        productRepo.delete(product);
    }

    public Object lookupUPC(String barcode) {
        String url = "https://world.openfoodfacts.org/api/v0/product/" + barcode + ".json";
        RestTemplate restTemplate = new RestTemplate();
        try {
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            if (response != null && Integer.valueOf(1).equals(response.get("status"))) {
                Map<String, Object> productData = (Map<String, Object>) response.get("product");
                return Map.of(
                    "status", "OK",
                    "name", productData.getOrDefault("product_name", ""),
                    "imageUrl", productData.getOrDefault("image_url", ""),
                    "brand", productData.getOrDefault("brands", ""),
                    "source", "Open Food Facts"
                );
            }
        } catch (Exception e) {
            System.err.println("❌ OFF Error: " + e.getMessage());
        }
        return Map.of("status", "NOT_FOUND", "message", "Product not found in Open Food Facts. Please enter details manually.");
    }

    private boolean isFound(Object result) {
        if (result instanceof Map) {
            return "OK".equals(((Map<?, ?>) result).get("status"));
        }
        return false;
    }

    // ================= IMAGE UPLOAD =================
    private static final String PRODUCT_UPLOAD_DIR = "uploads/products/";

    public String uploadProductImage(MultipartFile file) throws IOException {
        Path uploadPath = Paths.get(PRODUCT_UPLOAD_DIR);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        String filename = "prod_" + System.currentTimeMillis() + "_" + file.getOriginalFilename().replaceAll("\\s+", "_");
        Path filePath = uploadPath.resolve(filename);

        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
        
        return filename;
    }

    public byte[] getProductImage(String filename) throws IOException {
        Path filePath = Paths.get(PRODUCT_UPLOAD_DIR).resolve(filename);
        if (!Files.exists(filePath)) {
            throw new RuntimeException("File not found: " + filename);
        }
        return Files.readAllBytes(filePath);
    }
}
