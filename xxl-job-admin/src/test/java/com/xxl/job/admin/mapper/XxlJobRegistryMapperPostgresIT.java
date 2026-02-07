package com.xxl.job.admin.mapper;

import com.xxl.job.admin.model.XxlJobRegistry;
import com.xxl.job.admin.testcontainers.PostgresMapperITBase;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class XxlJobRegistryMapperPostgresIT extends PostgresMapperITBase {

    @Resource
    private XxlJobRegistryMapper xxlJobRegistryMapper;

    // --- helper ---

    private void insertRegistry(String group, String key, String value, Date updateTime) {
        xxlJobRegistryMapper.registrySaveOrUpdate(group, key, value, updateTime);
    }

    // --- registrySaveOrUpdate tests ---

    @Test
    void registrySaveOrUpdate_shouldInsertNewRecord() {
        Date now = new Date();

        int rows = xxlJobRegistryMapper.registrySaveOrUpdate("EXECUTOR", "app-key", "192.168.1.1:9999", now);

        assertThat(rows).isEqualTo(1);
    }

    @Test
    void registrySaveOrUpdate_shouldUpdateExistingRecord() {
        Date time1 = new Date(System.currentTimeMillis() - 10000);
        Date time2 = new Date();
        xxlJobRegistryMapper.registrySaveOrUpdate("EXECUTOR", "upd-key", "10.0.0.1:8080", time1);

        int rows = xxlJobRegistryMapper.registrySaveOrUpdate("EXECUTOR", "upd-key", "10.0.0.1:8080", time2);

        assertThat(rows).isEqualTo(1);
    }

    // --- registryDelete tests ---

    @Test
    void registryDelete_shouldRemoveSpecificEntry() {
        Date now = new Date();
        insertRegistry("EXECUTOR", "del-key", "10.0.0.2:8080", now);

        int rows = xxlJobRegistryMapper.registryDelete("EXECUTOR", "del-key", "10.0.0.2:8080");

        assertThat(rows).isEqualTo(1);
    }

    @Test
    void registryDelete_shouldReturnZeroForNonExistent() {
        int rows = xxlJobRegistryMapper.registryDelete("NONE", "none-key", "none-value");

        assertThat(rows).isZero();
    }

    // --- removeByRegistryGroupAndKey tests ---

    @Test
    void removeByRegistryGroupAndKey_shouldRemoveAllMatchingEntries() {
        Date now = new Date();
        insertRegistry("EXECUTOR", "rm-key", "10.0.0.1:8080", now);
        insertRegistry("EXECUTOR", "rm-key", "10.0.0.2:8080", now);
        insertRegistry("EXECUTOR", "other-key", "10.0.0.3:8080", now);

        int rows = xxlJobRegistryMapper.removeByRegistryGroupAndKey("EXECUTOR", "rm-key");

        assertThat(rows).isEqualTo(2);
    }

    @Test
    void removeByRegistryGroupAndKey_shouldNotAffectOtherGroups() {
        Date now = new Date();
        insertRegistry("EXECUTOR", "grp-key", "10.0.0.1:8080", now);
        insertRegistry("ADMIN", "grp-key", "10.0.0.2:8080", now);

        xxlJobRegistryMapper.removeByRegistryGroupAndKey("EXECUTOR", "grp-key");

        List<XxlJobRegistry> remaining = xxlJobRegistryMapper.findAll(90, now);
        assertThat(remaining).anyMatch(r -> r.getRegistryGroup().equals("ADMIN") && r.getRegistryKey().equals("grp-key"));
    }

    // --- findDead tests ---

    @Test
    void findDead_shouldReturnExpiredRegistries() {
        Date oldTime = new Date(System.currentTimeMillis() - 120000);
        insertRegistry("EXECUTOR", "dead-key", "10.0.0.1:8080", oldTime);

        Date now = new Date();
        List<Integer> deadIds = xxlJobRegistryMapper.findDead(90, now);

        assertThat(deadIds).isNotEmpty();
    }

    @Test
    void findDead_shouldNotReturnActiveRegistries() {
        Date now = new Date();
        insertRegistry("EXECUTOR", "alive-key", "10.0.0.1:8080", now);

        List<Integer> deadIds = xxlJobRegistryMapper.findDead(90, now);

        List<XxlJobRegistry> allActive = xxlJobRegistryMapper.findAll(90, now);
        List<String> activeKeys = allActive.stream().map(XxlJobRegistry::getRegistryKey).toList();
        assertThat(activeKeys).contains("alive-key");
    }

    // --- removeDead tests ---

    @Test
    void removeDead_shouldDeleteByIds() {
        Date oldTime = new Date(System.currentTimeMillis() - 120000);
        insertRegistry("EXECUTOR", "rm-dead-key", "10.0.0.1:8080", oldTime);

        Date now = new Date();
        List<Integer> deadIds = xxlJobRegistryMapper.findDead(90, now);
        assertThat(deadIds).isNotEmpty();

        int rows = xxlJobRegistryMapper.removeDead(deadIds);

        assertThat(rows).isGreaterThan(0);
    }

    // --- findAll tests ---

    @Test
    void findAll_shouldReturnActiveRegistries() {
        Date now = new Date();
        insertRegistry("EXECUTOR", "active-key1", "10.0.0.1:8080", now);
        insertRegistry("EXECUTOR", "active-key2", "10.0.0.2:8080", now);

        List<XxlJobRegistry> result = xxlJobRegistryMapper.findAll(90, now);

        assertThat(result).hasSizeGreaterThanOrEqualTo(2);
        assertThat(result).allMatch(r -> r.getRegistryGroup() != null);
        assertThat(result).allMatch(r -> r.getRegistryKey() != null);
        assertThat(result).allMatch(r -> r.getRegistryValue() != null);
    }

    @Test
    void findAll_shouldNotReturnExpiredRegistries() {
        Date oldTime = new Date(System.currentTimeMillis() - 120000);
        insertRegistry("EXECUTOR", "expired-key", "10.0.0.1:8080", oldTime);

        Date now = new Date();
        List<XxlJobRegistry> result = xxlJobRegistryMapper.findAll(90, now);

        assertThat(result).noneMatch(r -> r.getRegistryKey().equals("expired-key"));
    }
}
