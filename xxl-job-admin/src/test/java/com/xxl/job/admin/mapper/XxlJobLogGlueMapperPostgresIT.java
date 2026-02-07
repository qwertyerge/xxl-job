package com.xxl.job.admin.mapper;

import com.xxl.job.admin.model.XxlJobLogGlue;
import com.xxl.job.admin.testcontainers.PostgresMapperITBase;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class XxlJobLogGlueMapperPostgresIT extends PostgresMapperITBase {

    @Resource
    private XxlJobLogGlueMapper xxlJobLogGlueMapper;

    // --- helper ---

    private XxlJobLogGlue createLogGlue(int jobId, String glueType, String glueSource, String glueRemark) {
        XxlJobLogGlue logGlue = new XxlJobLogGlue();
        logGlue.setJobId(jobId);
        logGlue.setGlueType(glueType);
        logGlue.setGlueSource(glueSource);
        logGlue.setGlueRemark(glueRemark);
        logGlue.setAddTime(new Date());
        logGlue.setUpdateTime(new Date());
        return logGlue;
    }

    // --- CRUD tests ---

    @Test
    void save_shouldInsertAndReturnGeneratedId() {
        XxlJobLogGlue logGlue = createLogGlue(1, "BEAN", "source code", "init");

        int rows = xxlJobLogGlueMapper.save(logGlue);

        assertThat(rows).isEqualTo(1);
        assertThat(logGlue.getId()).isGreaterThan(0);
    }

    @Test
    void findByJobId_shouldReturnSavedRecords() {
        int jobId = 100;
        xxlJobLogGlueMapper.save(createLogGlue(jobId, "BEAN", "source1", "remark1"));
        xxlJobLogGlueMapper.save(createLogGlue(jobId, "BEAN", "source2", "remark2"));
        xxlJobLogGlueMapper.save(createLogGlue(999, "BEAN", "other", "other"));

        List<XxlJobLogGlue> result = xxlJobLogGlueMapper.findByJobId(jobId);

        assertThat(result).hasSize(2);
        assertThat(result).allMatch(g -> g.getJobId() == jobId);
    }

    @Test
    void findByJobId_shouldReturnOrderedByIdDesc() {
        int jobId = 200;
        xxlJobLogGlueMapper.save(createLogGlue(jobId, "BEAN", "first", "first"));
        xxlJobLogGlueMapper.save(createLogGlue(jobId, "BEAN", "second", "second"));

        List<XxlJobLogGlue> result = xxlJobLogGlueMapper.findByJobId(jobId);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getId()).isGreaterThan(result.get(1).getId());
    }

    @Test
    void findByJobId_shouldReturnEmptyForNonExistentJob() {
        List<XxlJobLogGlue> result = xxlJobLogGlueMapper.findByJobId(99999);

        assertThat(result).isEmpty();
    }

    // --- removeOld tests ---

    @Test
    void removeOld_shouldKeepLatestRecords() {
        int jobId = 300;
        for (int i = 0; i < 5; i++) {
            XxlJobLogGlue glue = createLogGlue(jobId, "BEAN", "source" + i, "remark" + i);
            glue.setUpdateTime(new Date(System.currentTimeMillis() + i * 1000));
            xxlJobLogGlueMapper.save(glue);
        }

        int deleted = xxlJobLogGlueMapper.removeOld(jobId, 3);

        assertThat(deleted).isEqualTo(2);
        List<XxlJobLogGlue> remaining = xxlJobLogGlueMapper.findByJobId(jobId);
        assertThat(remaining).hasSize(3);
    }

    @Test
    void removeOld_shouldNotDeleteWhenLimitExceedsCount() {
        int jobId = 400;
        xxlJobLogGlueMapper.save(createLogGlue(jobId, "BEAN", "source1", "remark1"));
        xxlJobLogGlueMapper.save(createLogGlue(jobId, "BEAN", "source2", "remark2"));

        int deleted = xxlJobLogGlueMapper.removeOld(jobId, 10);

        assertThat(deleted).isZero();
        List<XxlJobLogGlue> remaining = xxlJobLogGlueMapper.findByJobId(jobId);
        assertThat(remaining).hasSize(2);
    }

    @Test
    void removeOld_shouldNotAffectOtherJobs() {
        int jobId = 500;
        int otherJobId = 501;
        for (int i = 0; i < 3; i++) {
            xxlJobLogGlueMapper.save(createLogGlue(jobId, "BEAN", "s" + i, "r" + i));
        }
        xxlJobLogGlueMapper.save(createLogGlue(otherJobId, "BEAN", "other", "other"));

        xxlJobLogGlueMapper.removeOld(jobId, 1);

        List<XxlJobLogGlue> otherRecords = xxlJobLogGlueMapper.findByJobId(otherJobId);
        assertThat(otherRecords).hasSize(1);
    }

    // --- deleteByJobId tests ---

    @Test
    void deleteByJobId_shouldRemoveAllRecordsForJob() {
        int jobId = 600;
        xxlJobLogGlueMapper.save(createLogGlue(jobId, "BEAN", "source1", "remark1"));
        xxlJobLogGlueMapper.save(createLogGlue(jobId, "BEAN", "source2", "remark2"));

        int deleted = xxlJobLogGlueMapper.deleteByJobId(jobId);

        assertThat(deleted).isEqualTo(2);
        assertThat(xxlJobLogGlueMapper.findByJobId(jobId)).isEmpty();
    }

    @Test
    void deleteByJobId_shouldNotAffectOtherJobs() {
        int jobId = 700;
        int otherJobId = 701;
        xxlJobLogGlueMapper.save(createLogGlue(jobId, "BEAN", "source", "remark"));
        xxlJobLogGlueMapper.save(createLogGlue(otherJobId, "BEAN", "other", "other"));

        xxlJobLogGlueMapper.deleteByJobId(jobId);

        assertThat(xxlJobLogGlueMapper.findByJobId(otherJobId)).hasSize(1);
    }

    @Test
    void deleteByJobId_shouldReturnZeroForNonExistentJob() {
        int deleted = xxlJobLogGlueMapper.deleteByJobId(99999);

        assertThat(deleted).isZero();
    }
}
