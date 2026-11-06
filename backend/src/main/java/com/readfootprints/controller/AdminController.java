package com.readfootprints.controller;

import com.readfootprints.config.AuthFilter;
import com.readfootprints.model.User;
import com.readfootprints.service.UserService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 管理員 API：使用者列表、核准 / 停用（規劃書 4.6）。非管理員回 403。
 * 1.0 資料匯入已在階段 5 移除（規劃書第 9 節），需要時可從 git 歷史找回 LegacyImporter。
 */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final UserService service;

    public AdminController(UserService service) {
        this.service = service;
    }

    @GetMapping("/users")
    public List<User> list(@RequestAttribute(AuthFilter.USER) User admin) {
        return service.list(admin);
    }

    /** body：{ status: active / disabled / pending } */
    @PutMapping("/users/{id}/status")
    public User setStatus(@RequestAttribute(AuthFilter.USER) User admin, @PathVariable String id,
                          @RequestBody Map<String, String> body) {
        return service.setStatus(admin, id, body.get("status"));
    }
}
