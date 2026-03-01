package org.poolpool.mohaeng.event.participation.repository;

import java.util.List;
import java.util.Optional;

import org.poolpool.mohaeng.event.participation.entity.EventParticipationEntity;
import org.poolpool.mohaeng.event.participation.entity.ParticipationBoothEntity;
import org.poolpool.mohaeng.event.participation.entity.ParticipationBoothFacilityEntity;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Repository
public class EventParticipationRepository {

    @PersistenceContext
    private EntityManager em;

    // =========================
    // EVENT_PARTICIPATION
    // =========================

    public Optional<EventParticipationEntity> findParticipationById(Long pctId) {
        return Optional.ofNullable(em.find(EventParticipationEntity.class, pctId));
    }

    public List<EventParticipationEntity> findParticipationByUserId(Long userId) {
        return em.createQuery(
                "select p from EventParticipationEntity p " +
                "where p.userId = :userId " +
                "and p.pctStatus not in ('취소','참여삭제') " +
                "order by p.pctDate desc",
                EventParticipationEntity.class)
                .setParameter("userId", userId)
                .getResultList();
    }

    // ✅ Issue 6: 행사 참여 중복 체크
    // EventParticipationEntity에 eventId, userId, pctStatus 직접 컬럼으로 있음 → JPQL 사용
    public boolean existsActiveParticipation(Long userId, Long eventId) {
        Long count = em.createQuery(
                "select count(p) from EventParticipationEntity p " +
                "where p.userId = :userId " +
                "and p.eventId = :eventId " +
                "and p.pctStatus in ('결제완료', '결제대기', '참여확정')",
                Long.class)
                .setParameter("userId", userId)
                .setParameter("eventId", eventId)
                .getSingleResult();
        return count > 0;
    }

    public EventParticipationEntity saveParticipation(EventParticipationEntity entity) {
        if (entity.getPctId() == null) {
            em.persist(entity);
            return entity;
        }
        return em.merge(entity);
    }

    // =========================
    // PARTICIPATION_BOOTH
    // =========================

    public Optional<ParticipationBoothEntity> findBoothById(Long pctBoothId) {
        return Optional.ofNullable(em.find(ParticipationBoothEntity.class, pctBoothId));
    }

    public List<ParticipationBoothEntity> findBoothByUserId(Long userId) {
        return em.createQuery(
                "select b from ParticipationBoothEntity b " +
                "where b.userId = :userId " +
                "order by b.createdAt desc",
                ParticipationBoothEntity.class)
                .setParameter("userId", userId)
                .getResultList();
    }

    // ✅ Issue 6: 부스 중복 체크
    // ParticipationBoothEntity에 eventId 없음 → HOST_BOOTH JOIN으로 확인
    // 상태 컬럼명: STATUS (pctBoothStatus 아님)
    public boolean existsActiveBooth(Long userId, Long eventId) {
        Number count = (Number) em.createNativeQuery(
                "SELECT COUNT(*) FROM PARTICIPATION_BOOTH pb " +
                "JOIN HOST_BOOTH hb ON pb.HOST_BOOTH_ID = hb.BOOTH_ID " +
                "WHERE pb.USER_ID = :userId " +
                "AND hb.EVENT_ID = :eventId " +
                "AND pb.STATUS NOT IN ('취소', '부스삭제', '반려')")
                .setParameter("userId", userId)
                .setParameter("eventId", eventId)
                .getSingleResult();
        return count.longValue() > 0;
    }

    public ParticipationBoothEntity saveBooth(ParticipationBoothEntity entity) {
        if (entity.getPctBoothId() == null) {
            em.persist(entity);
            return entity;
        }
        return em.merge(entity);
    }

    @Transactional
    public void decreaseBoothRemainCount(Long hostBoothId) {
        em.createQuery(
            "UPDATE HostBoothEntity h SET h.remainCount = h.remainCount - 1 " +
            "WHERE h.boothId = :boothId AND h.remainCount > 0")
          .setParameter("boothId", hostBoothId)
          .executeUpdate();
    }

    // =========================
    // PARTICIPATION_BOOTH_FACILITY
    // =========================

    public List<ParticipationBoothFacilityEntity> findFacilitiesByPctBoothId(Long pctBoothId) {
        return em.createQuery(
                "select f from ParticipationBoothFacilityEntity f " +
                "where f.pctBoothId = :pctBoothId",
                ParticipationBoothFacilityEntity.class)
                .setParameter("pctBoothId", pctBoothId)
                .getResultList();
    }

    @Transactional
    public void decreaseFacilityRemainCount(Long hostBoothFaciId, int count) {
        em.createQuery(
            "UPDATE HostFacilityEntity h SET h.remainCount = h.remainCount - :count " +
            "WHERE h.hostBoothfaciId = :faciId AND h.remainCount >= :count")
          .setParameter("faciId", hostBoothFaciId)
          .setParameter("count", count)
          .executeUpdate();
    }

    // =========================
    // HOST_BOOTH → EVENT 검증용
    // =========================
    public Optional<Long> findEventIdByHostBoothId(Long hostBoothId) {
        Object value = em.createNativeQuery(
                "SELECT EVENT_ID FROM HOST_BOOTH WHERE BOOTH_ID = ?")
                .setParameter(1, hostBoothId)
                .getResultStream()
                .findFirst()
                .orElse(null);
        if (value == null) return Optional.empty();
        return Optional.of(((Number) value).longValue());
    }

    public void deleteFacilitiesByPctBoothId(Long pctBoothId) {
        em.createQuery(
                "delete from ParticipationBoothFacilityEntity f where f.pctBoothId = :pctBoothId")
                .setParameter("pctBoothId", pctBoothId)
                .executeUpdate();
    }

    public void saveFacilities(List<ParticipationBoothFacilityEntity> facilities) {
        for (ParticipationBoothFacilityEntity f : facilities) {
            if (f.getPctBoothFaciId() == null) em.persist(f);
            else em.merge(f);
        }
    }
}
