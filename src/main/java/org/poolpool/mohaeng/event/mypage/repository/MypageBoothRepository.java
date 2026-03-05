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
import org.poolpool.mohaeng.event.participation.dto.ParticipationBoothFacilityDto;
import org.springframework.stereotype.Repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Repository
public class MypageBoothRepository {

    @PersistenceContext
    private EntityManager em;

    // ─────────────────────────────────────────────────────────────
    // 부스 신청 내역 (유저 기준)
    // ─────────────────────────────────────────────────────────────
    public List<BoothMypageResponse> findMyBooths(Long userId) {
        String sql =
                "SELECT\n" +
                "  pb.PCT_BOOTH_ID, pb.HOST_BOOTH_ID, pb.BOOTH_TITLE, pb.BOOTH_TOPIC,\n" +
                "  pb.BOOTH_COUNT, pb.TOTAL_PRICE, pb.STATUS AS VIEW_STATUS, pb.CREATED_AT,\n" +
                "  hb.EVENT_ID, e.TITLE, e.THUMBNAIL,\n" +
                "  COALESCE(e.DESCRIPTION, e.SIMPLE_EXPLAIN) AS EVENT_DESC,\n" +
                "  e.START_DATE, e.END_DATE, e.EVENT_STATUS\n" +
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

    // ─────────────────────────────────────────────────────────────
    // 부스 관리 (주최자 기준)
    // ─────────────────────────────────────────────────────────────
    public List<BoothMypageResponse> findReceivedBooths(Long hostUserId) {
        String sql =
                "SELECT\n" +
                "  pb.PCT_BOOTH_ID, pb.HOST_BOOTH_ID, pb.BOOTH_TITLE, pb.BOOTH_TOPIC,\n" +
                "  pb.BOOTH_COUNT, pb.TOTAL_PRICE, pb.STATUS AS VIEW_STATUS, pb.CREATED_AT,\n" +
                "  hb.EVENT_ID, e.TITLE, e.THUMBNAIL,\n" +
                "  COALESCE(e.DESCRIPTION, e.SIMPLE_EXPLAIN) AS EVENT_DESC,\n" +
                "  e.START_DATE, e.END_DATE, e.EVENT_STATUS\n" +
                "FROM participation_booth pb\n" +
                "JOIN host_booth hb ON pb.HOST_BOOTH_ID = hb.BOOTH_ID\n" +
                "JOIN `event` e ON hb.EVENT_ID = e.EVENT_ID\n" +
                "JOIN users u ON pb.USER_ID = u.USER_ID\n" +
                "WHERE e.HOST_ID = ? AND pb.STATUS != '취소'\n" +
                "  AND u.USER_STATUS NOT IN ('WITHDRAWAL', 'DORMANT')\n" +
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

    // ─────────────────────────────────────────────────────────────
    // 신청서 상세
    // ─────────────────────────────────────────────────────────────
    public BoothApplicationDetailResponse findBoothDetail(Long userId, Long pctBoothId) {

        // ✅ host_booth JOIN 추가 → 부스명/규격/메모/단가/수량 전부 포함
        String sql =
                "SELECT\n" +
                "  pb.PCT_BOOTH_ID,        -- 0\n" +
                "  pb.HOST_BOOTH_ID,       -- 1\n" +
                "  pb.USER_ID,             -- 2\n" +
                "  pb.HOMEPAGE_URL,        -- 3\n" +
                "  pb.BOOTH_TITLE,         -- 4\n" +
                "  pb.BOOTH_TOPIC,         -- 5\n" +
                "  pb.MAIN_ITEMS,          -- 6\n" +
                "  pb.DESCRIPTION,         -- 7\n" +
                "  pb.BOOTH_COUNT,         -- 8\n" +
                "  pb.BOOTH_PRICE,         -- 9\n" +
                "  pb.FACILITY_PRICE,      -- 10\n" +
                "  pb.TOTAL_PRICE,         -- 11\n" +
                "  pb.STATUS,              -- 12\n" +
                "  pb.CREATED_AT,          -- 13\n" +
                "  pb.APPROVED_DATE,       -- 14\n" +
                "  hb.EVENT_ID,            -- 15\n" +
                "  e.TITLE,               -- 16\n" +
                "  e.THUMBNAIL,           -- 17\n" +
                "  e.START_DATE,          -- 18\n" +
                "  e.END_DATE,            -- 19\n" +
                "  u.NAME,               -- 20\n" +
                "  u.EMAIL,              -- 21\n" +
                "  u.PHONE,              -- 22\n" +
                "  u.BUSINESS_NUM,       -- 23\n" +
                "  hb.BOOTH_NAME,         -- 24\n" +
                "  hb.BOOTH_SIZE,         -- 25\n" +
                "  hb.BOOTH_NOTE,         -- 26\n" +
                "  hb.BOOTH_PRICE,        -- 27  (단가)\n" +
                "  hb.TOTAL_COUNT,        -- 28\n" +
                "  hb.REMAIN_COUNT        -- 29\n" +
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

        List<BoothApplicationFileResponse>  files      = findBoothFiles(pctBoothId);
        List<ParticipationBoothFacilityDto> facilities = findBoothFacilities(pctBoothId);

        return BoothApplicationDetailResponse.builder()
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
                .boothName(toStr(r[24]))
                .boothSize(toStr(r[25]))
                .boothNote(toStr(r[26]))
                .boothUnitPrice(toInt(r[27]))
                .boothTotal(toInt(r[28]))
                .boothRemain(toInt(r[29]))
                .files(files)
                .facilities(facilities)
                .build();
    }

    // ─────────────────────────────────────────────────────────────
    // 첨부파일 조회
    // ─────────────────────────────────────────────────────────────
    public List<BoothApplicationFileResponse> findBoothFiles(Long pctBoothId) {
        String sql =
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

    // ─────────────────────────────────────────────────────────────
    // ✅ 부대시설 조회 (신청 정보 + 시설 원본 정보 전부)
    //    participation_booth_facility + host_booth_facility JOIN
    // ─────────────────────────────────────────────────────────────
    public List<ParticipationBoothFacilityDto> findBoothFacilities(Long pctBoothId) {
        String sql =
                "SELECT\n" +
                "  pbf.PCT_BOOTHFACI_ID,   -- 0  신청 시설 PK\n" +
                "  pbf.HOST_BOOTHFACI_ID,  -- 1  host 시설 FK\n" +
                "  pbf.PCT_BOOTH_ID,       -- 2\n" +
                "  pbf.FACI_COUNT,         -- 3  신청 수량\n" +
                "  pbf.FACI_PRICE,         -- 4  신청 당시 금액\n" +
                "  hf.FACI_NAME,           -- 5  시설명\n" +
                "  hf.FACI_UNIT,           -- 6  단위\n" +
                "  hf.FACI_PRICE,          -- 7  단가\n" +
                "  hf.HAS_COUNT,           -- 8  수량 제한 여부\n" +
                "  hf.TOTAL_COUNT,         -- 9  총 수량\n" +
                "  hf.REMAIN_COUNT         -- 10 잔여 수량\n" +
                "FROM participation_booth_facility pbf\n" +
                "JOIN host_booth_facility hf ON pbf.HOST_BOOTHFACI_ID = hf.HOST_BOOTHFACI_ID\n" +
                "WHERE pbf.PCT_BOOTH_ID = ?\n" +
                "ORDER BY pbf.PCT_BOOTHFACI_ID ASC";

        @SuppressWarnings("unchecked")
        List<Object[]> rows = em.createNativeQuery(sql)
                .setParameter(1, pctBoothId)
                .getResultList();

        List<ParticipationBoothFacilityDto> out = new ArrayList<>();
        for (Object[] r : rows) {
            ParticipationBoothFacilityDto d = new ParticipationBoothFacilityDto();
            d.setPctBoothFaciId(toLong(r[0]));
            d.setHostBoothFaciId(toLong(r[1]));
            d.setPctBoothId(toLong(r[2]));
            d.setFaciCount(toInt(r[3]));
            d.setFaciPrice(toInt(r[4]));
            d.setFaciName(toStr(r[5]));
            d.setFaciUnit(toStr(r[6]));
            d.setUnitPrice(toInt(r[7]));
            d.setHasCount(toBool(r[8]));
            d.setTotalCount(toInt(r[9]));
            d.setRemainCount(toInt(r[10]));
            out.add(d);
        }
        return out;
    }

    // ─────────────────────────────────────────────────────────────
    // 상태 업데이트 (주최자)
    // ─────────────────────────────────────────────────────────────
    public int updateBoothStatusAsHost(Long hostUserId, Long pctBoothId, String status) {
        String sql =
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
        String sql =
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

    // ─────────────────────────────────────────────────────────────
    // 타입 변환 유틸
    // ─────────────────────────────────────────────────────────────
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

    private static Boolean toBool(Object v) {
        if (v == null) return null;
        if (v instanceof Boolean b) return b;
        if (v instanceof Number n) return n.intValue() != 0;
        return Boolean.valueOf(String.valueOf(v));
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
