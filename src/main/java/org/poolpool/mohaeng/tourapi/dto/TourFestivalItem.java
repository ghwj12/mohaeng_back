package org.poolpool.mohaeng.tourapi.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TourFestivalItem {
    private String contentId;
    private String contentTypeId;
    private String title;
    private String addr1;
    private String addr2;
    private String firstImage;
    private String firstImage2;
    private String overview;
    private String eventStartDate;
    private String eventEndDate;
    private String areaCode;
    private String sigunguCode;
    private String tel;
    private String mapX;
    private String mapY;
}
