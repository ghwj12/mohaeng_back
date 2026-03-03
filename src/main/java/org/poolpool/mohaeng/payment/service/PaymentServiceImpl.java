package org.poolpool.mohaeng.payment.service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.UUID;

import org.poolpool.mohaeng.event.participation.repository.EventParticipationRepository;
import org.poolpool.mohaeng.payment.dto.PaymentDto;
import org.poolpool.mohaeng.payment.entity.PaymentEntity;
import org.poolpool.mohaeng.payment.repository.PaymentRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final EventParticipationRepository participationRepository;
    private final ObjectMapper objectMapper;

    @Value("${toss.payments.client-key}")
    private String clientKey;

    @Value("${toss.payments.secret-key}")
    private String secretKey;

    private static final String TOSS_CONFIRM_URL = "https://api.tosspayments.com/v1/payments/confirm";
    private static final String TOSS_CANCEL_URL  = "https://api.tosspayments.com/v1/payments/%s/cancel";

    // ─── 결제 준비 ───
    @Override
    @Transactional
    public PaymentDto.PrepareResponse prepare(Long userId, PaymentDto.PrepareRequest req) {
        boolean isBooth = req.getPctBoothId() != null;
        String payType  = isBooth ? "BOOTH" : "PCT";
        String prefix   = isBooth ? "BOOTH-" + req.getPctBoothId() : "PCT-" + req.getPctId();
        String orderId  = prefix + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        PaymentEntity payment = PaymentEntity.builder()
                .userId(userId)
                .eventId(req.getEventId())
                .pctBoothId(req.getPctBoothId())
                .pctId(req.getPctId())
                .payType(payType)
                .paymentKey(orderId)
                .payMethod("UNKNOWN")
                .amountTotal(req.getAmount())
                .paymentStatus("READY")
                .build();

        paymentRepository.save(payment);
        log.info("[결제 준비] orderId={}, userId={}, payType={}, amount={}", orderId, userId, payType, req.getAmount());

        return PaymentDto.PrepareResponse.builder()
                .orderId(orderId)
                .orderName(req.getOrderName())
                .amount(req.getAmount())
                .clientKey(clientKey)
                .build();
    }

    // ─── 결제 승인 ───
    @Override
    @Transactional
    public PaymentDto.ConfirmResponse confirm(Long userId, PaymentDto.ConfirmRequest req) {

        PaymentEntity payment = paymentRepository.findByPaymentKey(req.getOrderId())
                .orElseThrow(() -> new IllegalArgumentException("결제 정보를 찾을 수 없습니다."));

        // ✅ 문제 4: 이미 취소된 결제 재확인 방지 (결제 취소 후 브라우저 뒤로가기 등으로 재진입 시)
        if ("CANCELLED".equals(payment.getPaymentStatus())) {
            throw new IllegalStateException("이미 취소된 결제입니다. 다시 신청해주세요.");
        }
        if ("APPROVED".equals(payment.getPaymentStatus())) {
            // 멱등성 처리: 이미 승인된 경우 에러 없이 기존 결과 반환
            log.warn("[결제 중복 승인 시도 무시] orderId={}", req.getOrderId());
            return PaymentDto.ConfirmResponse.builder()
                    .paymentId(payment.getPaymentId())
                    .paymentNo(payment.getPaymentKey())
                    .paymentStatus(payment.getPaymentStatus())
                    .amountTotal(payment.getAmountTotal())
                    .approvedAt(payment.getApprovedAt())
                    .orderName("")
                    .build();
        }

        // ✅ 문제 4: 참가 신청이 이미 '취소' 상태이면 결제 진행 중단
        if (payment.getPctId() != null) {
            participationRepository.findParticipationById(payment.getPctId()).ifPresent(pct -> {
                if ("취소".equals(pct.getPctStatus())) {
                    throw new IllegalStateException("이미 취소된 참가 신청입니다. 다시 신청해주세요.");
                }
            });
        }
        if (payment.getPctBoothId() != null) {
            participationRepository.findBoothById(payment.getPctBoothId()).ifPresent(booth -> {
                if ("취소".equals(booth.getStatus())) {
                    throw new IllegalStateException("이미 취소된 부스 신청입니다. 다시 신청해주세요.");
                }
            });
        }

        if (!payment.getAmountTotal().equals(req.getAmount())) {
            throw new IllegalArgumentException("결제 금액이 일치하지 않습니다.");
        }

        TossConfirmResult result = callTossConfirmAPI(req.getPaymentKey(), req.getOrderId(), req.getAmount());

        // ✅ 결제 승인 후 실제 토스 paymentKey로 업데이트 (환불 시 이 키 사용)
        payment.setPaymentKey(req.getPaymentKey());
        payment.setPayMethod(result.method);
        payment.setPaymentStatus("APPROVED");
        payment.setApprovedAt(LocalDateTime.now());
        paymentRepository.save(payment);

        // 부스 참여 상태 → 결제완료
        if (payment.getPctBoothId() != null) {
            participationRepository.findBoothById(payment.getPctBoothId())
                    .ifPresent(booth -> {
                        booth.setStatus("결제완료");
                        participationRepository.saveBooth(booth);
                    });
        }

        // 일반 행사 참여 상태 → 결제완료
        if (payment.getPctId() != null) {
            participationRepository.findParticipationById(payment.getPctId())
                    .ifPresent(pct -> {
                        pct.setPctStatus("결제완료");
                        participationRepository.saveParticipation(pct);
                    });
        }

        log.info("[결제 승인 완료] orderId={}, paymentKey={}, payType={}", req.getOrderId(), req.getPaymentKey(), payment.getPayType());

        return PaymentDto.ConfirmResponse.builder()
                .paymentId(payment.getPaymentId())
                .paymentNo(payment.getPaymentKey())
                .paymentStatus(payment.getPaymentStatus())
                .amountTotal(payment.getAmountTotal())
                .approvedAt(payment.getApprovedAt())
                .orderName(result.orderName)
                .build();
    }

    // ─── 토스 환불 API 호출 ───
    @Override
    @Transactional
    public void cancelPayment(String tossPaymentKey, int cancelAmount, String cancelReason) {
        try {
            String encoded = Base64.getEncoder()
                    .encodeToString((secretKey + ":").getBytes(StandardCharsets.UTF_8));

            String body = String.format(
                    "{\"cancelReason\":\"%s\",\"cancelAmount\":%d}",
                    cancelReason, cancelAmount);

            String url = String.format(TOSS_CANCEL_URL, tossPaymentKey);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Authorization", "Basic " + encoded)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response = HttpClient.newHttpClient()
                    .send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                JsonNode errorNode = objectMapper.readTree(response.body());
                String errorMsg = errorNode.path("message").asText("환불 실패");
                log.error("[토스 환불 실패] paymentKey={}, status={}, msg={}", tossPaymentKey, response.statusCode(), errorMsg);
                throw new RuntimeException("토스 환불 실패: " + errorMsg);
            }

            // DB 상태 업데이트
            paymentRepository.findByPaymentKey(tossPaymentKey).ifPresent(p -> {
                p.setPaymentStatus("CANCELLED");
                p.setCanceledAt(LocalDateTime.now());
                paymentRepository.save(p);
            });

            log.info("[토스 환불 완료] paymentKey={}, cancelAmount={}원, reason={}", tossPaymentKey, cancelAmount, cancelReason);

        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("토스 환불 API 호출 중 오류 발생", e);
        }
    }

    // ─── 토스 결제 승인 API 호출 ───
    private TossConfirmResult callTossConfirmAPI(String paymentKey, String orderId, Integer amount) {
        try {
            String encoded = Base64.getEncoder()
                    .encodeToString((secretKey + ":").getBytes(StandardCharsets.UTF_8));

            String body = String.format(
                    "{\"paymentKey\":\"%s\",\"orderId\":\"%s\",\"amount\":%d}",
                    paymentKey, orderId, amount);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(TOSS_CONFIRM_URL))
                    .header("Authorization", "Basic " + encoded)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response = HttpClient.newHttpClient()
                    .send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                JsonNode errorNode = objectMapper.readTree(response.body());
                String errorMsg = errorNode.path("message").asText("결제 승인 실패");
                throw new RuntimeException("토스 결제 승인 실패: " + errorMsg);
            }

            JsonNode json = objectMapper.readTree(response.body());
            return new TossConfirmResult(
                    json.path("method").asText("UNKNOWN"),
                    json.path("orderName").asText(""));

        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("토스 API 호출 중 오류 발생", e);
        }
    }

    private record TossConfirmResult(String method, String orderName) {}
}
