package com.readfootprints.service;

import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class StatsServiceTest {

    private static final LocalDate TODAY = LocalDate.parse("2026-11-06");

    @Test
    void streakCountsBackFromYesterday() {
        Map<String, Integer> logs = Map.of(
                "2026-11-06", 5,   // 今天不算
                "2026-11-05", 20,
                "2026-11-04", 45,
                "2026-11-03", 19,  // 不足 20 分鐘，中斷
                "2026-11-02", 60);
        assertEquals(2, StatsService.streak(logs, TODAY));
    }

    @Test
    void streakIsZeroWhenYesterdayMissing() {
        assertEquals(0, StatsService.streak(Map.of("2026-11-04", 30), TODAY));
        assertEquals(0, StatsService.streak(Map.of(), TODAY));
    }

    @Test
    void streakCrossesMonthAndYear() {
        assertEquals(3, StatsService.streak(Map.of("2025-12-31", 20, "2025-12-30", 20, "2026-01-01", 30),
                LocalDate.parse("2026-01-02")));
    }

    @Test
    void logValidation() {
        assertEquals(30, ReadingLogService.minutes(30));
        assertThrows(ResponseStatusException.class, () -> ReadingLogService.minutes(0));
        assertThrows(ResponseStatusException.class, () -> ReadingLogService.minutes(1441));
        assertThrows(ResponseStatusException.class, () -> ReadingLogService.minutes(null));
        assertThrows(ResponseStatusException.class, () -> ReadingLogService.date("2026-02-30"));
        assertThrows(ResponseStatusException.class, () -> ReadingLogService.date(null));
    }
}
