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
        try (java.io.ByteArrayInputStream bis = new java.io.ByteArrayInputStream(imageBytes)) {
            BufferedImage original = ImageIO.read(bis);
            if (original == null) {
                System.err.println("❌ OCR FAIL: ImageIO.read() returned NULL (Unsupported format?)");
                return "";
            }

            Tesseract tesseract = new Tesseract();
            tesseract.setDatapath(new File("tessdata").getAbsolutePath());
            tesseract.setLanguage("eng");
            tesseract.setPageSegMode(3);

            // Pass 0: Original No-Scale
            System.err.println("📸 Pass 0: Original No-Scale");
            String res = runOcrPass(tesseract, original, "Orig");
            if (!res.isEmpty()) return res;

            // 1. Preprocess BASE image (Scale-Up + Grayscale)
            BufferedImage baseImage = new BufferedImage(original.getWidth() * 2, original.getHeight() * 2, BufferedImage.TYPE_BYTE_GRAY);
            Graphics2D g = baseImage.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g.drawImage(original, 0, 0, baseImage.getWidth(), baseImage.getHeight(), null);
            g.dispose();

            // Pass 1: 0°
            System.err.println("📖 Pass 1: 0°");
            res = runOcrPass(tesseract, baseImage, "0°");
            if (!res.isEmpty()) return res;

            // Pass 2: Rotated 90°
            System.err.println("🔄 Pass 2: 90°");
            res = runOcrPass(tesseract, rotateImage(baseImage, 90), "90°");
            if (!res.isEmpty()) return res;

            // Pass 3: Rotated 270°
            System.err.println("🔄 Pass 3: 270°");
            res = runOcrPass(tesseract, rotateImage(baseImage, 270), "270°");
            if (!res.isEmpty()) return res;

            // Pass 4: Center Crop
            System.err.println("🎯 Pass 4: Center Crop");
            BufferedImage cropped = cropImage(baseImage, 0.6);
            res = runOcrPass(tesseract, cropped, "Crop");
            if (!res.isEmpty()) return res;

            // Pass 5: Inverse Crop
            System.err.println("🌓 Pass 5: Inverse Crop");
            res = runOcrPass(tesseract, invertImage(cropped), "Inverse");
            if (!res.isEmpty()) return res;

            // Pass 6: Top Half Crop
            System.err.println("🔝 Pass 6: Top Half");
            BufferedImage top = baseImage.getSubimage(0, 0, baseImage.getWidth(), baseImage.getHeight() / 2);
            res = runOcrPass(tesseract, top, "Top");
            if (!res.isEmpty()) return res;

            return "";
        } catch (Exception e) {
            System.err.println("❌ OCR Error: " + e.getMessage());
            return "";
        }
    }

    public String extractText(MultipartFile imageFile) {
        try {
            return extractTextFromBytes(imageFile.getBytes());
        } catch (IOException e) {
            return "";
        }
    }

    private String runOcrPass(Tesseract tesseract, BufferedImage image, String label) throws TesseractException {
        String raw = tesseract.doOCR(image);
        System.err.println("🔍 [OCR-RAW] " + label + " RAW: [" + raw + "]"); // ✅ LOG EVERYTHING
        System.err.println("📄 Raw [" + label + "]: [" + raw.replace("\n", " ").trim() + "]");
        String best = extractBestText(raw);
        if (!best.isEmpty()) System.err.println("✅ Match [" + label + "]: " + best);
        return best;
    }

    private BufferedImage rotateImage(BufferedImage img, double angle) {
        int w = img.getWidth();
        int h = img.getHeight();
        BufferedImage rotated = new BufferedImage(h, w, img.getType());
        Graphics2D g = rotated.createGraphics();
        if (angle == 90) {
            g.translate(h, 0);
            g.rotate(Math.toRadians(90));
        } else if (angle == 270) {
            g.translate(0, w);
            g.rotate(Math.toRadians(270));
        }
        g.drawImage(img, 0, 0, null);
        g.dispose();
        return rotated;
    }

    private BufferedImage cropImage(BufferedImage img, double factor) {
        int w = img.getWidth();
        int h = img.getHeight();
        int cw = (int)(w * factor);
        int ch = (int)(h * factor);
        return img.getSubimage((w - cw) / 2, (h - ch) / 2, cw, ch);
    }

    private BufferedImage invertImage(BufferedImage img) {
        BufferedImage inverted = new BufferedImage(img.getWidth(), img.getHeight(), img.getType());
        for (int x = 0; x < img.getWidth(); x++) {
            for (int y = 0; y < img.getHeight(); y++) {
                int rgba = img.getRGB(x, y);
                Color col = new Color(rgba, true);
                col = new Color(255 - col.getRed(), 255 - col.getGreen(), 255 - col.getBlue());
                inverted.setRGB(x, y, col.getRGB());
            }
        }
        return inverted;
    }

    /**
     * Extract meaningful text from raw OCR output
     * Uses smart pattern matching for brands and measurements
     */
    private String extractBestText(String rawText) {
        if (rawText == null || rawText.trim().isEmpty()) {
            return "";
        }

        // Remove excessive whitespace and newlines
        String cleaned = rawText.replaceAll("\\s+", " ").trim();
        
        // If raw text is empty after basic cleanup, return empty
        if (cleaned.isEmpty()) {
            return "";
        }

        // Strategy 1: Look for brand names and measurements using regex
        StringBuilder result = new StringBuilder();
        
        // Looks for words that look like brands:
        // 1. ALL CAPS or punctuated caps (AASHIRVAAD, M.P.)
        // 2. TitleCase (Aashirvaad, Chakki)
        java.util.regex.Pattern brandPattern = java.util.regex.Pattern.compile("\\b[A-Z][A-Z0-9.]{1,}\\b|\\b[A-Z][a-z]{2,}\\b");
        java.util.regex.Matcher brandMatcher = brandPattern.matcher(cleaned);
        
        while (brandMatcher.find()) {
            String word = brandMatcher.group();
            // Filter out common noise words, but KEEP MRP/Price for downstream price extraction
            // Added words like "FOR", "NET", "THE" to be more selective but keeping brands
            if (!word.matches("(?i)(NET|USP|THE|FOR|WITH|AND|FOR|ONLY|BEST|SINCE|BBE|OF)")) {
                result.append(word).append(" ");
            }
        }

        // Find Price (MRP 50, Price 100, ₹ 100)
        java.util.regex.Pattern pricePattern = java.util.regex.Pattern.compile("(MRP|Price|Rs|₹)\\s*:?\\s*(\\d+(\\.\\d+)?)", java.util.regex.Pattern.CASE_INSENSITIVE);
        java.util.regex.Matcher priceMatcher = pricePattern.matcher(cleaned);
        if (priceMatcher.find()) {
            result.append(priceMatcher.group()).append(" ");
        }
        
        // Find measurements (50g, 100ml, 1.5kg, etc.)
        java.util.regex.Pattern measurePattern = java.util.regex.Pattern.compile("\\b\\d+(\\.\\d+)?\\s*(g|kg|ml|l|ltr|pcs|pc)\\b", java.util.regex.Pattern.CASE_INSENSITIVE);
        java.util.regex.Matcher measureMatcher = measurePattern.matcher(cleaned);
        
        while (measureMatcher.find()) {
            result.append(measureMatcher.group()).append(" ");
        }

        String smartResult = result.toString().trim();
        
        // If we found brand names or measurements, return them
        if (!smartResult.isEmpty()) {
            System.err.println("🎯 Smart extraction found: [" + smartResult + "]");
            return smartResult;
        }

        // 🚀 FALLBACK: If nothing smart found, return the first 3 cleaned words (if they look like text)
        String[] words = cleaned.split(" ");
        StringBuilder fallback = new StringBuilder();
        int count = 0;
        for (String w : words) {
            if (w.length() >= 3 && w.matches("[a-zA-Z]+")) {
                fallback.append(w).append(" ");
                if (++count >= 4) break;
            }
        }
        
        String fallbackStr = fallback.toString().trim();
        if (!fallbackStr.isEmpty()) {
            System.err.println("💡 Fallback extraction: [" + fallbackStr + "]");
            return fallbackStr;
        }

        // Strategy 2: Extract all words 3+ characters (more lenient)
        // This catches "gentle", "clean", "lasting", etc.
        StringBuilder wordsBuilder = new StringBuilder();
        java.util.regex.Pattern wordPattern = java.util.regex.Pattern.compile("\\b[a-zA-Z]{3,}\\b");
        java.util.regex.Matcher wordMatcher = wordPattern.matcher(cleaned);
        
        while (wordMatcher.find()) {
            wordsBuilder.append(wordMatcher.group()).append(" ");
        }
        
        String wordsResult = wordsBuilder.toString().trim();
        if (!wordsResult.isEmpty()) {
            System.err.println("📝 Extracted words: [" + wordsResult + "]");
            return wordsResult;
        }

        // Strategy 3: Greedy Fallback - Take a significant block of text
        // Basic cleanup - remove problematic special chars but keep most alphanumeric
        String basic = cleaned.replaceAll("[^a-zA-Z0-9\\s%.₹-]", " ")
                             .replaceAll("\\s+", " ")
                             .trim();
        
        // Return up to 60 characters of the cleaned text if it contains letters
        if (basic.length() >= 3 && basic.matches(".*[a-zA-Z].*")) {
             String suggestion = basic.length() > 60 ? basic.substring(0, 60).trim() : basic;
             System.err.println("⚠️ Greedy suggestion: [" + suggestion + "]");
             return suggestion;
        }

        return "";
    }
}
