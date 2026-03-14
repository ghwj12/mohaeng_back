package org.poolpool.mohaeng.ai.service;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import javax.imageio.ImageIO;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.poolpool.mohaeng.ai.client.AiAgentClient;
import org.poolpool.mohaeng.ai.dto.BizOcrResponse;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BizOcrService {

    private final AiAgentClient aiAgentClient;

    public BizOcrResponse extractBusinessInfo(MultipartFile file) throws Exception {
        byte[] imageBytes;
        String contentType = file.getContentType();

        if (contentType != null && contentType.equals("application/pdf")) {
            // PDF → JPEG
            try (PDDocument document = PDDocument.load(file.getInputStream())) {
                PDFRenderer renderer = new PDFRenderer(document);
                BufferedImage image = renderer.renderImageWithDPI(0, 600);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ImageIO.write(image, "jpg", baos);
                imageBytes = baos.toByteArray();
            }
        } else {
            // 이미지 → 무조건 JPEG로 변환 (PNG, BMP 등 포맷 문제 방지)
        	BufferedImage image = ImageIO.read(file.getInputStream());
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "jpg", baos);
            imageBytes = baos.toByteArray();
        }

        String base64Image = Base64.getEncoder().encodeToString(imageBytes);
        Map<String, String> request = new HashMap<>();
        request.put("imageBase64", base64Image);

        return aiAgentClient
                .post("/biz/signup/ocr", request, BizOcrResponse.class)
                .block();
    }
}
