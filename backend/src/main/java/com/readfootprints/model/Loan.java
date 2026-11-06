package com.readfootprints.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.Instant;

/** 借閱紀錄（規劃書第 5 節、第 8 節）。只能借管理員的書。 */
@Document("loans")
@CompoundIndex(name = "book_status", def = "{'book_id': 1, 'status': 1}")
@CompoundIndex(name = "borrower_status", def = "{'borrower_id': 1, 'status': 1}")
public class Loan {

    public static final String REQUESTED = "requested";
    public static final String LENT = "lent";
    public static final String RETURNED = "returned";
    public static final String BY_BORROWER = "borrower";
    public static final String BY_OWNER = "owner";

    @Id
    public String id;
    @Field("book_id")
    public String bookId;
    @Field("owner_id")
    public String ownerId;
    @Field("borrower_id")
    public String borrowerId;
    public String status;
    @Field("requested_at")
    public Instant requestedAt;
    @Field("lent_at")
    public Instant lentAt;
    @Field("returned_at")
    public Instant returnedAt;
    @Field("ended_by")
    public String endedBy;
    /**
     * 進行中（申請中或借出中）時等於 book_id，結束時清空（不寫進文件）。
     * 唯一 + sparse 索引保證一本書同時只有一筆進行中的借閱，兩人同時按借書也不會重複。
     */
    @Indexed(unique = true, sparse = true)
    @Field("active_book_id")
    public String activeBookId;

    public boolean active() {
        return REQUESTED.equals(status) || LENT.equals(status);
    }
}
