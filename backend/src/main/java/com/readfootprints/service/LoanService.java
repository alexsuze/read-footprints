package com.readfootprints.service;

import com.readfootprints.config.AppProperties;
import com.readfootprints.model.Book;
import com.readfootprints.model.BookCover;
import com.readfootprints.model.Loan;
import com.readfootprints.model.User;
import com.readfootprints.repository.BookCoverRepository;
import com.readfootprints.repository.BookRepository;
import com.readfootprints.repository.LoanRepository;
import com.readfootprints.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.HtmlUtils;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 借書流程（規劃書第 5 節）：申請 → 核定借出 → 歸還 / 收回。
 * - 只能借管理員的書，管理員不能借自己的書；一本書同時只有一筆進行中
 * - 借書人：申請、歸還（申請中按歸還 = 取消申請）；管理員：核定借出、收回（申請中收回 = 拒絕）
 * - 申請與歸還借出中的書時寄信給管理員，寄不出去只記 log
 * - 別人的紀錄、非管理員操作管理員動作一律 404
 */
@Service
public class LoanService {

    private static final Logger log = LoggerFactory.getLogger(LoanService.class);
    private static final DateTimeFormatter WHEN = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm");
    private static final Set<String> FILTERS = Set.of("active", Loan.REQUESTED, Loan.LENT, Loan.RETURNED);

    private final LoanRepository loans;
    private final BookRepository books;
    private final BookCoverRepository covers;
    private final UserRepository userRepo;
    private final UserService users;
    private final EmailService email;
    private final AppProperties props;

    public LoanService(LoanRepository loans, BookRepository books, BookCoverRepository covers, UserRepository userRepo,
                       UserService users, EmailService email, AppProperties props) {
        this.loans = loans;
        this.books = books;
        this.covers = covers;
        this.userRepo = userRepo;
        this.users = users;
        this.email = email;
        this.props = props;
    }

    /** 回給前端的借閱紀錄，帶書名與借書人。書封只在查進行中的紀錄時附上（總覽租借中用）。 */
    public record LoanView(String id, String bookId, String bookTitle, String bookSpineColor, String bookCover,
                           String borrowerId, String borrowerName, String borrowerEmail, String borrowerPicture,
                           String status, Instant requestedAt, Instant lentAt, Instant returnedAt, String endedBy) {}

    /** all：管理員看全部；status：active / requested / lent / returned，空 = 全部 */
    public List<LoanView> list(User me, boolean all, String status) {
        if (all) UserService.requireAdmin(me);
        if (status != null && !status.isBlank() && !FILTERS.contains(status)) throw badRequest("狀態不正確");
        List<Loan> found = (all ? loans.findAllByOrderByRequestedAtDesc() : loans.findByBorrowerIdOrderByRequestedAtDesc(me.id))
                .stream().filter(l -> matches(l, status)).toList();
        return views(found, "active".equals(status));
    }

    public LoanView request(User me, String bookId) {
        if (me.admin) throw badRequest("管理員不能借自己的書");
        User admin = users.admin();
        Book book = bookId == null ? null : books.findById(bookId).orElse(null);
        if (admin == null || book == null || !admin.id.equals(book.userId)) throw notFound("找不到此書");
        if (loans.existsByActiveBookId(book.id)) throw busy();
        Loan l = new Loan();
        l.bookId = book.id;
        l.ownerId = admin.id;
        l.borrowerId = me.id;
        l.status = Loan.REQUESTED;
        l.requestedAt = Instant.now();
        l.activeBookId = book.id;
        try {
            loans.save(l);
        } catch (DuplicateKeyException e) {
            throw busy();
        }
        notifyAdmin("[借書申請] %s 想借《%s》".formatted(display(me), book.title), "有新的借書申請", me, book, l.requestedAt);
        return view(l);
    }

    public LoanView lend(User me, String id) {
        Loan l = adminLoan(me, id);
        if (!Loan.REQUESTED.equals(l.status)) throw conflict("只有申請中的紀錄可以核定借出");
        l.status = Loan.LENT;
        l.lentAt = Instant.now();
        return view(loans.save(l));
    }

    /** 借書人歸還；申請中按歸還視為取消申請，不寄信 */
    public LoanView giveBack(User me, String id) {
        Loan l = loans.findById(id).filter(x -> me.id.equals(x.borrowerId)).orElseThrow(() -> notFound("找不到此借閱紀錄"));
        if (!l.active()) throw conflict("這筆借閱已經結束");
        boolean wasLent = Loan.LENT.equals(l.status);
        end(l, Loan.BY_BORROWER);
        if (wasLent) {
            Book book = books.findById(l.bookId).orElse(null);
            String title = book == null ? "（已刪除的書）" : book.title;
            notifyAdmin("[歸還] %s 歸還了《%s》".formatted(display(me), title), "有書被歸還了", me, book, l.returnedAt);
        }
        return view(l);
    }

