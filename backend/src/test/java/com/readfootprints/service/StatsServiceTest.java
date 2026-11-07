package com.readfootprints.service;

import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import com.readfootprints.model.Book;

import java.time.LocalDate;
import java.util.List;
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

    private static Book book(String id, Integer price, Integer words, boolean read, String since, String readDate) {
        Book b = new Book();
        b.id = id;
        b.title = id;
        b.price = price;
        b.wordCount = words;
        b.read = read;
        b.readingSince = since;
        b.readDate = readDate;
        b.reading = !read && since != null;
        return b;
    }

    @Test
    void wordPriceUsesBooksWithBoth() {
        var wp = StatsService.wordPrice(List.of(
                book("a", 300, 100_000, true, null, null),
                book("b", 400, 50_000, false, null, null),
                book("c", 0, 80_000, false, null, null),      // 金額 0 不算
                book("d", 250, null, false, null, null)));    // 沒字數不算
        assertEquals(2, wp.books());
        assertEquals(700, wp.totalPrice());
        assertEquals(150_000, wp.totalWords());
        assertEquals("a", wp.cheapest().id());
        assertEquals("b", wp.priciest().id());
        assertEquals(0, StatsService.wordPrice(List.of()).books());
    }

    @Test
    void speedSplitsMinutesWhenReadingTwoBooks() {
        // a：11/01～11/02 讀完 24,000 字；b：11/02～11/03 讀完 12,000 字；11/02 兩本同時在讀
        Map<String, Integer> logs = Map.of("2026-11-01", 60, "2026-11-02", 40, "2026-11-03", 20);
        var sp = StatsService.speed(List.of(
                book("a", null, 24_000, true, "2026-11-01", "2026-11-02"),
                book("b", null, 18_000, true, "2026-11-02", "2026-11-03")), logs, TODAY);
        // a 分到 60 + 20 = 80 分（300 字/分）；b 分到 20 + 20 = 40 分（450 字/分）
        assertEquals(2, sp.books());
        assertEquals(375.0, sp.medianWpm());
        assertEquals(42_000, sp.totalWords());
        assertEquals(120.0, sp.totalMinutes());
        assertEquals(4, sp.totalDays());
        var a = sp.details().stream().filter(d -> d.id().equals("a")).findFirst().orElseThrow();
        assertEquals(80.0, a.minutes());
        assertEquals(2, a.maxConcurrent());
        assertEquals(2, a.loggedDays());
    }

    @Test
    void speedSkipsLongMissingOrUnrealisticBooks() {
        Map<String, Integer> logs = Map.of("2026-10-01", 30, "2026-11-01", 1, "2026-10-10", 30);
        var sp = StatsService.speed(List.of(
                book("long", null, 90_000, true, "2026-09-01", "2026-10-15"),    // 超過 30 天
                book("nowords", null, null, true, "2026-10-01", "2026-10-01"),    // 沒字數
                book("fast", null, 90_000, true, "2026-11-01", "2026-11-01"),     // 1 分鐘讀 9 萬字
                book("gaps", null, 9_000, true, "2026-10-10", "2026-10-13"),      // 4 天只有 1 天有時數
                book("old", null, 50_000, true, null, "2026-01-01")), logs, TODAY); // 沒開書日期，不列
        assertEquals(0, sp.books());
        assertEquals(0.0, sp.medianWpm());
        Map<String, String> reasons = new java.util.HashMap<>();
        sp.details().forEach(d -> reasons.put(d.id(), d.reason()));
        assertEquals("超過 30 天", reasons.get("long"));
        assertEquals("沒有字數", reasons.get("nowords"));
        assertEquals("速度不合理", reasons.get("fast"));
        assertEquals("有填時數的天數不到一半", reasons.get("gaps"));
        assertEquals(4, reasons.size());
    }

    @Test
    void medianOfSpeeds() {
        assertEquals(0.0, StatsService.median(List.of()));
        assertEquals(300.0, StatsService.median(List.of(300.0)));
        assertEquals(400.0, StatsService.median(List.of(900.0, 300.0, 400.0)));
        assertEquals(350.0, StatsService.median(List.of(300.0, 400.0, 900.0, 200.0)));
    }

    @Test
    void backlogCountsUnfinishedBooks() {
        var bl = StatsService.backlog(List.of(
                book("a", null, 100_000, false, null, null),
                book("b", null, 50_000, false, "2026-11-01", null),
                book("c", null, null, false, null, null),
                book("d", null, 80_000, true, null, "2026-01-01")));
        assertEquals(2, bl.booksWithWords());
        assertEquals(150_000, bl.words());
        assertEquals(1, bl.booksWithoutWords());
    }
}
