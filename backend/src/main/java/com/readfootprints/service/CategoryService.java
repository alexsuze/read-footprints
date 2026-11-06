package com.readfootprints.service;

import com.readfootprints.model.Book;
import com.readfootprints.model.Category;
import com.readfootprints.model.User;
import com.readfootprints.repository.CategoryRepository;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.springframework.data.mongodb.core.query.Criteria.where;

/** 分類管理（規劃書 4.6）：每人各自一套，刪除後該分類的書變未分類。 */
@Service
public class CategoryService {

    private final CategoryRepository repo;
    private final MongoTemplate mongo;

    public CategoryService(CategoryRepository repo, MongoTemplate mongo) {
        this.repo = repo;
        this.mongo = mongo;
    }

    public List<Category> list(User me) {
        return repo.findByUserIdOrderByNameAsc(me.id);
    }

    public Category create(User me, String name) {
        Category c = new Category();
        c.userId = me.id;
        c.name = validName(me, name);
        return repo.save(c);
    }

    public Category rename(User me, String id, String name) {
        Category c = owned(me, id);
        String next = name == null ? "" : name.trim();
        if (next.equals(c.name)) return c;
        c.name = validName(me, next);
        return repo.save(c);
    }

    public void delete(User me, String id) {
        repo.delete(owned(me, id));
        mongo.updateMulti(new Query(where("userId").is(me.id).and("categoryId").is(id)),
                new Update().unset("categoryId"), Book.class);
    }

    private String validName(User me, String name) {
        String n = name == null ? "" : name.trim();
        if (n.isEmpty() || n.length() > 30) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "分類名稱需為 1 到 30 字");
        }
        if (repo.existsByUserIdAndName(me.id, n)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "已有此分類");
        }
        return n;
    }

    private Category owned(User me, String id) {
        return repo.findById(id).filter(c -> me.id.equals(c.userId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "找不到此分類"));
    }
}
