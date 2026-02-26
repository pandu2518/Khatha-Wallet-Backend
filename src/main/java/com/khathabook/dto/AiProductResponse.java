package com.khathabook.dto;

public class AiProductResponse {

    private Long id;
    private String name;
    private String barcode;
    private double price;
    private double confidence;
    private String ocrText;
    private String imageUrl; // 🖼️ NEW FIELD

    public AiProductResponse(
            Long id,
            String name,
            String barcode,
            double price,
            double confidence,
            String ocrText,
            String imageUrl
    ) {
        this.id = id;
        this.name = name;
        this.barcode = barcode;
        this.price = price;
        this.confidence = confidence;
        this.ocrText = ocrText;
        this.imageUrl = imageUrl;
    }

    public AiProductResponse(Long id, String name, String barcode, double price, double confidence) {
        this(id, name, barcode, price, confidence, null, null);
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getBarcode() { return barcode; }
    public double getPrice() { return price; }
    public double getConfidence() { return confidence; }
    public String getOcrText() { return ocrText; }
    public String getImageUrl() { return imageUrl; }
}
