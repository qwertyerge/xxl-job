package com.xxl.job.admin.mapper;

import com.xxl.job.admin.test.support.MapperITBase;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.sql.SQLException;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Integration tests for XxlJobLockMapper using TestContainers MySQL.
 * US-013: Tests pessimistic locking behavior with SELECT ... FOR UPDATE.
 */
class XxlJobLockMapperIT extends MapperITBase {

    @Autowired
    private XxlJobLockMapper xxlJobLockMapper;

    @Autowired
    private TransactionTemplate transactionTemplate;

    // Seed data already contains lock_name='schedule_lock'

    @Test
    void testScheduleLock_returnsLockName() {
        // when - acquire lock in a transaction
        String lockName = transactionTemplate.execute(status -> {
            return xxlJobLockMapper.scheduleLock();
        });

        // then
        Assertions.assertThat(lockName).isEqualTo("schedule_lock");
    }

    @Test
    void testScheduleLock_concurrentAcquisition_timeout() throws Exception {
        // CountDownLatch to coordinate thread execution
        CountDownLatch threadAStarted = new CountDownLatch(1);
        CountDownLatch threadBShouldProceed = new CountDownLatch(1);
        CountDownLatch threadBFinished = new CountDownLatch(1);

        AtomicReference<Exception> threadBException = new AtomicReference<>();
        AtomicBoolean threadAGotLock = new AtomicBoolean(false);

        ExecutorService executorService = Executors.newFixedThreadPool(2);

        try {
            // Thread A: Acquire lock and hold it
            Future<String> threadAFuture = executorService.submit(() -> {
                return transactionTemplate.execute(status -> {
                    String lockName = xxlJobLockMapper.scheduleLock();
                    threadAGotLock.set(true);

                    // Signal Thread B to proceed
                    threadAStarted.countDown();

                    try {
                        // Wait for Thread B to finish (hold the lock)
                        boolean finished = threadBFinished.await(10, TimeUnit.SECONDS);
                        if (!finished) {
                            throw new RuntimeException("Timeout waiting for Thread B");
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("Thread A interrupted", e);
                    }

                    return lockName;
                });
            });

            // Wait for Thread A to acquire the lock
            threadAStarted.await(10, TimeUnit.SECONDS);
            Assertions.assertThat(threadAGotLock.get()).isTrue();

            // Thread B: Try to acquire the same lock (should timeout)
            executorService.submit(() -> {
                return transactionTemplate.execute(status -> {
                    try {
                        // Set innodb_lock_wait_timeout=1 for this session
                        jdbcTemplate.update("SET SESSION innodb_lock_wait_timeout=1");

                        // Signal Thread B is ready to proceed
                        threadBShouldProceed.countDown();

                        // Try to acquire lock (should timeout)
                        String lockName = xxlJobLockMapper.scheduleLock();

                        // Should not reach here
                        threadBException.set(new RuntimeException("Thread B should have timed out but got lock: " + lockName));
                        return null;

                    } catch (Exception e) {
                        // Expected to get a lock timeout exception
                        threadBException.set(e);
                        threadBFinished.countDown();
                        return null;
                    }
                });
            });

            // Wait for Thread B to finish or timeout
            boolean finished = threadBFinished.await(15, TimeUnit.SECONDS);
            Assertions.assertThat(finished).isTrue();

            // Verify Thread B got a lock timeout exception
            Exception exception = threadBException.get();
            Assertions.assertThat(exception)
                    .isNotNull()
                    .hasMessageContaining("timeout");

            // Verify Thread A completed successfully
            String threadALockName = threadAFuture.get(10, TimeUnit.SECONDS);
            Assertions.assertThat(threadALockName).isEqualTo("schedule_lock");

        } finally {
            executorService.shutdownNow();
            executorService.awaitTermination(5, TimeUnit.SECONDS);
        }
    }

    @Test
    void testScheduleLock_multipleTransactions_sequential() throws Exception {
        // Test that multiple transactions can acquire the lock sequentially
        for (int i = 0; i < 3; i++) {
            String lockName = transactionTemplate.execute(status -> {
                return xxlJobLockMapper.scheduleLock();
            });
            Assertions.assertThat(lockName).isEqualTo("schedule_lock");
        }
    }
}
