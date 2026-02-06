package com.xxl.job.admin.mapper;

import com.xxl.job.admin.model.XxlJobInfo;
import com.xxl.job.admin.test.support.MapperITBase;
import com.xxl.job.admin.test.support.TestFixtures;
import org.assertj.core.api.Assertions;
import org.assertj.core.data.Offset;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

/**
 * Integration tests for XxlJobInfoMapper using TestContainers MySQL.
 */
class XxlJobInfoMapperIT extends MapperITBase {

    @Autowired
    private XxlJobInfoMapper xxlJobInfoMapper;

    @Test
    void testSave_returnsOneAndIdGreaterThanZero() {
        // given
        XxlJobInfo info = TestFixtures.createJobInfo();

        // when
        int result = xxlJobInfoMapper.save(info);

        // then
        Assertions.assertThat(result).isEqualTo(1);
        Assertions.assertThat(info.getId()).isPositive();
    }

    @Test
    void testLoadById_returnsSavedJob() {
        // given
        XxlJobInfo info = TestFixtures.createJobInfo();
        xxlJobInfoMapper.save(info);
        int savedId = info.getId();

        // when
        XxlJobInfo loaded = xxlJobInfoMapper.loadById(savedId);

        // then
        Assertions.assertThat(loaded).isNotNull();
        Assertions.assertThat(loaded.getId()).isEqualTo(savedId);
        Assertions.assertThat(loaded.getJobGroup()).isEqualTo(1);
        Assertions.assertThat(loaded.getJobDesc()).isEqualTo("test-job-desc");
        Assertions.assertThat(loaded.getAuthor()).isEqualTo("tester");
        Assertions.assertThat(loaded.getScheduleType()).isEqualTo("NONE");
    }

    @Test
    void testLoadById_withNonExistentId_returnsNull() {
        // when
        XxlJobInfo loaded = xxlJobInfoMapper.loadById(-999);

        // then
        Assertions.assertThat(loaded).isNull();
    }

    @Test
    void testUpdate_modifiesFields() {
        // given
        XxlJobInfo info = TestFixtures.createJobInfo();
        xxlJobInfoMapper.save(info);
        int savedId = info.getId();

        // when
        info.setJobDesc("updated-job-desc");
        info.setAuthor("updated-author");
        info.setUpdateTime(new java.util.Date());
        int result = xxlJobInfoMapper.update(info);

        // then
        Assertions.assertThat(result).isEqualTo(1);
        XxlJobInfo loaded = xxlJobInfoMapper.loadById(savedId);
        Assertions.assertThat(loaded.getJobDesc()).isEqualTo("updated-job-desc");
        Assertions.assertThat(loaded.getAuthor()).isEqualTo("updated-author");
    }

    @Test
    void testDelete_deletesJob() {
        // given
        XxlJobInfo info = TestFixtures.createJobInfo();
        xxlJobInfoMapper.save(info);
        int savedId = info.getId();

        // when
        int result = xxlJobInfoMapper.delete(savedId);

        // then
        Assertions.assertThat(result).isEqualTo(1);
        XxlJobInfo loaded = xxlJobInfoMapper.loadById(savedId);
        Assertions.assertThat(loaded).isNull();
    }

    @Test
    void testGetJobsByGroup_returnsOnlyJobsInGroup() {
        // given
        XxlJobInfo job1 = TestFixtures.createJobInfo();
        job1.setJobGroup(1);
        xxlJobInfoMapper.save(job1);

        XxlJobInfo job2 = TestFixtures.createJobInfo();
        job2.setJobGroup(2);
        job2.setJobDesc("job-in-group-2");
        xxlJobInfoMapper.save(job2);

        // when
        List<XxlJobInfo> jobsInGroup1 = xxlJobInfoMapper.getJobsByGroup(1);
        List<XxlJobInfo> jobsInGroup2 = xxlJobInfoMapper.getJobsByGroup(2);

        // then
        Assertions.assertThat(jobsInGroup1)
                .extracting(XxlJobInfo::getId)
                .contains(job1.getId());

        Assertions.assertThat(jobsInGroup2)
                .extracting(XxlJobInfo::getId)
                .contains(job2.getId());
    }

    @Test
    void testFindAllCount_increasesWhenJobAdded() {
        // given
        int countBefore = xxlJobInfoMapper.findAllCount();

        // when
        XxlJobInfo info = TestFixtures.createJobInfo();
        xxlJobInfoMapper.save(info);

        // then
        int countAfter = xxlJobInfoMapper.findAllCount();
        Assertions.assertThat(countAfter).isEqualTo(countBefore + 1);
    }

