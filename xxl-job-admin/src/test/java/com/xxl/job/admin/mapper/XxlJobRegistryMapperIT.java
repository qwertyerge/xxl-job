package com.xxl.job.admin.mapper;

import com.xxl.job.admin.model.XxlJobRegistry;
import com.xxl.job.admin.test.support.MapperITBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Integration tests for {@link XxlJobRegistryMapper}.
 * <p>
 * Tests executor registration, heartbeat timeout detection, and cleanup operations.
 */
class XxlJobRegistryMapperIT extends MapperITBase {

    @Autowired
    private XxlJobRegistryMapper registryMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void testRegistrySaveOrUpdate_shouldInsertNewRecord() {
        // Given
        String registryGroup = "EXECUTOR";
        String registryKey = "test-executor";
        String registryValue = "127.0.0.1:9999";
        Date updateTime = new Date();

        // When
        int result = registryMapper.registrySaveOrUpdate(registryGroup, registryKey, registryValue, updateTime);

        // Then
        assertThat(result).isGreaterThanOrEqualTo(1);

        // Verify record was inserted
        List<XxlJobRegistry> registries = jdbcTemplate.query(
                "SELECT * FROM xxl_job_registry WHERE registry_group = ? AND registry_key = ? AND registry_value = ?",
                (rs, rowNum) -> {
                    XxlJobRegistry registry = new XxlJobRegistry();
                    registry.setId(rs.getInt("id"));
                    registry.setRegistryGroup(rs.getString("registry_group"));
                    registry.setRegistryKey(rs.getString("registry_key"));
                    registry.setRegistryValue(rs.getString("registry_value"));
                    registry.setUpdateTime(rs.getTimestamp("update_time"));
                    return registry;
                },
                registryGroup, registryKey, registryValue
        );

        assertThat(registries).hasSize(1);
        XxlJobRegistry registry = registries.get(0);
        assertThat(registry.getRegistryGroup()).isEqualTo(registryGroup);
        assertThat(registry.getRegistryKey()).isEqualTo(registryKey);
        assertThat(registry.getRegistryValue()).isEqualTo(registryValue);
        // MySQL DATETIME has second precision, so use 1-second tolerance
        assertThat(registry.getUpdateTime()).isCloseTo(updateTime, 1000L);
    }

    @Test
    void testRegistrySaveOrUpdate_shouldUpdateExistingRecord() {
        // Given - insert initial record
        String registryGroup = "EXECUTOR";
        String registryKey = "test-executor-update";
        String registryValue = "127.0.0.1:9999";
        Date initialUpdateTime = new Date();
        registryMapper.registrySaveOrUpdate(registryGroup, registryKey, registryValue, initialUpdateTime);

        // When - update with same (group, key, value) but new time
        Date newUpdateTime = new Date(initialUpdateTime.getTime() + 10000); // 10 seconds later
        int result = registryMapper.registrySaveOrUpdate(registryGroup, registryKey, registryValue, newUpdateTime);

        // Then
        assertThat(result).isGreaterThanOrEqualTo(1);

        // Verify record was updated (should still be only 1 record)
        List<XxlJobRegistry> registries = jdbcTemplate.query(
                "SELECT * FROM xxl_job_registry WHERE registry_group = ? AND registry_key = ? AND registry_value = ?",
                (rs, rowNum) -> {
                    XxlJobRegistry registry = new XxlJobRegistry();
                    registry.setId(rs.getInt("id"));
                    registry.setRegistryGroup(rs.getString("registry_group"));
                    registry.setRegistryKey(rs.getString("registry_key"));
                    registry.setRegistryValue(rs.getString("registry_value"));
                    registry.setUpdateTime(rs.getTimestamp("update_time"));
                    return registry;
                },
                registryGroup, registryKey, registryValue
        );

        assertThat(registries).hasSize(1);
        XxlJobRegistry registry = registries.get(0);
        assertThat(registry.getRegistryGroup()).isEqualTo(registryGroup);
        assertThat(registry.getRegistryKey()).isEqualTo(registryKey);
        assertThat(registry.getRegistryValue()).isEqualTo(registryValue);
        // Verify update_time was updated
        assertThat(registry.getUpdateTime()).isCloseTo(newUpdateTime, 1000L);
    }

