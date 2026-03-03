package org.poolpool.mohaeng.event.mypage.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BoothApplicationFileResponse {
    private String originalFileName;
    private String renameFileName;
    private Integer sortOrder;
}
