package com.xxl.job.admin.test.support;

import com.xxl.job.admin.model.XxlJobGroup;
import com.xxl.job.admin.model.XxlJobInfo;
import com.xxl.job.admin.model.XxlJobLog;
import com.xxl.job.admin.model.XxlJobLogGlue;
import com.xxl.job.admin.model.XxlJobLogReport;
import com.xxl.job.admin.model.XxlJobUser;

import java.util.Date;

/**
 * Factory class providing minimal valid objects for mapper integration tests.
 * Each method fills all NOT NULL fields with sensible defaults.
 */
public final class TestFixtures {

    private TestFixtures() {
        // utility class
    }

    public static XxlJobGroup createJobGroup() {
        XxlJobGroup group = new XxlJobGroup();
        group.setAppname("test-app");
        group.setTitle("Test Executor");
        group.setAddressType(0);
        group.setUpdateTime(new Date());
        return group;
    }

    public static XxlJobInfo createJobInfo() {
        XxlJobInfo info = new XxlJobInfo();
        info.setJobGroup(1);
        info.setJobDesc("test-job-desc");
        info.setAddTime(new Date());
        info.setUpdateTime(new Date());
        info.setAuthor("tester");
        info.setScheduleType("NONE");
        info.setMisfireStrategy("DO_NOTHING");
        info.setExecutorRouteStrategy("FIRST");
        info.setExecutorHandler("testHandler");
        info.setExecutorBlockStrategy("SERIAL_EXECUTION");
        info.setExecutorTimeout(0);
        info.setExecutorFailRetryCount(0);
        info.setGlueType("BEAN");
        info.setTriggerStatus(0);
        info.setTriggerLastTime(0L);
        info.setTriggerNextTime(0L);
        return info;
    }

    public static XxlJobLog createJobLog() {
        XxlJobLog log = new XxlJobLog();
        log.setJobGroup(1);
        log.setJobId(1);
        log.setExecutorAddress("127.0.0.1:9999");
        log.setExecutorHandler("testHandler");
        log.setExecutorParam("");
        log.setExecutorFailRetryCount(0);
        log.setTriggerTime(new Date());
        log.setTriggerCode(200);
        log.setHandleCode(0);
        log.setAlarmStatus(0);
        return log;
    }

    public static XxlJobLogGlue createLogGlue() {
        XxlJobLogGlue glue = new XxlJobLogGlue();
        glue.setJobId(1);
        glue.setGlueType("GLUE_GROOVY");
        glue.setGlueSource("// test source");
        glue.setGlueRemark("test remark");
        glue.setAddTime(new Date());
        glue.setUpdateTime(new Date());
        return glue;
    }

    public static XxlJobLogReport createLogReport() {
        XxlJobLogReport report = new XxlJobLogReport();
        report.setTriggerDay(new Date());
        report.setRunningCount(0);
        report.setSucCount(0);
        report.setFailCount(0);
        return report;
    }

    public static XxlJobUser createUser() {
        XxlJobUser user = new XxlJobUser();
        user.setUsername("testuser");
        user.setPassword("testpassword");
        user.setRole(0);
        user.setPermission("");
        return user;
    }

}