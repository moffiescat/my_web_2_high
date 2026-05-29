package com.seckill.utils;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class SnowflakeUtilTest {

    @Test
    void testNextId() {
        SnowflakeUtil sf = new SnowflakeUtil(1, 1);
        long id = sf.nextId();
        assertTrue(id > 0, "ID 应大于 0");
    }

    @Test
    void testIdUniqueness() {
        SnowflakeUtil sf = new SnowflakeUtil(1, 1);
        Set<Long> ids = new HashSet<>();
        for (int i = 0; i < 10000; i++) {
            ids.add(sf.nextId());
        }
        assertEquals(10000, ids.size(), "1 万个 ID 应无重复");
    }

    @Test
    void testExtractField() {
        SnowflakeUtil sf = new SnowflakeUtil(3, 5);
        long id = sf.nextId();
        assertEquals(3, SnowflakeUtil.extractWorkerId(id));
        assertEquals(5, SnowflakeUtil.extractDatacenterId(id));
    }

    @Test
    void testWorkerIdOutOfRange() {
        assertThrows(IllegalArgumentException.class, () -> new SnowflakeUtil(32, 1));
        assertThrows(IllegalArgumentException.class, () -> new SnowflakeUtil(1, 32));
    }
}
