package com.readfootprints.service;

import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UserServiceTest {

    @Test
    void columnsKeepOrderAndDropDuplicates() {
        assertEquals(List.of("state", "authors", "price"), UserService.cleanColumns(List.of("state", "authors", "state", "price")));
        assertEquals(List.of(), UserService.cleanColumns(List.of()));
    }

    @Test
    void columnsRejectUnknownOrMissing() {
        assertThrows(ResponseStatusException.class, () -> UserService.cleanColumns(List.of("title")));
        assertThrows(ResponseStatusException.class, () -> UserService.cleanColumns(List.of("password")));
        assertThrows(ResponseStatusException.class, () -> UserService.cleanColumns(null));
    }
}
