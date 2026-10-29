package com.copick.recommendations.controller;

import com.copick.algorithm.dto.ScoredMovie;
import com.copick.recommendations.service.RecommendationService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/groups")
public class RecommendationController {

    private final RecommendationService recommendationService;

    public RecommendationController(RecommendationService recommendationService) {
        this.recommendationService = recommendationService;
    }

    @GetMapping("/{id}/recommendations")
    public List<ScoredMovie> getRecommendations(
            @PathVariable("id") int groupId,
            @RequestParam(required = false) List<Integer> profileIds,
            @RequestParam(defaultValue = "10") int limit
    ) {
        return recommendationService.getRecommendations(groupId, profileIds, limit);
    }
}