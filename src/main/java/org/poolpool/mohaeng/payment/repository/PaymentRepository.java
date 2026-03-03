package org.poolpool.mohaeng.payment.repository;

import java.util.Optional;

import org.poolpool.mohaeng.payment.entity.PaymentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface PaymentRepository extends JpaRepository<PaymentEntity, Long> {

    // 토스 paymentKey(승인 후 실제 키) 로 조회
    Optional<PaymentEntity> findByPaymentKey(String paymentKey);

    // ✅ orderId(=PAYMENT_NO 컬럼) 로 조회 - 결제 성공 후 참여 신청 시 사용
    @Query("SELECT p FROM PaymentEntity p WHERE p.paymentKey = :orderId")
    Optional<PaymentEntity> findByOrderId(@Param("orderId") String orderId);

    // ✅ 일반 행사 참가 취소 환불용
    Optional<PaymentEntity> findByPctId(Long pctId);

    // ✅ 부스 취소/반려 환불용
    Optional<PaymentEntity> findByPctBoothId(Long pctBoothId);

    // ✅ 행사 참가 실제 수익 (결제금액 - 환불금액, 부분환불 포함)
    @Query("SELECT COALESCE(SUM(p.amountTotal - COALESCE(p.canceledAmount, 0)), 0) FROM PaymentEntity p " +
           "WHERE p.eventId = :eventId AND p.payType = 'PCT' " +
           "AND p.paymentStatus IN ('APPROVED', 'PARTIAL_CANCEL')")
    Long sumApprovedParticipantRevenue(@Param("eventId") Long eventId);

    // ✅ 부스 실제 수익 (결제금액 - 환불금액, 부분환불 포함)
    @Query("SELECT COALESCE(SUM(p.amountTotal - COALESCE(p.canceledAmount, 0)), 0) FROM PaymentEntity p " +
           "WHERE p.eventId = :eventId AND p.payType = 'BOOTH' " +
           "AND p.paymentStatus IN ('APPROVED', 'PARTIAL_CANCEL')")
    Long sumApprovedBoothRevenue(@Param("eventId") Long eventId);
}
