package org.poolpool.mohaeng.user.service;

import java.util.List;

import org.poolpool.mohaeng.auth.dto.response.TokenResponse;
import org.poolpool.mohaeng.user.dto.SocialUserDto;
import org.poolpool.mohaeng.user.dto.UserDto;
import org.springframework.web.multipart.MultipartFile;

public interface UserService {

	//이메일 중복 확인
	int existsByEmail(String email);
	
	//일반 회원가입(개인/업체)
	int insertUser(UserDto user);

	//이메일 찾기
	List<UserDto> findAllByNameAndPhone(String name, String phone);
	
	//비밀번호 찾기
	UserDto findByEmailAndPhone(String email, String phone);
	
	//비밀번호 재설정 안내 메일 전송
	void sendRenewPwd(String email, String renewPwd);
	
	//랜덤 비밀번호 업데이트
	void updateRenewPwd(Long userId, String email, String renewPwd);

	//개인정보 조회
	UserDto findById(Long userId);

	//개인정보 수정
	void patchUser(UserDto user, boolean deletePhoto, MultipartFile photo);

	//회원 탈퇴
	void patchWithdrawal(UserDto user);

	//소셜 회원가입
	TokenResponse socialSignup(SocialUserDto socialUserDto);
	
	
}
