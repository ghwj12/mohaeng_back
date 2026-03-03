package org.poolpool.mohaeng.auth.service;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.poolpool.mohaeng.auth.dto.request.LoginRequest;
import org.poolpool.mohaeng.auth.dto.response.TokenResponse;
import org.poolpool.mohaeng.auth.token.jwt.JwtProperties;
import org.poolpool.mohaeng.auth.token.jwt.JwtTokenProvider;
import org.poolpool.mohaeng.auth.token.refresh.service.RefreshTokenService;
import org.poolpool.mohaeng.user.entity.UserEntity;
import org.poolpool.mohaeng.user.repository.UserRepository;
import org.poolpool.mohaeng.user.type.UserStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {
	
	private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final JwtProperties jwtProperties;
    private final RefreshTokenService refreshTokenService;
    private final UserRepository userRepository;
	
	public TokenResponse login(LoginRequest req) {
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(req.userId(), req.userPwd())
        );

        String email = auth.getName();
        String role = auth.getAuthorities().stream().findFirst().map(a -> a.getAuthority()).orElse("ROLE_USER");
        System.out.println("role : " + role);

        //이메일로 회원 고유ID 찾기
    	UserEntity user = userRepository.findByEmail(email)
    	        .orElseThrow(() -> new BadCredentialsException("아이디 또는 비밀번호가 올바르지 않습니다."));
    	
        // 탈퇴한 회원인지 확인
        if (user.getUserStatus() == UserStatus.WITHDRAWAL) {
            throw new UsernameNotFoundException("탈퇴된 계정입니다.");
        }
        
        Long userId = user.getUserId();
        user.setLastLoginAt(LocalDate.now()); 
    	
        String access = jwtTokenProvider.createAccessToken(userId, role, user.getName());
        String refresh = jwtTokenProvider.createRefreshToken(userId, role);

        LocalDateTime now = LocalDateTime.now();
        refreshTokenService.upsert(userId, refresh, now, now.plusDays(1)); // refresh 1일

        return new TokenResponse(access, refresh, jwtProperties.accessExp(), jwtProperties.refreshExp());
    }

    public TokenResponse refresh(String refreshToken, boolean extendLogin) {
        if (!jwtTokenProvider.validate(refreshToken) || jwtTokenProvider.isExpired(refreshToken)) {
            throw new RuntimeException("refresh expired");
        }

        Long userId = Long.valueOf(jwtTokenProvider.getUserId(refreshToken));
        String role = jwtTokenProvider.getRole(refreshToken);
        
        if (!refreshTokenService.matches(userId, refreshToken)) {
            throw new RuntimeException("refresh not matched");
        }
        
        System.out.println("refresh userId : " + userId);
        
    	UserEntity user = userRepository.findById(userId)
    	        .orElseThrow(() -> new BadCredentialsException("아이디 또는 비밀번호가 올바르지 않습니다."));

        String newAccess = jwtTokenProvider.createAccessToken(userId, role, user.getName());

        String newRefresh = null;
        if (extendLogin) {
            newRefresh = jwtTokenProvider.createRefreshToken(userId, role);
            LocalDateTime now = LocalDateTime.now();
            refreshTokenService.upsert(userId, newRefresh, now, now.plusDays(1));
        }

        return new TokenResponse(newAccess, newRefresh, jwtProperties.accessExp(), jwtProperties.refreshExp());
    }

    public void logout(String accessToken) {
    	if (accessToken == null || !accessToken.startsWith("Bearer ")) return;

        String token = accessToken.substring("Bearer ".length()).trim();
        if (!jwtTokenProvider.validate(token)) return;

        Long userId = Long.valueOf(jwtTokenProvider.getUserId(token));
        refreshTokenService.deleteByUserId(userId);
    }
}
