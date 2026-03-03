package org.poolpool.mohaeng.admin.dormantmanage.controller;

import java.util.List;

import org.poolpool.mohaeng.admin.dormantmanage.dto.DormantUserDto;
import org.poolpool.mohaeng.admin.dormantmanage.service.AdminDormantManageService;
import org.poolpool.mohaeng.admin.dormantmanage.type.DormantStatus;
import org.poolpool.mohaeng.common.api.ApiResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/admin/dormantmanage")
@RequiredArgsConstructor
public class AdminDormantManageController {

	private final AdminDormantManageService adminDormantManageService;
	
	
	//휴면 계정 관리 조회
	@GetMapping("/getDormantUsers")
	public ResponseEntity<Page<DormantUserDto>> getDormantUsers(@PageableDefault(size = 10) Pageable pageable) {
		//휴면 계정 관리 프로시저 호출
		adminDormantManageService.callDormantUserProc();
		
		//휴면 계정 관리 조회
		Page<DormantUserDto> dormantUsers = adminDormantManageService.findDormantUsers(pageable);

		return ResponseEntity.ok(dormantUsers);
	}
	
	//안내 메일 전송
	@GetMapping("/sendDormantUserEmail")
	public ResponseEntity<ApiResponse<Void>> sendDormantUserEmail() {
		
		//안내 메일 보낼 휴면 계정 조회
		List<DormantUserDto> DormantUsers = adminDormantManageService.findDormantUsersByDormantStatus(DormantStatus.DORMANT);
		
		//안내 메일 발송 및 DB 업데이트
		for (DormantUserDto dormantUserDto : DormantUsers) {
			//안내 메일 전송
			adminDormantManageService.sendDormantMail(dormantUserDto.getEmail(), dormantUserDto.getLastLoginAt());
			
			//DB 업데이트
			adminDormantManageService.updateDormantUser(DormantStatus.NOTIFIED, dormantUserDto);
		}
		
		return ResponseEntity.ok(ApiResponse.ok("안내 메일이 전송되었습니다.", null));
	}
	
	//휴면 계정 탈퇴 처리
	@GetMapping("/handleDormantWithdrawal")
	public ResponseEntity<ApiResponse<Void>> handleDormantWithdrawal() {
		//탈퇴 처리할 휴면 계정 조회
		List<DormantUserDto> DormantUsers = adminDormantManageService.findDormantUsersByDormantStatus(DormantStatus.NOTIFIED);
		
		//DB 업데이트 및 탈퇴 처리
		for (DormantUserDto dormantUserDto : DormantUsers) {
			//DB 업데이트
			adminDormantManageService.updateDormantUser(DormantStatus.WITHDRAWN, dormantUserDto);
			
			//탈퇴 처리
			adminDormantManageService.handleWithdrawal(dormantUserDto);
			
		}
		
		return ResponseEntity.ok(ApiResponse.ok("탈퇴 처리되었습니다.", null));
	}
}
