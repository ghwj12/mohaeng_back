package org.poolpool.mohaeng.tourapi.dto;

import java.util.ArrayList;
import java.util.List;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TourFestivalImportResponse {
    private int requestedPages;
    private int fetchedItems;
    private int createdCount;
    private int skippedCount;
    @Builder.Default
    private List<String> createdTitles = new ArrayList<>();
    @Builder.Default
    private List<String> skippedReasons = new ArrayList<>();
}
