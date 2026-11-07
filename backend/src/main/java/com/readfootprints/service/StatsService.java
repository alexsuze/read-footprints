package com.readfootprints.service;

import com.readfootprints.model.Book;
import com.readfootprints.model.ReadingLog;
import com.readfootprints.model.User;
import com.readfootprints.repository.ReadingLogRepository;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.springframework.data.mongodb.core.query.Criteria.where;

/**
 * 閱讀紀錄 KPI 與閱讀狀況（規劃書 4.5），公式照 1.0 首頁與統計頁：
 * - 本年完讀：已讀且完讀日期在今年（台北時間）
 * - 閱讀天數：分鐘數 > 0 的天數；平均每日 = 總分鐘 / 閱讀天數（前端四捨五入）
 * - 連續閱讀：從昨天往前數，每天 ≥ 20 分鐘才算，遇到不足就停
 */
@Service
public class StatsService {

    static final int STREAK_MINUTES = 20;

    private final MongoTemplate mongo;
    private final ReadingLogRepository logs;

    public StatsService(MongoTemplate mongo, ReadingLogRepository logs) {
        this.mongo = mongo;
        this.logs = logs;
    }

    public record Summary(long totalBooks, long readBooks, long readThisYear,
                          long totalMinutes, long readingDays, int streakDays,
                          WordPrice wordPrice, Speed speed, Backlog backlog) {}

    /** 一字千金：同時有字數與金額（> 0）的書；cheapest / priciest 依每字價格 */
    public record WordPrice(int books, long totalPrice, long totalWords, BookRef cheapest, BookRef priciest) {}

    public record BookRef(String id, String title, int price, int words) {}

    /**
     * 看書速度：開書到完讀 30 天內、有字數、閱讀期間至少一半的天數有填時數、速度合理的書。
     * medianWpm = 各本每分鐘字數的中位數（主數字）；totalWords / totalDays 給「每天字數」參考。
     * details 列出所有有開書與完讀日期的書，含沒採計的原因，給「計算明細」用。
     */
    public record Speed(int books, double medianWpm, long totalWords, double totalMinutes, long totalDays,
                        List<SpeedBook> details) {}

    public record SpeedBook(String id, String title, String from, String to, long days, int loggedDays,
                            Integer words, double minutes, int maxConcurrent, Double wpm, boolean included,
                            String reason) {}

    /** 書架上還沒讀完（未讀 + 閱讀中）的書：有字數的本數與字數、沒字數的本數 */
    public record Backlog(int booksWithWords, long words, int booksWithoutWords) {}

    static final int MAX_SPEED_DAYS = 30;
    /** 每分鐘字數超出這個範圍多半是時數漏填或資料錯誤，不採計 */
    static final double MIN_WPM = 50, MAX_WPM = 1000;
    /** 閱讀期間有填時數的天數至少要這個比例，不然分母太少、速度會被高估 */
    static final double MIN_LOGGED_RATIO = 0.5;

    public Summary summary(User me) {
        LocalDate today = LocalDate.now(UserService.ZONE);
        Query q = new Query(where("user_id").is(me.id));
        q.fields().include("title", "read", "read_date", "reading", "reading_since", "price", "word_count");
        List<Book> books = mongo.find(q, Book.class);
        String year = String.valueOf(today.getYear());
        long read = books.stream().filter(b -> b.read).count();
        long readThisYear = books.stream().filter(b -> b.read && b.readDate != null && b.readDate.startsWith(year)).count();

        List<ReadingLog> all = logs.findByUserIdOrderByDateDesc(me.id);
        Map<String, Integer> byDate = new HashMap<>();
        long total = 0, days = 0;
        for (ReadingLog l : all) {
            byDate.put(l.date, l.minutes);
            total += l.minutes;
            if (l.minutes > 0) days++;
        }
        return new Summary(books.size(), read, readThisYear, total, days, streak(byDate, today),
                wordPrice(books), speed(books, byDate, today), backlog(books));
    }

    static WordPrice wordPrice(List<Book> books) {
        List<Book> priced = books.stream()
                .filter(b -> b.price != null && b.price > 0 && b.wordCount != null && b.wordCount > 0).toList();
        long price = priced.stream().mapToLong(b -> b.price).sum();
        long words = priced.stream().mapToLong(b -> b.wordCount).sum();
        java.util.Comparator<Book> perWord = java.util.Comparator.comparingDouble(b -> (double) b.price / b.wordCount);
        BookRef cheap = priced.stream().min(perWord).map(StatsService::ref).orElse(null);
        BookRef dear = priced.stream().max(perWord).map(StatsService::ref).orElse(null);
        return new WordPrice(priced.size(), price, words, cheap, dear);
    }

