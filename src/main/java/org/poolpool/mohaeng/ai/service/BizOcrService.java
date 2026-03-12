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

        // PDF 파일이면 이미지로 변환
        if (contentType != null && contentType.equals("application/pdf")) {

            try (PDDocument document = PDDocument.load(file.getInputStream())) {

                PDFRenderer renderer = new PDFRenderer(document);

                // 첫 페이지를 이미지로 변환
                BufferedImage image = renderer.renderImageWithDPI(0, 300);

                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ImageIO.write(image, "jpg", baos);

                imageBytes = baos.toByteArray();
            }

        } else {
            // 이미지 파일이면 그대로 사용
            imageBytes = file.getBytes();
        }

        // Base64 변환
        String base64Image = Base64.getEncoder().encodeToString(imageBytes);

        Map<String, String> request = new HashMap<>();
        request.put("imageBase64", base64Image);
    	
        // FastAPI 호출
        return aiAgentClient
                .post("/biz/signup/ocr", request, BizOcrResponse.class)
                .block();
    }
}
