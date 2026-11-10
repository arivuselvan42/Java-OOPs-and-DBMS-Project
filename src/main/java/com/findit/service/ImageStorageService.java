package com.findit.service;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.UUID;

/**
 * Service for securely uploading, validating, scaling, and retrieving item images.
 * Enforces file size limits (<= 5MB) and true image format content validation.
 */
public class ImageStorageService {

    public static final long MAX_FILE_SIZE_BYTES = 5 * 1024 * 1024; // 5 MB
    public static final String UPLOADS_DIR = "uploads";

    private static final java.util.Map<String, ImageIcon> THUMBNAIL_CACHE = new java.util.concurrent.ConcurrentHashMap<>();

    /**
     * Validates and saves an uploaded image file into the local uploads directory.
     * Returns the relative path to be saved in MySQL.
     */
    public static String saveImage(File sourceFile) throws IOException {
        if (sourceFile == null || !sourceFile.exists()) {
            throw new IllegalArgumentException("Selected file does not exist.");
        }

        if (sourceFile.length() > MAX_FILE_SIZE_BYTES) {
            throw new IllegalArgumentException("Image file size exceeds the 5 MB limit.");
        }

        String name = sourceFile.getName();
        String ext = getFileExtension(name).toLowerCase(Locale.ROOT);
        if (!ext.equals("jpg") && !ext.equals("jpeg") && !ext.equals("png") && !ext.equals("webp")) {
            throw new IllegalArgumentException("Unsupported image format. Allowed formats: JPG, PNG, WEBP.");
        }

        // Validate actual image content (prevent spoofed extensions)
        BufferedImage bufferedImage;
        try {
            bufferedImage = ImageIO.read(sourceFile);
            if (bufferedImage == null) {
                throw new IllegalArgumentException("File content is not a valid or readable image.");
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to decode image file: " + e.getMessage());
        }

        File uploadDir = new File(UPLOADS_DIR);
        if (!uploadDir.exists() && !uploadDir.mkdirs()) {
            throw new IOException("Failed to create uploads directory on disk.");
        }

        String uniqueFileName = UUID.randomUUID().toString() + "." + ext;
        File targetFile = new File(uploadDir, uniqueFileName);

        Files.copy(sourceFile.toPath(), targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

        return UPLOADS_DIR + "/" + uniqueFileName;
    }

    /**
     * Loads and scales an image smoothly into an ImageIcon.
     * Returns null if path is invalid or missing.
     */
    public static ImageIcon loadScaledIcon(String relativePath, int maxWidth, int maxHeight) {
        if (relativePath == null || relativePath.isBlank()) {
            return null;
        }

        File file = new File(relativePath);
        if (!file.exists() || !file.isFile()) {
            return null;
        }

        try {
            BufferedImage original = ImageIO.read(file);
            if (original == null) {
                return null;
            }

            int origW = original.getWidth();
            int origH = original.getHeight();

            // Calculate scaled dimensions preserving aspect ratio
            double scale = Math.min((double) maxWidth / origW, (double) maxHeight / origH);
            int targetW = (int) Math.round(origW * scale);
            int targetH = (int) Math.round(origH * scale);

            Image scaled = original.getScaledInstance(Math.max(1, targetW), Math.max(1, targetH), Image.SCALE_SMOOTH);
            return new ImageIcon(scaled);
        } catch (Exception ignored) {
            return null;
        }
    }

    /**
     * Generates or retrieves a high-quality cached rounded thumbnail for item images.
     */
    public static ImageIcon getRoundedThumbnail(String relativePath, int size, int cornerRadius) {
        if (relativePath == null || relativePath.isBlank()) {
            return null;
        }
        String cacheKey = relativePath + "_" + size + "_" + cornerRadius;
        ImageIcon cached = THUMBNAIL_CACHE.get(cacheKey);
        if (cached != null) {
            return cached;
        }

        File file = new File(relativePath);
        if (!file.exists() || !file.isFile()) {
            return null;
        }

        try {
            BufferedImage orig = ImageIO.read(file);
            if (orig == null) return null;

            int w = orig.getWidth();
            int h = orig.getHeight();
            int minDim = Math.min(w, h);
            int x = (w - minDim) / 2;
            int y = (h - minDim) / 2;
            BufferedImage square = orig.getSubimage(x, y, minDim, minDim);

            BufferedImage thumb = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2 = thumb.createGraphics();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

            g2.setClip(new java.awt.geom.RoundRectangle2D.Float(0, 0, size, size, cornerRadius, cornerRadius));
            g2.drawImage(square, 0, 0, size, size, null);
            g2.setClip(null);

            g2.setColor(new Color(0xCB, 0xD5, 0xE1));
            g2.setStroke(new BasicStroke(1.2f));
            g2.draw(new java.awt.geom.RoundRectangle2D.Float(0.5f, 0.5f, size - 1f, size - 1f, cornerRadius, cornerRadius));
            g2.dispose();

            ImageIcon icon = new ImageIcon(thumb);
            THUMBNAIL_CACHE.put(cacheKey, icon);
            return icon;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Generates or retrieves a circular avatar image thumbnail (for user/reporter).
     */
    public static ImageIcon getCircularAvatarThumbnail(String imagePath, String initial, int size) {
        String cacheKey = (imagePath != null ? imagePath : "initial_" + initial) + "_circle_" + size;
        ImageIcon cached = THUMBNAIL_CACHE.get(cacheKey);
        if (cached != null) {
            return cached;
        }

        BufferedImage avatar = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = avatar.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

        boolean drawnImage = false;
        if (imagePath != null && !imagePath.isBlank()) {
            File f = new File(imagePath);
            if (f.exists() && f.isFile()) {
                try {
                    BufferedImage orig = ImageIO.read(f);
                    if (orig != null) {
                        int w = orig.getWidth();
                        int h = orig.getHeight();
                        int minDim = Math.min(w, h);
                        int x = (w - minDim) / 2;
                        int y = (h - minDim) / 2;
                        BufferedImage square = orig.getSubimage(x, y, minDim, minDim);

                        g2.setClip(new java.awt.geom.Ellipse2D.Float(1, 1, size - 2, size - 2));
                        g2.drawImage(square, 1, 1, size - 2, size - 2, null);
                        g2.setClip(null);

                        g2.setColor(new Color(0x38, 0xBD, 0xF8)); // Crisp cyan ring
                        g2.setStroke(new BasicStroke(1.5f));
                        g2.draw(new java.awt.geom.Ellipse2D.Float(1, 1, size - 2, size - 2));
                        drawnImage = true;
                    }
                } catch (Exception ignored) {}
            }
        }

        if (!drawnImage) {
            g2.setPaint(new GradientPaint(0, 0, new Color(0x4F, 0x46, 0xE5), size, size, new Color(0x06, 0xB6, 0xD4)));
            g2.fill(new java.awt.geom.Ellipse2D.Float(0, 0, size, size));

            String text = (initial != null && !initial.isBlank()) ? initial.substring(0, 1).toUpperCase() : "U";
            g2.setColor(Color.WHITE);
            g2.setFont(new Font("Segoe UI", Font.BOLD, (int) (size * 0.48)));
            FontMetrics fm = g2.getFontMetrics();
            int tx = (size - fm.stringWidth(text)) / 2;
            int ty = ((size - fm.getHeight()) / 2) + fm.getAscent();
            g2.drawString(text, tx, ty);
        }

        g2.dispose();
        ImageIcon icon = new ImageIcon(avatar);
        THUMBNAIL_CACHE.put(cacheKey, icon);
        return icon;
    }

    /**
     * Clears cached thumbnails.
     */
    public static void clearThumbnailCache() {
        THUMBNAIL_CACHE.clear();
    }

    /**
     * Safely deletes an uploaded image file if present.
     */
    public static void deleteImage(String relativePath) {
        if (relativePath != null && !relativePath.isBlank()) {
            try {
                File file = new File(relativePath);
                if (file.exists() && file.isFile()) {
                    file.delete();
                }
            } catch (Exception ignored) {
            }
        }
    }

    private static String getFileExtension(String filename) {
        int dot = filename.lastIndexOf('.');
        return (dot > 0 && dot < filename.length() - 1) ? filename.substring(dot + 1) : "";
    }
}

