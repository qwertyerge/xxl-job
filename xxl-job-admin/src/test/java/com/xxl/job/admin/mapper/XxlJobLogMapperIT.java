package com.xxl.job.admin.mapper;

import com.xxl.job.admin.model.XxlJobLog;
import com.xxl.job.admin.test.support.MapperITBase;
import com.xxl.job.admin.test.support.TestFixtures;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * Integration tests for XxlJobLogMapper using TestContainers MySQL.
 * US-011: CRUD and pagination tests.
 * US-012: Report statistics and log cleanup tests.
 */
class XxlJobLogMapperIT extends MapperITBase {

    @Autowired
    private XxlJobLogMapper xxlJobLogMapper;

    @Autowired
    private XxlJobRegistryMapper xxlJobRegistryMapper;

    // ========== US-011: CRUD and Pagination Tests ==========

    @Test
    void testSave_and_load_roundTrip() {
        // given
        XxlJobLog log = TestFixtures.createJobLog();
        log.setJobGroup(1);
        log.setJobId(100);

        // when
        xxlJobLogMapper.save(log);
        long savedId = log.getId();

        // then
        Assertions.assertThat(savedId).isPositive();
        XxlJobLog loaded = xxlJobLogMapper.load(savedId);
        Assertions.assertThat(loaded).isNotNull();
        Assertions.assertThat(loaded.getId()).isEqualTo(savedId);
        Assertions.assertThat(loaded.getJobGroup()).isEqualTo(1);
        Assertions.assertThat(loaded.getJobId()).isEqualTo(100);
        Assertions.assertThat(loaded.getTriggerCode()).isEqualTo(200);
        Assertions.assertThat(loaded.getHandleCode()).isEqualTo(0);
    }

    @Test
    void testUpdateTriggerInfo_modifiesTriggerFields() {
        // given
        XxlJobLog log = TestFixtures.createJobLog();
        log.setJobGroup(1);
        log.setJobId(100);
        xxlJobLogMapper.save(log);
        long savedId = log.getId();

        // when
        log.setTriggerTime(new Date());
        log.setTriggerCode(500);
        log.setTriggerMsg("trigger failed");
        log.setExecutorAddress("127.0.0.1:8888");
        log.setExecutorHandler("updatedHandler");
        log.setExecutorParam("updated-param");
        log.setExecutorShardingParam("1/2");
        log.setExecutorFailRetryCount(3);

        int result = xxlJobLogMapper.updateTriggerInfo(log);

        // then
        Assertions.assertThat(result).isEqualTo(1);
        XxlJobLog updated = xxlJobLogMapper.load(savedId);
        Assertions.assertThat(updated.getTriggerCode()).isEqualTo(500);
        Assertions.assertThat(updated.getTriggerMsg()).isEqualTo("trigger failed");
        Assertions.assertThat(updated.getExecutorAddress()).isEqualTo("127.0.0.1:8888");
        Assertions.assertThat(updated.getExecutorHandler()).isEqualTo("updatedHandler");
        Assertions.assertThat(updated.getExecutorParam()).isEqualTo("updated-param");
        Assertions.assertThat(updated.getExecutorShardingParam()).isEqualTo("1/2");
        Assertions.assertThat(updated.getExecutorFailRetryCount()).isEqualTo(3);
    }

    @Test
    void testUpdateHandleInfo_modifiesHandleFields() {
        // given
        XxlJobLog log = TestFixtures.createJobLog();
        log.setJobGroup(1);
        log.setJobId(100);
        xxlJobLogMapper.save(log);
        long savedId = log.getId();

        // when
        log.setHandleTime(new Date());
        log.setHandleCode(200);
        log.setHandleMsg("handle success");

        int result = xxlJobLogMapper.updateHandleInfo(log);

        // then
        Assertions.assertThat(result).isEqualTo(1);
        XxlJobLog updated = xxlJobLogMapper.load(savedId);
        Assertions.assertThat(updated.getHandleCode()).isEqualTo(200);
        Assertions.assertThat(updated.getHandleMsg()).isEqualTo("handle success");
    }

