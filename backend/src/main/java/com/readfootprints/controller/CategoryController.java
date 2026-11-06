package com.readfootprints.controller;

import com.readfootprints.config.AuthFilter;
import com.readfootprints.model.Category;
import com.readfootprints.model.User;
import com.readfootprints.service.CategoryService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/** 分類 API，body：{ name }。 */
@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    private final CategoryService service;

    public CategoryController(CategoryService service) {
        this.service = service;
    }

    @GetMapping
    public List<Category> list(@RequestAttribute(AuthFilter.USER) User me) {
        return service.list(me);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Category create(@RequestAttribute(AuthFilter.USER) User me, @RequestBody Map<String, String> body) {
        return service.create(me, body.get("name"));
    }

    @PutMapping("/{id}")
    public Category rename(@RequestAttribute(AuthFilter.USER) User me, @PathVariable String id,
                           @RequestBody Map<String, String> body) {
        return service.rename(me, id, body.get("name"));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@RequestAttribute(AuthFilter.USER) User me, @PathVariable String id) {
        service.delete(me, id);
    }
}