    @Test
    void testFindDead_shouldReturnExpiredRegistryIds() {
        // Given - insert a registry with old update_time (2 minutes ago)
        String registryGroup = "EXECUTOR";
        String registryKey = "dead-executor";
        String registryValue = "127.0.0.1:9999";

        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.MINUTE, -2);
        Date oldUpdateTime = cal.getTime();

        registryMapper.registrySaveOrUpdate(registryGroup, registryKey, registryValue, oldUpdateTime);

        // When - find dead with timeout=60 seconds
        Date nowTime = new Date();
        int timeout = 60; // 60 seconds
        List<Integer> deadIds = registryMapper.findDead(timeout, nowTime);

        // Then
        assertThat(deadIds).isNotEmpty();
        assertThat(deadIds.get(0)).isGreaterThan(0);
    }

    @Test
    void testFindDead_shouldNotReturnFreshRegistries() {
        // Given - insert a registry with recent update_time (10 seconds ago)
        String registryGroup = "EXECUTOR";
        String registryKey = "fresh-executor";
        String registryValue = "127.0.0.1:9999";

        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.SECOND, -10);
        Date recentUpdateTime = cal.getTime();

        registryMapper.registrySaveOrUpdate(registryGroup, registryKey, registryValue, recentUpdateTime);

        // Get the ID of the fresh registry
        Integer freshRegistryId = jdbcTemplate.queryForObject(
                "SELECT id FROM xxl_job_registry WHERE registry_group = ? AND registry_key = ?",
                Integer.class,
                registryGroup, registryKey
        );

        // When - find dead with timeout=60 seconds
        Date nowTime = new Date();
        int timeout = 60; // 60 seconds
        List<Integer> deadIds = registryMapper.findDead(timeout, nowTime);

        // Then - should not contain the fresh registry ID
        assertThat(deadIds).doesNotContain(freshRegistryId);
    }

    @Test
    void testFindAll_shouldReturnOnlyAliveRegistries() {
        // Given - insert a fresh registry (10 seconds ago)
        String registryGroup = "EXECUTOR";
        String registryKey = "alive-executor";
        String registryValue = "127.0.0.1:9999";

        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.SECOND, -10);
        Date recentUpdateTime = cal.getTime();

        registryMapper.registrySaveOrUpdate(registryGroup, registryKey, registryValue, recentUpdateTime);

        // When - find all with timeout=60 seconds
        Date nowTime = new Date();
        int timeout = 60;
        List<XxlJobRegistry> aliveRegistries = registryMapper.findAll(timeout, nowTime);

        // Then - should contain the fresh registry
        assertThat(aliveRegistries)
                .anyMatch(registry ->
                        registryGroup.equals(registry.getRegistryGroup()) &&
                                registryKey.equals(registry.getRegistryKey()) &&
                                registryValue.equals(registry.getRegistryValue())
                );
    }

    @Test
    void testRemoveDead_shouldDeleteSpecifiedIds() {
        // Given - insert multiple registries with old update_time
        String registryGroup = "EXECUTOR";
        Date oldUpdateTime = new Date(System.currentTimeMillis() - 120000); // 2 minutes ago

        registryMapper.registrySaveOrUpdate(registryGroup, "dead-1", "127.0.0.1:9999", oldUpdateTime);
        registryMapper.registrySaveOrUpdate(registryGroup, "dead-2", "127.0.0.1:8888", oldUpdateTime);

        // Get the IDs of the inserted records
        Date nowTime = new Date();
        List<Integer> deadIds = registryMapper.findDead(60, nowTime);

        assertThat(deadIds).hasSizeGreaterThanOrEqualTo(2);

        // When - remove dead
        int deleteCount = registryMapper.removeDead(deadIds);

        // Then
        assertThat(deleteCount).isEqualTo(deadIds.size());

        // Verify records were deleted
        List<Integer> remainingDeadIds = registryMapper.findDead(60, nowTime);
        assertThat(remainingDeadIds).doesNotContainAnyElementsOf(deadIds);
    }

    @Test
    void testRegistryDelete_shouldDeleteExactMatch() {
        // Given - insert a registry
        String registryGroup = "EXECUTOR";
        String registryKey = "delete-test";
        String registryValue = "127.0.0.1:9999";
        Date updateTime = new Date();

        registryMapper.registrySaveOrUpdate(registryGroup, registryKey, registryValue, updateTime);

        // When - delete by exact (group, key, value)
        int result = registryMapper.registryDelete(registryGroup, registryKey, registryValue);

        // Then
        assertThat(result).isEqualTo(1);

        // Verify record was deleted
        List<XxlJobRegistry> registries = registryMapper.findAll(60, new Date());
        assertThat(registries)
                .noneMatch(registry ->
                        registryGroup.equals(registry.getRegistryGroup()) &&
                                registryKey.equals(registry.getRegistryKey()) &&
                                registryValue.equals(registry.getRegistryValue())
                );
    }

    @Test
    void testRegistryDelete_shouldNotDeleteMismatchedValue() {
        // Given - insert a registry
        String registryGroup = "EXECUTOR";
        String registryKey = "delete-test-value";
        String registryValue = "127.0.0.1:9999";
        Date updateTime = new Date();

        registryMapper.registrySaveOrUpdate(registryGroup, registryKey, registryValue, updateTime);

        // When - try to delete with different value
        int result = registryMapper.registryDelete(registryGroup, registryKey, "192.168.1.1:8888");

        // Then - should not delete anything
        assertThat(result).isEqualTo(0);

        // Verify record still exists
        List<XxlJobRegistry> registries = registryMapper.findAll(60, new Date());
        assertThat(registries)
                .anyMatch(registry ->
                        registryGroup.equals(registry.getRegistryGroup()) &&
                                registryKey.equals(registry.getRegistryKey()) &&
                                registryValue.equals(registry.getRegistryValue())
                );
    }

    @Test
    void testRemoveByRegistryGroupAndKey_shouldDeleteAllValuesForKey() {
        // Given - insert multiple registries with same (group, key) but different values
        String registryGroup = "EXECUTOR";
        String registryKey = "multi-value-executor";
        Date updateTime = new Date();

        registryMapper.registrySaveOrUpdate(registryGroup, registryKey, "127.0.0.1:9999", updateTime);
        registryMapper.registrySaveOrUpdate(registryGroup, registryKey, "127.0.0.1:8888", updateTime);
        registryMapper.registrySaveOrUpdate(registryGroup, registryKey, "192.168.1.1:7777", updateTime);

        // When - delete by (group, key)
        int result = registryMapper.removeByRegistryGroupAndKey(registryGroup, registryKey);

        // Then
        assertThat(result).isEqualTo(3);

        // Verify all records with that (group, key) were deleted
        List<XxlJobRegistry> registries = registryMapper.findAll(60, new Date());
        assertThat(registries)
                .noneMatch(registry ->
                        registryGroup.equals(registry.getRegistryGroup()) &&
                                registryKey.equals(registry.getRegistryKey())
                );
    }

    @Test
    void testRemoveByRegistryGroupAndKey_shouldNotDeleteDifferentKey() {
        // Given - insert registries with different keys
        String registryGroup = "EXECUTOR";
        Date updateTime = new Date();

        registryMapper.registrySaveOrUpdate(registryGroup, "key-1", "127.0.0.1:9999", updateTime);
        registryMapper.registrySaveOrUpdate(registryGroup, "key-2", "127.0.0.1:8888", updateTime);

        // When - delete only key-1
        int result = registryMapper.removeByRegistryGroupAndKey(registryGroup, "key-1");

        // Then
        assertThat(result).isEqualTo(1);

        // Verify key-2 still exists
        List<XxlJobRegistry> registries = registryMapper.findAll(60, new Date());
        assertThat(registries)
                .anyMatch(registry ->
                        registryGroup.equals(registry.getRegistryGroup()) &&
                                "key-2".equals(registry.getRegistryKey())
                );
    }
}
