package org.poolpool.mohaeng.event.participation.repository;

import java.util.List;
import java.util.Optional;

import org.poolpool.mohaeng.event.participation.entity.EventParticipationEntity;
import org.poolpool.mohaeng.event.participation.entity.ParticipationBoothEntity;
import org.poolpool.mohaeng.event.participation.entity.ParticipationBoothFacilityEntity;
import org.springframework.stereotype.Repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Repository
public class EventParticipationRepository {

    @PersistenceContext
    private EntityManager em;

    // ──────────────────────────────────────
    //   EventParticipationEntity (일반 참가)
    // ──────────────────────────────────────

    public Optional<EventParticipationEntity> findParticipationById(Long pctId) {
        return Optional.ofNullable(em.find(EventParticipationEntity.class, pctId));
    }

    public void saveParticipation(EventParticipationEntity pct) {
        if (pct.getPctId() == null) em.persist(pct);
        else em.merge(pct);
    }

    /**
     * ✅ 문제 4: 취소 상태 제외한 활성 신청 여부 확인 (결제대기도 이미 신청됨으로 처리)
     */
    public boolean existsActiveParticipation(Long userId, Long eventId) {
        Long count = em.createQuery(
                "SELECT COUNT(p) FROM EventParticipationEntity p " +
                "WHERE p.userId = :userId AND p.eventId = :eventId " +
                "AND p.pctStatus NOT IN ('취소')", Long.class)
                .setParameter("userId", userId)
                .setParameter("eventId", eventId)
                .getSingleResult();
        return count != null && count > 0;
    }

    /**
     * 유저의 행사 참여 내역 전체 조회 (마이페이지용)
     */
    public List<EventParticipationEntity> findParticipationsByUserId(Long userId) {
        return em.createQuery(
                "SELECT p FROM EventParticipationEntity p WHERE p.userId = :userId " +
                "ORDER BY p.pctId DESC",
                EventParticipationEntity.class)
                .setParameter("userId", userId)
                .getResultList();
    }

    // ──────────────────────────────────────
    //   ParticipationBoothEntity (부스 신청)
    // ──────────────────────────────────────

    public Optional<ParticipationBoothEntity> findBoothById(Long pctBoothId) {
        return Optional.ofNullable(em.find(ParticipationBoothEntity.class, pctBoothId));
    }

    public void saveBooth(ParticipationBoothEntity booth) {
        if (booth.getPctBoothId() == null) em.persist(booth);
        else em.merge(booth);
    }

    /**
     * 특정 행사의 모든 부스 신청 목록 (주최자용)
     * ✅ 문제 2: ParticipationBoothEntity에 eventId 컬럼이 없으므로
     *           host_booth 테이블 조인으로 event_id 필터
     */
    @SuppressWarnings("unchecked")
    public List<ParticipationBoothEntity> findBoothsByEventId(Long eventId) {
        return em.createNativeQuery(
                "SELECT pb.* FROM participation_booth pb " +
                "JOIN host_booth hb ON pb.host_booth_id = hb.booth_id " +
                "WHERE hb.event_id = :eventId " +
                "ORDER BY pb.pct_booth_id ASC",
                ParticipationBoothEntity.class)
                .setParameter("eventId", eventId)
                .getResultList();
    }

    /**
     * 유저의 부스 신청 목록 조회 (마이페이지용)
     */
    public List<ParticipationBoothEntity> findBoothsByUserId(Long userId) {
        return em.createQuery(
                "SELECT b FROM ParticipationBoothEntity b WHERE b.userId = :userId " +
                "ORDER BY b.pctBoothId DESC",
                ParticipationBoothEntity.class)
                .setParameter("userId", userId)
                .getResultList();
    }

    /**
     * 특정 행사에 유저가 이미 부스 신청했는지 확인 (취소/반려 제외)
     */
    public boolean existsActiveBoothParticipation(Long userId, Long eventId) {
        Long count = ((Number) em.createNativeQuery(
                "SELECT COUNT(*) FROM participation_booth pb " +
                "JOIN host_booth hb ON pb.host_booth_id = hb.booth_id " +
                "WHERE pb.user_id = :userId AND hb.event_id = :eventId " +
                "AND pb.status NOT IN ('취소', '반려')")
                .setParameter("userId", userId)
                .setParameter("eventId", eventId)
                .getSingleResult()).longValue();
        return count > 0;
    }

    // ──────────────────────────────────────
    //   ParticipationBoothFacilityEntity
    // ──────────────────────────────────────

    /**
     * 부스 신청에 포함된 부대시설 목록 (취소/반려 시 재고 복원용)
     */
    public List<ParticipationBoothFacilityEntity> findFacilitiesByPctBoothId(Long pctBoothId) {
        return em.createQuery(
                "SELECT f FROM ParticipationBoothFacilityEntity f " +
                "WHERE f.pctBoothId = :pctBoothId",
                ParticipationBoothFacilityEntity.class)
                .setParameter("pctBoothId", pctBoothId)
                .getResultList();
    }

    // ──────────────────────────────────────
    //   재고 복원 (문제 1 - 부스 반려/취소 시)
    // ──────────────────────────────────────

    /**
     * 부스 잔여 수량 +1
     * HostBoothEntity의 실제 PK 필드명이 다르면 맞춰서 수정 필요
     */
    public void increaseBoothRemainCount(Long hostBoothId) {
        em.createQuery(
                "UPDATE HostBoothEntity h " +
                "SET h.remainCount = h.remainCount + 1 " +
                "WHERE h.boothId = :boothId")
                .setParameter("boothId", hostBoothId)
                .executeUpdate();
    }

    /**
     * 부대시설 잔여 수량 복원
     * HostFacilityEntity의 실제 PK 필드명이 다르면 맞춰서 수정 필요
     */
    public void increaseFacilityRemainCount(Long hostBoothFaciId, Integer count) {
        em.createQuery(
                "UPDATE HostFacilityEntity h " +
                "SET h.remainCount = h.remainCount + :count " +
                "WHERE h.hostBoothfaciId = :faciId")
                .setParameter("faciId", hostBoothFaciId)
                .setParameter("count", count)
                .executeUpdate();
    }
}
