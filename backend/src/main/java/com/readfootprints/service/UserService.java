package com.readfootprints.service;

import com.readfootprints.config.AppProperties;
import com.readfootprints.model.User;
import com.readfootprints.repository.UserRepository;
import com.readfootprints.service.GoogleVerifier.GoogleUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.HtmlUtils;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;

/**
 * 登入、驗證通行證、管理員核准 / 停用（規劃書 4.1、4.6）。
 * 有人第一次申請時寄信通知管理員。
 */
@Service
public class UserService {

    public static final ZoneId ZONE = ZoneId.of("Asia/Taipei");
    private static final Set<String> STATUSES = Set.of(User.PENDING, User.ACTIVE, User.DISABLED);
    private static final DateTimeFormatter WHEN = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm");
    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final UserRepository repo;
    private final GoogleVerifier google;
    private final TokenService tokens;
    private final EmailService email;
    private final AppProperties props;

    public UserService(UserRepository repo, GoogleVerifier google, TokenService tokens,
                       EmailService email, AppProperties props) {
        this.repo = repo;
        this.google = google;
        this.tokens = tokens;
        this.email = email;
        this.props = props;
    }

    public record LoginResult(String token, User user) {}

    /** 用 Google 身分憑證登入；第一次登入建立待核准帳號。待核准 / 停用回 403。 */
    public LoginResult login(String credential) {
        GoogleUser g = google.verify(credential);
        User u = repo.findByGoogleSub(g.sub()).orElseGet(User::new);
        boolean isNew = u.id == null;
        if (isNew) {
            u.googleSub = g.sub();
            u.createdAt = Instant.now();
        }
        u.email = g.email();
        u.name = g.name();
        u.picture = g.picture();
        u.lastLoginAt = Instant.now();
        if (isAdminEmail(g.email())) {
            u.admin = true;
            u.status = User.ACTIVE; // 管理員不會被鎖在外面
        }
        repo.save(u);
        if (isNew && User.PENDING.equals(u.status)) notifyAdmin(u);

        if (User.PENDING.equals(u.status)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "已送出申請，請等待管理員核准後再登入");
        }
        if (User.DISABLED.equals(u.status)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "此帳號已停用，請聯絡管理員");
        }
        return new LoginResult(tokens.issue(u.id), u);
    }

    /** 新申請寄信給管理員；寄不出去只記錄，不影響申請。 */
    private void notifyAdmin(User u) {
        String to = props.auth().adminEmail();
        if (to == null || to.isBlank()) return;
        String name = u.name == null || u.name.isBlank() ? u.email : u.name;
        String link = props.frontendUrl() + "#/settings?tab=users";
        String when = u.createdAt.atZone(ZONE).format(WHEN);
        String html = """
                <div style="font-family:sans-serif;line-height:1.6;color:#2B2A28">
                <h2 style="margin:0 0 12px">有新的使用申請待審核</h2>
                <p>名稱：%s<br>Email：%s<br>申請時間：%s（台北時間）</p>
                <p><a href="%s" style="display:inline-block;padding:10px 18px;border-radius:5px;background:#8B6B4A;color:#F5F0E6;text-decoration:none">前往設定頁審核</a></p>
                </div>""".formatted(HtmlUtils.htmlEscape(name), HtmlUtils.htmlEscape(u.email), when, HtmlUtils.htmlEscape(link));
        String text = "有新的使用申請待審核\n名稱：%s\nEmail：%s\n申請時間：%s（台北時間）\n前往設定頁審核：%s"
                .formatted(name, u.email, when, link);
        try {
            email.send(to.trim(), "[待審核] %s（%s）申請使用讀跡".formatted(name, u.email), html, text);
        } catch (IllegalStateException e) {
            log.warn("通知管理員新申請失敗", e);
        }
    }

    /** 通行證有效且帳號使用中才回傳使用者，否則 null。 */
    public User authenticate(String token) {
        String id = token == null ? null : tokens.verify(token);
        if (id == null) return null;
        return repo.findById(id).filter(u -> User.ACTIVE.equals(u.status)).orElse(null);
    }

    public List<User> list(User admin) {
        requireAdmin(admin);
        return repo.findAllByOrderByCreatedAtDesc();
    }

    public User setStatus(User admin, String id, String status) {
        requireAdmin(admin);
        if (!STATUSES.contains(status)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "狀態不正確");
        if (admin.id.equals(id)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "不能變更自己的狀態");
        User u = repo.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "找不到此使用者"));
        u.status = status;
        return repo.save(u);
    }

    /** 報表欄位可選的 key（書名固定顯示，不在清單內） */
    static final List<String> COLUMN_KEYS = List.of("isbn", "authors", "translators", "publisher", "publish_date",
            "category", "word_count", "state", "reading_since", "read_date", "buy_date", "price", "buy_url", "created_at");

    /** 儲存報表欄位：只收認得的 key，去掉重複，保留順序；空陣列 = 只顯示書名 */
    public User saveListColumns(User me, List<String> columns) {
        me.listColumns = cleanColumns(columns);
        return repo.save(me);
    }

    static List<String> cleanColumns(List<String> columns) {
        if (columns == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "請提供欄位");
        List<String> unknown = columns.stream().filter(c -> !COLUMN_KEYS.contains(c)).toList();
        if (!unknown.isEmpty()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "不認得的欄位：" + String.join("、", unknown));
        return columns.stream().distinct().toList();
    }

    /** 管理員帳號（借書的書主）。尚未登入過時為 null。 */
    public User admin() {
        return repo.findFirstByAdminTrue().orElse(null);
    }

    private boolean isAdminEmail(String email) {
        String admin = props.auth().adminEmail();
        return admin != null && !admin.isBlank() && admin.trim().equalsIgnoreCase(email);
    }

    public static void requireAdmin(User u) {
        if (!u.admin) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "只有管理員可以使用");
    }
}
