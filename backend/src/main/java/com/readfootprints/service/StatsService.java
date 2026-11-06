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
                          long totalMinutes, long readingDays, int streakDays) {}

    public Summary summary(User me) {
        LocalDate today = LocalDate.now(UserService.ZONE);
        Query q = new Query(where("user_id").is(me.id));
        q.fields().include("read", "read_date");
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
        return new Summary(books.size(), read, readThisYear, total, days, streak(byDate, today));
    }

    static int streak(Map<String, Integer> minutesByDate, LocalDate today) {
        int days = 0;
        for (LocalDate d = today.minusDays(1); minutesByDate.getOrDefault(d.toString(), 0) >= STREAK_MINUTES; d = d.minusDays(1)) {
            days++;
        }
        return days;
    }
}
