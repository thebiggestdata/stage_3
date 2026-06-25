package com.thebiggestdata.measurementmetrics;

import com.hazelcast.client.HazelcastClient;
import com.hazelcast.cluster.MembershipEvent;
import com.hazelcast.cluster.MembershipListener;
import com.hazelcast.core.HazelcastInstance;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public final class RecoveryTime {

    private RecoveryTime() {}

    public static void main(String[] args) throws Exception {
        System.setProperty("hazelcast.logging.type", "none");
        HazelcastInstance hazelcast = HazelcastClient.newHazelcastClient(BenchmarkEnvironment.clientConfig());

        try {
            RecoveryMonitor monitor = new RecoveryMonitor(
                    hazelcast,
                    BenchmarkEnvironment.stableMillis(),
                    BenchmarkEnvironment.pollMillis()
            );
            monitor.waitForStableCluster();
            monitor.run();
        } finally {
            hazelcast.shutdown();
        }
    }

    private static final class RecoveryMonitor {

        private final HazelcastInstance hazelcast;
        private final long stableMillis;
        private final long pollMillis;
        private final AtomicBoolean measuring = new AtomicBoolean();
        private final AtomicLong startedAtNanos = new AtomicLong();
        private final AtomicInteger baselineMembers = new AtomicInteger();

        private RecoveryMonitor(
                HazelcastInstance hazelcast,
                long stableMillis,
                long pollMillis
        ) {
            this.hazelcast = hazelcast;
            this.stableMillis = stableMillis;
            this.pollMillis = pollMillis;
        }

        private void waitForStableCluster() throws InterruptedException {
            long stableSince = 0;
            int lastMembers = -1;

            while (true) {
                int members = memberCount();
                boolean stableMembership = members == lastMembers;
                boolean clusterSafe = hazelcast.getPartitionService().isClusterSafe();

                if (members >= 2 && stableMembership && clusterSafe) {
                    if (stableSince == 0) {
                        stableSince = System.nanoTime();
                    }
                    if (elapsedMillis(stableSince) >= stableMillis) {
                        baselineMembers.set(members);
                        System.out.printf(
                                "%n>>> CLUSTER IS SAFE AND STABLE. baselineMembers=%d%n",
                                members
                        );
                        System.out.println(">>> Stop one service node now.");
                        return;
                    }
                } else {
                    stableSince = 0;
                    lastMembers = members;
                }
                Thread.sleep(pollMillis);
            }
        }

        private void run() throws InterruptedException {
            hazelcast.getCluster().addMembershipListener(new MembershipListener() {
                @Override
                public void memberAdded(MembershipEvent event) {
                    System.out.println("New member added: " + event.getMember());
                    if (!measuring.get()) {
                        baselineMembers.set(memberCount());
                    }
                }

                @Override
                public void memberRemoved(MembershipEvent event) {
                    startMeasurement("member-removed " + event.getMember());
                }
            });

            boolean wasSafe = hazelcast.getPartitionService().isClusterSafe();
            while (true) {
                int members = memberCount();
                boolean safe = hazelcast.getPartitionService().isClusterSafe();
                if (members < baselineMembers.get()) {
                    startMeasurement("member-count " + baselineMembers.get() + "->" + members);
                }
                if (wasSafe && !safe) {
                    startMeasurement("cluster-unsafe");
                }
                if (measuring.get() && safe) {
                    finishMeasurement(members);
                }
                wasSafe = safe;
                Thread.sleep(pollMillis);
            }
        }

        private void startMeasurement(String reason) {
            if (measuring.compareAndSet(false, true)) {
                startedAtNanos.set(System.nanoTime());
                System.out.println("Recovery measurement started: " + reason);
            }
        }

        private void finishMeasurement(int members) {
            long recoveryTimeMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAtNanos.get());
            System.out.printf("RECOVERY TIME: %.3f s (%d ms) [members=%d]%n",
                    recoveryTimeMs / 1_000.0,
                    recoveryTimeMs,
                    members);
            baselineMembers.set(members);
            measuring.set(false);
        }

        private int memberCount() {
            return hazelcast.getCluster().getMembers().size();
        }

        private long elapsedMillis(long startedAtNanos) {
            return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAtNanos);
        }
    }
}
