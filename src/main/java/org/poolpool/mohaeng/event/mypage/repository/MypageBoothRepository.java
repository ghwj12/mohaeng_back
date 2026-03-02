package org.poolpool.mohaeng.event.mypage.repository;

import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.poolpool.mohaeng.event.mypage.dto.BoothApplicationDetailResponse;
import org.poolpool.mohaeng.event.mypage.dto.BoothApplicationFileResponse;
import org.poolpool.mohaeng.event.mypage.dto.BoothMypageResponse;
import org.springframework.stereotype.Repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Repository
public class MypageBoothRepository {

    @PersistenceContext
    private EntityManager em;

    /**
     * ✅ 부스 신청/관리 내역(유저 기준)
     * - PARTICIPATION_BOOTH -> HOST_BOOTH -> event 조인
     * - 마이페이지에서 필요한 event 요약정보까지 같이 반환
     */
    public List<BoothMypageResponse> findMyBooths(Long userId) {
        // ⚠️ `event`는 예약어 충돌 가능성이 있어서 백틱 사용
        String sql = "\n" +
                "SELECT \n" +
                "  pb.PCT_BOOTH_ID, pb.HOST_BOOTH_ID, pb.BOOTH_TITLE, pb.BOOTH_TOPIC, pb.BOOTH_COUNT, pb.TOTAL_PRICE,\n" +
                "  CASE WHEN pb.STATUS IN ('신청','결제완료') THEN '대기' ELSE pb.STATUS END AS VIEW_STATUS,\n" +
                "  pb.CREATED_AT,\n" +
                "  hb.EVENT_ID, e.TITLE, e.THUMBNAIL, COALESCE(e.DESCRIPTION, e.SIMPLE_EXPLAIN) AS EVENT_DESC, e.START_DATE, e.END_DATE\n" +
                "FROM PARTICIPATION_BOOTH pb\n" +
                "JOIN HOST_BOOTH hb ON pb.HOST_BOOTH_ID = hb.BOOTH_ID\n" +
                "JOIN `event` e ON hb.EVENT_ID = e.EVENT_ID\n" +
                "WHERE pb.USER_ID = ?\n" +
                "ORDER BY pb.CREATED_AT DESC";

        @SuppressWarnings("unchecked")
        List<Object[]> rows = em.createNativeQuery(sql)
                .setParameter(1, userId)
                .getResultList();

        List<BoothMypageResponse> out = new ArrayList<>();
        for (Object[] r : rows) {
            out.add(BoothMypageResponse.builder()
                    .pctBoothId(toLong(r[0]))
                    .hostBoothId(toLong(r[1]))
                    .boothTitle(toStr(r[2]))
                    .boothTopic(toStr(r[3]))
                    .boothCount(toInt(r[4]))
                    .totalPrice(toInt(r[5]))
                    .status(toStr(r[6]))
                    .createdAt(toLdt(r[7]))
                    .eventId(toLong(r[8]))
                    .eventTitle(toStr(r[9]))
                    .eventThumbnail(toStr(r[10]))
                    .eventDescription(toStr(r[11]))
                    .startDate(toLd(r[12]))
                    .endDate(toLd(r[13]))
                    .build());
        }
        return out;
    }

    /**
     * ✅ 부스 관리(내가 주최한 행사에서 받은 부스)
     * - event.host_id = 현재 로그인 사용자
     * - 상태: 승인/반려면 '완료', 그 외는 '대기'
     */
    public List<BoothMypageResponse> findReceivedBooths(Long hostUserId) {
        String sql = "\n" +
                "SELECT \n" +
                "  pb.PCT_BOOTH_ID, pb.HOST_BOOTH_ID, pb.BOOTH_TITLE, pb.BOOTH_TOPIC, pb.BOOTH_COUNT, pb.TOTAL_PRICE,\n" +
                "  CASE WHEN pb.STATUS IN ('승인','반려') THEN '완료' ELSE '대기' END AS VIEW_STATUS,\n" +
                "  pb.CREATED_AT,\n" +
                "  hb.EVENT_ID, e.TITLE, e.THUMBNAIL, COALESCE(e.DESCRIPTION, e.SIMPLE_EXPLAIN) AS EVENT_DESC, e.START_DATE, e.END_DATE\n" +
                "FROM PARTICIPATION_BOOTH pb\n" +
                "JOIN HOST_BOOTH hb ON pb.HOST_BOOTH_ID = hb.BOOTH_ID\n" +
                "JOIN `event` e ON hb.EVENT_ID = e.EVENT_ID\n" +
                "WHERE e.HOST_ID = ?\n" +
                "ORDER BY pb.CREATED_AT DESC";

        @SuppressWarnings("unchecked")
        List<Object[]> rows = em.createNativeQuery(sql)
                .setParameter(1, hostUserId)
                .getResultList();

        List<BoothMypageResponse> out = new ArrayList<>();
        for (Object[] r : rows) {
            out.add(BoothMypageResponse.builder()
                    .pctBoothId(toLong(r[0]))
                    .hostBoothId(toLong(r[1]))
                    .boothTitle(toStr(r[2]))
                    .boothTopic(toStr(r[3]))
                    .boothCount(toInt(r[4]))
                    .totalPrice(toInt(r[5]))
                    .status(toStr(r[6]))
                    .createdAt(toLdt(r[7]))
                    .eventId(toLong(r[8]))
                    .eventTitle(toStr(r[9]))
                    .eventThumbnail(toStr(r[10]))
                    .eventDescription(toStr(r[11]))
                    .startDate(toLd(r[12]))
                    .endDate(toLd(r[13]))
                    .build());
        }
        return out;
    }

