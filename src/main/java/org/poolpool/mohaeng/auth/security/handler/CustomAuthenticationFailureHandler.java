package org.poolpool.mohaeng.auth.security.handler;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
@Component
public class CustomAuthenticationFailureHandler implements AuthenticationFailureHandler {
    @Override
    public void onAuthenticationFailure(HttpServletRequest request,
                                        HttpServletResponse response,
                                        AuthenticationException exception) throws IOException {

    	String errorMessage = "구글 계정 연동 회원가입에 실패했습니다. 다시 시도해주세요.";

        if (exception instanceof OAuth2AuthenticationException oauthEx) {
            String description = oauthEx.getError().getDescription();
            if (description != null && !description.isBlank()) {
                errorMessage = description;
            }
        }
        System.out.println("errorMessage : " + errorMessage);

        // 이미 응답이 커밋된 경우 redirect 불가 → 무시
        if (response.isCommitted()) {
            System.out.println("[AuthFailureHandler] 응답이 이미 커밋됨, redirect 생략");
            return;
        }

        String safeMessage = URLEncoder.encode(errorMessage, StandardCharsets.UTF_8);
        Cookie cookie = new Cookie("OAUTH_ERROR", safeMessage);
        cookie.setPath("/");
        cookie.setMaxAge(10);
        cookie.setHttpOnly(false);
        response.addCookie(cookie);
        response.sendRedirect("http://localhost:3000/oauthFailure");
    }
}