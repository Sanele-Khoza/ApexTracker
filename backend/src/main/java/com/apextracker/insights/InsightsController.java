package com.apextracker.insights;

import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.apextracker.config.CurrentUserService;

@RestController
@RequestMapping("/api/insights")
public class InsightsController {

    private final InsightsService insightsService;
    private final CurrentUserService currentUser;

    public InsightsController(InsightsService insightsService, CurrentUserService currentUser) {
        this.insightsService = insightsService;
        this.currentUser = currentUser;
    }

    @GetMapping
    public List<Map<String, Object>> list() {
        return insightsService.insights(currentUser.get());
    }
}