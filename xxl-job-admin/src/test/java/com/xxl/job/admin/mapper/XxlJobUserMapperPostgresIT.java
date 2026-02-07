package com.xxl.job.admin.mapper;

import com.xxl.job.admin.model.XxlJobUser;
import com.xxl.job.admin.testcontainers.PostgresMapperITBase;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class XxlJobUserMapperPostgresIT extends PostgresMapperITBase {

    @Resource
    private XxlJobUserMapper xxlJobUserMapper;

    // --- helper ---

    private XxlJobUser createUser(String username, String password, int role, String permission) {
        XxlJobUser user = new XxlJobUser();
        user.setUsername(username);
        user.setPassword(password);
        user.setRole(role);
        user.setPermission(permission);
        return user;
    }

    // --- CRUD tests ---

    @Test
    void save_shouldInsertAndReturnGeneratedId() {
        XxlJobUser user = createUser("testuser", "password123", 0, "1,2");

        int rows = xxlJobUserMapper.save(user);

        assertThat(rows).isEqualTo(1);
        assertThat(user.getId()).isGreaterThan(0);
    }

    @Test
    void loadById_shouldReturnSavedUser() {
        XxlJobUser user = createUser("loaduser", "pass123", 0, "1");
        xxlJobUserMapper.save(user);

        XxlJobUser loaded = xxlJobUserMapper.loadById(user.getId());

        assertThat(loaded).isNotNull();
        assertThat(loaded.getUsername()).isEqualTo("loaduser");
        assertThat(loaded.getPassword()).isEqualTo("pass123");
        assertThat(loaded.getRole()).isZero();
        assertThat(loaded.getPermission()).isEqualTo("1");
    }

    @Test
    void loadByUserName_shouldReturnUser() {
        // seed data has 'admin' user
        XxlJobUser admin = xxlJobUserMapper.loadByUserName("admin");

        assertThat(admin).isNotNull();
        assertThat(admin.getUsername()).isEqualTo("admin");
        assertThat(admin.getRole()).isEqualTo(1);
    }

    @Test
    void loadByUserName_shouldReturnNullForNonExistent() {
        XxlJobUser result = xxlJobUserMapper.loadByUserName("nonexistent");

        assertThat(result).isNull();
    }

    @Test
    void update_shouldModifyExistingUser() {
        XxlJobUser user = createUser("upduser", "oldpass", 0, "1");
        xxlJobUserMapper.save(user);

        user.setPassword("newpass");
        user.setRole(1);
        user.setPermission("1,2,3");
        int rows = xxlJobUserMapper.update(user);

        assertThat(rows).isEqualTo(1);

        XxlJobUser loaded = xxlJobUserMapper.loadById(user.getId());
        assertThat(loaded.getPassword()).isEqualTo("newpass");
        assertThat(loaded.getRole()).isEqualTo(1);
        assertThat(loaded.getPermission()).isEqualTo("1,2,3");
    }

    @Test
    void update_shouldNotUpdatePasswordWhenEmpty() {
        XxlJobUser user = createUser("nopwdupd", "origpass", 0, "1");
        xxlJobUserMapper.save(user);

        user.setPassword("");
        user.setRole(1);
        user.setPermission("2");
        xxlJobUserMapper.update(user);

        XxlJobUser loaded = xxlJobUserMapper.loadById(user.getId());
        assertThat(loaded.getPassword()).isEqualTo("origpass");
        assertThat(loaded.getRole()).isEqualTo(1);
    }

    @Test
    void delete_shouldRemoveUser() {
        XxlJobUser user = createUser("deluser", "pass", 0, null);
        xxlJobUserMapper.save(user);

        int rows = xxlJobUserMapper.delete(user.getId());

        assertThat(rows).isEqualTo(1);
        assertThat(xxlJobUserMapper.loadById(user.getId())).isNull();
    }

    @Test
    void delete_shouldReturnZeroForNonExistent() {
        int rows = xxlJobUserMapper.delete(99999);

        assertThat(rows).isZero();
    }

    // --- updateToken tests ---

    @Test
    void updateToken_shouldSetToken() {
        XxlJobUser user = createUser("tokenuser", "pass", 0, null);
        xxlJobUserMapper.save(user);

        int rows = xxlJobUserMapper.updateToken(user.getId(), "my-token-123");

        assertThat(rows).isEqualTo(1);

        XxlJobUser loaded = xxlJobUserMapper.loadById(user.getId());
        assertThat(loaded.getToken()).isEqualTo("my-token-123");
    }

    // --- pagination tests ---

    @Test
    void pageList_shouldReturnPagedResults() {
        for (int i = 0; i < 5; i++) {
            xxlJobUserMapper.save(createUser("pageuser" + i, "pass", 0, null));
        }

        List<XxlJobUser> page1 = xxlJobUserMapper.pageList(0, 3, null, -1);
        assertThat(page1).hasSize(3);

        List<XxlJobUser> page2 = xxlJobUserMapper.pageList(3, 3, null, -1);
        assertThat(page2).isNotEmpty();
    }

    @Test
    void pageListCount_shouldReturnTotalCount() {
        int countBefore = xxlJobUserMapper.pageListCount(0, 10, null, -1);

        xxlJobUserMapper.save(createUser("cntuser", "pass", 0, null));

        int countAfter = xxlJobUserMapper.pageListCount(0, 10, null, -1);
        assertThat(countAfter).isEqualTo(countBefore + 1);
    }

    @Test
    void pageList_shouldFilterByUsername() {
        xxlJobUserMapper.save(createUser("uniquefilter", "pass", 0, null));

        List<XxlJobUser> filtered = xxlJobUserMapper.pageList(0, 10, "uniquefilter", -1);
        assertThat(filtered).isNotEmpty();
        assertThat(filtered).allMatch(u -> u.getUsername().contains("uniquefilter"));

        int count = xxlJobUserMapper.pageListCount(0, 10, "uniquefilter", -1);
        assertThat(count).isEqualTo(filtered.size());
    }

    @Test
    void pageList_shouldFilterByRole() {
        xxlJobUserMapper.save(createUser("roleuser1", "pass", 0, null));
        xxlJobUserMapper.save(createUser("roleuser2", "pass", 1, null));

        List<XxlJobUser> admins = xxlJobUserMapper.pageList(0, 10, null, 1);
        assertThat(admins).isNotEmpty();
        assertThat(admins).allMatch(u -> u.getRole() == 1);

        List<XxlJobUser> normals = xxlJobUserMapper.pageList(0, 10, null, 0);
        assertThat(normals).isNotEmpty();
        assertThat(normals).allMatch(u -> u.getRole() == 0);
    }
}
