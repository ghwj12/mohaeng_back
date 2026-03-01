package org.poolpool.mohaeng.sms.controller;

import org.poolpool.mohaeng.common.api.ApiResponse;
import org.poolpool.mohaeng.sms.request.SmsRequest;
import org.poolpool.mohaeng.sms.service.SmsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/sms")
@RequiredArgsConstructor
public class SmsController {

    private final SmsService smsService;

    @PostMapping("/send")
    public ResponseEntity<ApiResponse<String>> send(@RequestBody SmsRequest req) {
        boolean result = smsService.sendSms(req.phone());
        if(result) {
        	return ResponseEntity.ok(ApiResponse.ok("인증번호 전송 성공", "인증번호가 전송되었습니다."));
        }else {
        	return ResponseEntity.ok(ApiResponse.fail("인증번호 전송 실패", "인증번호 전송 중 오류가 발생했습니다."));
        }
    }

    @PostMapping("/verify")
    public ResponseEntity<ApiResponse<String>> verify(@RequestBody SmsRequest req) {
    	boolean result = smsService.verifyCode(req.phone(), req.code());
    	if(result) {
        	return ResponseEntity.ok(ApiResponse.ok("인증번호 확인 성공", "인증되었습니다."));
        }else {
        	return ResponseEntity.ok(ApiResponse.fail("인증번호 확인 실패", "인증번호가 일치하지 않습니다."));
        }
    }
}
