package com.readfootprints.controller;

import com.readfootprints.config.AuthFilter;
import com.readfootprints.model.User;
import com.readfootprints.service.StatsService;
import com.readfootprints.service.StatsService.Summary;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 閱讀統計 API（規劃書第 10 節）。購買 / 完讀分析由前端用書籍列表計算。 */
@RestController
@RequestMapping("/api/stats")
public class StatsController {

    private final StatsService service;

    public StatsController(StatsService service) {
        this.service = service;
    }

    @GetMapping("/summary")
    public Summary summary(@RequestAttribute(AuthFilter.USER) User me) {
        return service.summary(me);
    }
}
