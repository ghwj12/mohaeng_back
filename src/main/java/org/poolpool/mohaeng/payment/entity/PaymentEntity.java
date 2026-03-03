package org.poolpool.mohaeng.payment.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "payment")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "PAYMENT_ID")
    private Long paymentId;

    @Column(name = "USER_ID", nullable = false)
    private Long userId;

    @Column(name = "EVENT_ID", nullable = false)
    private Long eventId;

    @Column(name = "PCT_ID")
    private Long pctId;

    @Column(name = "PCT_BOOTH_ID")
    private Long pctBoothId;

    @Column(name = "PAY_TYPE", nullable = false, length = 20)
    @Builder.Default
    private String payType = "BOOTH";

    @Column(name = "PAY_METHOD", nullable = false, length = 30)
    private String payMethod;

    @Column(name = "AMOUNT_TOTAL", nullable = false)
    @Builder.Default
    private Integer amountTotal = 0;

    // ✅ 환불된 금액 (부분 환불 시 사용, 전액 환불이면 amountTotal과 동일)
    @Column(name = "CANCELED_AMOUNT")
    @Builder.Default
    private Integer canceledAmount = 0;

    @Column(name = "PAYMENT_STATUS", nullable = false, length = 30)
    @Builder.Default
    private String paymentStatus = "READY"; // READY, APPROVED, PARTIAL_CANCEL, CANCELLED

    @Column(name = "APPROVED_AT")
    private LocalDateTime approvedAt;

    @Column(name = "CANCELED_AT")
    private LocalDateTime canceledAt;

    @Column(name = "PAYMENT_NO", length = 200)
    private String paymentKey;
}
