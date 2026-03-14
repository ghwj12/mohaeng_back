package org.poolpool.mohaeng.ai.chat.controller;

import java.util.List;

import org.poolpool.mohaeng.ai.chat.service.AiEventSearchService;
import org.poolpool.mohaeng.event.list.dto.EventDto;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/ai/events")
@RequiredArgsConstructor
public class AiEventSearchController {

    private final AiEventSearchService aiEventSearchService;

    @GetMapping("/search")
    public ResponseEntity<List<EventDto>> search(
            @RequestParam(name = "question", required = false) String question,
            @RequestParam(name = "region", required = false) String region,
            @RequestParam(name = "eventStatus", required = false) String eventStatus,
            @RequestParam(name = "limit", required = false) Integer limit
    ) {
        return ResponseEntity.ok(aiEventSearchService.search(question, region, eventStatus, limit));
    }
}
