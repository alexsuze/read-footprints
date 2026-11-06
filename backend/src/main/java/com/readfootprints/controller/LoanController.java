package com.readfootprints.controller;

import com.readfootprints.config.AuthFilter;
import com.readfootprints.model.User;
import com.readfootprints.service.LoanService;
import com.readfootprints.service.LoanService.LoanView;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 借書 API（規劃書第 10 節）。 */
@RestController
@RequestMapping("/api/loans")
public class LoanController {

    private final LoanService service;

    public LoanController(LoanService service) {
        this.service = service;
    }

    public record LoanRequest(String bookId) {}

    @GetMapping
    public List<LoanView> list(@RequestAttribute(AuthFilter.USER) User me,
                               @RequestParam(defaultValue = "false") boolean all,
                               @RequestParam(required = false) String status) {
        return service.list(me, all, status);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public LoanView request(@RequestAttribute(AuthFilter.USER) User me, @RequestBody LoanRequest body) {
        return service.request(me, body.bookId());
    }

    @PostMapping("/{id}/lend")
    public LoanView lend(@RequestAttribute(AuthFilter.USER) User me, @PathVariable String id) {
        return service.lend(me, id);
    }

    @PostMapping("/{id}/return")
    public LoanView giveBack(@RequestAttribute(AuthFilter.USER) User me, @PathVariable String id) {
        return service.giveBack(me, id);
    }

    @PostMapping("/{id}/reclaim")
    public LoanView reclaim(@RequestAttribute(AuthFilter.USER) User me, @PathVariable String id) {
        return service.reclaim(me, id);
    }
}
