package com.readfootprints.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

/** 分類，每人各自一套。規劃書第 8 節。 */
@Document("categories")
@CompoundIndex(name = "owner_name", def = "{'user_id': 1, 'name': 1}", unique = true)
public class Category {

    @Id
    public String id;
    @Field("user_id")
    public String userId;
    public String name;
    @Field("legacy_id")
    public Integer legacyId; // 1.0 的 id，匯入時用；匯入功能已移除，舊資料保留
}
