package com.readfootprints.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/** 書封 Base64 data URL，_id 等於書的 id。 */
@Document("book_covers")
public class BookCover {

    @Id
    public String id;
    public String data;

    public BookCover() {}

    public BookCover(String id, String data) {
        this.id = id;
        this.data = data;
    }
}
