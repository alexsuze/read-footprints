package com.readfootprints.controller;

import com.readfootprints.config.AuthFilter;
import com.readfootprints.model.Activity;
import com.readfootprints.model.User;
import com.readfootprints.service.ActivityService;
import com.readfootprints.service.ActivityService.ActivityForm;
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

/** 活動 API（規劃書第 10 節）。 */
@RestController
@RequestMapping("/api/activities")
public class ActivityController {

    private final ActivityService service;

    public ActivityController(ActivityService service) {
        this.service = service;
    }

    @GetMapping
    public List<Activity> list(@RequestAttribute(AuthFilter.USER) User me) {
        return service.list(me);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Activity create(@RequestAttribute(AuthFilter.USER) User me, @RequestBody ActivityForm form) {
        return service.create(me, form);
    }

    @PutMapping("/{id}")
    public Activity update(@RequestAttribute(AuthFilter.USER) User me, @PathVariable String id,
                           @RequestBody ActivityForm form) {
        return service.update(me, id, form);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@RequestAttribute(AuthFilter.USER) User me, @PathVariable String id) {
        service.delete(me, id);
    }
}
