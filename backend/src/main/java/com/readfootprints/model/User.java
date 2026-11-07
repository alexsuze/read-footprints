package com.readfootprints.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.Instant;
import java.util.List;

/**
 * 使用者（Google 登入）。規劃書第 8 節。
 */
@Document("users")
public class User {

    public static final String PENDING = "pending";
    public static final String ACTIVE = "active";
    public static final String DISABLED = "disabled";

    @Id
    public String id;
    /** Google 帳號的唯一 id（Email 可能會變，用這個認人）。 */
    @Field("google_sub")
    public String googleSub;
    public String email;
    public String name;
    public String picture;
    /** pending 待核准 / active 使用中 / disabled 已停用 */
    public String status = PENDING;
    public boolean admin;
    @Field("created_at")
    public Instant createdAt;
    @Field("last_login_at")
    public Instant lastLoginAt;
    /** 報表（圖書館列表與 CSV）要顯示的欄位與順序；null = 用前端預設 */
    @Field("list_columns")
    public List<String> listColumns;
}