    @Test
    void testDelete_byJobId() {
        // given
        XxlJobLog log1 = TestFixtures.createJobLog();
        log1.setJobId(100);
        xxlJobLogMapper.save(log1);

        XxlJobLog log2 = TestFixtures.createJobLog();
        log2.setJobId(100);
        xxlJobLogMapper.save(log2);

        XxlJobLog log3 = TestFixtures.createJobLog();
        log3.setJobId(200);
        xxlJobLogMapper.save(log3);

        // when
        int result = xxlJobLogMapper.delete(100);

        // then
        Assertions.assertThat(result).isEqualTo(2); // 2 logs deleted
        Assertions.assertThat(xxlJobLogMapper.load(log1.getId())).isNull();
        Assertions.assertThat(xxlJobLogMapper.load(log2.getId())).isNull();
        Assertions.assertThat(xxlJobLogMapper.load(log3.getId())).isNotNull(); // Different jobId
    }

    @Test
    void testPageList_and_pageListCount_withJobGroupFilter() {
        // given
        XxlJobLog log1 = TestFixtures.createJobLog();
        log1.setJobGroup(1);
        log1.setJobId(0); // Use jobGroup filter when jobId==0
        xxlJobLogMapper.save(log1);

        XxlJobLog log2 = TestFixtures.createJobLog();
        log2.setJobGroup(2);
        log2.setJobId(0);
        xxlJobLogMapper.save(log2);

        // when - filter by jobGroup=1
        List<XxlJobLog> page1 = xxlJobLogMapper.pageList(0, 10, 1, 0, null, null, 0);
        int count1 = xxlJobLogMapper.pageListCount(0, 10, 1, 0, null, null, 0);

        // then
        Assertions.assertThat(page1).hasSize(count1);
        Assertions.assertThat(page1)
                .allMatch(log -> log.getJobGroup() == 1);

        // when - filter by jobGroup=2
        List<XxlJobLog> page2 = xxlJobLogMapper.pageList(0, 10, 2, 0, null, null, 0);
        int count2 = xxlJobLogMapper.pageListCount(0, 10, 2, 0, null, null, 0);

        // then
        Assertions.assertThat(page2).hasSize(count2);
        Assertions.assertThat(page2)
                .allMatch(log -> log.getJobGroup() == 2);
    }

    @Test
    void testPageList_and_pageListCount_withJobIdFilter() {
        // given
        XxlJobLog log1 = TestFixtures.createJobLog();
        log1.setJobId(100);
        xxlJobLogMapper.save(log1);

        XxlJobLog log2 = TestFixtures.createJobLog();
        log2.setJobId(200);
        xxlJobLogMapper.save(log2);

        // when - filter by jobId=100
        List<XxlJobLog> page100 = xxlJobLogMapper.pageList(0, 10, 0, 100, null, null, 0);
        int count100 = xxlJobLogMapper.pageListCount(0, 10, 0, 100, null, null, 0);

        // then
        Assertions.assertThat(page100).hasSize(count100);
        Assertions.assertThat(page100)
                .allMatch(log -> log.getJobId() == 100);

        // when - filter by jobId=200
        List<XxlJobLog> page200 = xxlJobLogMapper.pageList(0, 10, 0, 200, null, null, 0);
        int count200 = xxlJobLogMapper.pageListCount(0, 10, 0, 200, null, null, 0);

        // then
        Assertions.assertThat(page200).hasSize(count200);
        Assertions.assertThat(page200)
                .allMatch(log -> log.getJobId() == 200);
    }

    @Test
    void testPageList_and_pageListCount_withTriggerTimeRange() {
        // given
        Date yesterday = getDateDaysAgo(1);
        Date today = new Date();
        Date tomorrow = getDateDaysFromNow(1);

        XxlJobLog logYesterday = TestFixtures.createJobLog();
        logYesterday.setTriggerTime(yesterday);
        logYesterday.setJobId(1);
        xxlJobLogMapper.save(logYesterday);

        XxlJobLog logToday = TestFixtures.createJobLog();
        logToday.setTriggerTime(today);
        logToday.setJobId(2);
        xxlJobLogMapper.save(logToday);

        XxlJobLog logTomorrow = TestFixtures.createJobLog();
        logTomorrow.setTriggerTime(tomorrow);
        logTomorrow.setJobId(3);
        xxlJobLogMapper.save(logTomorrow);

        // when - filter by date range (yesterday to today)
        Date rangeStart = getDateDaysAgo(2);
        Date rangeEnd = getDateDaysFromNow(1);
        List<XxlJobLog> page = xxlJobLogMapper.pageList(0, 10, 0, 0, rangeStart, rangeEnd, 0);
        int count = xxlJobLogMapper.pageListCount(0, 10, 0, 0, rangeStart, rangeEnd, 0);

        // then
        Assertions.assertThat(page).hasSize(count);
        Assertions.assertThat(page)
                .allMatch(log -> log.getTriggerTime().compareTo(rangeStart) >= 0
                        && log.getTriggerTime().compareTo(rangeEnd) <= 0);
    }

