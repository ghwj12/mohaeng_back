package org.poolpool.mohaeng.notification.type;

public final class NotiTypeId {
    public static final long EVENT_DAY_BEFORE = 1L;
    public static final long EVENT_DAY_OF = 2L;

    public static final long INQUIRY_RECEIVER = 3L;
    public static final long INQUIRY_SENDER = 4L;

    public static final long REPORT_RECEIVER = 5L;
    public static final long REPORT_ACCEPT = 6L;
    public static final long REPORT_REJECT = 7L;

    public static final long BOOTH_RECEIVER = 8L;
    public static final long BOOTH_ACCEPT = 9L;
    public static final long BOOTH_REJECT = 10L;
    public static final long REPORT_REFUND = 11L;
    
    private NotiTypeId() {}
}