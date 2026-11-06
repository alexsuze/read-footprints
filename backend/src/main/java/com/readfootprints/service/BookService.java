package com.readfootprints.service;

import com.readfootprints.model.Book;
import com.readfootprints.model.BookCover;
import com.readfootprints.model.Category;
import com.readfootprints.model.Loan;
import com.readfootprints.model.User;
import com.readfootprints.repository.BookCoverRepository;
import com.readfootprints.repository.BookRepository;
import com.readfootprints.repository.CategoryRepository;
import com.readfootprints.repository.LoanRepository;
import com.readfootprints.repository.UserRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.springframework.data.mongodb.core.query.Criteria.where;

/**
 * 書籍查詢與維護（規劃書 4.3、第 6 節）。
 * 能看：自己的書、管理員的書。能改：只有書主。其餘一律 404。
 * 看別人的書時拿掉金額與購買日期。
 */
@Service
public class BookService {

    public static final int MAX_SIZE = 5000;
    private static final int MAX_COVER = 1_000_000; // 前端壓到 500KB，Base64 後約 680KB
    private static final Map<String, String> SORTS = Map.of(
            "title", "title", "author", "authors", "publisher", "publisher",
            "publish_date", "publishDate", "buy_date", "buyDate", "read_date", "readDate",
            "price", "price", "created_at", "createdAt", "reading_since", "readingSince");

    private final BookRepository books;
    private final BookCoverRepository covers;
    private final CategoryRepository categories;
    private final MongoTemplate mongo;
    private final UserService users;
    private final LoanRepository loans;
    private final UserRepository userRepo;

    public BookService(BookRepository books, BookCoverRepository covers, CategoryRepository categories,
                       MongoTemplate mongo, UserService users, LoanRepository loans, UserRepository userRepo) {
        this.loans = loans;
        this.userRepo = userRepo;
        this.books = books;
        this.covers = covers;
        this.categories = categories;
        this.mongo = mongo;
        this.users = users;
    }

    public record BookPage(List<Book> items, long total, long readCount, int page, int size) {}

    public record BookForm(String title, String isbn, List<String> authors, List<String> translators,
                           String publisher, String publishDate, String categoryId, Boolean read, String readDate,
                           String buyUrl, Integer price, String buyDate, Integer wordCount, String cover,
                           Boolean reading, String readingSince) {}

    /** 攤開頁快速切換：unread（未讀 / 放棄）、reading（開書）、read（讀完了，date = 完讀日期） */
    public record StateForm(String state, String date) {}

    /** shelf：mine / admin。read：all / read / unread / reading。page 從 1 開始。閱讀中只有書主看得到 */
    public BookPage list(User me, String shelf, String keyword, String read, String sort, String order,
                         int page, int size) {
        int pageNo = Math.max(page, 1);
        int pageSize = Math.clamp(size, 1, MAX_SIZE);
        String owner = ownerOf(me, shelf);
        if (owner == null) return new BookPage(List.of(), 0, 0, pageNo, pageSize);

        boolean isOwner = owner.equals(me.id);
        Map<String, String> names = categoryNames(owner);
        Criteria base = where("userId").is(owner);
        if (keyword != null && !keyword.isBlank()) {
            Pattern p = Pattern.compile(Pattern.quote(keyword.trim()), Pattern.CASE_INSENSITIVE);
            List<String> matched = names.entrySet().stream()
                    .filter(e -> p.matcher(e.getValue()).find()).map(Map.Entry::getKey).toList();
            base.orOperator(where("title").regex(p), where("isbn").regex(p), where("authors").regex(p),
                    where("translators").regex(p), where("publisher").regex(p), where("categoryId").in(matched));
        }
        Criteria filtered = switch (read) {
            case "read" -> and(base, where("read").is(true));
            // 別人看管理員書架時，閱讀中也算未讀
            case "unread" -> isOwner ? and(base, where("read").is(false), where("reading").ne(true)) : and(base, where("read").is(false));
            case "reading" -> isOwner ? and(base, where("read").is(false), where("reading").is(true)) : and(base, where("id").is(null));
            default -> base;
        };

        // 金額、開書日期排序會洩漏別人書的資訊
        String field = SORTS.get(!isOwner && ("price".equals(sort) || "reading_since".equals(sort)) ? "created_at" : sort);
        if (field == null) field = "buyDate";
        Sort.Direction direction = "asc".equals(order) ? Sort.Direction.ASC : Sort.Direction.DESC;

        List<Book> items = mongo.find(new Query(filtered)
                .with(PageRequest.of(pageNo - 1, pageSize, Sort.by(direction, field).and(Sort.by("id")))), Book.class);
        items.forEach(b -> prepare(b, names, isOwner));
        attachLoans(me, items);
        return new BookPage(items, mongo.count(new Query(filtered), Book.class),
                mongo.count(new Query(and(base, where("read").is(true))), Book.class), pageNo, pageSize);
    }

