package com.readfootprints.controller;

import com.readfootprints.config.AuthFilter;
import com.readfootprints.model.ReadingLog;
import com.readfootprints.model.User;
import com.readfootprints.service.ReadingLogService;
import com.readfootprints.service.ReadingLogService.LogForm;
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

import java.util.List;

/** 每日閱讀分鐘數 API（規劃書第 10 節）。 */
@RestController
@RequestMapping("/api/reading-logs")
public class ReadingLogController {

    private final ReadingLogService service;

    public ReadingLogController(ReadingLogService service) {
        this.service = service;
    }

    @GetMapping
    public List<ReadingLog> list(@RequestAttribute(AuthFilter.USER) User me,
                                 @RequestParam(required = false) String from,
                                 @RequestParam(required = false) String to) {
        return service.list(me, from, to);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ReadingLog create(@RequestAttribute(AuthFilter.USER) User me, @RequestBody LogForm form) {
        return service.create(me, form);
    }

    @PutMapping("/{id}")
    public ReadingLog update(@RequestAttribute(AuthFilter.USER) User me, @PathVariable String id,
                             @RequestBody LogForm form) {
        return service.update(me, id, form);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@RequestAttribute(AuthFilter.USER) User me, @PathVariable String id) {
        service.delete(me, id);
    }
}
