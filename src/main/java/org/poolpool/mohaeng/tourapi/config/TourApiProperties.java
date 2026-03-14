package org.poolpool.mohaeng.tourapi.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "tour.api")
public record TourApiProperties(
        String serviceKey,
        String baseUrl,
        String mobileOs,
        String mobileApp,
        String arrange,
        Integer defaultNumOfRows
) {
}
