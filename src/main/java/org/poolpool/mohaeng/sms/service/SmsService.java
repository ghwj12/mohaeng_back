package org.poolpool.mohaeng.sms.service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.solapi.sdk.message.service.DefaultMessageService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SmsService {

    @Value("${solapi.api-key}")
    private String apiKey;

    @Value("${solapi.api-secret}")
    private String apiSecret;

    @Value("${solapi.from-number}")
    private String fromNumber;

    //테스트용 메모리 저장소
    private final Map<String, String> authStore = new ConcurrentHashMap<>();

    //인증번호 발송
    public boolean sendSms(String phone) {

    	String authCode = String.valueOf((int)((Math.random() * 900000) + 100000));
    	
    	DefaultMessageService messageService = new DefaultMessageService(apiKey, apiSecret, "https://api.solapi.com");
    	Message message = new Message();
    	message.setFrom(fromNumber);
    	message.setTo(phone);
    	message.setText("[모행] 본인 확인 인증번호 [" + authCode + "]입니다.");

    	try {
    	  messageService.send(message);
    	  authStore.put(phone, authCode);
    	  return true;
    	} catch (Exception exception) {
    	  System.out.println(exception.getMessage());
    	  return false;
    	}

    }

    //인증번호 확인
    public boolean verifyCode(String phone, String code) {

        String savedCode = authStore.get(phone);

        if (savedCode != null && savedCode.equals(code)) {
//            authStore.remove(phone);
            return true;
        }
        return false;
    }
}