    @Test
    void testPageList_and_pageListCount_consistency() {
        // given
        XxlJobInfo job1 = TestFixtures.createJobInfo();
        job1.setJobDesc("searchable-job");
        job1.setAuthor("author1");
        xxlJobInfoMapper.save(job1);

        XxlJobInfo job2 = TestFixtures.createJobInfo();
        job2.setJobDesc("other-job");
        job2.setAuthor("searchable-author");
        xxlJobInfoMapper.save(job2);

        // when - search by jobDesc
        List<XxlJobInfo> pageByJobDesc = xxlJobInfoMapper.pageList(0, 10, 0, -1, "searchable", null, null);
        int countByJobDesc = xxlJobInfoMapper.pageListCount(0, 10, 0, -1, "searchable", null, null);

        // then
        Assertions.assertThat(pageByJobDesc).hasSize(countByJobDesc);
        Assertions.assertThat(pageByJobDesc)
                .extracting(XxlJobInfo::getJobDesc)
                .allMatch(jobDesc -> jobDesc.contains("searchable"));

        // when - search by author
        List<XxlJobInfo> pageByAuthor = xxlJobInfoMapper.pageList(0, 10, 0, -1, null, null, "searchable-author");
        int countByAuthor = xxlJobInfoMapper.pageListCount(0, 10, 0, -1, null, null, "searchable-author");

        // then
        Assertions.assertThat(pageByAuthor).hasSize(countByAuthor);
        Assertions.assertThat(pageByAuthor)
                .extracting(XxlJobInfo::getAuthor)
                .allMatch(author -> author.contains("searchable-author"));
    }

    @Test
    void testPageList_withTriggerStatusFilter() {
        // given
        XxlJobInfo jobStopped = TestFixtures.createJobInfo();
        jobStopped.setJobDesc("stopped-job");
        jobStopped.setTriggerStatus(0);
        xxlJobInfoMapper.save(jobStopped);

        XxlJobInfo jobRunning = TestFixtures.createJobInfo();
        jobRunning.setJobDesc("running-job");
        jobRunning.setTriggerStatus(1);
        xxlJobInfoMapper.save(jobRunning);

        // when - filter by triggerStatus=0
        List<XxlJobInfo> stoppedJobs = xxlJobInfoMapper.pageList(0, 10, 0, 0, null, null, null);
        int stoppedCount = xxlJobInfoMapper.pageListCount(0, 10, 0, 0, null, null, null);

        // then
        Assertions.assertThat(stoppedJobs).hasSize(stoppedCount);
        Assertions.assertThat(stoppedJobs)
                .allMatch(job -> job.getTriggerStatus() == 0);

        // when - filter by triggerStatus=1
        List<XxlJobInfo> runningJobs = xxlJobInfoMapper.pageList(0, 10, 0, 1, null, null, null);
        int runningCount = xxlJobInfoMapper.pageListCount(0, 10, 0, 1, null, null, null);

        // then
        Assertions.assertThat(runningJobs).hasSize(runningCount);
        Assertions.assertThat(runningJobs)
                .allMatch(job -> job.getTriggerStatus() == 1);
    }

    @Test
    void testPageList_withPagination() {
        // given
        for (int i = 0; i < 5; i++) {
            XxlJobInfo job = TestFixtures.createJobInfo();
            job.setJobDesc("paginated-job-" + i);
            xxlJobInfoMapper.save(job);
        }

        // when - first page
        List<XxlJobInfo> firstPage = xxlJobInfoMapper.pageList(0, 2, 0, -1, "paginated-job", null, null);

        // then
        Assertions.assertThat(firstPage).hasSize(2);

        // when - second page
        List<XxlJobInfo> secondPage = xxlJobInfoMapper.pageList(2, 2, 0, -1, "paginated-job", null, null);

        // then
        Assertions.assertThat(secondPage).hasSize(2);

        // when - third page
        List<XxlJobInfo> thirdPage = xxlJobInfoMapper.pageList(4, 2, 0, -1, "paginated-job", null, null);

        // then
        Assertions.assertThat(thirdPage).hasSize(1);
    }

    // ========== US-010: Schedule-related tests ==========

