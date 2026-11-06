package com.readfootprints.repository;

import com.readfootprints.model.Activity;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface ActivityRepository extends MongoRepository<Activity, String> {

    List<Activity> findByUserIdOrderByStartDateDesc(String userId);
}
