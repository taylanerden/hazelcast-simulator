package com.hazelcast.simulator.tests.external;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.ICountDownLatch;
import com.hazelcast.core.IList;
import com.hazelcast.logging.ILogger;
import com.hazelcast.logging.Logger;
import com.hazelcast.simulator.probes.probes.IntervalProbe;
import com.hazelcast.simulator.probes.probes.SimpleProbe;
import com.hazelcast.simulator.test.TestContext;
import com.hazelcast.simulator.test.annotations.Run;
import com.hazelcast.simulator.test.annotations.Setup;
import com.hazelcast.util.EmptyStatement;

import java.util.concurrent.TimeUnit;

import static java.lang.String.format;

public class ExternalClientTest {

    private static final ILogger LOGGER = Logger.getLogger(ExternalClientTest.class);

    // properties
    public String basename = "externalClientsRunning";
    public int waitForClientsCount = 1;
    public int waitIntervalSeconds = 60;

    IntervalProbe externalClientLatency;
    SimpleProbe externalClientThroughput;

    private HazelcastInstance hazelcastInstance;
    private ICountDownLatch clientsRunning;
    private boolean isSingletonInstance;

    @Setup
    public void setUp(TestContext testContext) throws Exception {
        hazelcastInstance = testContext.getTargetInstance();

        // limit to one instance per cluster
        if (hazelcastInstance.getMap(basename).putIfAbsent(basename, true) != null) {
            return;
        }

        clientsRunning = hazelcastInstance.getCountDownLatch(basename);
        clientsRunning.trySetCount(waitForClientsCount);

        isSingletonInstance = true;
    }

    @Run
    public void run() {
        if (!isSingletonInstance) {
            return;
        }
        while (true) {
            try {
                clientsRunning.await(waitIntervalSeconds, TimeUnit.SECONDS);
            } catch (InterruptedException ignored) {
                EmptyStatement.ignore(ignored);
            }
            long clientsRunningCount = clientsRunning.getCount();
            if (clientsRunningCount > 0) {
                LOGGER.info("Waiting for " + clientsRunningCount + " clients...");
            } else {
                LOGGER.info("Got response from " + waitForClientsCount + " clients, stopping now!");
                break;
            }
        }

        IList<Long> latencyResults = hazelcastInstance.getList("externalClientsLatencyResults");
        for (Long latency : latencyResults) {
            externalClientLatency.recordValue(latency);
        }

        double totalDuration = 0;
        int totalInvocations = 0;
        IList<String> throughputResults = hazelcastInstance.getList("externalClientsThroughputResults");
        for (String throughputString : throughputResults) {
            String[] throughput = throughputString.split("\\|");
            long operationCount = Long.valueOf(throughput[0]);
            long duration = TimeUnit.NANOSECONDS.toMillis(Long.valueOf(throughput[1]));

            LOGGER.info(format("External client executed %d operations in %d ms", operationCount, duration));

            totalDuration += duration;
            totalInvocations += operationCount;
        }
        long durationAvg = Math.round(totalDuration / throughputResults.size());

        LOGGER.info(format("All external clients executed %d operations in %d ms", totalInvocations, durationAvg));

        externalClientThroughput.setValues(durationAvg, totalInvocations);
    }
}
