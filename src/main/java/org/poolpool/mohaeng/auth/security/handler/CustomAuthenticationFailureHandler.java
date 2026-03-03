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

    	String errorMessage = "소셜 로그인에 실패했습니다.";

        if (exception instanceof OAuth2AuthenticationException oauthEx) {
            String description = oauthEx.getError().getDescription();
            if (description != null && !description.isBlank()) {
                errorMessage = description;
            }
        }

        System.out.println("errorMessage : " + errorMessage);

        String safeMessage = URLEncoder.encode(
                errorMessage,
                StandardCharsets.UTF_8
        );

        Cookie cookie = new Cookie("OAUTH_ERROR", safeMessage);
        cookie.setPath("/");
        cookie.setMaxAge(10);
        cookie.setHttpOnly(false);
        response.addCookie(cookie);

        response.sendRedirect("http://localhost:3000/oauthFailure");
    }
}
