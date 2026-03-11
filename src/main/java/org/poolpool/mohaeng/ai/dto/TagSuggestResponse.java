package org.poolpool.mohaeng.ai.dto;

import lombok.Getter;
import lombok.Setter;
import java.util.List;

@Getter
@Setter
public class TagSuggestResponse {
    private Integer categoryId;
    private List<Integer> topicIds;
    private List<String> hashtagNames;
    private String simpleExplain;
}
