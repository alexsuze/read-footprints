package com.readfootprints.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.List;

/** 閱讀活動，每人各自一套。規劃書 4.4、第 8 節。目標與階段只有馬拉松有。 */
@Document("activities")
public class Activity {

    public static final String MARATHON = "marathon";
    public static final String QUIZ = "quiz";

    @Id
    public String id;
    @Indexed
    @Field("user_id")
    public String userId;
    public String type;
    public String name;
    @Field("start_date")
    public String startDate;
    @Field("end_date")
    public String endDate;
    public String url;
    public String color;
    @Field("goal_minutes")
    public Integer goalMinutes;
    /** 手動維護，不從每日時數加總 */
    @Field("current_minutes")
    public Integer currentMinutes;
    @Field("stage_minutes")
    public List<Integer> stageMinutes;
}