    public Book get(User me, String id) {
        Book b = books.findById(id).orElseThrow(BookService::notFound);
        boolean isOwner = b.userId.equals(me.id);
        User admin = isOwner ? null : users.admin();
        if (!isOwner && (admin == null || !b.userId.equals(admin.id))) throw notFound();
        prepare(b, categoryNames(b.userId), isOwner);
        attachLoans(me, List.of(b));
        b.cover = covers.findById(id).map(c -> c.data).orElse(null);
        return b;
    }

    public Book create(User me, BookForm f) {
        validate(me, f);
        Book b = new Book();
        b.userId = me.id;
        b.createdAt = Instant.now();
        apply(b, f);
        books.save(b);
        saveCover(b, f.cover());
        return b;
    }

    public Book update(User me, String id, BookForm f) {
        Book b = owned(me, id);
        validate(me, f);
        apply(b, f);
        books.save(b);
        saveCover(b, f.cover());
        return b;
    }

    public Book setState(User me, String id, StateForm f) {
        Book b = owned(me, id);
        String today = LocalDate.now(UserService.ZONE).toString();
        String state = f == null ? null : f.state();
        if ("reading".equals(state)) {
            b.read = false;
            b.readDate = "";
            b.reading = true;
            b.readingSince = today;
        } else if ("read".equals(state)) {
            String date = f.date() == null || f.date().isBlank() ? today : f.date();
            if (!validDate(date)) throw badRequest("完讀日期格式不正確");
            b.read = true;
            b.readDate = date;
            b.reading = false;
        } else if ("unread".equals(state)) {
            b.read = false;
            b.readDate = "";
            b.reading = false;
            b.readingSince = null;
        } else {
            throw badRequest("狀態不正確");
        }
        books.save(b);
        prepare(b, categoryNames(b.userId), true);
        attachLoans(me, List.of(b));
        return b;
    }

    public void delete(User me, String id) {
        Book b = owned(me, id);
        if (loans.existsByActiveBookId(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "這本書有人申請或借出中，請先收回再刪除");
        }
        books.delete(b);
        covers.deleteById(id);
    }

    /** cover：null 不變、空字串移除、其他為新書封。 */
    private void saveCover(Book b, String cover) {
        if (cover == null) return;
        if (cover.isEmpty()) covers.deleteById(b.id);
        else covers.save(new BookCover(b.id, cover));
        b.spineColor = SpineColor.of(cover);
        books.save(b);
    }

