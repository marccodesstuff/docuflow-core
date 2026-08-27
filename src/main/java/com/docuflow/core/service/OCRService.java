package com.docuflow.core.service;

import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

@Service
public class OCRService {

    @Value("${docuflow.pipeline.ocr.tesseract.language:eng}")
    private String tesseractLanguage;

    @Value("${docuflow.pipeline.ocr.tesseract.dpi:300}")
    private int tesseractDpi;

    @Value("${storage.bucket:docuflow}")
    private String bucket;

    private final StorageService storageService;

    public OCRService(StorageService storageService) {
        this.storageService = storageService;
    }

    public List<OcrPageResult> processDocument(String storagePath, String mimeType) throws IOException {
        List<OcrPageResult> results = new ArrayList<>();
        
        if ("application/pdf".equals(mimeType)) {
            results.addAll(processPdf(storagePath));
        } else if (mimeType.startsWith("image/")) {
            results.add(processImage(storagePath));
        } else {
            throw new IllegalArgumentException("Unsupported MIME type: " + mimeType);
        }
        
        return results;
    }

    private List<OcrPageResult> processPdf(String storagePath) throws IOException {
        List<OcrPageResult> results = new ArrayList<>();
        
        // Download PDF from storage
        byte[] pdfBytes = storageService.download(storagePath);
        
        try (PDDocument document = Loader.loadPDF(pdfBytes)) {
            PDFRenderer renderer = new PDFRenderer(document);
            
            for (int pageIndex = 0; pageIndex < document.getNumberOfPages(); pageIndex++) {
                BufferedImage image = renderer.renderImageWithDPI(pageIndex, tesseractDpi);
                
                // Save rendered page image
                String pageStoragePath = storagePath.replace(".pdf", "") + "_page_" + (pageIndex + 1) + ".png";
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ImageIO.write(image, "PNG", baos);
                storageService.upload(pageStoragePath, baos.toByteArray(), "image/png");
                
                // Run OCR
                String text = runTesseract(image);
                
                // Detect elements (simplified - would use layout analysis in production)
                String elementsJson = detectElements(image, text);
                
                results.add(new OcrPageResult(pageStoragePath, image.getWidth(), image.getHeight(), text, elementsJson));
            }
        }
        
        return results;
    }

    private OcrPageResult processImage(String storagePath) throws IOException {
        byte[] imageBytes = storageService.download(storagePath);
        BufferedImage image = ImageIO.read(new java.io.ByteArrayInputStream(imageBytes));
        
        String text = runTesseract(image);
        String elementsJson = detectElements(image, text);
        
        return new OcrPageResult(storagePath, image.getWidth(), image.getHeight(), text, elementsJson);
    }

    private String runTesseract(BufferedImage image) {
        ITesseract tesseract = new Tesseract();
        tesseract.setLanguage(tesseractLanguage);
        tesseract.setDatapath(System.getenv("TESSDATA_PREFIX"));
        
        try {
            return tesseract.doOCR(image);
        } catch (TesseractException e) {
            throw new RuntimeException("Tesseract OCR failed", e);
        }
    }

    private String detectElements(BufferedImage image, String text) {
        // Simplified element detection - in production would use layout analysis model
        List<BoundingBoxElement> elements = new ArrayList<>();
        
        // Split text into lines and create bounding boxes
        String[] lines = text.split("\n");
        int lineHeight = image.getHeight() / Math.max(lines.length, 1);
        
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            if (!line.isEmpty()) {
                elements.add(new BoundingBoxElement(
                    0.0, (double) (i * lineHeight),
                    (double) image.getWidth(), (double) lineHeight,
                    "text", 0.9f
                ));
            }
        }
        
        return toJson(elements);
    }

    private String toJson(List<BoundingBoxElement> elements) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < elements.size(); i++) {
            BoundingBoxElement e = elements.get(i);
            sb.append(String.format("{\"x\":%.1f,\"y\":%.1f,\"width\":%.1f,\"height\":%.1f,\"element_type\":\"%s\",\"confidence\":%.2f}",
                e.x, e.y, e.width, e.height, e.elementType, e.confidence));
            if (i < elements.size() - 1) sb.append(",");
        }
        sb.append("]");
        return sb.toString();
    }

    public record OcrPageResult(String imagePath, int width, int height, String text, String elementsJson) {}
    
    private record BoundingBoxElement(double x, double y, double width, double height, String elementType, float confidence) {}
}