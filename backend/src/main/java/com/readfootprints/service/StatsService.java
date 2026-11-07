package com.readfootprints.service;

import com.readfootprints.model.Book;
import com.readfootprints.model.ReadingLog;
import com.readfootprints.model.User;
import com.readfootprints.repository.CategoryRepository;
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
    private final CategoryRepository categories;

    public StatsService(MongoTemplate mongo, ReadingLogRepository logs, CategoryRepository categories) {
        this.mongo = mongo;
        this.logs = logs;
        this.categories = categories;
    }

    public record Summary(long totalBooks, long readBooks, long readThisYear,
                          long totalMinutes, long readingDays, int streakDays,
                          WordPrice wordPrice, Speed speed, Backlog backlog,
                          List<CategorySpeed> categorySpeeds, WordRich wordRich) {}

    /** 分類閱讀速度：看書速度採計的書依分類分組，平均每分鐘字數（各本速度的平均），快到慢 */
    public record CategorySpeed(String name, int books, double avgWpm) {}

    /** 字數富翁：自己書架上字數加總最多的作者（合著的書每位作者都算全本字數） */
    public record WordRich(String author, long words, int books) {}

    /** 分類速度至少要幾本才排名，避免一本書就決定快慢 */
    static final int MIN_CATEGORY_BOOKS = 2;

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

    /** reading = 閱讀中（期間算到今天，只參與分配、不算速度）；included 只會是已讀且採計的書 */
    public record SpeedBook(String id, String title, String from, String to, long days, int loggedDays,
                            Integer words, double minutes, int maxConcurrent, Double wpm, boolean included,
                            String reason, boolean reading) {}

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
        q.fields().include("title", "read", "read_date", "reading", "reading_since", "price", "word_count",
                "category_id", "authors");
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
        Speed speed = speed(books, byDate, today);
        Map<String, String> names = new HashMap<>();
        categories.findByUserIdOrderByNameAsc(me.id).forEach(c -> names.put(c.id, c.name));
        return new Summary(books.size(), read, readThisYear, total, days, streak(byDate, today),
                wordPrice(books), speed, backlog(books), categorySpeeds(books, speed, names), wordRich(books));
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
     * 看書速度（2026-11-07 改成先排除再分配）：
     * 1. 每本有開書日期的書是一段期間（已讀：開書 ~ 完讀；閱讀中：開書 ~ 今天），頭尾都算
     * 2. 先排除不靠分鐘就能判斷的：沒有字數、超過 30 天、期間沒有登錄分鐘、有填時數的天數不到一半
     * 3. 只用通過的書算每天同時在讀幾本，當天分鐘平分給這幾本
     * 4. 算出每分鐘字數，超出 50 ~ 1,000 的排除，排除後重新分配，直到沒有書再被排除
     * 5. 主數字取通過的已讀書每分鐘字數的中位數
     * 被排除的書不分走別人的分鐘；明細裡它的「分到分鐘」是假設它也在讀時會分到的量，只供參考
     */
    static Speed speed(List<Book> books, Map<String, Integer> minutesByDate, LocalDate today) {
        record Span(Book book, LocalDate from, LocalDate to, long days, int logged) {}
        List<Span> spans = new java.util.ArrayList<>();
        for (Book b : books) {
            LocalDate from = parse(b.readingSince);
            if (from == null) continue;
            LocalDate to = b.read ? parse(b.readDate) : b.reading ? today : null;
            if (to == null || to.isBefore(from)) continue;
            int logged = 0;
            for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1)) {
                if (minutesByDate.getOrDefault(d.toString(), 0) > 0) logged++;
            }
            spans.add(new Span(b, from, to, java.time.temporal.ChronoUnit.DAYS.between(from, to) + 1, logged));
        }

        // 第 2 步：先排除
        Map<Span, String> reasons = new HashMap<>();
        for (Span sp : spans) {
            Book b = sp.book();
            String reason = null;
            if (b.wordCount == null || b.wordCount < 0) reason = "沒有字數";
            else if (b.wordCount == 0) reason = "版式，無法計算字數";
            else if (sp.days() > MAX_SPEED_DAYS) reason = "超過 " + MAX_SPEED_DAYS + " 天";
            else if (sp.logged() == 0) reason = "期間沒有登錄分鐘";
            else if (sp.logged() < sp.days() * MIN_LOGGED_RATIO) reason = "有填時數的天數不到一半";
            if (reason != null) reasons.put(sp, reason);
        }

        // 第 3、4 步：分配分鐘，排除速度不合理的再重算
        Map<LocalDate, Integer> open = new HashMap<>();
        Map<Span, Double> share = new HashMap<>();
        boolean changed = true;
        while (changed) {
            changed = false;
            open.clear();
            for (Span sp : spans) {
                if (reasons.containsKey(sp)) continue;
                for (LocalDate d = sp.from(); !d.isAfter(sp.to()); d = d.plusDays(1)) open.merge(d, 1, Integer::sum);
            }
            for (Span sp : spans) {
                boolean active = !reasons.containsKey(sp);
                double sum = 0;
                for (LocalDate d = sp.from(); !d.isAfter(sp.to()); d = d.plusDays(1)) {
                    int n = open.getOrDefault(d, 0) + (active ? 0 : 1); // 被排除的書：假設它也在讀
                    sum += (double) minutesByDate.getOrDefault(d.toString(), 0) / n;
                }
                share.put(sp, Math.round(sum * 10) / 10.0);
            }
            for (Span sp : spans) {
                if (reasons.containsKey(sp) || !sp.book().read) continue;
                double wpm = sp.book().wordCount / share.get(sp);
                if (wpm < MIN_WPM || wpm > MAX_WPM) {
                    reasons.put(sp, "速度不合理");
                    changed = true;
                }
            }
        }

        // 第 5 步與明細（只列已讀的書）
        List<SpeedBook> details = new java.util.ArrayList<>();
        List<Double> speeds = new java.util.ArrayList<>();
        long words = 0, dayTotal = 0;
        double minutes = 0;
        for (Span sp : spans) {
            Book b = sp.book();
            double m = share.get(sp);
            // 閱讀中的書還沒讀完，不算速度
            Double wpm = b.read && b.wordCount != null && b.wordCount > 0 && m > 0 ? Math.round(b.wordCount / m * 10) / 10.0 : null;
            int maxOpen = 1;
            for (LocalDate d = sp.from(); !d.isAfter(sp.to()); d = d.plusDays(1)) {
                maxOpen = Math.max(maxOpen, open.getOrDefault(d, 0) + (reasons.containsKey(sp) ? 1 : 0));
            }
            String reason = reasons.get(sp);
            boolean counted = b.read && reason == null;
            if (counted) {
                speeds.add(wpm);
                words += b.wordCount;
                minutes += m;
                dayTotal += sp.days();
            }
            details.add(new SpeedBook(b.id, b.title, sp.from().toString(), sp.to().toString(), sp.days(), sp.logged(),
                    b.wordCount, m, maxOpen, wpm, counted, reason, !b.read));
        }
        details.sort(java.util.Comparator.comparing(SpeedBook::to).reversed());
        return new Speed(speeds.size(), median(speeds), words, Math.round(minutes * 10) / 10.0, dayTotal, details);
    }

    /** 只用看書速度「採計」的書；未分類不排名；每個分類至少 MIN_CATEGORY_BOOKS 本 */
    static List<CategorySpeed> categorySpeeds(List<Book> books, Speed speed, Map<String, String> names) {
        Map<String, Book> byId = new HashMap<>();
        books.forEach(b -> byId.put(b.id, b));
        Map<String, List<Double>> groups = new java.util.TreeMap<>();
        for (SpeedBook d : speed.details()) {
            if (!d.included()) continue;
            Book b = byId.get(d.id());
            String name = b == null || b.categoryId == null ? null : names.get(b.categoryId);
            if (name != null) groups.computeIfAbsent(name, k -> new java.util.ArrayList<>()).add(d.wpm());
        }
        return groups.entrySet().stream()
                .filter(e -> e.getValue().size() >= MIN_CATEGORY_BOOKS)
                .map(e -> new CategorySpeed(e.getKey(), e.getValue().size(),
                        Math.round(e.getValue().stream().mapToDouble(Double::doubleValue).average().orElse(0) * 10) / 10.0))
                .sorted(java.util.Comparator.comparingDouble(CategorySpeed::avgWpm).reversed())
                .toList();
    }

    /** 書架上所有有字數的書（不管讀了沒），依作者加總字數；同字數時本數多的優先 */
    static WordRich wordRich(List<Book> books) {
        Map<String, long[]> sum = new HashMap<>(); // [字數, 本數]
        for (Book b : books) {
            if (b.wordCount == null || b.wordCount <= 0 || b.authors == null) continue;
            for (String a : new java.util.LinkedHashSet<>(b.authors)) {
                if (a == null || a.isBlank()) continue;
                long[] v = sum.computeIfAbsent(a.trim(), k -> new long[2]);
                v[0] += b.wordCount;
                v[1]++;
            }
        }
        return sum.entrySet().stream()
                .max(java.util.Comparator.<Map.Entry<String, long[]>>comparingLong(e -> e.getValue()[0])
                        .thenComparingLong(e -> e.getValue()[1]))
                .map(e -> new WordRich(e.getKey(), e.getValue()[0], (int) e.getValue()[1]))
                .orElse(null);
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
