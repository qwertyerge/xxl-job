package com.xxl.job.admin.mapper;

import com.xxl.job.admin.model.XxlJobGroup;
import com.xxl.job.admin.testcontainers.PostgresMapperITBase;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class XxlJobGroupMapperPostgresIT extends PostgresMapperITBase {

    @Resource
    private XxlJobGroupMapper xxlJobGroupMapper;

    // --- helper ---

    private XxlJobGroup createGroup(String appname, String title, int addressType) {
        XxlJobGroup group = new XxlJobGroup();
        group.setAppname(appname);
        group.setTitle(title);
        group.setAddressType(addressType);
        group.setAddressList(null);
        group.setUpdateTime(new Date());
        return group;
    }

    // --- CRUD tests ---

    @Test
    void save_shouldInsertAndReturnGeneratedId() {
        XxlJobGroup group = createGroup("test-app", "TestTitle", 0);

        int rows = xxlJobGroupMapper.save(group);

        assertThat(rows).isEqualTo(1);
        assertThat(group.getId()).isGreaterThan(0);
    }

    @Test
    void load_shouldReturnSavedGroup() {
        XxlJobGroup group = createGroup("load-app", "LoadTitle", 0);
        xxlJobGroupMapper.save(group);

        XxlJobGroup loaded = xxlJobGroupMapper.load(group.getId());

        assertThat(loaded).isNotNull();
        assertThat(loaded.getAppname()).isEqualTo("load-app");
        assertThat(loaded.getTitle()).isEqualTo("LoadTitle");
        assertThat(loaded.getAddressType()).isZero();
    }

    @Test
    void update_shouldModifyExistingGroup() {
        XxlJobGroup group = createGroup("upd-app", "UpdTitle", 0);
        xxlJobGroupMapper.save(group);

        group.setAppname("upd-app-v2");
        group.setTitle("UpdTitleV2");
        group.setAddressType(1);
        group.setAddressList("192.168.1.1:9999");
        group.setUpdateTime(new Date());
        int rows = xxlJobGroupMapper.update(group);

        assertThat(rows).isEqualTo(1);

        XxlJobGroup loaded = xxlJobGroupMapper.load(group.getId());
        assertThat(loaded.getAppname()).isEqualTo("upd-app-v2");
        assertThat(loaded.getTitle()).isEqualTo("UpdTitleV2");
        assertThat(loaded.getAddressType()).isEqualTo(1);
        assertThat(loaded.getAddressList()).isEqualTo("192.168.1.1:9999");
    }

    @Test
    void remove_shouldDeleteGroup() {
        XxlJobGroup group = createGroup("rm-app", "RmTitle", 0);
        xxlJobGroupMapper.save(group);

        int rows = xxlJobGroupMapper.remove(group.getId());

        assertThat(rows).isEqualTo(1);
        assertThat(xxlJobGroupMapper.load(group.getId())).isNull();
    }

    // --- query tests ---

    @Test
    void findAll_shouldReturnAllGroups() {
        List<XxlJobGroup> all = xxlJobGroupMapper.findAll();

        // DDL inserts 2 seed rows
        assertThat(all).hasSizeGreaterThanOrEqualTo(2);
    }

    @Test
    void findByAddressType_shouldFilterCorrectly() {
        // seed data has addressType=0; insert one with addressType=1
        XxlJobGroup manual = createGroup("manual-app", "ManualTitle", 1);
        manual.setAddressList("10.0.0.1:9999");
        xxlJobGroupMapper.save(manual);

        List<XxlJobGroup> autoList = xxlJobGroupMapper.findByAddressType(0);
        assertThat(autoList).isNotEmpty();
        assertThat(autoList).allMatch(g -> g.getAddressType() == 0);

        List<XxlJobGroup> manualList = xxlJobGroupMapper.findByAddressType(1);
        assertThat(manualList).isNotEmpty();
        assertThat(manualList).allMatch(g -> g.getAddressType() == 1);
    }

    // --- pagination tests ---

    @Test
    void pageList_shouldReturnPagedResults() {
        // insert extra groups to ensure pagination works
        for (int i = 0; i < 5; i++) {
            xxlJobGroupMapper.save(createGroup("page-app-" + i, "Page" + i, 0));
        }

        List<XxlJobGroup> page1 = xxlJobGroupMapper.pageList(0, 3, null, null);
        assertThat(page1).hasSize(3);

        List<XxlJobGroup> page2 = xxlJobGroupMapper.pageList(3, 3, null, null);
        assertThat(page2).isNotEmpty();
    }

    @Test
    void pageListCount_shouldReturnTotalCount() {
        int countBefore = xxlJobGroupMapper.pageListCount(0, 10, null, null);

        xxlJobGroupMapper.save(createGroup("cnt-app", "CntTitle", 0));

        int countAfter = xxlJobGroupMapper.pageListCount(0, 10, null, null);
        assertThat(countAfter).isEqualTo(countBefore + 1);
    }

    @Test
    void pageList_shouldFilterByAppname() {
        xxlJobGroupMapper.save(createGroup("unique-app", "UniqueTitle", 0));

        List<XxlJobGroup> filtered = xxlJobGroupMapper.pageList(0, 10, "unique-app", null);
        assertThat(filtered).isNotEmpty();
        assertThat(filtered).allMatch(g -> g.getAppname().contains("unique-app"));

        int count = xxlJobGroupMapper.pageListCount(0, 10, "unique-app", null);
        assertThat(count).isEqualTo(filtered.size());
    }

    @Test
    void pageList_shouldFilterByTitle() {
        xxlJobGroupMapper.save(createGroup("title-app", "TitleFilter", 0));

        List<XxlJobGroup> filtered = xxlJobGroupMapper.pageList(0, 10, null, "TitleFilter");
        assertThat(filtered).isNotEmpty();
        assertThat(filtered).allMatch(g -> g.getTitle().contains("TitleFilter"));

        int count = xxlJobGroupMapper.pageListCount(0, 10, null, "TitleFilter");
        assertThat(count).isEqualTo(filtered.size());
    }
}
