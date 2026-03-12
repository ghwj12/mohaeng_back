package org.poolpool.mohaeng.user.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.poolpool.mohaeng.ai.dto.BizOcrResponse;
import org.poolpool.mohaeng.ai.service.BizOcrService;
import org.poolpool.mohaeng.auth.dto.request.LoginRequest;
import org.poolpool.mohaeng.auth.dto.response.TokenResponse;
import org.poolpool.mohaeng.common.api.ApiResponse;
import org.poolpool.mohaeng.event.host.service.EventHostService;
import org.poolpool.mohaeng.user.dto.SocialUserDto;
import org.poolpool.mohaeng.user.dto.UserDto;
import org.poolpool.mohaeng.user.service.UserService;
import org.poolpool.mohaeng.user.type.SignupType;
import org.poolpool.mohaeng.user.type.UserStatus;
import org.poolpool.mohaeng.user.type.UserType;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

	private final UserService userService;
	private final EventHostService eventHostService;
	private final BizOcrService bizOcrService;
	
	//이메일(아이디) 중복 확인
	@PostMapping("/checkId")
    public ResponseEntity<ApiResponse<String>> checkEmail(@RequestBody LoginRequest req) {
        int result = userService.existsByEmail(req.userId());	// 0일 경우 사용 가능
        
        return ResponseEntity.ok(ApiResponse.ok("이메일 중복 확인 완료", (result == 0) ? "ok" : "dup"));
    }
	
	//일반 회원가입(개인/업체)
	@PostMapping("/createUser")
    public ResponseEntity<ApiResponse<Void>> signUp(
    		@ModelAttribute @Valid UserDto user,
    		@RequestPart(value = "businessFile", required = false) MultipartFile businessFile) {

		// 업체 회원일 경우 사업자 등록증 확인
		if(user.getUserType() == UserType.COMPANY && businessFile != null) {
			try {
				BizOcrResponse ocrResult = bizOcrService.extractBusinessInfo(businessFile);
				
				String businessNum = ocrResult.getBusinessNumber();
				String ownerName = ocrResult.getOwnerName();
				String openDate = ocrResult.getOpenDate();
				
				if(businessNum.isEmpty() || ownerName.isEmpty() || openDate.isEmpty()) {
					return ResponseEntity.status(500)
				            .body(ApiResponse.fail("사업자 등록증 OCR 중 오류 발생하여 회원 가입 실패", null));
				}
				
				// 로그 확인
				System.out.println("사업자번호: " + businessNum);
				System.out.println("대표자명: " + ownerName);
				System.out.println("개업일자: " + openDate);			
				
				user.setBusinessNum(businessNum);
			} catch (Exception e) {
				return ResponseEntity.status(500)
			            .body(ApiResponse.fail("사업자 등록증 OCR 중 오류 발생하여 회원 가입 실패", null));
			}
		}
		
//		int result = userService.insertUser(user);
		int result = 2;
        
        if (result > 0) {
            return ResponseEntity.status(201).body(ApiResponse.ok("회원 가입 성공", null));
        }
        return ResponseEntity.status(500).body(ApiResponse.fail("회원 가입 실패", null));
    }
	
	//이메일(아이디) 찾기
	@PostMapping("/searchId")
    public ResponseEntity<ApiResponse<Object>> findEmail(@RequestBody LoginRequest req) {
		List<UserDto> users = userService.findAllByNameAndPhone(req.name(), req.phone());

        if (users.isEmpty()) {
            return ResponseEntity.status(404)
                    .body(ApiResponse.fail("No User", "회원 정보를 찾을 수 없습니다."));
        }

        // 조회 결과 데이터 1개일 경우
        if (users.size() == 1) {
            UserDto user = users.get(0);
            if (user.getSignupType() == SignupType.GOOGLE) {
                return ResponseEntity.status(404)
                        .body(ApiResponse.fail("Social User", "구글 계정(" + user.getEmail() + ")과 연동하여 가입되어 있습니다."));
            } else if (user.getUserStatus() == UserStatus.WITHDRAWAL) {
                return ResponseEntity.status(404)
                        .body(ApiResponse.fail("Withdrawal User", "회원 정보를 찾을 수 없습니다."));
            }
            return ResponseEntity.ok(ApiResponse.ok("이메일 찾기 성공!", user.getEmail()));
        }

        // 조회 결과 데이터 여러 개일 경우
        Map<String, List<String>> emailMap = new HashMap<>();
        List<String> googleEmails = new ArrayList<>();
        List<String> normalEmails = new ArrayList<>();

        for (UserDto user : users) {
            if (user.getUserStatus() == UserStatus.WITHDRAWAL) {
                continue; // 탈퇴된 계정 제외
            }
            if (user.getSignupType() == SignupType.GOOGLE) {
                googleEmails.add(user.getEmail());
            } else {
                normalEmails.add(user.getEmail());
            }
        }

        emailMap.put("googleEmails", googleEmails);
        emailMap.put("normalEmails", normalEmails);

        if (googleEmails.isEmpty() && normalEmails.isEmpty()) {
            return ResponseEntity.status(404)
                    .body(ApiResponse.fail("No Valid Users", "회원 정보를 찾을 수 없습니다."));
        }

        return ResponseEntity.ok(ApiResponse.ok("이메일 찾기 성공!", emailMap));
	}
	
	//비밀번호 찾기
	@PostMapping("/renewPwd")
    public ResponseEntity<ApiResponse<String>> sendNewPwd(@RequestBody LoginRequest req) {
		UserDto user = userService.findByEmailAndPhone(req.userId(), req.phone());
		if (user == null) {
            return ResponseEntity.status(404).body(ApiResponse.fail("No User", "회원 정보를 찾을 수 없습니다."));
        } else if(user.getSignupType() == SignupType.GOOGLE) {
        	return ResponseEntity.status(404).body(ApiResponse.fail("Social User", "구글 계정과 연동하여 가입되어 있습니다."));
        } else if(user.getUserStatus() == UserStatus.WITHDRAWAL) {
        	return ResponseEntity.status(404).body(ApiResponse.fail("Withdrawal User", "탈퇴된 계정입니다."));
        } else {
        	//랜덤 비밀번호 생성
        	String randomPwd = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        	//비밀번호 재설정 안내 메일 전송
        	userService.sendRenewPwd(req.userId(), randomPwd);
        	//DB 업데이트
        	userService.updateRenewPwd(user.getUserId(), req.userId(), randomPwd);
        }
		
		return ResponseEntity.ok(ApiResponse.ok("비밀번호 재설정 안내 메일을 보냈습니다. 이메일을 확인해주세요.", null));
	}
	
	//개인정보 조회
	@GetMapping("/profile")
    public ResponseEntity<ApiResponse<UserDto>> getProfile(@AuthenticationPrincipal String userId) {
		UserDto user = userService.findById(Long.valueOf(userId));
        if (user == null) {
            return ResponseEntity.status(404).body(ApiResponse.fail("회원 정보를 찾을 수 없습니다.", null));
        }
        return ResponseEntity.ok(ApiResponse.ok("회원 조회 성공", user));
    }
	
	//개인정보 수정
	@PatchMapping(value = "/profile", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> updateProfile(
    		@AuthenticationPrincipal String userId,
    		@RequestPart(value = "userInfo") UserDto user,
    		@RequestParam(value = "deletePhoto", defaultValue = "false") boolean deletePhoto,
    		@RequestPart(value = "newPhoto", required = false) MultipartFile photo) {
        
		user.setUserId(Long.valueOf(userId));
        
        try {
            userService.patchUser(user, deletePhoto, photo);
            return ResponseEntity.ok("회원 정보 수정 성공");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).body(e.getMessage());
        } catch (RuntimeException e) {
            log.error("회원 사진 업로드 실패", e);
            return ResponseEntity.status(500).body(e.getMessage());
        } catch (Exception e) {
            log.error("회원 정보 수정 실패", e);
            return ResponseEntity.status(500).body("회원 정보 수정 실패");
        }

    }
	
	//회원 탈퇴
	@PatchMapping(value = "/withdrawal")
	public ResponseEntity<ApiResponse<String>> withdrawal(
			@AuthenticationPrincipal String userId, 
			@RequestBody UserDto user) {
		
		user.setUserId(Long.valueOf(userId));

		// 회원 탈퇴 시 주최 행사 중 행사종료/삭제되지 않은 행사 존재 유무 조회
		boolean hasActiveEvent = eventHostService.hasActiveEvent(user.getUserId());
		
		if (hasActiveEvent) {
			return ResponseEntity.ok(ApiResponse.fail("주최 행사 존재", "행사 종료되지 않은 주최 행사가 존재하여 탈퇴가 불가합니다."));
		} else {
			userService.patchWithdrawal(user);
			return ResponseEntity.ok(ApiResponse.ok("회원 탈퇴 성공", "회원 탈퇴 되었습니다."));			
		}
        
    }
	
	// 사업자 등록증 인증 (회원가입 전 별도 호출)
	@PostMapping(value = "/verifyBiz", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<ApiResponse<Map<String, Object>>> verifyBiz(
	        @RequestPart("businessFile") MultipartFile businessFile) {
	    try {
	        BizOcrResponse ocrResult = bizOcrService.extractBusinessInfo(businessFile);

	        String businessNum = ocrResult.getBusinessNumber();
	        String ownerName   = ocrResult.getOwnerName();
	        String openDate    = ocrResult.getOpenDate();
	        
	        log.info("===== 사업자 인증 결과 =====");
	        log.info("사업자번호: {}", businessNum);
	        log.info("대표자명: {}", ownerName);
	        log.info("개업일자: {}", openDate);
	        log.info("회사명: {}", ocrResult.getCompanyName());
	        log.info("isValid: {}", ocrResult.getIsValid());
	        log.info("validationStatus: {}", ocrResult.getValidationStatus());
	        log.info("validationMessage: {}", ocrResult.getValidationMessage());
	        log.info("===========================");

	        if (businessNum.isEmpty() || ownerName.isEmpty() || openDate.isEmpty()) {
	            return ResponseEntity.status(400)
	                .<ApiResponse<Map<String, Object>>>body(
	                    ApiResponse.fail("인증 실패", null));
	        }

	        Map<String, Object> result = new HashMap<>();
	        result.put("businessNumber", businessNum);
	        result.put("companyName",    ocrResult.getCompanyName());
	        result.put("ownerName",      ownerName);
	        result.put("isValid",        ocrResult.getIsValid());
	        result.put("message",        ocrResult.getValidationMessage());

	        if (Boolean.FALSE.equals(ocrResult.getIsValid())) {
	            return ResponseEntity.status(400)
	                .<ApiResponse<Map<String, Object>>>body(
	                    ApiResponse.fail(ocrResult.getValidationMessage(), null));
	        }

	        return ResponseEntity.ok(ApiResponse.ok("인증 성공", result));

	    } catch (Exception e) {
	        return ResponseEntity.status(500)
	            .<ApiResponse<Map<String, Object>>>body(
	                ApiResponse.fail("사업자 인증 중 오류가 발생했습니다.", null));
	    }
	}
	
	// 행사 등록 시 회원 정보 조회 (userId)
	@GetMapping("/me")
	public ResponseEntity<ApiResponse<UserDto>> getMyProfile(
	        @AuthenticationPrincipal String userId) {  // 토큰에서 꺼낸 userId (숫자)
	    UserDto user = userService.findById(Long.valueOf(userId));
	    if (user == null) {
	        return ResponseEntity.status(404).body(ApiResponse.fail("회원 정보 없음", null));
	    }
	    return ResponseEntity.ok(ApiResponse.ok("조회 성공", user));
	}
	
	//소셜 회원가입
	@PostMapping("/socialSignupComplete")
	public ResponseEntity<TokenResponse> completeSocialSignup(@RequestBody SocialUserDto socialUserDto) {
	    
	    try {
	        // 저장 후 access/refresh 토큰 생성
	    	TokenResponse tokens = userService.socialSignup(socialUserDto);
	        return ResponseEntity.ok(tokens);
	    } catch (Exception e) {
	        log.error("소셜 회원가입 실패", e);
	        return ResponseEntity.status(500).body(null);
	    }
	}
	
}
