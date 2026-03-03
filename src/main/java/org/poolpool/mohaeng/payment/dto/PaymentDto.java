package org.poolpool.mohaeng.payment.dto;

import lombok.*;
import java.time.LocalDateTime;

public class PaymentDto {

    // ─── 결제 준비 요청 ───
    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class PrepareRequest {
        private Long eventId;
        private Integer amount;
        private String orderName;
    }

    // ─── 결제 준비 응답 ───
    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class PrepareResponse {
        private String orderId;
        private String orderName;
        private Integer amount;
        private String clientKey;
    }

    // ─── 결제 승인 요청 ───
    @Getter @Setter @NoArgsConstructor @AllArgsConstructor
    public static class ConfirmRequest {
        private String paymentKey;
        private String orderId;
        private Integer amount;
    }

    // ─── 결제 승인 응답 ───
    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class ConfirmResponse {
        private Long paymentId;
        private String paymentNo;
        private String paymentStatus;
        private Integer amountTotal;
        private LocalDateTime approvedAt;
        private String orderName;
    }
}
