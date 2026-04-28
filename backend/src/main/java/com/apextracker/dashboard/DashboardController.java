package com.apextracker.dashboard;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.apextracker.config.CurrentUserService;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;
    private final CurrentUserService currentUser;

    public DashboardController(DashboardService dashboardService, CurrentUserService currentUser) {
        this.dashboardService = dashboardService;
        this.currentUser = currentUser;
    }

    @GetMapping
    public Map<String, Object> summary() {
        return dashboardService.summary(currentUser.get());
    }

    @GetMapping("/plan-vs-actual")
    public Map<String, Object> planVsActual(@RequestParam(required = false) String date) {
        return dashboardService.planVsActual(currentUser.get(), date);
    }
}