    private static BookRef ref(Book b) {
        return new BookRef(b.id, b.title, b.price, b.wordCount);
    }

    /**
     * 每本書分到的分鐘：閱讀期間每天的分鐘數，除以當天同時在讀的本數（開書日 ~ 完讀日，或閱讀中到今天）。
     * 主數字用各本速度的中位數，不容易被少數幾本拉偏。
     */
    static Speed speed(List<Book> books, Map<String, Integer> minutesByDate, LocalDate today) {
        record Span(Book book, LocalDate from, LocalDate to) {}
        List<Span> spans = new java.util.ArrayList<>();
        for (Book b : books) {
            LocalDate from = parse(b.readingSince);
            if (from == null) continue;
            LocalDate to = b.read ? parse(b.readDate) : b.reading ? today : null;
            if (to != null && !to.isBefore(from)) spans.add(new Span(b, from, to));
        }
        Map<LocalDate, Integer> open = new HashMap<>(); // 每天同時在讀幾本
        for (Span sp : spans) {
            for (LocalDate d = sp.from(); !d.isAfter(sp.to()); d = d.plusDays(1)) open.merge(d, 1, Integer::sum);
        }
        List<SpeedBook> details = new java.util.ArrayList<>();
        List<Double> speeds = new java.util.ArrayList<>();
        long words = 0, dayTotal = 0;
        double minutes = 0;
        for (Span sp : spans) {
            Book b = sp.book();
            if (!b.read) continue;
            long days = java.time.temporal.ChronoUnit.DAYS.between(sp.from(), sp.to()) + 1;
            double share = 0;
            int logged = 0, maxOpen = 1;
            for (LocalDate d = sp.from(); !d.isAfter(sp.to()); d = d.plusDays(1)) {
                int m = minutesByDate.getOrDefault(d.toString(), 0);
                if (m > 0) logged++;
                share += (double) m / open.get(d);
                maxOpen = Math.max(maxOpen, open.get(d));
            }
            share = Math.round(share * 10) / 10.0;
            Double wpm = b.wordCount != null && b.wordCount > 0 && share > 0 ? Math.round(b.wordCount / share * 10) / 10.0 : null;
            String reason = null;
            if (b.wordCount == null || b.wordCount <= 0) reason = "沒有字數";
            else if (days > MAX_SPEED_DAYS) reason = "超過 " + MAX_SPEED_DAYS + " 天";
            else if (share <= 0) reason = "期間沒有登錄分鐘";
            else if (logged < days * MIN_LOGGED_RATIO) reason = "有填時數的天數不到一半";
            else if (wpm < MIN_WPM || wpm > MAX_WPM) reason = "速度不合理";
            boolean ok = reason == null;
            if (ok) {
                speeds.add(wpm);
                words += b.wordCount;
                minutes += share;
                dayTotal += days;
            }
            details.add(new SpeedBook(b.id, b.title, sp.from().toString(), sp.to().toString(), days, logged,
                    b.wordCount, share, maxOpen, wpm, ok, reason));
        }
        details.sort(java.util.Comparator.comparing(SpeedBook::to).reversed());
        return new Speed(speeds.size(), median(speeds), words, Math.round(minutes * 10) / 10.0, dayTotal, details);
    }

    static double median(List<Double> values) {
        if (values.isEmpty()) return 0;
        List<Double> v = values.stream().sorted().toList();
        int n = v.size();
        return n % 2 == 1 ? v.get(n / 2) : (v.get(n / 2 - 1) + v.get(n / 2)) / 2;
    }

    static Backlog backlog(List<Book> books) {
        int with = 0, without = 0;
        long words = 0;
        for (Book b : books) {
            if (b.read) continue;
            if (b.wordCount != null && b.wordCount > 0) {
                with++;
                words += b.wordCount;
            } else {
                without++;
            }
        }
        return new Backlog(with, words, without);
    }

    private static LocalDate parse(String date) {
        if (date == null || date.isBlank()) return null;
        try {
            return LocalDate.parse(date);
        } catch (java.time.format.DateTimeParseException e) {
            return null;
        }
    }

    static int streak(Map<String, Integer> minutesByDate, LocalDate today) {
        int days = 0;
        for (LocalDate d = today.minusDays(1); minutesByDate.getOrDefault(d.toString(), 0) >= STREAK_MINUTES; d = d.minusDays(1)) {
            days++;
        }
        return days;
    }
}
