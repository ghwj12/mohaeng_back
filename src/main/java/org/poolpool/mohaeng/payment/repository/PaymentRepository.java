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

    // ✅ 일반 행사 참가 취소 환불용
    Optional<PaymentEntity> findByPctId(Long pctId);

    // ✅ 부스 취소/반려 환불용
    Optional<PaymentEntity> findByPctBoothId(Long pctBoothId);

    // ✅ 문제 3 - 행사 참가 실제 수익 (APPROVED 결제만 합산)
    @Query("SELECT COALESCE(SUM(p.amountTotal), 0) FROM PaymentEntity p " +
           "WHERE p.eventId = :eventId AND p.payType = 'PCT' AND p.paymentStatus = 'APPROVED'")
    Long sumApprovedParticipantRevenue(@Param("eventId") Long eventId);

    // ✅ 문제 3 - 부스 실제 수익 (APPROVED 결제만 합산)
    @Query("SELECT COALESCE(SUM(p.amountTotal), 0) FROM PaymentEntity p " +
           "WHERE p.eventId = :eventId AND p.payType = 'BOOTH' AND p.paymentStatus = 'APPROVED'")
    Long sumApprovedBoothRevenue(@Param("eventId") Long eventId);
}
