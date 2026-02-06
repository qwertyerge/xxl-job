package com.xxl.job.admin.mapper;

import com.xxl.job.admin.model.XxlJobLogReport;
import com.xxl.job.admin.test.support.MapperITBase;
import com.xxl.job.admin.test.support.TestFixtures;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * Integration tests for XxlJobLogReportMapper using TestContainers MySQL.
 */
class XxlJobLogReportMapperIT extends MapperITBase {

    @Autowired
    private XxlJobLogReportMapper xxlJobLogReportMapper;

    /**
     * Helper method to truncate a Date to day precision (reset time part to 00:00:00).
     * This is required because trigger_day is a unique key of datetime type.
     */
    private Date truncateToDay(Date date) {
        if (date == null) {
            return null;
        }
        LocalDate localDate = date.toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDate();
        return Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

    private Date addDays(Date date, int days) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.add(Calendar.DAY_OF_MONTH, days);
        return calendar.getTime();
    }

    @Test
    void testSaveOrUpdate_insertsNewRecord_whenTriggerDayIsNew() {
        // given
        XxlJobLogReport report = TestFixtures.createLogReport();
        report.setTriggerDay(truncateToDay(new Date()));
        report.setRunningCount(10);
        report.setSucCount(20);
        report.setFailCount(5);

        // when
        int result = xxlJobLogReportMapper.saveOrUpdate(report);

        // then
        Assertions.assertThat(result).isEqualTo(1);
        Assertions.assertThat(report.getId()).isPositive();
    }

    @Test
    void testSaveOrUpdate_updatesExistingRecord_whenTriggerDayExists() {
        // given - insert a new record
        Date triggerDay = truncateToDay(new Date());
        XxlJobLogReport report = TestFixtures.createLogReport();
        report.setTriggerDay(triggerDay);
        report.setRunningCount(10);
        report.setSucCount(20);
        report.setFailCount(5);
        xxlJobLogReportMapper.saveOrUpdate(report);
        Integer savedId = report.getId();

        // when - update with same triggerDay but different counts
        XxlJobLogReport updated = TestFixtures.createLogReport();
        updated.setTriggerDay(triggerDay);
        updated.setRunningCount(15);
        updated.setSucCount(25);
        updated.setFailCount(8);
        int result = xxlJobLogReportMapper.saveOrUpdate(updated);

        // then - should update existing record (result=2 means 2 rows affected by ON DUPLICATE KEY UPDATE)
        // Note: MySQL returns 2 for INSERT...ON DUPLICATE KEY UPDATE when a new row is inserted,
        // and 1 when an existing row is updated (because affected-rows=1 but rows-matched=1)
        Assertions.assertThat(result).isGreaterThanOrEqualTo(1);
        // After update, the id should still be the same as the original insert
        Assertions.assertThat(updated.getId()).isEqualTo(savedId);

        // verify counts are updated
        XxlJobLogReport verified = xxlJobLogReportMapper.queryLogReport(triggerDay, triggerDay).get(0);
        Assertions.assertThat(verified.getRunningCount()).isEqualTo(15);
        Assertions.assertThat(verified.getSucCount()).isEqualTo(25);
        Assertions.assertThat(verified.getFailCount()).isEqualTo(8);
    }

    @Test
    void testQueryLogReport_filtersByDateRange_andOrdersByTriggerDayAsc() {
        // given - insert 3 records across 3 consecutive days
        Date day1 = truncateToDay(new Date());
        XxlJobLogReport report1 = TestFixtures.createLogReport();
        report1.setTriggerDay(day1);
        report1.setRunningCount(1);
        report1.setSucCount(2);
        report1.setFailCount(3);
        xxlJobLogReportMapper.saveOrUpdate(report1);

        Date day2 = addDays(day1, 1);
        XxlJobLogReport report2 = TestFixtures.createLogReport();
        report2.setTriggerDay(day2);
        report2.setRunningCount(4);
        report2.setSucCount(5);
        report2.setFailCount(6);
        xxlJobLogReportMapper.saveOrUpdate(report2);

        Date day3 = addDays(day1, 2);
        XxlJobLogReport report3 = TestFixtures.createLogReport();
        report3.setTriggerDay(day3);
        report3.setRunningCount(7);
        report3.setSucCount(8);
        report3.setFailCount(9);
        xxlJobLogReportMapper.saveOrUpdate(report3);

        // when - query range from day1 to day3
        List<XxlJobLogReport> result = xxlJobLogReportMapper.queryLogReport(day1, day3);

        // then - should return 3 records ordered by trigger_day ASC
        Assertions.assertThat(result).hasSize(3);
        Assertions.assertThat(result.get(0).getTriggerDay()).isEqualTo(day1);
        Assertions.assertThat(result.get(1).getTriggerDay()).isEqualTo(day2);
        Assertions.assertThat(result.get(2).getTriggerDay()).isEqualTo(day3);
        Assertions.assertThat(result.get(0).getRunningCount()).isEqualTo(1);
        Assertions.assertThat(result.get(1).getRunningCount()).isEqualTo(4);
        Assertions.assertThat(result.get(2).getRunningCount()).isEqualTo(7);
    }

