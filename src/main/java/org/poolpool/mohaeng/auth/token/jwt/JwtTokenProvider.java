package org.poolpool.mohaeng.auth.token.jwt;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Component
public class JwtTokenProvider {

    private final JwtProperties props;
    private final SecretKey key;

    public JwtTokenProvider(JwtProperties props) {
        this.props = props;        
        this.key = Keys.hmacShaKeyFor(props.secret().getBytes(StandardCharsets.UTF_8));
    }

    public String createAccessToken(Long userId, String role, String userName) {
        return createToken(userId, role, JwtClaims.ACCESS, props.accessExp(), userName);
    }

    public String createRefreshToken(Long userId, String role) {
        return createToken(userId, role, JwtClaims.REFRESH, props.refreshExp(), null);
    }

    private String createToken(Long userId, String role, String type, long expMs, String userName) {
        long now = System.currentTimeMillis();
        
        //access 토큰일 경우 사용자 이름 추가
        if(type == JwtClaims.ACCESS) {
        	return Jwts.builder()
        			.subject(String.valueOf(userId))
        			.claim(JwtClaims.ROLE, role)
        			.claim(JwtClaims.TYPE, type)
        			.claim(JwtClaims.USERNAME, userName)
        			.issuedAt(new Date(now))
        			.expiration(new Date(now + expMs))
        			.signWith(key)
        			.compact();

        } else {
        	return Jwts.builder()
        			.subject(String.valueOf(userId))
        			.claim(JwtClaims.ROLE, role)
        			.claim(JwtClaims.TYPE, type)
        			.issuedAt(new Date(now))
        			.expiration(new Date(now + expMs))
        			.signWith(key)
        			.compact();
        }
    }

    
    public Jws<Claims> parse(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token);
    }

    public boolean validate(String token) {
        try {
            parse(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public boolean isExpired(String token) {
        try {
            return parse(token).getPayload().getExpiration().before(new Date());
        } catch (JwtException e) {
            return true;
        }
    }

    public String getUserId(String token) {
        return parse(token).getPayload().getSubject();
    }

    public String getRole(String token) {
        return parse(token).getPayload().get(JwtClaims.ROLE, String.class);
    }

    public String getType(String token) {
        return parse(token).getPayload().get(JwtClaims.TYPE, String.class);
    }
}