    /**
     * ✅ 신청서 상세: 주최자(해당 이벤트 host) 또는 신청자(pb.user_id)만 조회 가능
     */
    public BoothApplicationDetailResponse findBoothDetail(Long userId, Long pctBoothId) {
        String sql = "\n" +
                "SELECT\n" +
                "  pb.PCT_BOOTH_ID, pb.HOST_BOOTH_ID, pb.USER_ID, pb.HOMEPAGE_URL, pb.BOOTH_TITLE, pb.BOOTH_TOPIC,\n" +
                "  pb.MAIN_ITEMS, pb.DESCRIPTION, pb.BOOTH_COUNT, pb.BOOTH_PRICE, pb.FACILITY_PRICE, pb.TOTAL_PRICE,\n" +
                "  pb.STATUS, pb.CREATED_AT, pb.APPROVED_DATE,\n" +
                "  hb.EVENT_ID, e.TITLE, e.THUMBNAIL, COALESCE(e.DESCRIPTION, e.SIMPLE_EXPLAIN) AS EVENT_DESC, e.START_DATE, e.END_DATE\n" +
                "FROM PARTICIPATION_BOOTH pb\n" +
                "JOIN HOST_BOOTH hb ON pb.HOST_BOOTH_ID = hb.BOOTH_ID\n" +
                "JOIN `event` e ON hb.EVENT_ID = e.EVENT_ID\n" +
                "WHERE pb.PCT_BOOTH_ID = ?\n" +
                "  AND (pb.USER_ID = ? OR e.HOST_ID = ?)";

        @SuppressWarnings("unchecked")
        List<Object[]> rows = em.createNativeQuery(sql)
                .setParameter(1, pctBoothId)
                .setParameter(2, userId)
                .setParameter(3, userId)
                .getResultList();

        if (rows.isEmpty()) return null;
        Object[] r = rows.get(0);

        BoothApplicationDetailResponse base = BoothApplicationDetailResponse.builder()
                .pctBoothId(toLong(r[0]))
                .hostBoothId(toLong(r[1]))
                .userId(toLong(r[2]))
                .homepageUrl(toStr(r[3]))
                .boothTitle(toStr(r[4]))
                .boothTopic(toStr(r[5]))
                .mainItems(toStr(r[6]))
                .description(toStr(r[7]))
                .boothCount(toInt(r[8]))
                .boothPrice(toInt(r[9]))
                .facilityPrice(toInt(r[10]))
                .totalPrice(toInt(r[11]))
                .status(toStr(r[12]))
                .createdAt(toLdt(r[13]))
                .approvedDate(toLdt(r[14]))
                .eventId(toLong(r[15]))
                .eventTitle(toStr(r[16]))
                .eventThumbnail(toStr(r[17]))
                .startDate(toLd(r[18]))
                .endDate(toLd(r[19]))
                .build();

        // files
        List<BoothApplicationFileResponse> files = findBoothFiles(pctBoothId);
        return BoothApplicationDetailResponse.builder()
                .pctBoothId(base.getPctBoothId())
                .hostBoothId(base.getHostBoothId())
                .userId(base.getUserId())
                .homepageUrl(base.getHomepageUrl())
                .boothTitle(base.getBoothTitle())
                .boothTopic(base.getBoothTopic())
                .mainItems(base.getMainItems())
                .description(base.getDescription())
                .boothCount(base.getBoothCount())
                .boothPrice(base.getBoothPrice())
                .facilityPrice(base.getFacilityPrice())
                .totalPrice(base.getTotalPrice())
                .status(base.getStatus())
                .createdAt(base.getCreatedAt())
                .approvedDate(base.getApprovedDate())
                .eventId(base.getEventId())
                .eventTitle(base.getEventTitle())
                .eventThumbnail(base.getEventThumbnail())
                .startDate(base.getStartDate())
                .endDate(base.getEndDate())
                .files(files)
                .build();
    }

