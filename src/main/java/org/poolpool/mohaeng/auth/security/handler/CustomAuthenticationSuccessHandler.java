package org.poolpool.mohaeng.auth.security.handler;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import org.poolpool.mohaeng.auth.token.jwt.JwtTokenProvider;
import org.poolpool.mohaeng.auth.token.refresh.service.RefreshTokenService;
import org.poolpool.mohaeng.user.entity.UserEntity;
import org.poolpool.mohaeng.user.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class CustomAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;
    private final RefreshTokenService refreshTokenService;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException {
        OAuth2User oauthUser = (OAuth2User) authentication.getPrincipal();
        String email = oauthUser.getAttribute("email");
        Boolean isNewUser = oauthUser.getAttribute("isNewUser");

        if (Boolean.TRUE.equals(isNewUser)) {
            // 신규회원 - 토큰 발급 안하고 소셜정보 쿠키 저장 후 리다이렉트
            Map<String, String> socialInfo = new HashMap<>();
            socialInfo.put("provider", oauthUser.getAttribute("provider"));
            socialInfo.put("providerId", oauthUser.getAttribute("providerId"));
            socialInfo.put("email", oauthUser.getAttribute("oauthEmail"));
            socialInfo.put("name", oauthUser.getAttribute("oauthName"));
            
            ObjectMapper mapper = new ObjectMapper();
            String json = mapper.writeValueAsString(socialInfo);

            String socialJson = Base64.getUrlEncoder().encodeToString(json.getBytes(StandardCharsets.UTF_8));
            
            Cookie socialCookie = new Cookie("SOCIAL_INFO", socialJson);
            socialCookie.setPath("/");
            socialCookie.setMaxAge(1800); // 30분
            socialCookie.setHttpOnly(false); // JS 접근 허용
            response.addCookie(socialCookie);

            response.sendRedirect("http://localhost:3000/socialSignup");
        }

        // 기존 회원: 기존 토큰 발급 로직
        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        Long userId = user.getUserId();
        String access = jwtTokenProvider.createAccessToken(userId, "ROLE_" + user.getUserRole());
        String refresh = jwtTokenProvider.createRefreshToken(userId, "ROLE_" + user.getUserRole());
        LocalDateTime now = LocalDateTime.now();
        refreshTokenService.upsert(userId, refresh, now, now.plusDays(1));

        String redirectUrl = "http://localhost:3000/oauthSuccess?accessToken=" + access + "&refreshToken=" + refresh + "&isNewUser=" + isNewUser;
        response.sendRedirect(redirectUrl);
    }
}