    @Test
    void testPageList_and_pageListCount_withLogStatusOne_success() {
        // given - logStatus=1: handle_code = 200 (success)
        XxlJobLog logSuccess = TestFixtures.createJobLog();
        logSuccess.setJobId(1);
        logSuccess.setTriggerCode(200);
        logSuccess.setHandleCode(200);
        xxlJobLogMapper.save(logSuccess);

        XxlJobLog logFail = TestFixtures.createJobLog();
        logFail.setJobId(2);
        logFail.setTriggerCode(500);
        logFail.setHandleCode(0);
        xxlJobLogMapper.save(logFail);

        // when - filter by logStatus=1
        List<XxlJobLog> page = xxlJobLogMapper.pageList(0, 10, 0, 0, null, null, 1);
        int count = xxlJobLogMapper.pageListCount(0, 10, 0, 0, null, null, 1);

        // then
        Assertions.assertThat(page).hasSize(count);
        Assertions.assertThat(page)
                .allMatch(log -> log.getHandleCode() == 200);
        Assertions.assertThat(page)
                .anyMatch(log -> log.getId() == logSuccess.getId());
        Assertions.assertThat(page)
                .noneMatch(log -> log.getId() == logFail.getId());
    }

    @Test
    void testPageList_and_pageListCount_withLogStatusTwo_failure() {
        // given - logStatus=2: trigger_code NOT IN (0, 200) OR handle_code NOT IN (0, 200)
        XxlJobLog logFailTrigger = TestFixtures.createJobLog();
        logFailTrigger.setJobId(1);
        logFailTrigger.setTriggerCode(500); // Not in (0, 200)
        logFailTrigger.setHandleCode(0);
        xxlJobLogMapper.save(logFailTrigger);

        XxlJobLog logFailHandle = TestFixtures.createJobLog();
        logFailHandle.setJobId(2);
        logFailHandle.setTriggerCode(200);
        logFailHandle.setHandleCode(500); // Not in (0, 200)
        xxlJobLogMapper.save(logFailHandle);

        XxlJobLog logSuccess = TestFixtures.createJobLog();
        logSuccess.setJobId(3);
        logSuccess.setTriggerCode(200);
        logSuccess.setHandleCode(200);
        xxlJobLogMapper.save(logSuccess);

        // when - filter by logStatus=2
        List<XxlJobLog> page = xxlJobLogMapper.pageList(0, 10, 0, 0, null, null, 2);
        int count = xxlJobLogMapper.pageListCount(0, 10, 0, 0, null, null, 2);

        // then
        Assertions.assertThat(page).hasSize(count);
        Assertions.assertThat(page)
                .allMatch(log -> (log.getTriggerCode() != 0 && log.getTriggerCode() != 200)
                        || (log.getHandleCode() != 0 && log.getHandleCode() != 200));
        Assertions.assertThat(page)
                .anyMatch(log -> log.getId() == logFailTrigger.getId());
        Assertions.assertThat(page)
                .anyMatch(log -> log.getId() == logFailHandle.getId());
        Assertions.assertThat(page)
                .noneMatch(log -> log.getId() == logSuccess.getId());
    }

