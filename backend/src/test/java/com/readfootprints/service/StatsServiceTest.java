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
    void excludedBooksDoNotDiluteMinutes() {
        // a：10/01～10/04 讀完 72,000 字，每天 60 分；long：08/01～10/04（超過 30 天，先排除）
        Map<String, Integer> logs = Map.of("2026-10-01", 60, "2026-10-02", 60, "2026-10-03", 60, "2026-10-04", 60);
        var sp = StatsService.speed(List.of(
                book("a", null, 72_000, true, "2026-10-01", "2026-10-04"),
                book("long", null, 150_000, true, "2026-08-01", "2026-10-04"),
                book("nowords", null, null, true, "2026-10-02", "2026-10-03")), logs, TODAY);
        // 沒有先排除的話 a 只分到 30 + 20 + 20 + 30 = 100 分（720 字/分）；現在獨得 240 分（300 字/分）
        assertEquals(1, sp.books());
        assertEquals(300.0, sp.medianWpm());
        var a = sp.details().stream().filter(d -> d.id().equals("a")).findFirst().orElseThrow();
        assertEquals(240.0, a.minutes());
        assertEquals(1, a.maxConcurrent());
    }

    @Test
    void unrealisticBookIsRemovedThenMinutesReallocated() {
        // a、b 都是 10/01～10/02，每天 60 分。b 字數 300,000：分到 60 分 = 5,000 字/分，排除後 a 獨得 120 分
        Map<String, Integer> logs = Map.of("2026-10-01", 60, "2026-10-02", 60);
        var sp = StatsService.speed(List.of(
                book("a", null, 36_000, true, "2026-10-01", "2026-10-02"),
                book("b", null, 300_000, true, "2026-10-01", "2026-10-02")), logs, TODAY);
        assertEquals(1, sp.books());
        assertEquals(300.0, sp.medianWpm());
        var b = sp.details().stream().filter(d -> d.id().equals("b")).findFirst().orElseThrow();
        assertEquals("速度不合理", b.reason());
    }

    @Test
    void readingBooksShareMinutesAndAppearInDetails() {
        // a：11/01～11/02 讀完 18,000 字；r：11/02 開書、閱讀中（到今天 11/06）；每天 60 分
        Map<String, Integer> logs = Map.of("2026-11-01", 60, "2026-11-02", 60, "2026-11-03", 60,
                "2026-11-04", 60, "2026-11-05", 60, "2026-11-06", 60);
        var sp = StatsService.speed(List.of(
                book("a", null, 18_000, true, "2026-11-01", "2026-11-02"),
                book("r", null, 80_000, false, "2026-11-02", null)), logs, TODAY);
        // a 分到 60 + 30 = 90 分 → 200 字/分；r 只參與分配
        assertEquals(1, sp.books());
        assertEquals(200.0, sp.medianWpm());
        var r = sp.details().stream().filter(d -> d.id().equals("r")).findFirst().orElseThrow();
        assertEquals(true, r.reading());
        assertEquals(false, r.included());
        assertEquals(null, r.reason());
        assertEquals(null, r.wpm());
        assertEquals("2026-11-06", r.to());
        assertEquals(270.0, r.minutes()); // 30 + 60 × 4
    }

    private static Book withCat(Book b, String cat, String... authors) {
        b.categoryId = cat;
        b.authors = List.of(authors);
        return b;
    }

    @Test
    void categorySpeedsAverageIncludedBooks() {
        var details = List.of(
                new StatsService.SpeedBook("n1", "", "", "2026-10-01", 1, 1, 1, 1, 1, 600.0, true, null, false),
                new StatsService.SpeedBook("n2", "", "", "2026-10-02", 1, 1, 1, 1, 1, 400.0, true, null, false),
                new StatsService.SpeedBook("h1", "", "", "2026-10-03", 1, 1, 1, 1, 1, 200.0, true, null, false),
                new StatsService.SpeedBook("h2", "", "", "2026-10-04", 1, 1, 1, 1, 1, 300.0, true, null, false),
                new StatsService.SpeedBook("h3", "", "", "2026-10-05", 1, 1, 1, 1, 1, 900.0, false, "速度不合理", false),
                new StatsService.SpeedBook("s1", "", "", "2026-10-06", 1, 1, 1, 1, 1, 350.0, true, null, false),
                new StatsService.SpeedBook("u1", "", "", "2026-10-07", 1, 1, 1, 1, 1, 999.0, true, null, false));
        var speed = new StatsService.Speed(5, 350, 0, 0, 0, details);
        var books = List.of(withCat(book("n1", null, 1, true, null, null), "nov"), withCat(book("n2", null, 1, true, null, null), "nov"),
                withCat(book("h1", null, 1, true, null, null), "his"), withCat(book("h2", null, 1, true, null, null), "his"),
                withCat(book("h3", null, 1, true, null, null), "his"), withCat(book("s1", null, 1, true, null, null), "sci"),
                withCat(book("u1", null, 1, true, null, null), null));
        var result = StatsService.categorySpeeds(books, speed, Map.of("nov", "小說", "his", "歷史", "sci", "科普"));
        // 科普只有 1 本不排名；未分類不排名；歷史的 h3 沒採計
        assertEquals(2, result.size());
        assertEquals("小說", result.get(0).name());
        assertEquals(500.0, result.get(0).avgWpm());
        assertEquals("歷史", result.get(1).name());
        assertEquals(250.0, result.get(1).avgWpm());
        assertEquals(2, result.get(1).books());
    }

    @Test
    void wordRichSumsByAuthor() {
        var ranking = StatsService.wordRichRanking(List.of(
                withCat(book("a", null, 100_000, true, null, null), null, "東野圭吾"),
                withCat(book("b", null, 120_000, false, null, null), null, "東野圭吾"),
                withCat(book("c", null, 150_000, true, null, null), null, "村上春樹", "東野圭吾"), // 合著兩人都算
                withCat(book("d", null, 200_000, true, null, null), null, "村上春樹"),
                withCat(book("e", null, null, true, null, null), null, "村上春樹")));               // 沒字數不算
        assertEquals(2, ranking.size());
        var rich = ranking.getFirst();
        assertEquals("東野圭吾", rich.author());
        assertEquals(370_000, rich.words());
        assertEquals(3, rich.books());
        assertEquals("村上春樹", ranking.get(1).author());
        assertEquals(350_000, ranking.get(1).words());
        assertEquals(List.of(), StatsService.wordRichRanking(List.of()));
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
