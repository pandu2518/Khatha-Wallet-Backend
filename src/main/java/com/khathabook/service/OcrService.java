package com.khathabook.service;

import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

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
