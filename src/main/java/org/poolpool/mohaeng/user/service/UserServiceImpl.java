package org.poolpool.mohaeng.user.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.poolpool.mohaeng.auth.dto.response.TokenResponse;
import org.poolpool.mohaeng.auth.token.jwt.JwtProperties;
import org.poolpool.mohaeng.auth.token.jwt.JwtTokenProvider;
import org.poolpool.mohaeng.auth.token.refresh.repository.RefreshTokenRepository;
import org.poolpool.mohaeng.auth.token.refresh.service.RefreshTokenService;
import org.poolpool.mohaeng.common.service.MailService;
import org.poolpool.mohaeng.storage.s3.S3StorageService;
import org.poolpool.mohaeng.user.dto.SocialUserDto;
import org.poolpool.mohaeng.user.dto.UserDto;
import org.poolpool.mohaeng.user.entity.SocialUserEntity;
import org.poolpool.mohaeng.user.entity.UserEntity;
import org.poolpool.mohaeng.user.repository.SocialUserRepository;
import org.poolpool.mohaeng.user.repository.UserRepository;
import org.poolpool.mohaeng.user.type.SignupType;
import org.poolpool.mohaeng.user.type.UserRole;
import org.poolpool.mohaeng.user.type.UserStatus;
import org.poolpool.mohaeng.user.type.UserType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService{

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final S3StorageService s3StorageService;
	private final SocialUserRepository socialUserRepository;
	private final RefreshTokenRepository refreshTokenRepository;
	private final MailService mailService;
	private final JwtTokenProvider jwtTokenProvider;
    private final JwtProperties jwtProperties;
    private final RefreshTokenService refreshTokenService;

	//이메일 중복 확인
	@Override
	public int existsByEmail(String email) {
		
		Optional<UserEntity> existUser = userRepository.findByEmail(email);
		if (existUser.isPresent()) {
			if(existUser.get().getUserStatus() != UserStatus.WITHDRAWAL) return 1;
		}
		return 0;
	}

	//일반 회원가입(개인/업체)
	@Override
	@Transactional
	public int insertUser(UserDto user) {
		
	
		//사용자가 입력한 평문 비밀번호
	    String rawPassword = user.getUserPwd();

	    //BCrypt로 암호화
	    String encodedPassword = passwordEncoder.encode(rawPassword);

	    //DTO에 암호화된 비밀번호로 다시 세팅
	    user.setUserPwd(encodedPassword);
	    
	    //Entity 변환 후 저장	    
		return userRepository.save(user.toEntity()) != null ? 1 : 0;
	    
	}

	//이메일 찾기
	@Override
	public List<UserDto> findAllByNameAndPhone(String name, String phone) {
        List<UserEntity> users = userRepository.findAllByNameAndPhone(name, phone);
        return users.stream()
                    .map(UserDto::fromEntity)
                    .toList();
    }
	
	//비밀번호 찾기
	@Override
	public UserDto findByEmailAndPhone(String email, String phone) {
		return UserDto.fromEntity(userRepository.findByEmailAndPhone(email, phone));
	}
	
	//비밀번호 재설정 안내 메일 전송
	@Override
	public void sendRenewPwd(String email, String renewPwd) {
		String subject = "[모행] 비밀번호 재설정 안내";

        String content = """
                <div style="font-family: Arial; line-height:1.6;">
                <p>안녕하세요, <b>모행</b> 입니다.</p>

                <p>
                회원님의 요청에 따라 비밀번호 재설정을 위한 안내 메일을 보내드립니다.<br>
                회원님의 비밀번호가 아래와 같이 변경되었습니다.
                </p>

                <p style="font-size:18px;">
                <b>[%s]</b>
                </p>

                <p>
                로그인 후 반드시 비밀번호를 재설정하시길 바랍니다.
                </p>

                <br>
                <p style="font-size:12px; color:gray;">
                본 메일은 발신 전용으로, 회신이 불가한 점 양해 부탁드립니다.
                </p>

                <p>감사합니다.<br>모행 드림</p>
            </div>
            """.formatted(renewPwd);

        mailService.sendMail(email, subject, content);
		
	}
	
	//랜덤 비밀번호 업데이트
	@Override
	@Transactional
	public void updateRenewPwd(Long userId, String email, String renewPwd) {
		//BCrypt로 암호화
	    String encodedPassword = passwordEncoder.encode(renewPwd);
		
		userRepository.updateRenewPwd(userId, email, encodedPassword, LocalDateTime.now());
	}

	//개인정보 조회
	@Override
	public UserDto findById(Long userId) {
		return UserDto.fromEntity(
		        userRepository.findById(userId)
		                .orElseThrow(() -> new IllegalArgumentException("회원이 존재하지 않습니다."))
		);
	}

	//개인정보 수정
	@Override
	@Transactional
	public void patchUser(UserDto user, boolean deletePhoto, MultipartFile photo) {
		UserEntity updateUser = userRepository.findById(user.getUserId())
	            .orElseThrow(() -> new IllegalArgumentException("수정할 회원을 찾을 수 없습니다."));

	    // 비밀번호 변경
	    if (user.getUserPwd() != null && !user.getUserPwd().isBlank()) {
	    	updateUser.setUserPwd(passwordEncoder.encode(user.getUserPwd()));
	    }

	    // 프로필 사진 처리(S3)
	    boolean hasNewPhoto = (photo != null && !photo.isEmpty());
	    if (deletePhoto || hasNewPhoto) {
	        if (updateUser.getProfileImg() != null) {
	            s3StorageService.delete("photo", updateUser.getProfileImg());
	        }
	        updateUser.setProfileImg(null);
	    }

	    if (hasNewPhoto) {
	        try {
	            String savedName = s3StorageService.upload(photo, "photo");
	            updateUser.setProfileImg(savedName);
	        } catch (Exception e) {
	            throw new RuntimeException("사진 업로드 실패", e);
	        }
	    }

	    //전화번호 변경
	    if (user.getPhone() != null) updateUser.setPhone(user.getPhone());
		
	}

	//회원 탈퇴
	@Override
	@Transactional
	public void patchWithdrawal(UserDto user) {
		Long userId = user.getUserId();
		
		boolean socialYn = socialUserRepository.existsByUser_UserId(userId);
		if(socialYn) socialUserRepository.deleteByUser_UserId(userId);
		
		UserEntity updateUser = userRepository.findById(userId)
	            .orElseThrow(() -> new IllegalArgumentException("회원 없음"));
		
		updateUser.setEmail(updateUser.getEmail() + "#withdrawal" + userId);
		updateUser.setUserStatus(UserStatus.WITHDRAWAL);
		updateUser.setWithReasonId(user.getWithReasonId());
		updateUser.setWithdrawalReason(user.getWithdrawalReason());
		
		refreshTokenRepository.deleteByUserId(userId);
		
	}

	//소셜 회원가입
	@Override
	public TokenResponse socialSignup(SocialUserDto socialUserDto) {

		UserEntity user = userRepository.save(
                UserEntity.builder()
                        .email(socialUserDto.getEmail())
                        .name(socialUserDto.getName())
                        .phone(socialUserDto.getPhone())
                        .signupType(SignupType.GOOGLE)
                        .userType(UserType.PERSONAL)
                        .userRole(UserRole.USER)
                        .userStatus(UserStatus.ACTIVE)
                        .build()
        );

        socialUserRepository.save(
                SocialUserEntity.builder()
                        .user(user)
                        .provider(socialUserDto.getProvider())
                        .providerId(socialUserDto.getProviderId())
                        .build()
        );
        
        UserEntity createdUser = userRepository.findByEmail(socialUserDto.getEmail())
    	        .orElseThrow(() -> new BadCredentialsException("아이디 또는 비밀번호가 올바르지 않습니다."));
        Long userId = createdUser.getUserId();
        
        String access = jwtTokenProvider.createAccessToken(userId,  "ROLE_" + user.getUserRole(), user.getName());
        String refresh = jwtTokenProvider.createRefreshToken(userId,  "ROLE_" + user.getUserRole());

        LocalDateTime now = LocalDateTime.now();
        refreshTokenService.upsert(userId, refresh, now, now.plusDays(1));
        
        return new TokenResponse(access, refresh, jwtProperties.accessExp(), jwtProperties.refreshExp());
	}
}
