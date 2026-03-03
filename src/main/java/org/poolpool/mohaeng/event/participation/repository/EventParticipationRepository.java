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

    public List<ParticipationBoothEntity> findBoothsByUserId(Long userId) {
        return em.createQuery(
                "SELECT b FROM ParticipationBoothEntity b WHERE b.userId = :userId " +
                "ORDER BY b.pctBoothId DESC",
                ParticipationBoothEntity.class)
                .setParameter("userId", userId)
                .getResultList();
    }

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

    //  추가: pctBoothId → eventId (승인/반려 알림에 필요)
    public Long findEventIdByPctBoothId(Long pctBoothId) {
        Object value = em.createNativeQuery(
                "SELECT hb.event_id " +
                "FROM participation_booth pb " +
                "JOIN host_booth hb ON pb.host_booth_id = hb.booth_id " +
                "WHERE pb.pct_booth_id = :pctBoothId")
                .setParameter("pctBoothId", pctBoothId)
                .getResultStream()
                .findFirst()
                .orElse(null);

        if (value == null) return null;
        return ((Number) value).longValue();
    }

    // ──────────────────────────────────────
    //   ParticipationBoothFacilityEntity
    // ──────────────────────────────────────

    public List<ParticipationBoothFacilityEntity> findFacilitiesByPctBoothId(Long pctBoothId) {
        return em.createQuery(
                "SELECT f FROM ParticipationBoothFacilityEntity f " +
                "WHERE f.pctBoothId = :pctBoothId",
                ParticipationBoothFacilityEntity.class)
                .setParameter("pctBoothId", pctBoothId)
                .getResultList();
    }

    public void increaseBoothRemainCount(Long hostBoothId) {
        em.createQuery(
                "UPDATE HostBoothEntity h " +
                "SET h.remainCount = h.remainCount + 1 " +
                "WHERE h.boothId = :boothId")
                .setParameter("boothId", hostBoothId)
                .executeUpdate();
    }

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