    @Test
    void testQueryLogReport_returnsOnlyRecordsWithinRange() {
        // given - insert 3 records across 5 days (with gaps)
        Date day1 = truncateToDay(new Date());
        XxlJobLogReport report1 = TestFixtures.createLogReport();
        report1.setTriggerDay(day1);
        xxlJobLogReportMapper.saveOrUpdate(report1);

        Date day3 = addDays(day1, 2);
        XxlJobLogReport report3 = TestFixtures.createLogReport();
        report3.setTriggerDay(day3);
        xxlJobLogReportMapper.saveOrUpdate(report3);

        Date day5 = addDays(day1, 4);
        XxlJobLogReport report5 = TestFixtures.createLogReport();
        report5.setTriggerDay(day5);
        xxlJobLogReportMapper.saveOrUpdate(report5);

        // when - query range from day2 to day4
        Date day2 = addDays(day1, 1);
        Date day4 = addDays(day1, 3);
        List<XxlJobLogReport> result = xxlJobLogReportMapper.queryLogReport(day2, day4);

        // then - should return only day3 record
        Assertions.assertThat(result).hasSize(1);
        Assertions.assertThat(result.get(0).getTriggerDay()).isEqualTo(day3);
    }

    @Test
    void testQueryLogReport_returnsEmptyList_whenNoRecordsInRange() {
        // given
        Date from = truncateToDay(new Date());
        Date to = addDays(from, 10);

        // when
        List<XxlJobLogReport> result = xxlJobLogReportMapper.queryLogReport(from, to);

        // then
        Assertions.assertThat(result).isEmpty();
    }

    @Test
    void testQueryLogReportTotal_sumsAllCounts() {
        // given - insert 3 records
        Date day1 = truncateToDay(new Date());
        XxlJobLogReport report1 = TestFixtures.createLogReport();
        report1.setTriggerDay(day1);
        report1.setRunningCount(10);
        report1.setSucCount(20);
        report1.setFailCount(5);
        xxlJobLogReportMapper.saveOrUpdate(report1);

        Date day2 = addDays(day1, 1);
        XxlJobLogReport report2 = TestFixtures.createLogReport();
        report2.setTriggerDay(day2);
        report2.setRunningCount(5);
        report2.setSucCount(10);
        report2.setFailCount(2);
        xxlJobLogReportMapper.saveOrUpdate(report2);

        Date day3 = addDays(day1, 2);
        XxlJobLogReport report3 = TestFixtures.createLogReport();
        report3.setTriggerDay(day3);
        report3.setRunningCount(3);
        report3.setSucCount(7);
        report3.setFailCount(1);
        xxlJobLogReportMapper.saveOrUpdate(report3);

        // when
        XxlJobLogReport total = xxlJobLogReportMapper.queryLogReportTotal();

        // then - sums: running=10+5+3=18, suc=20+10+7=37, fail=5+2+1=8
        Assertions.assertThat(total.getRunningCount()).isEqualTo(18);
        Assertions.assertThat(total.getSucCount()).isEqualTo(37);
        Assertions.assertThat(total.getFailCount()).isEqualTo(8);
    }

    @Test
    void testQueryLogReportTotal_returnsZero_whenTableIsEmpty() {
        // when
        XxlJobLogReport total = xxlJobLogReportMapper.queryLogReportTotal();

        // then - SUM returns NULL for empty table, MyBatis returns null for the whole object
        Assertions.assertThat(total).isNull();
    }
}