    @Test
    void testScheduleJobQuery_returnsJobsWithTriggerStatusOneAndNextTimeInRange() {
        // given - job with triggerStatus=1 and triggerNextTime in the past
        XxlJobInfo job = TestFixtures.createJobInfo();
        job.setTriggerStatus(1);
        job.setTriggerNextTime(System.currentTimeMillis() - 1000); // 1 second ago
        xxlJobInfoMapper.save(job);

        long maxNextTime = System.currentTimeMillis();

        // when
        List<XxlJobInfo> result = xxlJobInfoMapper.scheduleJobQuery(maxNextTime, 10);

        // then
        Assertions.assertThat(result)
                .isNotEmpty()
                .allMatch(j -> j.getTriggerStatus() == 1)
                .anyMatch(j -> j.getId() == job.getId());
    }

    @Test
    void testScheduleJobQuery_doesNotReturnJobsWithTriggerStatusZero() {
        // given - job with triggerStatus=0 (stopped)
        XxlJobInfo stoppedJob = TestFixtures.createJobInfo();
        stoppedJob.setTriggerStatus(0);
        stoppedJob.setTriggerNextTime(System.currentTimeMillis() - 1000);
        xxlJobInfoMapper.save(stoppedJob);

        long maxNextTime = System.currentTimeMillis();

        // when
        List<XxlJobInfo> result = xxlJobInfoMapper.scheduleJobQuery(maxNextTime, 10);

        // then
        Assertions.assertThat(result)
                .noneMatch(j -> j.getId() == stoppedJob.getId());
    }

    @Test
    void testScheduleJobQuery_doesNotReturnJobsWithNextTimeGreaterThanMax() {
        // given - job with triggerStatus=1 but triggerNextTime in the future
        XxlJobInfo futureJob = TestFixtures.createJobInfo();
        futureJob.setTriggerStatus(1);
        futureJob.setTriggerNextTime(System.currentTimeMillis() + 100000); // 100 seconds in future
        xxlJobInfoMapper.save(futureJob);

        long maxNextTime = System.currentTimeMillis();

        // when
        List<XxlJobInfo> result = xxlJobInfoMapper.scheduleJobQuery(maxNextTime, 10);

        // then
        Assertions.assertThat(result)
                .noneMatch(j -> j.getId() == futureJob.getId());
    }

    @Test
    void testScheduleUpdate_updatesFieldsWhenTriggerStatusIsOne() {
        // given - job with triggerStatus=1
        XxlJobInfo job = TestFixtures.createJobInfo();
        job.setTriggerStatus(1);
        xxlJobInfoMapper.save(job);

        long newLastTime = System.currentTimeMillis();
        long newNextTime = System.currentTimeMillis() + 5000;
        job.setTriggerLastTime(newLastTime);
        job.setTriggerNextTime(newNextTime);
        job.setTriggerStatus(0); // This should be updated due to WHERE guard

        // when
        int result = xxlJobInfoMapper.scheduleUpdate(job);

        // then
        Assertions.assertThat(result).isEqualTo(1);
        XxlJobInfo updated = xxlJobInfoMapper.loadById(job.getId());
        Assertions.assertThat(updated.getTriggerLastTime()).isCloseTo(newLastTime, Offset.offset(1000L));
        Assertions.assertThat(updated.getTriggerNextTime()).isCloseTo(newNextTime, Offset.offset(1000L));
        Assertions.assertThat(updated.getTriggerStatus()).isEqualTo(0);
    }

    @Test
    void testScheduleUpdate_returnsZeroWhenTriggerStatusIsZero() {
        // given - job with triggerStatus=0 (stopped)
        XxlJobInfo job = TestFixtures.createJobInfo();
        job.setTriggerStatus(0);
        xxlJobInfoMapper.save(job);

        long newLastTime = System.currentTimeMillis();
        long newNextTime = System.currentTimeMillis() + 5000;
        job.setTriggerLastTime(newLastTime);
        job.setTriggerNextTime(newNextTime);
        job.setTriggerStatus(1); // Try to change to 1

        // when
        int result = xxlJobInfoMapper.scheduleUpdate(job);

        // then - WHERE trigger_status = 1 guard prevents update
        Assertions.assertThat(result).isEqualTo(0);
        XxlJobInfo unchanged = xxlJobInfoMapper.loadById(job.getId());
        Assertions.assertThat(unchanged.getTriggerStatus()).isEqualTo(0);
    }
}
