package com.apextracker.review;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;

import org.springframework.web.bind.annotation.*;

import com.apextracker.common.ApiException;
import com.apextracker.config.CurrentUserService;
import com.apextracker.user.User;

@RestController
@RequestMapping("/api/reviews")
public class DailyReviewController {

    private final DailyReviewService reviewService;
    private final CurrentUserService currentUser;

    public DailyReviewController(DailyReviewService reviewService, CurrentUserService currentUser) {
        this.reviewService = reviewService;
        this.currentUser = currentUser;
    }

    @GetMapping
    public List<DailyReviewResponse> list() {
        return reviewService.list(currentUser.get()).stream().map(DailyReviewResponse::from).toList();
    }

    @GetMapping("/{date}")
    public DailyReviewResponse get(@PathVariable String date) {
        LocalDate day = parseDate(date);
        return DailyReviewResponse.from(reviewService.getOrGenerate(currentUser.get(), day));
    }

    @PostMapping("/{date}/reflection")
    public DailyReviewResponse updateReflection(@PathVariable String date, @RequestBody ReflectionRequest req) {
        LocalDate day = parseDate(date);
        User user = currentUser.get();
        return DailyReviewResponse.from(reviewService.updateReflection(user, day, req.reflection()));
    }

    private LocalDate parseDate(String date) {
        try {
            return LocalDate.parse(date);
        } catch (DateTimeParseException e) {
            throw ApiException.badRequest("Date must be in YYYY-MM-DD format");
        }
    }

    public record ReflectionRequest(String reflection) {
    }
}