package com.xxl.job.admin.mapper;

import com.xxl.job.admin.model.XxlJobLog;
import com.xxl.job.admin.testcontainers.PostgresMapperITBase;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class XxlJobLogMapperPostgresIT extends PostgresMapperITBase {

    @Resource
    private XxlJobLogMapper xxlJobLogMapper;

    // --- helper ---

    private XxlJobLog createJobLog(int jobGroup, int jobId) {
        XxlJobLog log = new XxlJobLog();
        log.setJobGroup(jobGroup);
        log.setJobId(jobId);
        log.setTriggerTime(new Date());
        log.setTriggerCode(200);
        log.setHandleCode(0);
        return log;
    }

    private Date hoursAgo(int hours) {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.HOUR, -hours);
        return cal.getTime();
    }

    private Date hoursLater(int hours) {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.HOUR, hours);
        return cal.getTime();
    }

    // --- CRUD tests ---

    @Test
    void save_shouldInsertAndReturnGeneratedId() {
        XxlJobLog log = createJobLog(1, 1);

        long rows = xxlJobLogMapper.save(log);

        assertThat(rows).isEqualTo(1);
        assertThat(log.getId()).isGreaterThan(0);
    }

    @Test
    void load_shouldReturnSavedLog() {
        XxlJobLog log = createJobLog(1, 1);
        xxlJobLogMapper.save(log);

        XxlJobLog loaded = xxlJobLogMapper.load(log.getId());

        assertThat(loaded).isNotNull();
        assertThat(loaded.getJobGroup()).isEqualTo(1);
        assertThat(loaded.getJobId()).isEqualTo(1);
        assertThat(loaded.getTriggerCode()).isEqualTo(200);
    }

    @Test
    void updateTriggerInfo_shouldUpdateFields() {
        XxlJobLog log = createJobLog(1, 1);
        xxlJobLogMapper.save(log);

        log.setTriggerTime(new Date());
        log.setTriggerCode(500);
        log.setTriggerMsg("trigger failed");
        log.setExecutorAddress("127.0.0.1:9999");
        log.setExecutorHandler("testHandler");
        log.setExecutorParam("param1");
        log.setExecutorShardingParam("0/1");
        log.setExecutorFailRetryCount(3);
        int rows = xxlJobLogMapper.updateTriggerInfo(log);

        assertThat(rows).isEqualTo(1);

        XxlJobLog loaded = xxlJobLogMapper.load(log.getId());
        assertThat(loaded.getTriggerCode()).isEqualTo(500);
        assertThat(loaded.getTriggerMsg()).isEqualTo("trigger failed");
        assertThat(loaded.getExecutorAddress()).isEqualTo("127.0.0.1:9999");
        assertThat(loaded.getExecutorHandler()).isEqualTo("testHandler");
        assertThat(loaded.getExecutorParam()).isEqualTo("param1");
        assertThat(loaded.getExecutorShardingParam()).isEqualTo("0/1");
        assertThat(loaded.getExecutorFailRetryCount()).isEqualTo(3);
    }

    @Test
    void updateHandleInfo_shouldUpdateFields() {
        XxlJobLog log = createJobLog(1, 1);
        xxlJobLogMapper.save(log);

        log.setHandleTime(new Date());
        log.setHandleCode(200);
        log.setHandleMsg("handle success");
        int rows = xxlJobLogMapper.updateHandleInfo(log);

        assertThat(rows).isEqualTo(1);

        XxlJobLog loaded = xxlJobLogMapper.load(log.getId());
        assertThat(loaded.getHandleCode()).isEqualTo(200);
        assertThat(loaded.getHandleMsg()).isEqualTo("handle success");
    }

    @Test
    void delete_shouldRemoveLogsByJobId() {
        XxlJobLog log1 = createJobLog(1, 999);
        XxlJobLog log2 = createJobLog(1, 999);
        xxlJobLogMapper.save(log1);
        xxlJobLogMapper.save(log2);

        int rows = xxlJobLogMapper.delete(999);

        assertThat(rows).isEqualTo(2);
    }

    // --- pagination tests ---

    @Test
    void pageList_shouldReturnPagedResults() {
        for (int i = 0; i < 5; i++) {
            xxlJobLogMapper.save(createJobLog(1, 1));
        }

        List<XxlJobLog> page1 = xxlJobLogMapper.pageList(0, 3, 1, 1, null, null, 0);
        assertThat(page1).hasSize(3);

        List<XxlJobLog> page2 = xxlJobLogMapper.pageList(3, 3, 1, 1, null, null, 0);
        assertThat(page2).isNotEmpty();
    }

    @Test
    void pageListCount_shouldReturnTotalCount() {
        int countBefore = xxlJobLogMapper.pageListCount(0, 10, 1, 0, null, null, 0);

        xxlJobLogMapper.save(createJobLog(1, 1));

        int countAfter = xxlJobLogMapper.pageListCount(0, 10, 1, 0, null, null, 0);
        assertThat(countAfter).isEqualTo(countBefore + 1);
    }

    @Test
    void pageList_shouldFilterByJobGroup() {
        xxlJobLogMapper.save(createJobLog(2, 0));

        List<XxlJobLog> filtered = xxlJobLogMapper.pageList(0, 10, 2, 0, null, null, 0);
        assertThat(filtered).isNotEmpty();
        assertThat(filtered).allMatch(l -> l.getJobGroup() == 2);
    }

    @Test
    void pageList_shouldFilterByJobId() {
        xxlJobLogMapper.save(createJobLog(1, 888));

        List<XxlJobLog> filtered = xxlJobLogMapper.pageList(0, 10, 0, 888, null, null, 0);
        assertThat(filtered).isNotEmpty();
        assertThat(filtered).allMatch(l -> l.getJobId() == 888);
    }

    @Test
    void pageList_shouldFilterByTriggerTimeRange() {
        XxlJobLog log = createJobLog(1, 1);
        log.setTriggerTime(new Date());
        xxlJobLogMapper.save(log);

        List<XxlJobLog> filtered = xxlJobLogMapper.pageList(
                0, 10, 0, 0, hoursAgo(1), hoursLater(1), 0);
        assertThat(filtered).isNotEmpty();
    }

    @Test
    void pageList_shouldFilterByLogStatus_success() {
        XxlJobLog log = createJobLog(1, 1);
        xxlJobLogMapper.save(log);
        log.setHandleCode(200);
        xxlJobLogMapper.updateHandleInfo(log);

        // logStatus=1 means handle_code=200
        List<XxlJobLog> filtered = xxlJobLogMapper.pageList(0, 10, 0, 0, null, null, 1);
        assertThat(filtered).isNotEmpty();
        assertThat(filtered).allMatch(l -> l.getHandleCode() == 200);
    }

    @Test
    void pageList_shouldFilterByLogStatus_fail() {
        XxlJobLog log = createJobLog(1, 1);
        log.setTriggerCode(500);
        xxlJobLogMapper.save(log);

        // logStatus=2 means trigger_code NOT IN (0,200) OR handle_code NOT IN (0,200)
        List<XxlJobLog> filtered = xxlJobLogMapper.pageList(0, 10, 0, 0, null, null, 2);
        assertThat(filtered).isNotEmpty();
    }

    @Test
    void pageList_shouldFilterByLogStatus_running() {
        XxlJobLog log = createJobLog(1, 1);
        log.setTriggerCode(200);
        log.setHandleCode(0);
        xxlJobLogMapper.save(log);

        // logStatus=3 means trigger_code=200 AND handle_code=0
        List<XxlJobLog> filtered = xxlJobLogMapper.pageList(0, 10, 0, 0, null, null, 3);
        assertThat(filtered).isNotEmpty();
        assertThat(filtered).allMatch(l -> l.getTriggerCode() == 200 && l.getHandleCode() == 0);
    }

    // --- report tests ---

    @Test
    void findLogReport_shouldReturnAggregatedCounts() {
        XxlJobLog successLog = createJobLog(1, 1);
        successLog.setTriggerCode(200);
        successLog.setHandleCode(200);
        xxlJobLogMapper.save(successLog);

        XxlJobLog runningLog = createJobLog(1, 1);
        runningLog.setTriggerCode(200);
        runningLog.setHandleCode(0);
        xxlJobLogMapper.save(runningLog);

        XxlJobLog failLog = createJobLog(1, 1);
        failLog.setTriggerCode(500);
        failLog.setHandleCode(0);
        xxlJobLogMapper.save(failLog);

        Map<String, Object> report = xxlJobLogMapper.findLogReport(hoursAgo(1), hoursLater(1));

        assertThat(report).isNotNull();
        assertThat(((Number) report.get("triggerdaycount")).longValue()).isGreaterThanOrEqualTo(3);
        assertThat(((Number) report.get("triggerdaycountsuc")).longValue()).isGreaterThanOrEqualTo(1);
        assertThat(((Number) report.get("triggerdaycountrunning")).longValue()).isGreaterThanOrEqualTo(1);
    }

    // --- clear log tests ---

    @Test
    void findClearLogIds_shouldReturnLogIds() {
        for (int i = 0; i < 3; i++) {
            xxlJobLogMapper.save(createJobLog(1, 1));
        }

        List<Long> ids = xxlJobLogMapper.findClearLogIds(1, 1, null, 0, 100);
        assertThat(ids).isNotEmpty();
    }

    @Test
    void findClearLogIds_shouldFilterByClearBeforeTime() {
        XxlJobLog oldLog = createJobLog(1, 1);
        oldLog.setTriggerTime(hoursAgo(48));
        xxlJobLogMapper.save(oldLog);

        List<Long> ids = xxlJobLogMapper.findClearLogIds(0, 0, hoursAgo(24), 0, 100);
        assertThat(ids).isNotEmpty();
    }

    @Test
    void findClearLogIds_shouldRespectClearBeforeNum() {
        for (int i = 0; i < 5; i++) {
            xxlJobLogMapper.save(createJobLog(1, 50));
        }

        // keep latest 2, return the rest
        List<Long> ids = xxlJobLogMapper.findClearLogIds(0, 50, null, 2, 100);
        assertThat(ids).hasSizeGreaterThanOrEqualTo(3);
    }

    @Test
    void clearLog_shouldDeleteByIds() {
        XxlJobLog log1 = createJobLog(1, 1);
        XxlJobLog log2 = createJobLog(1, 1);
        xxlJobLogMapper.save(log1);
        xxlJobLogMapper.save(log2);

        int rows = xxlJobLogMapper.clearLog(List.of(log1.getId(), log2.getId()));

        assertThat(rows).isEqualTo(2);
        assertThat(xxlJobLogMapper.load(log1.getId())).isNull();
        assertThat(xxlJobLogMapper.load(log2.getId())).isNull();
    }

    // --- alarm tests ---

    @Test
    void findFailJobLogIds_shouldReturnFailedLogs() {
        XxlJobLog failLog = createJobLog(1, 1);
        failLog.setTriggerCode(500);
        failLog.setHandleCode(0);
        xxlJobLogMapper.save(failLog);

        List<Long> ids = xxlJobLogMapper.findFailJobLogIds(100);
        assertThat(ids).contains(failLog.getId());
    }

    @Test
    void updateAlarmStatus_shouldUpdateWhenOldStatusMatches() {
        XxlJobLog log = createJobLog(1, 1);
        log.setTriggerCode(500);
        xxlJobLogMapper.save(log);

        int rows = xxlJobLogMapper.updateAlarmStatus(log.getId(), 0, 1);
        assertThat(rows).isEqualTo(1);

        XxlJobLog loaded = xxlJobLogMapper.load(log.getId());
        assertThat(loaded.getAlarmStatus()).isEqualTo(1);
    }

    @Test
    void updateAlarmStatus_shouldNotUpdateWhenOldStatusMismatch() {
        XxlJobLog log = createJobLog(1, 1);
        xxlJobLogMapper.save(log);

        int rows = xxlJobLogMapper.updateAlarmStatus(log.getId(), 1, 2);
        assertThat(rows).isZero();
    }

    // --- lost job tests ---

    @Test
    void findLostJobIds_shouldReturnLogsWithNoRegistry() {
        XxlJobLog log = createJobLog(1, 1);
        log.setTriggerCode(200);
        log.setHandleCode(0);
        log.setExecutorAddress("192.168.1.100:9999");
        log.setTriggerTime(hoursAgo(2));
        xxlJobLogMapper.save(log);
        xxlJobLogMapper.updateTriggerInfo(log);

        List<Long> lostIds = xxlJobLogMapper.findLostJobIds(hoursAgo(1));
        assertThat(lostIds).contains(log.getId());
    }
}