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
    public Integer legacyId;
    @Field("created_at")
    public Instant createdAt;

    /** 只有單本 API 會帶，存在 book_covers */
    @Transient
    public String cover;
    /** 查詢時由分類表帶入；看管理員書架的人讀不到對方分類 */
    @Transient
    public String categoryName;
}