    /** 管理員收回；申請中收回視為拒絕 */
    public LoanView reclaim(User me, String id) {
        Loan l = adminLoan(me, id);
        if (!l.active()) throw conflict("這筆借閱已經結束");
        end(l, Loan.BY_OWNER);
        return view(l);
    }

    private void end(Loan l, String by) {
        l.status = Loan.RETURNED;
        l.returnedAt = Instant.now();
        l.endedBy = by;
        l.activeBookId = null;
        loans.save(l);
    }

    private Loan adminLoan(User me, String id) {
        if (!me.admin) throw notFound("找不到此借閱紀錄");
        return loans.findById(id).orElseThrow(() -> notFound("找不到此借閱紀錄"));
    }

    static boolean matches(Loan l, String status) {
        if (status == null || status.isBlank()) return true;
        return "active".equals(status) ? l.active() : status.equals(l.status);
    }

    private LoanView view(Loan l) {
        return views(List.of(l), false).getFirst();
    }

    private List<LoanView> views(List<Loan> list, boolean withCover) {
        Set<String> bookIds = list.stream().map(l -> l.bookId).collect(Collectors.toSet());
        Set<String> userIds = list.stream().map(l -> l.borrowerId).collect(Collectors.toSet());
        Map<String, Book> bookMap = books.findAllById(bookIds).stream().collect(Collectors.toMap(b -> b.id, Function.identity()));
        Map<String, User> userMap = userRepo.findAllById(userIds).stream().collect(Collectors.toMap(u -> u.id, Function.identity()));
        Map<String, String> coverMap = withCover
                ? covers.findAllById(bookIds).stream().collect(Collectors.toMap(c -> c.id, (BookCover c) -> c.data))
                : Map.of();
        return list.stream().map(l -> {
            Book b = bookMap.get(l.bookId);
            User u = userMap.get(l.borrowerId);
            return new LoanView(l.id, l.bookId, b == null ? "（已刪除的書）" : b.title, b == null ? null : b.spineColor,
                    coverMap.get(l.bookId), l.borrowerId, u == null ? "" : u.name, u == null ? "" : u.email,
                    u == null ? null : u.picture, l.status, l.requestedAt, l.lentAt, l.returnedAt, l.endedBy);
        }).toList();
    }

    private void notifyAdmin(String subject, String heading, User borrower, Book book, Instant at) {
        String to = props.auth().adminEmail();
        if (to == null || to.isBlank()) return;
        String link = props.frontendUrl() + "#/settings?tab=loans";
        String when = at.atZone(UserService.ZONE).format(WHEN);
        String title = book == null ? "（已刪除的書）" : book.title;
        String html = """
                <div style="font-family:sans-serif;line-height:1.6;color:#2B2A28">
                <h2 style="margin:0 0 12px">%s</h2>
                <p>書名：《%s》<br>借書人：%s<br>Email：%s<br>時間：%s（台北時間）</p>
                <p><a href="%s" style="display:inline-block;padding:10px 18px;border-radius:5px;background:#8B6B4A;color:#F5F0E6;text-decoration:none">前往租借管理</a></p>
                </div>""".formatted(heading, HtmlUtils.htmlEscape(title), HtmlUtils.htmlEscape(display(borrower)),
                HtmlUtils.htmlEscape(borrower.email), when, HtmlUtils.htmlEscape(link));
        String text = "%s\n書名：《%s》\n借書人：%s\nEmail：%s\n時間：%s（台北時間）\n前往租借管理：%s"
                .formatted(heading, title, display(borrower), borrower.email, when, link);
        try {
            email.send(to.trim(), subject, html, text);
        } catch (IllegalStateException e) {
            log.warn("借書通知寄送失敗：{}", subject, e);
        }
    }

    private static String display(User u) {
        return u.name == null || u.name.isBlank() ? u.email : u.name;
    }

    private static ResponseStatusException busy() {
        return conflict("這本書已被借出或有人申請中");
    }

    private static ResponseStatusException conflict(String message) {
        return new ResponseStatusException(HttpStatus.CONFLICT, message);
    }

    private static ResponseStatusException notFound(String message) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, message);
    }

    private static ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
}
