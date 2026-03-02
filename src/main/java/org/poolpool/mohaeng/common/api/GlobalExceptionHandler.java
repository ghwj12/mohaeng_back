package org.poolpool.mohaeng.common.api;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

//React에서 에러 메시지 일관되게 처리하기 위해 전역 예외처리 별도 작성
@RestControllerAdvice 
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleValidation(MethodArgumentNotValidException e) {
        Map<String, String> errors = new HashMap<>();
        for (FieldError fe : e.getBindingResult().getFieldErrors()) {
            errors.put(fe.getField(), fe.getDefaultMessage());
        }
        return ResponseEntity.badRequest().body(ApiResponse.fail("Validation 실패", errors));
    }
    
    //로그인 시 비밀번호 불일치 예외
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiResponse<String>> handleBadCredentials(BadCredentialsException e) {
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.fail("아이디 또는 비밀번호가 올바르지 않습니다.", null));
    }
    
    // 로그인 시 탈퇴된 계정인 경우 예외
    @ExceptionHandler(UsernameNotFoundException.class)
    public ResponseEntity<ApiResponse<String>> handleUsernameNotFound(UsernameNotFoundException e) {
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.fail(e.getMessage(), null));
    }

    // ✅ 추가 1: 잘못된 HTTP 메서드 (GET/POST 불일치) 요청 시 405 에러로 깔끔하게 반환
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<String>> handleMethodNotSupported(HttpRequestMethodNotSupportedException e) {
        return ResponseEntity
                .status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(ApiResponse.fail("지원하지 않는 HTTP 메서드 요청입니다.", e.getMessage()));
    }

    // ✅ 추가 2: 존재하지 않는 경로(404)나 정적 리소스(favicon 등) 요청 시 무시하거나 404 반환
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<String>> handleNoResourceFound(NoResourceFoundException e) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.fail("요청한 경로 또는 리소스를 찾을 수 없습니다.", e.getMessage()));
    }

    
    

// ✅ 인증/인가 예외: AuthException의 status/code를 그대로 내려줌
@ExceptionHandler(org.poolpool.mohaeng.auth.exception.AuthException.class)
public ResponseEntity<ApiResponse<String>> handleAuthException(org.poolpool.mohaeng.auth.exception.AuthException e) {
    return ResponseEntity
            .status(e.getStatus())
            .body(ApiResponse.fail(e.getMessage(), e.getCode()));
}

// ✅ 비즈니스 로직/검증 실패: 400으로 내려서 프론트에서 메시지 처리 가능하게
    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    public ResponseEntity<ApiResponse<String>> handleIllegalState(RuntimeException e) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.fail(e.getMessage(), null));
    }

    // ✅ 권한/소유자 불일치: 403
    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<ApiResponse<String>> handleSecurity(SecurityException e) {
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.fail(e.getMessage(), null));
    }

    // 최후의 보루: 진짜 알 수 없는 서버 에러들만 여기서 잡음
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<String>> handleAny(Exception e) {
        // 💡 꿀팁: 콘솔에 진짜 에러 원인을 찍어둬야 나중에 디버깅할 때 안 헤맵니다!
        e.printStackTrace(); 
        return ResponseEntity.status(500).body(ApiResponse.fail("서버 오류", e.getMessage()));
    }
}