    @Test
    void testPageList_and_pageListCount_withLogStatusThree_running() {
        // given - logStatus=3: trigger_code = 200 AND handle_code = 0 (running)
        XxlJobLog logRunning = TestFixtures.createJobLog();
        logRunning.setJobId(1);
        logRunning.setTriggerCode(200);
        logRunning.setHandleCode(0);
        xxlJobLogMapper.save(logRunning);

        XxlJobLog logSuccess = TestFixtures.createJobLog();
        logSuccess.setJobId(2);
        logSuccess.setTriggerCode(200);
        logSuccess.setHandleCode(200);
        xxlJobLogMapper.save(logSuccess);

        XxlJobLog logFail = TestFixtures.createJobLog();
        logFail.setJobId(3);
        logFail.setTriggerCode(500);
        logFail.setHandleCode(0);
        xxlJobLogMapper.save(logFail);

        // when - filter by logStatus=3
        List<XxlJobLog> page = xxlJobLogMapper.pageList(0, 10, 0, 0, null, null, 3);
        int count = xxlJobLogMapper.pageListCount(0, 10, 0, 0, null, null, 3);

        // then
        Assertions.assertThat(page).hasSize(count);
        Assertions.assertThat(page)
                .allMatch(log -> log.getTriggerCode() == 200 && log.getHandleCode() == 0);
        Assertions.assertThat(page)
                .anyMatch(log -> log.getId() == logRunning.getId());
        Assertions.assertThat(page)
                .noneMatch(log -> log.getId() == logSuccess.getId());
        Assertions.assertThat(page)
                .noneMatch(log -> log.getId() == logFail.getId());
    }

    @Test
    void testPageList_returnsLogsInDescOrder() {
        // given
        for (int i = 0; i < 5; i++) {
            XxlJobLog log = TestFixtures.createJobLog();
            log.setJobId(i);
            xxlJobLogMapper.save(log);
        }

        // when
        List<XxlJobLog> page = xxlJobLogMapper.pageList(0, 10, 0, 0, null, null, 0);

        // then - should be ordered by id DESC
        Assertions.assertThat(page)
                .hasSizeGreaterThanOrEqualTo(5);
        for (int i = 0; i < page.size() - 1; i++) {
            Assertions.assertThat(page.get(i).getId()).isGreaterThan(page.get(i + 1).getId());
        }
    }

    @Test
    void testPageList_withPagination() {
        // given
        for (int i = 0; i < 5; i++) {
            XxlJobLog log = TestFixtures.createJobLog();
            log.setJobId(i);
            xxlJobLogMapper.save(log);
        }

        // when - first page (offset=0, pagesize=2)
        List<XxlJobLog> firstPage = xxlJobLogMapper.pageList(0, 2, 0, 0, null, null, 0);

        // then
        Assertions.assertThat(firstPage).hasSize(2);

        // when - second page (offset=2, pagesize=2)
        List<XxlJobLog> secondPage = xxlJobLogMapper.pageList(2, 2, 0, 0, null, null, 0);

        // then
        Assertions.assertThat(secondPage).hasSize(2);

        // when - third page (offset=4, pagesize=2)
        List<XxlJobLog> thirdPage = xxlJobLogMapper.pageList(4, 2, 0, 0, null, null, 0);

        // then
        Assertions.assertThat(thirdPage).hasSizeGreaterThanOrEqualTo(1);
    }

    // ========== US-012: Report Statistics and Log Cleanup Tests ==========

    @Test
    void testFindLogReport_returnsStatisticsForDateRange() {
        // given
        Date threeDaysAgo = getDateDaysAgo(3);
        Date twoDaysAgo = getDateDaysAgo(2);
        Date yesterday = getDateDaysAgo(1);

        // Create logs with different statuses
        XxlJobLog logRunning1 = TestFixtures.createJobLog();
        logRunning1.setTriggerTime(threeDaysAgo);
        logRunning1.setTriggerCode(200);
        logRunning1.setHandleCode(0); // Running
        logRunning1.setJobId(1);
        xxlJobLogMapper.save(logRunning1);

        XxlJobLog logRunning2 = TestFixtures.createJobLog();
        logRunning2.setTriggerTime(threeDaysAgo);
        logRunning2.setTriggerCode(0);
        logRunning2.setHandleCode(0); // Running
        logRunning2.setJobId(2);
        xxlJobLogMapper.save(logRunning2);

        XxlJobLog logSuccess = TestFixtures.createJobLog();
        logSuccess.setTriggerTime(twoDaysAgo);
        logSuccess.setTriggerCode(200);
        logSuccess.setHandleCode(200); // Success
        logSuccess.setJobId(3);
        xxlJobLogMapper.save(logSuccess);

        XxlJobLog logFail = TestFixtures.createJobLog();
        logFail.setTriggerTime(yesterday);
        logFail.setTriggerCode(500); // Fail
        logFail.setHandleCode(0);
        logFail.setJobId(4);
        xxlJobLogMapper.save(logFail);

        // when - query from 4 days ago to today
        Date from = getDateDaysAgo(4);
        Date to = new Date();
        var report = xxlJobLogMapper.findLogReport(from, to);

        // then
        Assertions.assertThat(report).isNotNull();
        Assertions.assertThat(((Number) report.get("triggerDayCount")).longValue()).isEqualTo(4L); // Total 4 logs
        Assertions.assertThat(((Number) report.get("triggerDayCountRunning")).longValue()).isEqualTo(2L); // 2 running
        Assertions.assertThat(((Number) report.get("triggerDayCountSuc")).longValue()).isEqualTo(1L); // 1 success
    }

