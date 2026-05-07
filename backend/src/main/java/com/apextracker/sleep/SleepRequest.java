package com.apextracker.sleep;

import jakarta.validation.constraints.NotNull;

public record SleepRequest(
        @NotNull(message = "Date is required") String date,
        String bedTime,
        String sleepTime,
        @NotNull(message = "Wake up time is required") String wakeTime,
        Integer quality,
        String notes) {
}