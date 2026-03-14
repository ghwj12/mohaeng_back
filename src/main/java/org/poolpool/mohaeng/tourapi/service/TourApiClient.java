package org.poolpool.mohaeng.tourapi.service;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.poolpool.mohaeng.tourapi.config.TourApiProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class TourApiClient {

    private final TourApiProperties properties;
    private final ObjectMapper objectMapper;

    private RestClient restClient() {
        return RestClient.builder()
                .baseUrl(properties.baseUrl())
                .build();
    }

    public List<Map<String, Object>> fetchFestivalPage(String eventStartDate, Integer areaCode, int pageNo, Integer numOfRows) {

        URI uri = UriComponentsBuilder
                .fromHttpUrl(properties.baseUrl() + "/searchFestival2")
                .queryParam("serviceKey", properties.serviceKey())
                .queryParam("MobileOS", StringUtils.hasText(properties.mobileOs()) ? properties.mobileOs() : "ETC")
                .queryParam("MobileApp", StringUtils.hasText(properties.mobileApp()) ? properties.mobileApp() : "MOHAENG")
                .queryParam("_type", "json")
                .queryParam("arrange", StringUtils.hasText(properties.arrange()) ? properties.arrange() : "A")
                .queryParam("eventStartDate", eventStartDate)
                .queryParam("numOfRows", numOfRows)
                .queryParam("pageNo", pageNo)
                .queryParamIfPresent("areaCode", areaCode == null ? java.util.Optional.empty() : java.util.Optional.of(areaCode))
                .build(false)
                .toUri();

        System.out.println("=== TOUR API REQUEST URL ===");
        System.out.println(uri);

        String body = restClient().get()
                .uri(uri)
                .exchange((request, response) -> {
                    String responseBody = new String(response.getBody().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);

                    System.out.println("=== TOUR API STATUS === " + response.getStatusCode());
                    System.out.println("=== TOUR API BODY ===");
                    System.out.println(responseBody);

                    if (response.getStatusCode().isError()) {
                        throw new RuntimeException("TourAPI HTTP 오류: " + response.getStatusCode() + " / " + responseBody);
                    }

                    return responseBody;
                });

        JsonNode root = readTree(body);
        validateHeader(root);

        JsonNode itemNode = root.path("response").path("body").path("items").path("item");
        if (itemNode.isMissingNode() || itemNode.isNull()) {
            return Collections.emptyList();
        }

        if (itemNode.isArray()) {
            List<Map<String, Object>> result = new ArrayList<>();
            for (JsonNode node : itemNode) {
                result.add(objectMapper.convertValue(node, new TypeReference<LinkedHashMap<String, Object>>() {}));
            }
            return result;
        }

        return List.of(objectMapper.convertValue(itemNode, new TypeReference<LinkedHashMap<String, Object>>() {}));
    }

    public Map<String, Object> fetchDetailCommon(String contentId) {
        String body = restClient().get()
                .uri(UriComponentsBuilder.fromPath("/detailCommon2")
                        .queryParam("serviceKey", properties.serviceKey())
                        .queryParam("MobileOS", StringUtils.hasText(properties.mobileOs()) ? properties.mobileOs() : "ETC")
                        .queryParam("MobileApp", StringUtils.hasText(properties.mobileApp()) ? properties.mobileApp() : "MOHAENG")
                        .queryParam("_type", "json")
                        .queryParam("contentId", contentId)
                        .queryParam("contentTypeId", 15)
                        .queryParam("defaultYN", "Y")
                        .queryParam("firstImageYN", "Y")
                        .queryParam("addrinfoYN", "Y")
                        .queryParam("overviewYN", "Y")
                        .build()
                        .encode()
                        .toUri())
                .retrieve()
                .body(String.class);

        JsonNode root = readTree(body);
        validateHeader(root);

        JsonNode itemNode = root.path("response").path("body").path("items").path("item");
        if (itemNode.isMissingNode() || itemNode.isNull()) {
            return Collections.emptyMap();
        }

        if (itemNode.isArray() && itemNode.size() > 0) {
            return objectMapper.convertValue(itemNode.get(0), new TypeReference<LinkedHashMap<String, Object>>() {});
        }
        return objectMapper.convertValue(itemNode, new TypeReference<LinkedHashMap<String, Object>>() {});
    }

    private JsonNode readTree(String body) {
        try {
            return objectMapper.readTree(body);
        } catch (Exception e) {
            throw new RuntimeException("TourAPI 응답 파싱 실패: " + body, e);
        }
    }

    private void validateHeader(JsonNode root) {
        JsonNode header = root.path("response").path("header");
        String resultCode = header.path("resultCode").asText();
        if (!"0000".equals(resultCode)) {
            String resultMsg = header.path("resultMsg").asText();
            throw new RuntimeException("TourAPI 호출 실패: " + resultCode + " / " + resultMsg);
        }
    }
}
