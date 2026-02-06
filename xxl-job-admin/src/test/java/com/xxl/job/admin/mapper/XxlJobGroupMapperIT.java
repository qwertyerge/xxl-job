package com.xxl.job.admin.mapper;

import com.xxl.job.admin.model.XxlJobGroup;
import com.xxl.job.admin.test.support.MapperITBase;
import com.xxl.job.admin.test.support.TestFixtures;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

/**
 * Integration tests for XxlJobGroupMapper using TestContainers MySQL.
 */
class XxlJobGroupMapperIT extends MapperITBase {

    @Autowired
    private XxlJobGroupMapper xxlJobGroupMapper;

    @Test
    void testSave_returnsOneAndIdGreaterThanZero() {
        // given
        XxlJobGroup group = TestFixtures.createJobGroup();

        // when
        int result = xxlJobGroupMapper.save(group);

        // then
        Assertions.assertThat(result).isEqualTo(1);
        Assertions.assertThat(group.getId()).isPositive();
    }

    @Test
    void testLoad_returnsSavedGroup() {
        // given
        XxlJobGroup group = TestFixtures.createJobGroup();
        xxlJobGroupMapper.save(group);
        int savedId = group.getId();

        // when
        XxlJobGroup loaded = xxlJobGroupMapper.load(savedId);

        // then
        Assertions.assertThat(loaded).isNotNull();
        Assertions.assertThat(loaded.getId()).isEqualTo(savedId);
        Assertions.assertThat(loaded.getAppname()).isEqualTo("test-app");
        Assertions.assertThat(loaded.getTitle()).isEqualTo("TestExec");
        Assertions.assertThat(loaded.getAddressType()).isEqualTo(0);
    }

    @Test
    void testLoad_withNonExistentId_returnsNull() {
        // when
        XxlJobGroup loaded = xxlJobGroupMapper.load(-999);

        // then
        Assertions.assertThat(loaded).isNull();
    }

    @Test
    void testUpdate_modifiesFields() {
        // given
        XxlJobGroup group = TestFixtures.createJobGroup();
        xxlJobGroupMapper.save(group);
        int savedId = group.getId();

        // when
        group.setAppname("updated-app");
        group.setTitle("UpdatedT");
        int result = xxlJobGroupMapper.update(group);

        // then
        Assertions.assertThat(result).isEqualTo(1);
        XxlJobGroup loaded = xxlJobGroupMapper.load(savedId);
        Assertions.assertThat(loaded.getAppname()).isEqualTo("updated-app");
        Assertions.assertThat(loaded.getTitle()).isEqualTo("UpdatedT");
    }

    @Test
    void testRemove_deletesGroup() {
        // given
        XxlJobGroup group = TestFixtures.createJobGroup();
        xxlJobGroupMapper.save(group);
        int savedId = group.getId();

        // when
        int result = xxlJobGroupMapper.remove(savedId);

        // then
        Assertions.assertThat(result).isEqualTo(1);
        XxlJobGroup loaded = xxlJobGroupMapper.load(savedId);
        Assertions.assertThat(loaded).isNull();
    }

    @Test
    void testFindAll_containsNewlySavedGroup() {
        // given
        int countBefore = xxlJobGroupMapper.findAll().size();
        XxlJobGroup group = TestFixtures.createJobGroup();
        xxlJobGroupMapper.save(group);

        // when
        List<XxlJobGroup> all = xxlJobGroupMapper.findAll();

        // then
        Assertions.assertThat(all).hasSize(countBefore + 1);
        Assertions.assertThat(all)
                .extracting(XxlJobGroup::getId)
                .contains(group.getId());
    }

    @Test
    void testFindByAddressType_returnsOnlyMatchingGroups() {
        // given
        XxlJobGroup group0 = TestFixtures.createJobGroup();
        group0.setAddressType(0);
        xxlJobGroupMapper.save(group0);

        XxlJobGroup group1 = TestFixtures.createJobGroup();
        group1.setAddressType(1);
        group1.setAppname("test-app-2");
        xxlJobGroupMapper.save(group1);

        // when
        List<XxlJobGroup> addressType0 = xxlJobGroupMapper.findByAddressType(0);
        List<XxlJobGroup> addressType1 = xxlJobGroupMapper.findByAddressType(1);

        // then
        Assertions.assertThat(addressType0)
                .extracting(XxlJobGroup::getAddressType)
                .containsOnly(0);
        Assertions.assertThat(addressType0)
                .extracting(XxlJobGroup::getId)
                .contains(group0.getId());

        Assertions.assertThat(addressType1)
                .extracting(XxlJobGroup::getAddressType)
                .containsOnly(1);
        Assertions.assertThat(addressType1)
                .extracting(XxlJobGroup::getId)
                .contains(group1.getId());
    }

    @Test
    void testPageList_and_pageListCount_consistency() {
        // given
        XxlJobGroup group1 = TestFixtures.createJobGroup();
        group1.setAppname("searchable-app");
        xxlJobGroupMapper.save(group1);

        XxlJobGroup group2 = TestFixtures.createJobGroup();
        group2.setAppname("other-app");
        group2.setTitle("Searchable");
        xxlJobGroupMapper.save(group2);

        // when - search by appname
        List<XxlJobGroup> pageByAppname = xxlJobGroupMapper.pageList(0, 10, "searchable", null);
        int countByAppname = xxlJobGroupMapper.pageListCount(0, 10, "searchable", null);

        // then
        Assertions.assertThat(pageByAppname).hasSize(countByAppname);
        Assertions.assertThat(pageByAppname)
                .extracting(XxlJobGroup::getAppname)
                .allMatch(appname -> appname.contains("searchable"));

        // when - search by title
        List<XxlJobGroup> pageByTitle = xxlJobGroupMapper.pageList(0, 10, null, "Searchable");
        int countByTitle = xxlJobGroupMapper.pageListCount(0, 10, null, "Searchable");

        // then
        Assertions.assertThat(pageByTitle).hasSize(countByTitle);
        Assertions.assertThat(pageByTitle)
                .extracting(XxlJobGroup::getTitle)
                .allMatch(title -> title.contains("Searchable"));
    }

    @Test
    void testPageList_withPagination() {
        // given
        for (int i = 0; i < 5; i++) {
            XxlJobGroup group = TestFixtures.createJobGroup();
            group.setAppname("paginated-app-" + i);
            xxlJobGroupMapper.save(group);
        }

        // when - first page
        List<XxlJobGroup> firstPage = xxlJobGroupMapper.pageList(0, 2, "paginated-app", null);

        // then
        Assertions.assertThat(firstPage).hasSize(2);

        // when - second page
        List<XxlJobGroup> secondPage = xxlJobGroupMapper.pageList(2, 2, "paginated-app", null);

        // then
        Assertions.assertThat(secondPage).hasSize(2);

        // when - third page
        List<XxlJobGroup> thirdPage = xxlJobGroupMapper.pageList(4, 2, "paginated-app", null);

        // then
        Assertions.assertThat(thirdPage).hasSize(1);
    }
}
