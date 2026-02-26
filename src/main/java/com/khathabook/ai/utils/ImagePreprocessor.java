package com.khathabook.ai.utils;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.nio.FloatBuffer;

public class ImagePreprocessor {

    public static FloatBuffer preprocessFromBytes(byte[] imageBytes) throws Exception {
        try (java.io.ByteArrayInputStream bis = new java.io.ByteArrayInputStream(imageBytes)) {
            return preprocess(bis);
        }
    }

    public static FloatBuffer preprocess(InputStream imageStream) throws Exception {
        BufferedImage image = ImageIO.read(imageStream);
        if (image == null) {
            System.err.println("❌ AI PREPROCESS FAIL: ImageIO.read() returned NULL (WebP/AVIF unsupported?)");
            throw new RuntimeException("Could not read image");
        }
        // ... (rest same)

        BufferedImage resized =
                new BufferedImage(224, 224, BufferedImage.TYPE_3BYTE_BGR);

        Graphics2D g = resized.createGraphics();
        g.drawImage(image, 0, 0, 224, 224, null);
        g.dispose();

        FloatBuffer buffer = FloatBuffer.allocate(3 * 224 * 224);

        float[] red = new float[224 * 224];
        float[] green = new float[224 * 224];
        float[] blue = new float[224 * 224];

        for (int y = 0; y < 224; y++) {
            for (int x = 0; x < 224; x++) {
                int rgb = resized.getRGB(x, y);
                int idx = y * 224 + x;
                red[idx] = ((rgb >> 16) & 0xFF) / 255f;
                green[idx] = ((rgb >> 8) & 0xFF) / 255f;
                blue[idx] = (rgb & 0xFF) / 255f;
            }
        }

        buffer.put(red);
        buffer.put(green);
        buffer.put(blue);

        buffer.rewind();
        return buffer;
    }
}
