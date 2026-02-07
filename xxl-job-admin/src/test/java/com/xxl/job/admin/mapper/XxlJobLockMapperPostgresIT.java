package com.xxl.job.admin.mapper;

import com.xxl.job.admin.testcontainers.PostgreSQLContainerHolder;
import com.xxl.job.admin.testcontainers.PostgresMapperITBase;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

class XxlJobLockMapperPostgresIT extends PostgresMapperITBase {

    @Resource
    private XxlJobLockMapper xxlJobLockMapper;

    @Resource
    private DataSource dataSource;

    @Test
    void scheduleLock_shouldReturnLockRow() {
        String result = xxlJobLockMapper.scheduleLock();

        assertThat(result).isNotNull();
    }

    @Test
    void scheduleLock_shouldReturnScheduleLockName() {
        String result = xxlJobLockMapper.scheduleLock();

        // The query selects * from xxl_job_lock where lock_name = 'schedule_lock'
        // resultType is String, so it returns the lock_name column value
        assertThat(result).isEqualTo("schedule_lock");
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void scheduleLock_shouldBlockConcurrentAccess() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch lockAcquired = new CountDownLatch(1);
        AtomicBoolean secondThreadBlocked = new AtomicBoolean(false);

        try {
            // Thread 1: acquire FOR UPDATE lock and hold it
            Future<?> holder = executor.submit(() -> {
                try (Connection conn = dataSource.getConnection()) {
                    conn.setAutoCommit(false);
                    try (PreparedStatement ps = conn.prepareStatement(
                            "SELECT * FROM xxl_job_lock WHERE lock_name = 'schedule_lock' FOR UPDATE")) {
                        ResultSet rs = ps.executeQuery();
                        assertThat(rs.next()).isTrue();
                        lockAcquired.countDown();

                        // Hold the lock for a while
                        Thread.sleep(3000);
                    } finally {
                        conn.rollback();
                    }
                }
                return null;
            });

            // Wait for thread 1 to acquire the lock
            assertThat(lockAcquired.await(10, TimeUnit.SECONDS)).isTrue();

            // Thread 2: try to acquire the same lock with a short timeout
            Future<?> waiter = executor.submit(() -> {
                try (Connection conn = dataSource.getConnection()) {
                    conn.setAutoCommit(false);
                    // Set a short lock timeout so we don't wait forever
                    try (PreparedStatement setTimeout = conn.prepareStatement(
                            "SET lock_timeout = '500ms'")) {
                        setTimeout.execute();
                    }
                    try (PreparedStatement ps = conn.prepareStatement(
                            "SELECT * FROM xxl_job_lock WHERE lock_name = 'schedule_lock' FOR UPDATE")) {
                        ps.executeQuery();
                    } catch (SQLException e) {
                        // Expected: lock timeout or cancellation
                        secondThreadBlocked.set(true);
                    } finally {
                        conn.rollback();
                    }
                }
                return null;
            });

            // Wait for both threads to complete
            try {
                waiter.get(10, TimeUnit.SECONDS);
            } catch (TimeoutException e) {
                secondThreadBlocked.set(true);
                waiter.cancel(true);
            }
            holder.get(10, TimeUnit.SECONDS);

            assertThat(secondThreadBlocked.get())
                    .as("Second thread should be blocked by FOR UPDATE lock")
                    .isTrue();
        } finally {
            executor.shutdownNow();
            executor.awaitTermination(5, TimeUnit.SECONDS);
        }
    }
}
