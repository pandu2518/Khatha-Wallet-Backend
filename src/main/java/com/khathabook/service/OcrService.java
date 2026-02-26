package com.khathabook.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class OcrService {

    public String extractTextFromBytes(byte[] imageBytes) {
        System.out.println("📖 [OCR] OCR is currently disabled for stability.");
        return "";
    }

    public String extractText(MultipartFile imageFile) {
        return "";
    }
}
