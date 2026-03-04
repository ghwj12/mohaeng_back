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
     */
    public List<BoothMypageResponse> findMyBooths(Long userId) {
        String sql = "\n" +
                "SELECT \n" +
                "  pb.PCT_BOOTH_ID, pb.HOST_BOOTH_ID, pb.BOOTH_TITLE, pb.BOOTH_TOPIC, pb.BOOTH_COUNT, pb.TOTAL_PRICE,\n" +
                "  pb.STATUS AS VIEW_STATUS,\n" +
                "  pb.CREATED_AT,\n" +
                "  hb.EVENT_ID, e.TITLE, e.THUMBNAIL, COALESCE(e.DESCRIPTION, e.SIMPLE_EXPLAIN) AS EVENT_DESC, e.START_DATE, e.END_DATE,\n" +
                "  e.EVENT_STATUS\n" +
                "FROM participation_booth pb\n" +
                "JOIN host_booth hb ON pb.HOST_BOOTH_ID = hb.BOOTH_ID\n" +
                "JOIN `event` e ON hb.EVENT_ID = e.EVENT_ID\n" +
                "WHERE pb.USER_ID = ? AND pb.STATUS != '취소'\n" +
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
                    .eventStatus(toStr(r[14]))
                    .build());
        }
        return out;
    }

    /**
     * ✅ 부스 관리(내가 주최한 행사에서 받은 부스)
     * ✅ 탈퇴(WITHDRAWAL) / 휴면(DORMANT) 계정 신청 내역 제외
     */
    public List<BoothMypageResponse> findReceivedBooths(Long hostUserId) {
        String sql = "\n" +
                "SELECT \n" +
                "  pb.PCT_BOOTH_ID, pb.HOST_BOOTH_ID, pb.BOOTH_TITLE, pb.BOOTH_TOPIC, pb.BOOTH_COUNT, pb.TOTAL_PRICE,\n" +
                "  pb.STATUS AS VIEW_STATUS,\n" +
                "  pb.CREATED_AT,\n" +
                "  hb.EVENT_ID, e.TITLE, e.THUMBNAIL, COALESCE(e.DESCRIPTION, e.SIMPLE_EXPLAIN) AS EVENT_DESC, e.START_DATE, e.END_DATE,\n" +
                "  e.EVENT_STATUS\n" +
                "FROM participation_booth pb\n" +
                "JOIN host_booth hb ON pb.HOST_BOOTH_ID = hb.BOOTH_ID\n" +
                "JOIN `event` e ON hb.EVENT_ID = e.EVENT_ID\n" +
                "JOIN users u ON pb.USER_ID = u.USER_ID\n" + // ✅ 신청자 조인
                "WHERE e.HOST_ID = ? AND pb.STATUS != '취소'\n" +
                "  AND u.USER_STATUS NOT IN ('WITHDRAWAL', 'DORMANT')\n" + // ✅ 탈퇴/휴면 제외
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
                    .eventStatus(toStr(r[14]))
                    .build());
        }
        return out;
    }

    /**
     * ✅ 신청서 상세
     */
    public BoothApplicationDetailResponse findBoothDetail(Long userId, Long pctBoothId) {
        String sql = "\n" +
                "SELECT\n" +
                "  pb.PCT_BOOTH_ID, pb.HOST_BOOTH_ID, pb.USER_ID, pb.HOMEPAGE_URL, pb.BOOTH_TITLE, pb.BOOTH_TOPIC,\n" +
                "  pb.MAIN_ITEMS, pb.DESCRIPTION, pb.BOOTH_COUNT, pb.BOOTH_PRICE, pb.FACILITY_PRICE, pb.TOTAL_PRICE,\n" +
                "  pb.STATUS, pb.CREATED_AT, pb.APPROVED_DATE,\n" +
                "  hb.EVENT_ID, e.TITLE, e.THUMBNAIL, e.START_DATE, e.END_DATE,\n" +
                "  u.NAME, u.EMAIL, u.PHONE, u.BUSINESS_NUM\n" +
                "FROM participation_booth pb\n" +
                "JOIN host_booth hb ON pb.HOST_BOOTH_ID = hb.BOOTH_ID\n" +
                "JOIN `event` e ON hb.EVENT_ID = e.EVENT_ID\n" +
                "JOIN users u ON pb.USER_ID = u.USER_ID\n" +
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
                .applicantName(toStr(r[20]))
                .applicantEmail(toStr(r[21]))
                .applicantPhone(toStr(r[22]))
                .applicantBusinessNum(toStr(r[23]))
                .build();

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
                .applicantName(base.getApplicantName())
                .applicantEmail(base.getApplicantEmail())
                .applicantPhone(base.getApplicantPhone())
                .applicantBusinessNum(base.getApplicantBusinessNum())
                .files(files)
                .build();
    }

    public List<BoothApplicationFileResponse> findBoothFiles(Long pctBoothId) {
        String sql = "\n" +
                "SELECT f.ORIGINAL_FILE_NAME, f.RENAME_FILE_NAME, f.SORT_ORDER\n" +
                "FROM `file` f\n" +
                "WHERE f.PCT_BOOTH_ID = ? AND f.FILE_TYPE IN ('PBOOTH','P_BOOTH')\n" +
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

    public int updateBoothStatusAsHost(Long hostUserId, Long pctBoothId, String status) {
        String sql = "\n" +
                "UPDATE participation_booth pb\n" +
                "JOIN host_booth hb ON pb.HOST_BOOTH_ID = hb.BOOTH_ID\n" +
                "JOIN `event` e ON hb.EVENT_ID = e.EVENT_ID\n" +
                "SET pb.STATUS = ?, pb.APPROVED_DATE = NOW(), pb.UPDATED_AT = NOW()\n" +
                "WHERE pb.PCT_BOOTH_ID = ? AND e.HOST_ID = ? AND pb.STATUS IN ('신청','결제완료')";

        return em.createNativeQuery(sql)
                .setParameter(1, status)
                .setParameter(2, pctBoothId)
                .setParameter(3, hostUserId)
                .executeUpdate();
    }

    public int updateBoothStatusAsHostRelaxed(Long hostUserId, Long pctBoothId, String status) {
        String sql = "\n" +
                "UPDATE participation_booth pb\n" +
                "JOIN host_booth hb ON pb.HOST_BOOTH_ID = hb.BOOTH_ID\n" +
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
