package com.xxl.job.admin.mapper;

import com.xxl.job.admin.model.XxlJobLogGlue;
import com.xxl.job.admin.test.support.MapperITBase;
import com.xxl.job.admin.test.support.TestFixtures;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Date;
import java.util.List;

/**
 * Integration tests for XxlJobLogGlueMapper using TestContainers MySQL.
 */
class XxlJobLogGlueMapperIT extends MapperITBase {

    @Autowired
    private XxlJobLogGlueMapper xxlJobLogGlueMapper;

    @Test
    void testSave_and_findByJobId_roundTrip() {
        // given
        XxlJobLogGlue glue = TestFixtures.createLogGlue();

        // when
        int result = xxlJobLogGlueMapper.save(glue);

        // then
        Assertions.assertThat(result).isEqualTo(1);
        Assertions.assertThat(glue.getId()).isPositive();

        List<XxlJobLogGlue> found = xxlJobLogGlueMapper.findByJobId(glue.getJobId());
        Assertions.assertThat(found).isNotEmpty();
        Assertions.assertThat(found.get(0).getId()).isEqualTo(glue.getId());
        Assertions.assertThat(found.get(0).getJobId()).isEqualTo(glue.getJobId());
        Assertions.assertThat(found.get(0).getGlueType()).isEqualTo(glue.getGlueType());
        Assertions.assertThat(found.get(0).getGlueRemark()).isEqualTo(glue.getGlueRemark());
    }

    @Test
    void testFindByJobId_returnsListOrderedByIdDesc() {
        // given - insert 3 glue records
        int jobId = 100;
        for (int i = 0; i < 3; i++) {
            XxlJobLogGlue glue = TestFixtures.createLogGlue();
            glue.setJobId(jobId);
            glue.setGlueRemark("remark-" + i);
            glue.setUpdateTime(new Date());
            xxlJobLogGlueMapper.save(glue);
        }

        // when
        List<XxlJobLogGlue> found = xxlJobLogGlueMapper.findByJobId(jobId);

        // then - should return 3 records ordered by id DESC (latest first)
        Assertions.assertThat(found).hasSize(3);
        Assertions.assertThat(found.get(0).getId()).isGreaterThan(found.get(1).getId());
        Assertions.assertThat(found.get(1).getId()).isGreaterThan(found.get(2).getId());
    }

    @Test
    void testFindByJobId_withNonExistentJobId_returnsEmptyList() {
        // when
        List<XxlJobLogGlue> found = xxlJobLogGlueMapper.findByJobId(-999);

        // then
        Assertions.assertThat(found).isEmpty();
    }

    @Test
    void testRemoveOld_keepsLatestTwoRecords() throws InterruptedException {
        // given - insert 5 glue records with different update times
        int jobId = 200;
        for (int i = 0; i < 5; i++) {
            XxlJobLogGlue glue = TestFixtures.createLogGlue();
            glue.setJobId(jobId);
            glue.setGlueRemark("remark-" + i);
            // Set different update times to ensure proper ordering
            Date updateTime = new Date(System.currentTimeMillis() - (4 - i) * 1000);
            glue.setUpdateTime(updateTime);
            xxlJobLogGlueMapper.save(glue);
            Thread.sleep(10); // Ensure different timestamps
        }

        // when - removeOld should keep only the latest 2 records
        int deleted = xxlJobLogGlueMapper.removeOld(jobId, 2);

        // then - should delete 3 old records, keep 2 latest
        Assertions.assertThat(deleted).isEqualTo(3);
        List<XxlJobLogGlue> remaining = xxlJobLogGlueMapper.findByJobId(jobId);
        Assertions.assertThat(remaining).hasSize(2);
    }

    @Test
    void testDeleteByJobId_removesAllGlueRecordsForJob() {
        // given - insert 3 glue records for the same job
        int jobId = 300;
        for (int i = 0; i < 3; i++) {
            XxlJobLogGlue glue = TestFixtures.createLogGlue();
            glue.setJobId(jobId);
            glue.setGlueRemark("remark-" + i);
            xxlJobLogGlueMapper.save(glue);
        }

        // verify records exist
        List<XxlJobLogGlue> before = xxlJobLogGlueMapper.findByJobId(jobId);
        Assertions.assertThat(before).hasSize(3);

        // when
        int deleted = xxlJobLogGlueMapper.deleteByJobId(jobId);

        // then
        Assertions.assertThat(deleted).isEqualTo(3);
        List<XxlJobLogGlue> after = xxlJobLogGlueMapper.findByJobId(jobId);
        Assertions.assertThat(after).isEmpty();
    }
}
