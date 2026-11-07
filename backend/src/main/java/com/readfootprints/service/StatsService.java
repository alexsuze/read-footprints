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

    /** 看書速度：開書到完讀 30 天內、有字數、期間有登錄分鐘且速度合理的書 */
    public record Speed(int books, long totalWords, double totalMinutes, long totalDays) {}

    /** 書架上還沒讀完（未讀 + 閱讀中）的書：有字數的本數與字數、沒字數的本數 */
    public record Backlog(int booksWithWords, long words, int booksWithoutWords) {}

    static final int MAX_SPEED_DAYS = 30;
    /** 每分鐘字數超出這個範圍多半是時數漏填或資料錯誤，不採計 */
    static final double MIN_WPM = 50, MAX_WPM = 1500;

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
     * 每分鐘字數 = 採計書的總字數 ÷ 這些書閱讀期間分到的總分鐘。
     * 同一天有多本在讀（開書日 ~ 完讀日，或閱讀中到今天）時，當天分鐘數平分給這幾本。
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
        int count = 0;
        long words = 0, dayTotal = 0;
        double minutes = 0;
        for (Span sp : spans) {
            Book b = sp.book();
            long days = java.time.temporal.ChronoUnit.DAYS.between(sp.from(), sp.to()) + 1;
            if (!b.read || b.wordCount == null || b.wordCount <= 0 || days > MAX_SPEED_DAYS) continue;
            double share = 0;
            for (LocalDate d = sp.from(); !d.isAfter(sp.to()); d = d.plusDays(1)) {
                share += (double) minutesByDate.getOrDefault(d.toString(), 0) / open.get(d);
            }
            if (share <= 0) continue;
            double wpm = b.wordCount / share;
            if (wpm < MIN_WPM || wpm > MAX_WPM) continue;
            count++;
            words += b.wordCount;
            minutes += share;
            dayTotal += days;
        }
        return new Speed(count, words, Math.round(minutes * 10) / 10.0, dayTotal);
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
