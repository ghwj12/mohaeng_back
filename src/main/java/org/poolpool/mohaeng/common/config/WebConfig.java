package org.poolpool.mohaeng.common.config;

import org.poolpool.mohaeng.common.Interceptor.RequestLoggingInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(UploadProperties.class)
public class WebConfig implements WebMvcConfigurer {

	private final UploadProperties uploadProperties;
	@Autowired
    private RequestLoggingInterceptor interceptor;

	@Override
	public void addResourceHandlers(ResourceHandlerRegistry registry) {
	    System.out.println(">>> 현재 업로드 경로: " + uploadProperties.uploadDir()); 
	    
	    registry.addResourceHandler("/upload_files/**")
	            .addResourceLocations("file:" + uploadProperties.uploadDir() + "/");
	    
	    //React static 매핑 추가
	    registry.addResourceHandler("/**")
	            .addResourceLocations("classpath:/static/");
	}

    // 프론트엔드(React)에서 오는 요청을 허락해 주는 설정
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**") // 백엔드의 모든 API 주소에 대해
                .allowedOrigins("http://localhost:3000", "http://3.35.16.158:8080") // 리액트의 접근 허용
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS") // 허용할 방식들
                .allowedHeaders("*") // 모든 헤더 허용
                .allowCredentials(true); // 인증 정보(토큰, 쿠키 등) 포함 허용
    }
    
    // 요청 들어온 URL 콘솔 출력
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(interceptor);
    }
}