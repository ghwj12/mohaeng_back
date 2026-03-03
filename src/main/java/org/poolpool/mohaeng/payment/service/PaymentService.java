package org.poolpool.mohaeng.payment.service;

import org.poolpool.mohaeng.payment.dto.PaymentDto;

public interface PaymentService {

    /** 결제 준비 (orderId 생성, READY 상태 저장) */
    PaymentDto.PrepareResponse prepare(Long userId, PaymentDto.PrepareRequest req);

    /**
     * 결제 승인 (Toss confirm API 호출)
     * ✅ 문제 4: 이미 취소된 결제 재확인 방지
     */
    PaymentDto.ConfirmResponse confirm(Long userId, PaymentDto.ConfirmRequest req);

    /**
     * 환불 (Toss cancel API 호출)
     * ✅ 문제 7: 일반 행사 참여 취소 환불에도 사용
     */
    void cancelPayment(String tossPaymentKey, int cancelAmount, String cancelReason);
}
