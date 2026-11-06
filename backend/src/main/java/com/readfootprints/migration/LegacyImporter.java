package com.readfootprints.migration;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.readfootprints.model.Book;
import com.readfootprints.model.BookCover;
import com.readfootprints.model.Category;
import com.readfootprints.model.User;
import com.readfootprints.repository.BookCoverRepository;
import com.readfootprints.repository.BookRepository;
import com.readfootprints.repository.CategoryRepository;
import com.readfootprints.service.SpineColor;
import com.readfootprints.service.UserService;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import static org.springframework.data.mongodb.core.query.Criteria.where;

/**
 * 一次性匯入 1.0 的 book_admin（規劃書第 9 節），全部歸管理員，可重跑，已匯入的跳過。
 * reading_logs、activities 的程式在階段 2、3 才有，這裡先照規劃書欄位直接寫文件。
 */
@Service
public class LegacyImporter {

    private static final String LEGACY_DB = "book_admin";
    private static final Bson ALIVE = Filters.ne("deleteFlag", true);

    @Value("${LEGACY_MONGODB_URI:}")
    private String uri;

    private final BookRepository books;
    private final BookCoverRepository covers;
    private final CategoryRepository categories;
    private final MongoTemplate mongo;

    public LegacyImporter(BookRepository books, BookCoverRepository covers, CategoryRepository categories,
                          MongoTemplate mongo) {
        this.books = books;
        this.covers = covers;
        this.categories = categories;
        this.mongo = mongo;
    }

    public record Count(int imported, int skipped) {}

    public record Result(Count categories, Count books, Count readingLogs, Count activities, int undecodableCovers) {}

    public Result run(User admin) {
        UserService.requireAdmin(admin);
        if (uri == null || uri.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "伺服器尚未設定 LEGACY_MONGODB_URI");
        }
        try (MongoClient client = MongoClients.create(uri)) {
            MongoDatabase db = client.getDatabase(LEGACY_DB);
            Map<Integer, String> categoryIds = new HashMap<>();
            Count c = importCategories(db, admin, categoryIds);
            int[] undecodable = {0};
            Count b = importBooks(db, admin, categoryIds, undecodable);
            return new Result(c, b, importReadingLogs(db, admin), importActivities(db, admin), undecodable[0]);
        }
    }

    private Count importCategories(MongoDatabase db, User admin, Map<Integer, String> ids) {
        Map<String, String> byName = new HashMap<>();
        for (Category c : categories.findByUserIdOrderByNameAsc(admin.id)) {
            byName.put(c.name, c.id);
            if (c.legacyId != null) ids.put(c.legacyId, c.id);
        }
        int imported = 0, skipped = 0;
        for (Document d : db.getCollection("category_data").find(ALIVE)) {
            int legacyId = number(d.get("id"));
            String name = d.getString("name").trim();
            if (ids.containsKey(legacyId)) {
                skipped++;
            } else if (byName.containsKey(name)) { // 1.0 沒擋重複名稱
                ids.put(legacyId, byName.get(name));
                skipped++;
            } else {
                Category c = new Category();
                c.userId = admin.id;
                c.name = name;
                c.legacyId = legacyId;
                categories.save(c);
                byName.put(name, c.id);
                ids.put(legacyId, c.id);
                imported++;
            }
        }
        return new Count(imported, skipped);
    }

    private Count importBooks(MongoDatabase db, User admin, Map<Integer, String> categoryIds, int[] undecodable) {
        Query mine = new Query(where("userId").is(admin.id).and("legacyId").ne(null));
        mine.fields().include("legacyId");
        Set<Integer> done = new HashSet<>();
        mongo.find(mine, Book.class).forEach(b -> done.add(b.legacyId));

        int imported = 0, skipped = 0;
        for (Document d : db.getCollection("book_data").find(ALIVE)) {
            int legacyId = number(d.get("id"));
            if (!done.add(legacyId)) {
                skipped++;
                continue;
            }
            Book b = new Book();
            b.userId = admin.id;
            b.legacyId = legacyId;
            b.title = d.getString("name");
            b.isbn = d.getString("ISBN");
            b.authors = strings(d.get("author"));
            b.translators = strings(d.get("translator"));
            b.publisher = d.getString("publisher");
            b.publishDate = date(d.get("publishDate"));
            b.categoryId = categoryIds.get(number(d.get("categoryId")));
            b.read = Boolean.TRUE.equals(d.getBoolean("readed"));
            b.readDate = date(d.get("readedDate"));
            b.buyUrl = d.getString("buyUrl");
            b.price = number(d.get("price"));
            b.buyDate = date(d.get("buyDate"));
            b.createdAt = Instant.now();
            String image = d.getString("image");
            boolean hasCover = image != null && !image.isBlank();
            b.spineColor = SpineColor.of(image);
            if (hasCover && b.spineColor == null) undecodable[0]++;
            books.save(b);
            if (hasCover) covers.save(new BookCover(b.id, image));
            imported++;
        }
        return new Count(imported, skipped);
    }

    private Count importReadingLogs(MongoDatabase db, User admin) {
        Map<String, Integer> minutes = new TreeMap<>(); // 1.0 同一天可能多筆，2.0 一天一筆，加總
        for (Document d : db.getCollection("time_data").find(ALIVE)) {
            minutes.merge(date(d.get("date")), number(d.get("time")), Integer::sum);
        }
        Set<String> done = new HashSet<>();
        for (Document d : mongo.find(new Query(where("user_id").is(admin.id)), Document.class, "reading_logs")) {
            done.add(d.getString("date"));
        }
        int imported = 0;
        for (var e : minutes.entrySet()) {
            if (done.contains(e.getKey())) continue;
            mongo.insert(new Document("user_id", admin.id).append("date", e.getKey()).append("minutes", e.getValue()),
                    "reading_logs");
            imported++;
        }
        return new Count(imported, minutes.size() - imported);
    }

    private Count importActivities(MongoDatabase db, User admin) {
        Set<String> done = new HashSet<>();
        for (Document d : mongo.find(new Query(where("user_id").is(admin.id)), Document.class, "activities")) {
            done.add(d.getString("name") + "|" + d.getString("start_date"));
        }
        int imported = 0, skipped = 0;
        for (Document d : db.getCollection("activity_data").find(ALIVE)) {
            String start = date(d.get("startDate"));
            if (!done.add(d.getString("name") + "|" + start)) {
                skipped++;
                continue;
            }
            int goal = number(d.get("goalTime"));
            mongo.insert(new Document("user_id", admin.id)
                    .append("type", "marathon")
                    .append("name", d.getString("name"))
                    .append("start_date", start)
                    .append("end_date", date(d.get("endDate")))
                    .append("url", "")
                    .append("color", d.getString("color"))
                    .append("goal_minutes", goal)
                    .append("current_minutes", number(d.get("nowTime")))
                    .append("stage_minutes", List.of(goal)), "activities");
            imported++;
        }
        return new Count(imported, skipped);
    }

    /** Date 轉台北時區 yyyy-MM-dd；字串只統一分隔符號。 */
    static String date(Object v) {
        if (v instanceof Date d) return d.toInstant().atZone(UserService.ZONE).toLocalDate().toString();
        return v == null ? "" : v.toString().replace('/', '-');
    }

    private static int number(Object v) {
        return v instanceof Number n ? n.intValue() : 0;
    }

    private static List<String> strings(Object v) {
        List<String> out = new ArrayList<>();
        if (v instanceof List<?> list) list.forEach(x -> { if (x != null) out.add(x.toString()); });
        return out;
    }
}
