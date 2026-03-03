package org.poolpool.mohaeng.event.participation.dto;

import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class BoothApplyRequestDto {

    private Long    hostBoothId;
    private String  homepageUrl;
    private String  boothTitle;
    private String  boothTopic;
    private String  mainItems;
    private String  description;
    private Integer boothCount;
    private Integer boothPrice;
    private Integer facilityPrice;
    private Integer totalPrice;
    private List<FacilityItem> facilities;

    @Getter
    @NoArgsConstructor
    public static class FacilityItem {
        private Long    hostBoothFaciId;
        private Integer faciCount;
        private Integer faciPrice;
    }
}