    @Test
    void testFindLogReport_withEmptyDateRange() {
        // given
        Date future = getDateDaysFromNow(10);
        Date furtherFuture = getDateDaysFromNow(20);

        // when - query future date range (no logs)
        var report = xxlJobLogMapper.findLogReport(future, furtherFuture);

        // then
        Assertions.assertThat(report).isNotNull();
        Assertions.assertThat(((Number) report.get("triggerDayCount")).longValue()).isEqualTo(0L);
        Assertions.assertThat(((Number) report.get("triggerDayCountRunning")).longValue()).isEqualTo(0L);
        Assertions.assertThat(((Number) report.get("triggerDayCountSuc")).longValue()).isEqualTo(0L);
    }

    @Test
    void testFindClearLogIds_byTime() {
        // given
        Date oldDate = getDateDaysAgo(10);
        Date recentDate = getDateDaysAgo(1);

        XxlJobLog oldLog1 = TestFixtures.createJobLog();
        oldLog1.setTriggerTime(oldDate);
        oldLog1.setJobId(1);
        xxlJobLogMapper.save(oldLog1);

        XxlJobLog oldLog2 = TestFixtures.createJobLog();
        oldLog2.setTriggerTime(oldDate);
        oldLog2.setJobId(2);
        xxlJobLogMapper.save(oldLog2);

        XxlJobLog recentLog = TestFixtures.createJobLog();
        recentLog.setTriggerTime(recentDate);
        recentLog.setJobId(3);
        xxlJobLogMapper.save(recentLog);

        // when - find logs before 5 days ago
        Date clearBeforeTime = getDateDaysAgo(5);
        List<Long> ids = xxlJobLogMapper.findClearLogIds(0, 0, clearBeforeTime, 0, 10);

        // then
        Assertions.assertThat(ids)
                .hasSize(2)
                .containsExactlyInAnyOrder(oldLog1.getId(), oldLog2.getId());
    }

    @Test
    void testFindClearLogIds_byJobId() {
        // given
        XxlJobLog log1 = TestFixtures.createJobLog();
        log1.setJobId(100);
        xxlJobLogMapper.save(log1);

        XxlJobLog log2 = TestFixtures.createJobLog();
        log2.setJobId(100);
        xxlJobLogMapper.save(log2);

        XxlJobLog log3 = TestFixtures.createJobLog();
        log3.setJobId(200);
        xxlJobLogMapper.save(log3);

        // when - find logs for jobId=100
        List<Long> ids = xxlJobLogMapper.findClearLogIds(0, 100, null, 0, 10);

        // then
        Assertions.assertThat(ids)
                .hasSize(2)
                .containsExactlyInAnyOrder(log1.getId(), log2.getId());
    }

    @Test
    void testFindClearLogIds_byJobGroup() {
        // given
        XxlJobLog log1 = TestFixtures.createJobLog();
        log1.setJobGroup(1);
        log1.setJobId(0);
        xxlJobLogMapper.save(log1);

        XxlJobLog log2 = TestFixtures.createJobLog();
        log2.setJobGroup(1);
        log2.setJobId(0);
        xxlJobLogMapper.save(log2);

        XxlJobLog log3 = TestFixtures.createJobLog();
        log3.setJobGroup(2);
        log3.setJobId(0);
        xxlJobLogMapper.save(log3);

        // when - find logs for jobGroup=1
        List<Long> ids = xxlJobLogMapper.findClearLogIds(1, 0, null, 0, 10);

        // then
        Assertions.assertThat(ids)
                .hasSize(2)
                .containsExactlyInAnyOrder(log1.getId(), log2.getId());
    }

