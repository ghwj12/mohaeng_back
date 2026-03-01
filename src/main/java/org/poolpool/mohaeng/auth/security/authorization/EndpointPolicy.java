package org.poolpool.mohaeng.auth.security.authorization;

public final class EndpointPolicy {
    private EndpointPolicy() {}

    //누구나 가능
    public static final String[] PUBLIC_GET = {
            "/api/events/**",
            "/oauth2/**", "/login/oauth2/**",	//소셜 계정 연동
            "/api/eventInquiry/list"	//문의 목록
    };

    public static final String[] PUBLIC_POST = {
            "/api/user/checkId",	//이메일 중복 확인
            "/api/user/createUser",	//일반 회원가입
            "/api/user/searchId",	//이메일 찾기
            "/api/user/renewPwd",	//비밀번호 찾기
            "/api/sms/**"
    };

    //관리자(ADMIN)만 가능
    public static final String[] ADMIN_PAGE = { "/api/admin/**" };

    //회원(USER)만 가능
    public static final String[] USER_PAGE = { 
    		"/api/user/**"
    };

    // 회원(USER), 관리자(ADMIN) 가능
    public static final String[] SERVICE_PAGE = {
            "/api/eventParticipation/**",
            "/api/eventInquiry/**",
            "/api/mypage/**",
            "/api/reviews", "/api/reviews/**",
            "/api/reports",
            "/api/notifications", "/api/notifications/**" 
    };
}
