package org.poolpool.mohaeng.admin.report.type;

public final class ReportResult {
    public static final String PENDING = "PENDING";
    public static final String APPROVED = "APPROVED";
    public static final String REJECTED = "REJECTED";

    // 추가: 신고 승인으로 이벤트 비활성화(삭제) 된 상태
    public static final String REPORT_DELETED = "REPORT_DELETED";

    private ReportResult() {}
}