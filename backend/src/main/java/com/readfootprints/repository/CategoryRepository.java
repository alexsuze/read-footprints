package com.readfootprints.repository;

import com.readfootprints.model.Category;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface CategoryRepository extends MongoRepository<Category, String> {

    List<Category> findByUserIdOrderByNameAsc(String userId);

    boolean existsByUserIdAndName(String userId, String name);
}
