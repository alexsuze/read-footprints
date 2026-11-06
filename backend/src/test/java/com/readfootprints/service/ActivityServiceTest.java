package com.readfootprints.service;

import com.readfootprints.service.ActivityService.ActivityForm;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ActivityServiceTest {

    private static ActivityForm marathon(String start, String end, Integer goal, List<Integer> stages) {
        return new ActivityForm("marathon", "十一月馬拉松", start, end, "https://example.com", "#7A8C5C", goal, 0, stages);
    }

    private static void rejected(ActivityForm f) {
        assertThrows(ResponseStatusException.class, () -> ActivityService.validate(f));
    }

    @Test
    void acceptsValidActivities() {
        assertDoesNotThrow(() -> ActivityService.validate(marathon("2026-11-01", "2026-11-01", 900, List.of(300, 600, 900))));
        assertDoesNotThrow(() -> ActivityService.validate(
                new ActivityForm("quiz", "問答", "2026-11-01", "2026-11-02", "", "#5B6B7A", null, null, null)));
    }

    @Test
    void rejectsBadDates() {
        rejected(marathon("2026-11-02", "2026-11-01", 900, List.of(900)));
        rejected(marathon("2026-13-01", "2026-13-02", 900, List.of(900)));
        rejected(marathon(null, "2026-11-01", 900, List.of(900)));
    }

    @Test
    void stagesMustAscendAndEndAtGoal() {
        rejected(marathon("2026-11-01", "2026-11-02", 900, List.of(600, 300, 900)));
        rejected(marathon("2026-11-01", "2026-11-02", 900, List.of(300, 600)));
        rejected(marathon("2026-11-01", "2026-11-02", 900, List.of(100, 200, 300, 900)));
        rejected(marathon("2026-11-01", "2026-11-02", 0, List.of(0)));
    }

    @Test
    void rejectsUnsafeUrlAndBadColor() {
        rejected(new ActivityForm("quiz", "問答", "2026-11-01", "2026-11-02", "javascript:alert(1)", "#5B6B7A", null, null, null));
        rejected(new ActivityForm("quiz", "問答", "2026-11-01", "2026-11-02", "", "red", null, null, null));
    }
}