    @Test
    void testFindClearLogIds_withClearBeforeNum_keepsLatestN() {
        // given - create 5 logs with different trigger times
        for (int i = 0; i < 5; i++) {
            XxlJobLog log = TestFixtures.createJobLog();
            log.setJobId(100);
            // Ensure different trigger times
            Date triggerTime = getDateDaysAgo(10 - i);
            log.setTriggerTime(triggerTime);
            xxlJobLogMapper.save(log);
            // Small delay to ensure different timestamps
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        // when - keep latest 2 logs (clearBeforeNum=2)
        List<Long> ids = xxlJobLogMapper.findClearLogIds(0, 100, null, 2, 10);

        // then - should return oldest 3 logs (exclude latest 2)
        Assertions.assertThat(ids).hasSize(3);
    }

    @Test
    void testClearLog_deletesByIds() {
        // given
        XxlJobLog log1 = TestFixtures.createJobLog();
        log1.setJobId(1);
        xxlJobLogMapper.save(log1);

        XxlJobLog log2 = TestFixtures.createJobLog();
        log2.setJobId(2);
        xxlJobLogMapper.save(log2);

        XxlJobLog log3 = TestFixtures.createJobLog();
        log3.setJobId(3);
        xxlJobLogMapper.save(log3);

        // when - delete log1 and log2
        List<Long> idsToDelete = List.of(log1.getId(), log2.getId());
        int result = xxlJobLogMapper.clearLog(idsToDelete);

        // then
        Assertions.assertThat(result).isEqualTo(2);
        Assertions.assertThat(xxlJobLogMapper.load(log1.getId())).isNull();
        Assertions.assertThat(xxlJobLogMapper.load(log2.getId())).isNull();
        Assertions.assertThat(xxlJobLogMapper.load(log3.getId())).isNotNull();
    }

    @Test
    void testFindFailJobLogIds_returnsFailedLogs() {
        // given
        XxlJobLog failByTrigger = TestFixtures.createJobLog();
        failByTrigger.setTriggerCode(500); // Trigger failed (NOT IN 0, 200)
        failByTrigger.setHandleCode(0);
        failByTrigger.setJobId(1);
        xxlJobLogMapper.save(failByTrigger);

        XxlJobLog failByHandle = TestFixtures.createJobLog();
        failByHandle.setTriggerCode(200);
        failByHandle.setHandleCode(500); // Handle failed (NOT IN 0, 200)
        failByHandle.setJobId(2);
        xxlJobLogMapper.save(failByHandle);

        XxlJobLog success = TestFixtures.createJobLog();
        success.setTriggerCode(200);
        success.setHandleCode(200); // Success
        success.setJobId(3);
        xxlJobLogMapper.save(success);

        // Create a failed log and mark it as already alarmed
        XxlJobLog alreadyAlarmed = TestFixtures.createJobLog();
        alreadyAlarmed.setTriggerCode(500);
        alreadyAlarmed.setHandleCode(0);
        alreadyAlarmed.setJobId(4);
        xxlJobLogMapper.save(alreadyAlarmed);
        // Use updateAlarmStatus to mark it as alarmed (status=1)
        xxlJobLogMapper.updateAlarmStatus(alreadyAlarmed.getId(), 0, 1);

        // when
        List<Long> failIds = xxlJobLogMapper.findFailJobLogIds(10);

        // then - should include our new failed logs
        Assertions.assertThat(failIds)
                .contains(failByTrigger.getId(), failByHandle.getId());
        // should NOT include success log
        Assertions.assertThat(failIds)
                .doesNotContain(success.getId());
        // should NOT include already alarmed log (alarm_status != 0)
        Assertions.assertThat(failIds)
                .doesNotContain(alreadyAlarmed.getId());
    }

    @Test
    void testUpdateAlarmStatus_optimisticLock() {
        // given
        XxlJobLog log = TestFixtures.createJobLog();
        log.setAlarmStatus(0);
        log.setJobId(1);
        xxlJobLogMapper.save(log);

        // when - update from 0 to 1 (should succeed)
        int result1 = xxlJobLogMapper.updateAlarmStatus(log.getId(), 0, 1);

        // then
        Assertions.assertThat(result1).isEqualTo(1);
        XxlJobLog updated = xxlJobLogMapper.load(log.getId());
        Assertions.assertThat(updated.getAlarmStatus()).isEqualTo(1);

        // when - try to update from 0 to 2 (should fail because alarm_status is now 1)
        int result2 = xxlJobLogMapper.updateAlarmStatus(log.getId(), 0, 2);

        // then
        Assertions.assertThat(result2).isEqualTo(0); // No rows updated
        XxlJobLog stillUpdated = xxlJobLogMapper.load(log.getId());
        Assertions.assertThat(stillUpdated.getAlarmStatus()).isEqualTo(1); // Still 1, not 2
    }

    @Test
    void testFindLostJobIds_returnsLostJobs() {
        // given - Insert a registry entry using Mapper
        String registeredAddress = "127.0.0.1:8888";
        String unregisteredAddress = "127.0.0.1:9999";
        xxlJobRegistryMapper.registrySaveOrUpdate("executor", "test-executor", registeredAddress, new Date());

        // given - job that was triggered but executor is not in registry
        XxlJobLog lostJob = TestFixtures.createJobLog();
        lostJob.setTriggerCode(200);
        lostJob.setHandleCode(0); // Running
        lostJob.setTriggerTime(getDateDaysAgo(2)); // 2 days ago (older than losedTime)
        lostJob.setJobId(1);
        xxlJobLogMapper.save(lostJob);
        // Use updateTriggerInfo to set executor_address (save() doesn't insert it)
        lostJob.setExecutorAddress(unregisteredAddress);
        xxlJobLogMapper.updateTriggerInfo(lostJob);

        // given - job that is still in registry
        XxlJobLog notLostJob = TestFixtures.createJobLog();
        notLostJob.setTriggerCode(200);
        notLostJob.setHandleCode(0);
        notLostJob.setTriggerTime(getDateDaysAgo(2)); // 2 days ago (older than losedTime)
        notLostJob.setJobId(2);
        xxlJobLogMapper.save(notLostJob);
        // Use updateTriggerInfo to set executor_address
        notLostJob.setExecutorAddress(registeredAddress);
        xxlJobLogMapper.updateTriggerInfo(notLostJob);

        // Verify jobs are saved correctly with executor addresses
        Integer registeredLogCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM xxl_job_log WHERE executor_address = ?", Integer.class, registeredAddress);
        Assertions.assertThat(registeredLogCount).isGreaterThanOrEqualTo(1);

        // when - losedTime is 1 day ago (so jobs older than 1 day are considered lost)
        Date losedTime = getDateDaysAgo(1);
        List<Long> lostIds = xxlJobLogMapper.findLostJobIds(losedTime);

        // then - should only return lostJob (not notLostJob which is in registry)
        Assertions.assertThat(lostIds)
                .contains(lostJob.getId())
                .doesNotContain(notLostJob.getId());
    }

    @Test
    void testFindLostJobIds_withRecentTriggerTime() {
        // given - job with recent trigger time (should not be considered lost)
        XxlJobLog recentJob = TestFixtures.createJobLog();
        recentJob.setTriggerCode(200);
        recentJob.setHandleCode(0);
        recentJob.setTriggerTime(new Date()); // Recent
        recentJob.setExecutorAddress("127.0.0.1:9999"); // Not in registry
        recentJob.setJobId(1);
        xxlJobLogMapper.save(recentJob);

        // when - check for jobs lost before 1 day ago
        Date losedTime = getDateDaysAgo(1);
        List<Long> lostIds = xxlJobLogMapper.findLostJobIds(losedTime);

        // then - recent job should not be in lost list
        Assertions.assertThat(lostIds)
                .doesNotContain(recentJob.getId());
    }

    // ========== Helper Methods ==========

    private Date getDateDaysAgo(int days) {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_MONTH, -days);
        return cal.getTime();
    }

    private Date getDateDaysFromNow(int days) {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_MONTH, days);
        return cal.getTime();
    }
}
