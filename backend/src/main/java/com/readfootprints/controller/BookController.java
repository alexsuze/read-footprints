package com.readfootprints.controller;

import com.readfootprints.config.AuthFilter;
import com.readfootprints.model.Book;
import com.readfootprints.model.User;
import com.readfootprints.service.BookService;
import com.readfootprints.service.BookService.BookForm;
import com.readfootprints.service.BookService.BookPage;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** 書籍 API（規劃書第 10 節）。列表不含書封，單本才有。 */
@RestController
@RequestMapping("/api/books")
public class BookController {

    private final BookService service;

    public BookController(BookService service) {
        this.service = service;
    }

    @GetMapping
    public BookPage list(@RequestAttribute(AuthFilter.USER) User me,
                         @RequestParam(defaultValue = "mine") String shelf,
                         @RequestParam(required = false) String keyword,
                         @RequestParam(defaultValue = "all") String read,
                         @RequestParam(defaultValue = "buy_date") String sort,
                         @RequestParam(defaultValue = "desc") String order,
                         @RequestParam(defaultValue = "1") int page,
                         @RequestParam(defaultValue = "20") int size) {
        return service.list(me, shelf, keyword, read, sort, order, page, size);
    }

    @GetMapping("/{id}")
    public Book get(@RequestAttribute(AuthFilter.USER) User me, @PathVariable String id) {
        return service.get(me, id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Book create(@RequestAttribute(AuthFilter.USER) User me, @RequestBody BookForm form) {
        return service.create(me, form);
    }

    @PutMapping("/{id}")
    public Book update(@RequestAttribute(AuthFilter.USER) User me, @PathVariable String id,
                       @RequestBody BookForm form) {
        return service.update(me, id, form);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@RequestAttribute(AuthFilter.USER) User me, @PathVariable String id) {
        service.delete(me, id);
    }
}
