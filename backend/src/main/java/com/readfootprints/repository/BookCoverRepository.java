package com.readfootprints.repository;

import com.readfootprints.model.BookCover;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface BookCoverRepository extends MongoRepository<BookCover, String> {
}
