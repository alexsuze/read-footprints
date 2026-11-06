package com.readfootprints.service;

import com.readfootprints.model.ReadingLog;
import com.readfootprints.model.User;
import com.readfootprints.repository.ReadingLogRepository;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;

/**
 * 每日閱讀分鐘數（規劃書 4.5 時數統計）：同一天只能一筆，修改只改分鐘數（同 1.0）。
 * 只看得到自己的紀錄，別人的 id 一律 404。
 */
@Service
public class ReadingLogService {

    static final int MAX_MINUTES = 1440;

    private final ReadingLogRepository repo;

    public ReadingLogService(ReadingLogRepository repo) {
        this.repo = repo;
    }

    public record LogForm(String date, Integer minutes) {}

    /** from / to 可省略，含頭含尾；新到舊 */
    public List<ReadingLog> list(User me, String from, String to) {
        String start = from == null || from.isBlank() ? null : date(from).toString();
        String end = to == null || to.isBlank() ? null : date(to).toString();
        return repo.findByUserIdOrderByDateDesc(me.id).stream()
                .filter(l -> (start == null || l.date.compareTo(start) >= 0) && (end == null || l.date.compareTo(end) <= 0))
                .toList();
    }

    public ReadingLog create(User me, LogForm f) {
        String day = date(f.date()).toString();
        int minutes = minutes(f.minutes());
        if (repo.existsByUserIdAndDate(me.id, day)) throw duplicate();
        ReadingLog log = new ReadingLog();
        log.userId = me.id;
        log.date = day;
        log.minutes = minutes;
        try {
            return repo.save(log);
        } catch (DuplicateKeyException e) {
            throw duplicate();
        }
    }

    public ReadingLog update(User me, String id, LogForm f) {
        ReadingLog log = owned(me, id);
        log.minutes = minutes(f.minutes());
        return repo.save(log);
    }

    public void delete(User me, String id) {
        repo.delete(owned(me, id));
    }

    static int minutes(Integer value) {
        if (value == null || value < 1 || value > MAX_MINUTES) throw badRequest("分鐘數需介於 1 到 1440");
        return value;
    }

    static LocalDate date(String value) {
        try {
            return LocalDate.parse(value == null ? "" : value);
        } catch (DateTimeParseException e) {
            throw badRequest("日期格式不正確");
        }
    }

    private ReadingLog owned(User me, String id) {
        return repo.findById(id).filter(l -> me.id.equals(l.userId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "找不到此紀錄"));
    }

    private static ResponseStatusException duplicate() {
        return new ResponseStatusException(HttpStatus.CONFLICT, "這一天已經有紀錄，請改用修改");
    }

    private static ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
}
