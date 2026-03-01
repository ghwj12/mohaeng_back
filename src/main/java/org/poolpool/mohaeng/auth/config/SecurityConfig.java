package org.poolpool.mohaeng.auth.config;

import org.poolpool.mohaeng.auth.security.authorization.EndpointPolicy;
import org.poolpool.mohaeng.auth.security.filter.JwtAuthenticationFilter;
import org.poolpool.mohaeng.auth.security.filter.JwtExceptionFilter;
import org.poolpool.mohaeng.auth.security.handler.CustomAuthenticationSuccessHandler;
import org.poolpool.mohaeng.auth.security.handler.RestAccessDeniedHandler;
import org.poolpool.mohaeng.auth.security.handler.RestAuthenticationEntryPoint;
import org.poolpool.mohaeng.auth.service.CustomOAuth2UserService;
import org.poolpool.mohaeng.auth.token.jwt.JwtProperties;
import org.poolpool.mohaeng.auth.token.jwt.JwtTokenProvider;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import jakarta.servlet.DispatcherType;
import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSecurity
@EnableConfigurationProperties(JwtProperties.class)
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtTokenProvider jwt;
    private final CustomOAuth2UserService customOAuth2UserService;
    private final CustomAuthenticationSuccessHandler customAuthenticationSuccessHandler;

    /**
     * AuthenticationManager Bean 등록 (핵심!)
     */
    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration configuration
    ) throws Exception {
        return configuration.getAuthenticationManager();
    }

    /**
     * DaoAuthenticationProvider 등록
     * - CustomUserDetailsService + PasswordEncoder 연결
     */
    @Bean
    public DaoAuthenticationProvider daoAuthenticationProvider(
            UserDetailsService userDetailsService,
            PasswordEncoder passwordEncoder
    ) {
        DaoAuthenticationProvider provider =
                new DaoAuthenticationProvider(userDetailsService); 
        provider.setPasswordEncoder(passwordEncoder);
        
      //기본 설정에서 UsernameNotFoundException이 BadCredentialsException으로 변환되는 것을 막음
        provider.setHideUserNotFoundExceptions(false);
        
        return provider;
    }

    /**
     * JWT 필터 빈
     */
    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter() {
        return new JwtAuthenticationFilter(jwt);
    }

    @Bean
    public JwtExceptionFilter jwtExceptionFilter() {
        return new JwtExceptionFilter();
    }

    /**
     * SecurityFilterChain
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http.csrf(csrf -> csrf.disable())
        	.cors(Customizer.withDefaults())
            .formLogin(form -> form.disable())
            .httpBasic(basic -> basic.disable())

            .authorizeHttpRequests(auth -> auth
                // React static + SPA entry (절대 "/**" permitAll 금지)
            	.dispatcherTypeMatchers(DispatcherType.FORWARD, DispatcherType.ERROR).permitAll()
                .requestMatchers(HttpMethod.GET,
                        "/", "/index.html",
                        "/favicon.ico", "/manifest.json", "/robots.txt",
                        "/assets/**", "/static/**",
                        "/*.js", "/*.css", "/*.map",
                        "/*.png", "/*.jpg", "/*.jpeg", "/*.gif", "/*.svg", "/*.webp", "/*.ico",
                        "/error"
                ).permitAll()
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                // auth endpoints
                .requestMatchers("/auth/**").permitAll()
                
                .requestMatchers(HttpMethod.GET, "/api/eventParticipation/check/**").permitAll()
                
                // 업로드 파일 접근 권한
                .requestMatchers("/upload_files/**").permitAll()

                // PUBLIC GET
                .requestMatchers(HttpMethod.GET, EndpointPolicy.PUBLIC_GET).permitAll()

                // PUBLIC POST
                .requestMatchers(HttpMethod.POST, EndpointPolicy.PUBLIC_POST).permitAll()

                // ADMIN only
                .requestMatchers(HttpMethod.POST, EndpointPolicy.ADMIN_PAGE).hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT, EndpointPolicy.ADMIN_PAGE).hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, EndpointPolicy.ADMIN_PAGE).hasRole("ADMIN")
                .requestMatchers(HttpMethod.PATCH, EndpointPolicy.ADMIN_PAGE).hasRole("ADMIN")
                
                // Payment endpoints
                .requestMatchers("/api/payment/**").authenticated()
                
                // USER only
                .requestMatchers(HttpMethod.POST, EndpointPolicy.USER_PAGE).hasRole("USER")
                .requestMatchers(HttpMethod.PUT, EndpointPolicy.USER_PAGE).hasRole("USER")
                .requestMatchers(HttpMethod.DELETE, EndpointPolicy.USER_PAGE).hasRole("USER")
                .requestMatchers(HttpMethod.PATCH, EndpointPolicy.USER_PAGE).hasRole("USER")

                // USER or ADMIN
                .requestMatchers(HttpMethod.POST, EndpointPolicy.SERVICE_PAGE).hasAnyRole("USER","ADMIN")
                .requestMatchers(HttpMethod.PUT, EndpointPolicy.SERVICE_PAGE).hasAnyRole("USER","ADMIN")
                .requestMatchers(HttpMethod.DELETE, EndpointPolicy.SERVICE_PAGE).hasAnyRole("USER","ADMIN")
                .requestMatchers(HttpMethod.PATCH, EndpointPolicy.SERVICE_PAGE).hasAnyRole("USER","ADMIN")

                .anyRequest().authenticated()
            )

            // 소셜 로그인
            .oauth2Login(oauth -> oauth
                .userInfoEndpoint(userInfo -> userInfo
                    .oidcUserService(customOAuth2UserService)
                )
                .successHandler(customAuthenticationSuccessHandler)
            )

            // 401/403 JSON 처리
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint(new RestAuthenticationEntryPoint())
                .accessDeniedHandler(new RestAccessDeniedHandler())
            )

            // 필터 순서: ExceptionFilter → AuthenticationFilter → UsernamePasswordAuthenticationFilter
            .addFilterBefore(jwtExceptionFilter(), UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class)

            // /auth/logout
//            .logout(logout -> logout
//                .logoutUrl("/auth/logout")
//                .logoutSuccessHandler(new CustomLogoutSuccessHandler())
//            );
        	.logout(AbstractHttpConfigurer::disable);
        

        return http.build();
    }
    
    @Bean
    public org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer webSecurityCustomizer() {
        return (web) -> web.ignoring()
                .requestMatchers(org.springframework.boot.autoconfigure.security.servlet.PathRequest.toStaticResources().atCommonLocations())
                .requestMatchers("/favicon.ico", "/manifest.json", "/*.png", "/error"); // 💡 여기도 error 추가
    }
}