    public List<BoothApplicationFileResponse> findBoothFiles(Long pctBoothId) {
        String sql = "\n" +
                "SELECT f.ORIGINAL_FILE_NAME, f.RENAME_FILE_NAME, f.SORT_ORDER\n" +
                "FROM `file` f\n" +
                "WHERE f.PCT_BOOTH_ID = ? AND f.FILE_TYPE = 'P_BOOTH'\n" +
                "ORDER BY f.SORT_ORDER ASC";

        @SuppressWarnings("unchecked")
        List<Object[]> rows = em.createNativeQuery(sql)
                .setParameter(1, pctBoothId)
                .getResultList();

        List<BoothApplicationFileResponse> out = new ArrayList<>();
        for (Object[] r : rows) {
            out.add(BoothApplicationFileResponse.builder()
                    .originalFileName(toStr(r[0]))
                    .renameFileName(toStr(r[1]))
                    .sortOrder(toInt(r[2]))
                    .build());
        }
        return out;
    }

    /**
     * ✅ 주최자 승인/반려
     */
    public int updateBoothStatusAsHost(Long hostUserId, Long pctBoothId, String status) {
        String sql = "\n" +
                "UPDATE PARTICIPATION_BOOTH pb\n" +
                "JOIN HOST_BOOTH hb ON pb.HOST_BOOTH_ID = hb.BOOTH_ID\n" +
                "JOIN `event` e ON hb.EVENT_ID = e.EVENT_ID\n" +
                "SET pb.STATUS = ?, pb.APPROVED_DATE = NOW(), pb.UPDATED_AT = NOW()\n" +
                "WHERE pb.PCT_BOOTH_ID = ? AND e.HOST_ID = ? AND pb.STATUS IN ('신청','결제완료')";

        return em.createNativeQuery(sql)
                .setParameter(1, status)
                .setParameter(2, pctBoothId)
                .setParameter(3, hostUserId)
                .executeUpdate();
    }


    /**
     * ✅ 주최자 승인/반려 (완화 버전)
     * - 승인/반려/취소가 아닌 상태는 모두 '대기'로 보고 처리 가능
     * - (예: '신청', '결제완료', 기타 커스텀 상태)
     */
    public int updateBoothStatusAsHostRelaxed(Long hostUserId, Long pctBoothId, String status) {
        String sql = "\n" +
                "UPDATE PARTICIPATION_BOOTH pb\n" +
                "JOIN HOST_BOOTH hb ON pb.HOST_BOOTH_ID = hb.BOOTH_ID\n" +
                "JOIN `event` e ON hb.EVENT_ID = e.EVENT_ID\n" +
                "SET pb.STATUS = ?, pb.APPROVED_DATE = NOW(), pb.UPDATED_AT = NOW()\n" +
                "WHERE pb.PCT_BOOTH_ID = ? AND e.HOST_ID = ?\n" +
                "  AND (pb.STATUS IS NULL OR pb.STATUS NOT IN ('승인','반려','취소'))";

        return em.createNativeQuery(sql)
                .setParameter(1, status)
                .setParameter(2, pctBoothId)
                .setParameter(3, hostUserId)
                .executeUpdate();
    }


    private static Long toLong(Object v) {
        if (v == null) return null;
        if (v instanceof Number n) return n.longValue();
        return Long.valueOf(String.valueOf(v));
    }

    private static Integer toInt(Object v) {
        if (v == null) return null;
        if (v instanceof Number n) return n.intValue();
        return Integer.valueOf(String.valueOf(v));
    }

    private static String toStr(Object v) {
        return v == null ? null : String.valueOf(v);
    }

    private static LocalDateTime toLdt(Object v) {
        if (v == null) return null;
        if (v instanceof Timestamp ts) return ts.toLocalDateTime();
        // 일부 드라이버는 LocalDateTime으로 바로 줄 수도 있음
        if (v instanceof LocalDateTime ldt) return ldt;
        return null;
    }

    private static LocalDate toLd(Object v) {
        if (v == null) return null;
        if (v instanceof Date d) return d.toLocalDate();
        if (v instanceof LocalDate ld) return ld;
        return null;
    }
}
