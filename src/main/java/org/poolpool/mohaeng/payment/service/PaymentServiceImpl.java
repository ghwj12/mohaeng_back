package org.poolpool.mohaeng.payment.service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.UUID;

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
    private final ObjectMapper objectMapper;

    @Value("${toss.payments.client-key}")
    private String clientKey;

    @Value("${toss.payments.secret-key}")
    private String secretKey;

    private static final String TOSS_CONFIRM_URL = "https://api.tosspayments.com/v1/payments/confirm";
    private static final String TOSS_CANCEL_URL  = "https://api.tosspayments.com/v1/payments/%s/cancel";

    // ─── 결제 준비 ───────────────────────────────────────────────────────────
    // ✅ 새 플로우: pctId/pctBoothId 없이 orderId만 발급
    // 참여 레코드는 결제 성공 후 submitBoothApply/submitParticipation에서 생성됨
    @Override
    @Transactional
    public PaymentDto.PrepareResponse prepare(Long userId, PaymentDto.PrepareRequest req) {
        String orderId = "PAY-" + UUID.randomUUID().toString().substring(0, 16).toUpperCase();

        PaymentEntity payment = PaymentEntity.builder()
                .userId(userId)
                .eventId(req.getEventId())
                .payType(req.getOrderName() != null && req.getOrderName().contains("부스") ? "BOOTH" : "PCT")
                .paymentKey(orderId)   // confirm 시 실제 toss paymentKey로 교체됨
                .payMethod("UNKNOWN")
                .amountTotal(req.getAmount())
                .paymentStatus("READY")
                .build();

        paymentRepository.save(payment);
        log.info("[결제 준비] orderId={}, userId={}, amount={}", orderId, userId, req.getAmount());

        return PaymentDto.PrepareResponse.builder()
                .orderId(orderId)
                .orderName(req.getOrderName())
                .amount(req.getAmount())
                .clientKey(clientKey)
                .build();
    }

    // ─── 결제 승인 ───────────────────────────────────────────────────────────
    // ✅ 새 플로우: Toss API 승인 후 APPROVED 저장만 함
    // 참여 레코드 생성은 프론트에서 이후 submitBoothApply/submitParticipation 호출
    @Override
    @Transactional
    public PaymentDto.ConfirmResponse confirm(Long userId, PaymentDto.ConfirmRequest req) {

        PaymentEntity payment = paymentRepository.findByPaymentKey(req.getOrderId())
                .orElseThrow(() -> new IllegalArgumentException("결제 정보를 찾을 수 없습니다."));

        if ("CANCELLED".equals(payment.getPaymentStatus())) {
            throw new IllegalStateException("이미 취소된 결제입니다. 다시 신청해주세요.");
        }
        if ("APPROVED".equals(payment.getPaymentStatus())) {
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

        if (!payment.getAmountTotal().equals(req.getAmount())) {
            throw new IllegalArgumentException("결제 금액이 일치하지 않습니다.");
        }

        // Toss API 호출
        TossConfirmResult result = callTossConfirmAPI(req.getPaymentKey(), req.getOrderId(), req.getAmount());

        // ✅ 실제 토스 paymentKey로 교체 (환불 시 사용)
        payment.setPaymentKey(req.getPaymentKey());
        payment.setPayMethod(result.method);
        payment.setPaymentStatus("APPROVED");
        payment.setApprovedAt(LocalDateTime.now());
        paymentRepository.save(payment);

        log.info("[결제 승인 완료] orderId={}, paymentKey={}", req.getOrderId(), req.getPaymentKey());

        return PaymentDto.ConfirmResponse.builder()
                .paymentId(payment.getPaymentId())
                .paymentNo(payment.getPaymentKey())
                .paymentStatus(payment.getPaymentStatus())
                .amountTotal(payment.getAmountTotal())
                .approvedAt(payment.getApprovedAt())
                .orderName(result.orderName)
                .build();
    }

    // ─── 토스 환불 API 호출 ──────────────────────────────────────────────────
    @Override
    @Transactional
    public void cancelPayment(String tossPaymentKey, int cancelAmount, String cancelReason) {
        try {
            String encoded = Base64.getEncoder()
                    .encodeToString((secretKey + ":").getBytes(StandardCharsets.UTF_8));

            String body = String.format(
                    "{\"cancelReason\":\"%s\",\"cancelAmount\":%d}", cancelReason, cancelAmount);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(String.format(TOSS_CANCEL_URL, tossPaymentKey)))
                    .header("Authorization", "Basic " + encoded)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response = HttpClient.newHttpClient()
                    .send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                JsonNode err = objectMapper.readTree(response.body());
                throw new RuntimeException("토스 환불 실패: " + err.path("message").asText("환불 실패"));
            }

            // ✅ 부분/전액 환불 구분 처리
            paymentRepository.findByPaymentKey(tossPaymentKey).ifPresent(p -> {
                int total        = p.getAmountTotal() == null ? 0 : p.getAmountTotal();
                int prevCanceled = p.getCanceledAmount() == null ? 0 : p.getCanceledAmount();
                int newCanceled  = prevCanceled + cancelAmount;

                p.setCanceledAmount(newCanceled);
                p.setCanceledAt(LocalDateTime.now());

                if (newCanceled >= total) {
                    p.setPaymentStatus("CANCELLED");       // 전액 환불
                } else {
                    p.setPaymentStatus("PARTIAL_CANCEL");  // 부분 환불
                }
                paymentRepository.save(p);
            });

            log.info("[토스 환불 완료] paymentKey={}, cancelAmount={}원", tossPaymentKey, cancelAmount);

        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("토스 환불 API 호출 중 오류 발생", e);
        }
    }

    // ─── 토스 결제 승인 API 호출 ─────────────────────────────────────────────
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
                JsonNode err = objectMapper.readTree(response.body());
                throw new RuntimeException("토스 결제 승인 실패: " + err.path("message").asText("승인 실패"));
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
