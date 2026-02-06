package com.xxl.job.admin.mapper;

import com.xxl.job.admin.model.XxlJobUser;
import com.xxl.job.admin.test.support.MapperITBase;
import com.xxl.job.admin.test.support.TestFixtures;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

/**
 * Integration tests for XxlJobUserMapper using TestContainers MySQL.
 */
class XxlJobUserMapperIT extends MapperITBase {

    @Autowired
    private XxlJobUserMapper xxlJobUserMapper;

    @Test
    void testSave_and_loadById_roundTrip() {
        // given
        XxlJobUser user = TestFixtures.createUser();

        // when
        int result = xxlJobUserMapper.save(user);
        int savedId = user.getId();

        // then
        Assertions.assertThat(result).isEqualTo(1);
        Assertions.assertThat(savedId).isPositive();

        XxlJobUser loaded = xxlJobUserMapper.loadById(savedId);
        Assertions.assertThat(loaded).isNotNull();
        Assertions.assertThat(loaded.getId()).isEqualTo(savedId);
        Assertions.assertThat(loaded.getUsername()).isEqualTo("testuser");
        Assertions.assertThat(loaded.getPassword()).isEqualTo("testpassword");
        Assertions.assertThat(loaded.getRole()).isEqualTo(0);
        Assertions.assertThat(loaded.getPermission()).isEqualTo("");
    }

    @Test
    void testLoadByUserName_returnsExactMatch() {
        // given
        XxlJobUser user = TestFixtures.createUser();
        xxlJobUserMapper.save(user);

        // when
        XxlJobUser loaded = xxlJobUserMapper.loadByUserName("testuser");

        // then
        Assertions.assertThat(loaded).isNotNull();
        Assertions.assertThat(loaded.getUsername()).isEqualTo("testuser");
    }

    @Test
    void testLoadByUserName_withNonExistentUser_returnsNull() {
        // when
        XxlJobUser loaded = xxlJobUserMapper.loadByUserName("nonexistent");

        // then
        Assertions.assertThat(loaded).isNull();
    }

    @Test
    void testUpdate_modifiesRoleAndPermission() {
        // given
        XxlJobUser user = TestFixtures.createUser();
        xxlJobUserMapper.save(user);
        int savedId = user.getId();

        // when
        user.setRole(1);
        user.setPermission("1,2,3");
        int result = xxlJobUserMapper.update(user);

        // then
        Assertions.assertThat(result).isEqualTo(1);
        XxlJobUser loaded = xxlJobUserMapper.loadById(savedId);
        Assertions.assertThat(loaded.getRole()).isEqualTo(1);
        Assertions.assertThat(loaded.getPermission()).isEqualTo("1,2,3");
        // Password should remain unchanged
        Assertions.assertThat(loaded.getPassword()).isEqualTo("testpassword");
    }

    @Test
    void testUpdate_whenPasswordIsNull_doesNotUpdatePassword() {
        // given
        XxlJobUser user = TestFixtures.createUser();
        xxlJobUserMapper.save(user);
        int savedId = user.getId();
        String originalPassword = user.getPassword();

        // when - update with null password
        user.setPassword(null);
        user.setRole(1);
        xxlJobUserMapper.update(user);

        // then - password should not be changed
        XxlJobUser loaded = xxlJobUserMapper.loadById(savedId);
        Assertions.assertThat(loaded.getPassword()).isEqualTo(originalPassword);
        Assertions.assertThat(loaded.getRole()).isEqualTo(1);
    }

    @Test
    void testDelete_thenLoadByIdReturnsNull() {
        // given
        XxlJobUser user = TestFixtures.createUser();
        xxlJobUserMapper.save(user);
        int savedId = user.getId();

        // when
        int result = xxlJobUserMapper.delete(savedId);

        // then
        Assertions.assertThat(result).isEqualTo(1);
        XxlJobUser loaded = xxlJobUserMapper.loadById(savedId);
        Assertions.assertThat(loaded).isNull();
    }

    @Test
    void testUpdateToken_onlyUpdatesTokenField() {
        // given
        XxlJobUser user = TestFixtures.createUser();
        xxlJobUserMapper.save(user);
        int savedId = user.getId();

        // when
        String newToken = "test-token-12345";
        int result = xxlJobUserMapper.updateToken(savedId, newToken);

        // then
        Assertions.assertThat(result).isEqualTo(1);
        XxlJobUser loaded = xxlJobUserMapper.loadById(savedId);
        Assertions.assertThat(loaded.getToken()).isEqualTo(newToken);
        // Other fields should remain unchanged
        Assertions.assertThat(loaded.getUsername()).isEqualTo("testuser");
        Assertions.assertThat(loaded.getRole()).isEqualTo(0);
    }

    @Test
    void testPageList_and_pageListCount_consistency() {
        // given
        XxlJobUser user1 = TestFixtures.createUser();
        user1.setUsername("searchable-user-1");
        user1.setRole(0);
        xxlJobUserMapper.save(user1);

        XxlJobUser user2 = TestFixtures.createUser();
        user2.setUsername("searchable-user-2");
        user2.setRole(1);
        xxlJobUserMapper.save(user2);

        // when - search by username
        List<XxlJobUser> pageByUsername = xxlJobUserMapper.pageList(0, 10, "searchable", -1);
        int countByUsername = xxlJobUserMapper.pageListCount(0, 10, "searchable", -1);

        // then
        Assertions.assertThat(pageByUsername).hasSize(countByUsername);
        Assertions.assertThat(pageByUsername)
                .extracting(XxlJobUser::getUsername)
                .allMatch(username -> username.contains("searchable"));

        // when - search by role
        List<XxlJobUser> pageByRole = xxlJobUserMapper.pageList(0, 10, null, 1);
        int countByRole = xxlJobUserMapper.pageListCount(0, 10, null, 1);

        // then
        Assertions.assertThat(pageByRole).hasSize(countByRole);
        Assertions.assertThat(pageByRole)
                .extracting(XxlJobUser::getRole)
                .containsOnly(1);

        // when - search by both username and role
        List<XxlJobUser> pageByBoth = xxlJobUserMapper.pageList(0, 10, "searchable-user-2", 1);
        int countByBoth = xxlJobUserMapper.pageListCount(0, 10, "searchable-user-2", 1);

        // then
        Assertions.assertThat(pageByBoth).hasSize(countByBoth);
        Assertions.assertThat(pageByBoth).hasSize(1);
        Assertions.assertThat(pageByBoth.get(0).getUsername()).isEqualTo("searchable-user-2");
        Assertions.assertThat(pageByBoth.get(0).getRole()).isEqualTo(1);
    }
}
