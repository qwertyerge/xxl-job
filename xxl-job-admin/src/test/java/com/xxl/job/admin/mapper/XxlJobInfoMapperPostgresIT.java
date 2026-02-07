package com.xxl.job.admin.mapper;

import com.xxl.job.admin.model.XxlJobInfo;
import com.xxl.job.admin.testcontainers.PostgresMapperITBase;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class XxlJobInfoMapperPostgresIT extends PostgresMapperITBase {

    @Resource
    private XxlJobInfoMapper xxlJobInfoMapper;

    // --- helper ---

    private XxlJobInfo createJobInfo(int jobGroup, String jobDesc, String author) {
        XxlJobInfo info = new XxlJobInfo();
        info.setJobGroup(jobGroup);
        info.setJobDesc(jobDesc);
        info.setAddTime(new Date());
        info.setUpdateTime(new Date());
        info.setAuthor(author);
        info.setAlarmEmail("");
        info.setScheduleType("CRON");
        info.setScheduleConf("0 0 0 * * ? *");
        info.setMisfireStrategy("DO_NOTHING");
        info.setExecutorRouteStrategy("FIRST");
        info.setExecutorHandler("testHandler");
        info.setExecutorParam("");
        info.setExecutorBlockStrategy("SERIAL_EXECUTION");
        info.setExecutorTimeout(0);
        info.setExecutorFailRetryCount(0);
        info.setGlueType("BEAN");
        info.setGlueSource("");
        info.setGlueRemark("GLUE init");
        info.setGlueUpdatetime(new Date());
        info.setChildJobId("");
        info.setTriggerStatus(0);
        info.setTriggerLastTime(0);
        info.setTriggerNextTime(0);
        return info;
    }

    // --- CRUD tests ---

    @Test
    void save_shouldInsertAndReturnGeneratedId() {
        XxlJobInfo info = createJobInfo(1, "SaveTest", "tester");

        int rows = xxlJobInfoMapper.save(info);

        assertThat(rows).isEqualTo(1);
        assertThat(info.getId()).isGreaterThan(0);
    }

    @Test
    void loadById_shouldReturnSavedJob() {
        XxlJobInfo info = createJobInfo(1, "LoadTest", "tester");
        xxlJobInfoMapper.save(info);

        XxlJobInfo loaded = xxlJobInfoMapper.loadById(info.getId());

        assertThat(loaded).isNotNull();
        assertThat(loaded.getJobDesc()).isEqualTo("LoadTest");
        assertThat(loaded.getAuthor()).isEqualTo("tester");
        assertThat(loaded.getScheduleType()).isEqualTo("CRON");
        assertThat(loaded.getExecutorHandler()).isEqualTo("testHandler");
    }

    @Test
    void update_shouldModifyExistingJob() {
        XxlJobInfo info = createJobInfo(1, "UpdateTest", "tester");
        xxlJobInfoMapper.save(info);

        info.setJobDesc("UpdatedDesc");
        info.setAuthor("updatedAuthor");
        info.setExecutorHandler("updatedHandler");
        info.setUpdateTime(new Date());
        int rows = xxlJobInfoMapper.update(info);

        assertThat(rows).isEqualTo(1);

        XxlJobInfo loaded = xxlJobInfoMapper.loadById(info.getId());
        assertThat(loaded.getJobDesc()).isEqualTo("UpdatedDesc");
        assertThat(loaded.getAuthor()).isEqualTo("updatedAuthor");
        assertThat(loaded.getExecutorHandler()).isEqualTo("updatedHandler");
    }

    @Test
    void delete_shouldRemoveJob() {
        XxlJobInfo info = createJobInfo(1, "DeleteTest", "tester");
        xxlJobInfoMapper.save(info);

        int rows = xxlJobInfoMapper.delete(info.getId());

        assertThat(rows).isEqualTo(1);
        assertThat(xxlJobInfoMapper.loadById(info.getId())).isNull();
    }

    // --- query tests ---

    @Test
    void getJobsByGroup_shouldReturnJobsForGroup() {
        XxlJobInfo info1 = createJobInfo(1, "GroupJob1", "tester");
        XxlJobInfo info2 = createJobInfo(1, "GroupJob2", "tester");
        xxlJobInfoMapper.save(info1);
        xxlJobInfoMapper.save(info2);

        List<XxlJobInfo> jobs = xxlJobInfoMapper.getJobsByGroup(1);

        // seed data has 1 job in group 1, plus 2 we inserted
        assertThat(jobs).hasSizeGreaterThanOrEqualTo(2);
        assertThat(jobs).allMatch(j -> j.getJobGroup() == 1);
    }

    @Test
    void findAllCount_shouldReturnTotalCount() {
        int countBefore = xxlJobInfoMapper.findAllCount();

        xxlJobInfoMapper.save(createJobInfo(1, "CountTest", "tester"));

        int countAfter = xxlJobInfoMapper.findAllCount();
        assertThat(countAfter).isEqualTo(countBefore + 1);
    }

    // --- pagination tests ---

    @Test
    void pageList_shouldReturnPagedResults() {
        for (int i = 0; i < 5; i++) {
            xxlJobInfoMapper.save(createJobInfo(1, "PageJob" + i, "tester"));
        }

        List<XxlJobInfo> page1 = xxlJobInfoMapper.pageList(0, 3, 0, -1, null, null, null);
        assertThat(page1).hasSize(3);

        List<XxlJobInfo> page2 = xxlJobInfoMapper.pageList(3, 3, 0, -1, null, null, null);
        assertThat(page2).isNotEmpty();
    }

    @Test
    void pageListCount_shouldReturnTotalCount() {
        int countBefore = xxlJobInfoMapper.pageListCount(0, 10, 0, -1, null, null, null);

        xxlJobInfoMapper.save(createJobInfo(1, "PageCntTest", "tester"));

        int countAfter = xxlJobInfoMapper.pageListCount(0, 10, 0, -1, null, null, null);
        assertThat(countAfter).isEqualTo(countBefore + 1);
    }

    @Test
    void pageList_shouldFilterByJobGroup() {
        XxlJobInfo info = createJobInfo(2, "GroupFilter", "tester");
        xxlJobInfoMapper.save(info);

        List<XxlJobInfo> filtered = xxlJobInfoMapper.pageList(0, 10, 2, -1, null, null, null);
        assertThat(filtered).isNotEmpty();
        assertThat(filtered).allMatch(j -> j.getJobGroup() == 2);
    }

    @Test
    void pageList_shouldFilterByJobDesc() {
        xxlJobInfoMapper.save(createJobInfo(1, "UniqueDesc", "tester"));

        List<XxlJobInfo> filtered = xxlJobInfoMapper.pageList(0, 10, 0, -1, "UniqueDesc", null, null);
        assertThat(filtered).isNotEmpty();
        assertThat(filtered).allMatch(j -> j.getJobDesc().contains("UniqueDesc"));
    }

    @Test
    void pageList_shouldFilterByExecutorHandler() {
        XxlJobInfo info = createJobInfo(1, "HandlerFilter", "tester");
        info.setExecutorHandler("uniqueHandler");
        xxlJobInfoMapper.save(info);

        List<XxlJobInfo> filtered = xxlJobInfoMapper.pageList(0, 10, 0, -1, null, "uniqueHandler", null);
        assertThat(filtered).isNotEmpty();
        assertThat(filtered).allMatch(j -> j.getExecutorHandler().contains("uniqueHandler"));
    }

    @Test
    void pageList_shouldFilterByAuthor() {
        xxlJobInfoMapper.save(createJobInfo(1, "AuthorFilter", "uniqueAuthor"));

        List<XxlJobInfo> filtered = xxlJobInfoMapper.pageList(0, 10, 0, -1, null, null, "uniqueAuthor");
        assertThat(filtered).isNotEmpty();
        assertThat(filtered).allMatch(j -> j.getAuthor().contains("uniqueAuthor"));
    }

    @Test
    void pageList_shouldFilterByTriggerStatus() {
        XxlJobInfo info = createJobInfo(1, "StatusFilter", "tester");
        info.setTriggerStatus(1);
        info.setTriggerNextTime(System.currentTimeMillis() + 60000);
        xxlJobInfoMapper.save(info);

        List<XxlJobInfo> filtered = xxlJobInfoMapper.pageList(0, 10, 0, 1, null, null, null);
        assertThat(filtered).isNotEmpty();
        assertThat(filtered).allMatch(j -> j.getTriggerStatus() == 1);
    }

    // --- schedule tests ---

    @Test
    void scheduleJobQuery_shouldReturnTriggeredJobs() {
        XxlJobInfo info = createJobInfo(1, "ScheduleQuery", "tester");
        info.setTriggerStatus(1);
        long futureTime = System.currentTimeMillis() + 60000;
        info.setTriggerNextTime(futureTime);
        xxlJobInfoMapper.save(info);

        List<XxlJobInfo> jobs = xxlJobInfoMapper.scheduleJobQuery(futureTime + 1000, 10);

        assertThat(jobs).isNotEmpty();
        assertThat(jobs).anyMatch(j -> j.getId() == info.getId());
    }

    @Test
    void scheduleUpdate_shouldUpdateTriggerTimes() {
        XxlJobInfo info = createJobInfo(1, "ScheduleUpd", "tester");
        info.setTriggerStatus(1);
        info.setTriggerNextTime(System.currentTimeMillis());
        xxlJobInfoMapper.save(info);

        long newLastTime = System.currentTimeMillis();
        long newNextTime = System.currentTimeMillis() + 120000;
        info.setTriggerLastTime(newLastTime);
        info.setTriggerNextTime(newNextTime);
        info.setTriggerStatus(1);
        int rows = xxlJobInfoMapper.scheduleUpdate(info);

        assertThat(rows).isEqualTo(1);

        XxlJobInfo loaded = xxlJobInfoMapper.loadById(info.getId());
        assertThat(loaded.getTriggerLastTime()).isEqualTo(newLastTime);
        assertThat(loaded.getTriggerNextTime()).isEqualTo(newNextTime);
    }

    @Test
    void scheduleUpdate_shouldNotUpdateWhenStatusIsNotOne() {
        XxlJobInfo info = createJobInfo(1, "ScheduleNo", "tester");
        info.setTriggerStatus(0);
        xxlJobInfoMapper.save(info);

        info.setTriggerLastTime(System.currentTimeMillis());
        info.setTriggerNextTime(System.currentTimeMillis() + 60000);
        info.setTriggerStatus(1);
        int rows = xxlJobInfoMapper.scheduleUpdate(info);

        // should not update because original trigger_status was 0
        assertThat(rows).isZero();
    }
}
