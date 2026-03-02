package org.poolpool.mohaeng.auth.service;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.poolpool.mohaeng.user.entity.UserEntity;
import org.poolpool.mohaeng.user.repository.UserRepository;
import org.poolpool.mohaeng.user.type.SignupType;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends OidcUserService {

    private final UserRepository userRepository;

    @Override
    public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
        OidcUser oauth2User = super.loadUser(userRequest);
        
        boolean isNewUser = false;
        String provider = userRequest.getClientRegistration().getRegistrationId().toUpperCase();
        String providerId = oauth2User.getAttribute("sub");
        String email = oauth2User.getAttribute("email");
        String name = oauth2User.getAttribute("name");

        // claims에 소셜 정보 저장 (신규회원 처리용)
        Map<String, Object> claims = new HashMap<>(oauth2User.getClaims());
        claims.put("isNewUser", isNewUser);
        claims.put("provider", provider);
        claims.put("providerId", providerId);
        claims.put("oauthEmail", email);
        claims.put("oauthName", name);

        Optional<UserEntity> optionalUser = userRepository.findByEmail(email);
        if (optionalUser.isPresent()) {
            UserEntity existingUser = optionalUser.get();
            if (existingUser.getSignupType() == SignupType.BASIC) {
                // 기존 일반 회원: 소셜 연동 불가
                throw new OAuth2AuthenticationException(new OAuth2Error("social_link_error", "해당 이메일은 일반 회원가입으로 가입된 계정으로, 소셜 로그인으로 연동할 수 없습니다.", null));
            }
            // 소셜 연결된 기존 회원(isNewUser=false)
            return new DefaultOidcUser(oauth2User.getAuthorities(), oauth2User.getIdToken(), new OidcUserInfo(claims));
        } else {
            // 신규 회원: DB 저장 안하고 isNewUser=true 설정
            isNewUser = true;
            claims.put("isNewUser", true);
            return new DefaultOidcUser(oauth2User.getAuthorities(), oauth2User.getIdToken(), new OidcUserInfo(claims));
        }
    }
}
