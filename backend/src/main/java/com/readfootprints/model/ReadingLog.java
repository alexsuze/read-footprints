package com.readfootprints.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

/** 每日閱讀分鐘數（1.0 time_data），一人一天一筆。規劃書第 8 節。 */
@Document("reading_logs")
@CompoundIndex(name = "owner_date", def = "{'user_id': 1, 'date': 1}", unique = true)
public class ReadingLog {

    @Id
    public String id;
    @Field("user_id")
    public String userId;
    /** yyyy-MM-dd */
    public String date;
    public int minutes;
}