    private void apply(Book b, BookForm f) {
        b.title = f.title().trim();
        b.isbn = f.isbn();
        b.authors = clean(f.authors());
        b.translators = clean(f.translators());
        b.publisher = f.publisher();
        b.publishDate = f.publishDate();
        b.categoryId = f.categoryId() == null || f.categoryId().isBlank() ? null : f.categoryId();
        b.read = Boolean.TRUE.equals(f.read());
        b.readDate = b.read ? f.readDate() : "";
        // 未讀 → 閱讀中 → 已讀；已讀時保留開書日期，未讀時清空
        b.reading = !b.read && Boolean.TRUE.equals(f.reading());
        boolean started = b.read || b.reading;
        String since = f.readingSince() == null || f.readingSince().isBlank() ? null : f.readingSince();
        b.readingSince = !started ? null : since != null ? since : b.reading ? LocalDate.now(UserService.ZONE).toString() : null;
        b.buyUrl = f.buyUrl();
        b.price = f.price();
        b.buyDate = f.buyDate();
        b.wordCount = f.wordCount();
    }

    private void validate(User me, BookForm f) {
        if (f.title() == null || f.title().isBlank()) throw badRequest("請填寫書名");
        if (f.title().length() > 200) throw badRequest("書名太長");
        if (f.price() != null && f.price() < 0) throw badRequest("金額不可為負數");
        if (f.wordCount() != null && f.wordCount() < 0) throw badRequest("字數不可為負數");
        if (f.readingSince() != null && !f.readingSince().isBlank() && !validDate(f.readingSince())) {
            throw badRequest("開書日期格式不正確");
        }
        if (f.cover() != null && !f.cover().isEmpty()) {
            if (!f.cover().startsWith("data:image/")) throw badRequest("書封格式不正確");
            if (f.cover().length() > MAX_COVER) throw badRequest("書封太大");
        }
        if (f.categoryId() != null && !f.categoryId().isBlank()
                && categories.findById(f.categoryId()).filter(c -> me.id.equals(c.userId)).isEmpty()) {
            throw badRequest("找不到此分類");
        }
    }

    private Book owned(User me, String id) {
        return books.findById(id).filter(b -> b.userId.equals(me.id)).orElseThrow(BookService::notFound);
    }

    private String ownerOf(User me, String shelf) {
        if (!"admin".equals(shelf)) return me.id;
        User admin = users.admin();
        return admin == null ? null : admin.id;
    }

    private Map<String, String> categoryNames(String owner) {
        return categories.findByUserIdOrderByNameAsc(owner).stream()
                .collect(Collectors.toMap(c -> c.id, (Category c) -> c.name));
    }

    private static void prepare(Book b, Map<String, String> names, boolean isOwner) {
        b.categoryName = names.get(b.categoryId);
        if (!isOwner) {
            b.price = null;
            b.buyDate = null;
            b.reading = false;
            b.readingSince = null;
        }
    }

    /** 只有管理員的書會被借；管理員看得到借書人名稱 */
    private void attachLoans(User me, List<Book> list) {
        if (list.isEmpty()) return;
        Map<String, Loan> active = loans.findByActiveBookIdIn(list.stream().map(b -> b.id).toList()).stream()
                .collect(Collectors.toMap(l -> l.bookId, l -> l, (a, b) -> a));
        if (active.isEmpty()) return;
        Map<String, String> names = me.admin
                ? userRepo.findAllById(active.values().stream().map(l -> l.borrowerId).collect(Collectors.toSet())).stream()
                        .collect(Collectors.toMap(u -> u.id, u -> u.name == null || u.name.isBlank() ? u.email : u.name))
                : Map.of();
        for (Book b : list) {
            Loan l = active.get(b.id);
            if (l != null) b.loan = new Book.LoanBrief(l.id, l.status, me.id.equals(l.borrowerId), names.get(l.borrowerId));
        }
    }

    private static Criteria and(Criteria... parts) {
        return new Criteria().andOperator(parts);
    }

    static boolean validDate(String value) {
        try {
            LocalDate.parse(value);
            return true;
        } catch (DateTimeParseException e) {
            return false;
        }
    }

    private static List<String> clean(List<String> names) {
        return names == null ? List.of() : names.stream().map(String::trim).filter(s -> !s.isEmpty()).toList();
    }

    private static ResponseStatusException notFound() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "找不到此書");
    }

    private static ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
}
