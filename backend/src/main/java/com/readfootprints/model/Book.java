package com.readfootprints.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.Instant;
import java.util.List;

/**
 * 書籍。規劃書第 8 節。書封另存 book_covers，列表、搜尋、排序才不會拖著 Base64 跑。
 */
@Document("books")
@CompoundIndex(name = "owner_buy_date", def = "{'user_id': 1, 'buy_date': -1}")
@CompoundIndex(name = "owner_title", def = "{'user_id': 1, 'title': 1}")
public class Book {

    @Id
    public String id;
    @Field("user_id")
    public String userId;
    public String title;
    public String isbn;
    public List<String> authors = List.of();
    public List<String> translators = List.of();
    public String publisher;
    @Field("publish_date")
    public String publishDate;
    /** 空值 = 未分類 */
    @Field("category_id")
    public String categoryId;
    public boolean read;
    @Field("read_date")
    public String readDate;
    /** 閱讀中（已開書、還沒讀完）。只有書主看得到，別人看到的永遠是 false */
    public boolean reading;
    /** 開書日期 yyyy-MM-dd；讀完後保留，放棄或改回未讀時清空 */
    @Field("reading_since")
    public String readingSince;
    @Field("buy_url")
    public String buyUrl;
    public Integer price;
    @Field("buy_date")
    public String buyDate;
    @Field("word_count")
    public Integer wordCount;
    /** 書脊底色 #RRGGBB，由書封算出；空值時前端用書名雜湊配色 */
    @Field("spine_color")
    public String spineColor;
    @Field("legacy_id")
    public Integer legacyId; // 1.0 的 id，匯入時用；匯入功能已移除，舊資料保留
    @Field("created_at")
    public Instant createdAt;

    /** 只有單本 API 會帶，存在 book_covers */
    @Transient
    public String cover;
    /** 查詢時由分類表帶入；看管理員書架的人讀不到對方分類 */
    @Transient
    public String categoryName;
    /** 進行中的借閱（申請中 / 借出中），沒有則為 null。書架掛「借出」標籤、攤開頁按鈕用 */
    @Transient
    public LoanBrief loan;

    /** mine：目前使用者是借書人；borrowerName 只有書主（管理員）看得到 */
    public record LoanBrief(String id, String status, boolean mine, String borrowerName) {}
}
