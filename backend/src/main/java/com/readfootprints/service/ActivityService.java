package com.readfootprints.service;

import com.readfootprints.model.Activity;
import com.readfootprints.model.User;
import com.readfootprints.repository.ActivityRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Set;

/** 活動維護（規劃書 4.4）：只看得到自己的活動，別人的 id 一律 404。 */
@Service
public class ActivityService {

    private static final Set<String> TYPES = Set.of(Activity.MARATHON, Activity.QUIZ);

    private final ActivityRepository repo;

    public ActivityService(ActivityRepository repo) {
        this.repo = repo;
    }

    public record ActivityForm(String type, String name, String startDate, String endDate, String url, String color,
                               Integer goalMinutes, Integer currentMinutes, List<Integer> stageMinutes) {}

    public List<Activity> list(User me) {
        return repo.findByUserIdOrderByStartDateDesc(me.id);
    }

    public Activity create(User me, ActivityForm f) {
        validate(f);
        Activity a = new Activity();
        a.userId = me.id;
        return save(a, f);
    }

    public Activity update(User me, String id, ActivityForm f) {
        Activity a = owned(me, id);
        validate(f);
        return save(a, f);
    }

    public void delete(User me, String id) {
        repo.delete(owned(me, id));
    }

    private Activity save(Activity a, ActivityForm f) {
        boolean marathon = Activity.MARATHON.equals(f.type());
        a.type = f.type();
        a.name = f.name().trim();
        a.startDate = f.startDate();
        a.endDate = f.endDate();
        a.url = f.url() == null ? "" : f.url().trim();
        a.color = f.color();
        a.goalMinutes = marathon ? f.goalMinutes() : null;
        a.currentMinutes = marathon ? (f.currentMinutes() == null ? 0 : f.currentMinutes()) : null;
        a.stageMinutes = marathon ? f.stageMinutes() : null;
        return repo.save(a);
    }

    static void validate(ActivityForm f) {
        if (!TYPES.contains(f.type())) throw badRequest("活動種類不正確");
        if (f.name() == null || f.name().isBlank()) throw badRequest("請填寫活動名稱");
        if (f.name().trim().length() > 50) throw badRequest("活動名稱不可超過 50 字");
        LocalDate start = date(f.startDate());
        if (date(f.endDate()).isBefore(start)) throw badRequest("結束日期不可早於開始日期");
        if (f.url() != null && !f.url().isBlank() && !f.url().trim().matches("(?i)https?://\\S+")) {
            throw badRequest("活動網址需以 http:// 或 https:// 開頭");
        }
        if (f.color() == null || !f.color().matches("#[0-9A-Fa-f]{6}")) throw badRequest("顏色格式不正確");
        if (Activity.MARATHON.equals(f.type())) validateMarathon(f);
    }

    private static void validateMarathon(ActivityForm f) {
        Integer goal = f.goalMinutes();
        if (goal == null || goal <= 0) throw badRequest("請填寫目標分鐘數");
        if (f.currentMinutes() != null && f.currentMinutes() < 0) throw badRequest("目前分鐘數不可為負數");
        List<Integer> stages = f.stageMinutes();
        if (stages == null || stages.isEmpty() || stages.size() > 3) throw badRequest("階段需為 1 到 3 個");
        int previous = 0;
        for (Integer s : stages) {
            if (s == null || s <= previous) throw badRequest("階段分鐘數需由小到大");
            previous = s;
        }
        if (previous != goal) throw badRequest("最後一個階段需等於目標分鐘數");
    }

    private static LocalDate date(String value) {
        try {
            return LocalDate.parse(value == null ? "" : value);
        } catch (DateTimeParseException e) {
            throw badRequest("請填寫開始與結束日期");
        }
    }

    private Activity owned(User me, String id) {
        return repo.findById(id).filter(a -> me.id.equals(a.userId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "找不到此活動"));
    }

    private static ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
}
