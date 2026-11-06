package com.readfootprints.controller;

import com.readfootprints.config.AuthFilter;
import com.readfootprints.migration.LegacyImporter;
import com.readfootprints.model.User;
import com.readfootprints.service.UserService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 管理員 API：使用者列表、核准 / 停用（規劃書 4.6）、匯入 1.0 資料（第 9 節）。非管理員回 403。
 */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final UserService service;
    private final LegacyImporter importer;

    public AdminController(UserService service, LegacyImporter importer) {
        this.service = service;
        this.importer = importer;
    }

    @PostMapping("/import-legacy")
    public LegacyImporter.Result importLegacy(@RequestAttribute(AuthFilter.USER) User admin) {
        return importer.run(admin);
    }

    @GetMapping("/import-legacy/progress")
    public LegacyImporter.Progress importProgress(@RequestAttribute(AuthFilter.USER) User admin) {
        return importer.progress(admin);
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
