package org.poolpool.mohaeng.tourapi.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TourFestivalImportRequest {
    private String startDate;
    private String endDate;
    private Integer areaCode;
    private Integer numOfRows;
    private Integer maxPages;
}
