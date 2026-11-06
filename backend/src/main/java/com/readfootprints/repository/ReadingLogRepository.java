package com.readfootprints.repository;

import com.readfootprints.model.ReadingLog;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface ReadingLogRepository extends MongoRepository<ReadingLog, String> {

    List<ReadingLog> findByUserIdOrderByDateDesc(String userId);

    boolean existsByUserIdAndDate(String userId, String date);
}
