package com.thebiggestdata.measurementmetrics;

import com.hazelcast.cluster.MembershipEvent;
import com.hazelcast.cluster.MembershipListener;
import com.hazelcast.config.Config;
import com.hazelcast.config.JoinConfig;
import com.hazelcast.config.NetworkConfig;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;

import java.util.concurrent.atomic.AtomicBoolean;

public final class RecoveryTime {

    private RecoveryTime() {}

    public static void main(String[] args) throws Exception {
        System.setProperty("hazelcast.logging.type", "none");
        HazelcastInstance hazelcast = Hazelcast.newHazelcastInstance(memberConfig());

        while (hazelcast.getCluster().getMembers().size() < 2) {
            Thread.sleep(1_000);
        }
        while (!hazelcast.getPartitionService().isClusterSafe()) {
            Thread.sleep(500);
        }

        System.out.println("\n>>> CLUSTER IS SAFE. Stop one service node now.");
        AtomicBoolean measuring = new AtomicBoolean();
        hazelcast.getCluster().addMembershipListener(new MembershipListener() {
            @Override
            public void memberAdded(MembershipEvent event) {
                System.out.println("New member added: " + event.getMember());
            }

            @Override
            public void memberRemoved(MembershipEvent event) {
                if (measuring.compareAndSet(false, true)) {
                    System.out.println("Member removed: " + event.getMember());
                    measureRecovery(hazelcast, System.nanoTime(), measuring);
                }
            }
        });

        Thread.currentThread().join();
    }

    private static Config memberConfig() {
        Config config = new Config();
        config.setClusterName(System.getenv().getOrDefault("HAZELCAST_CLUSTER_NAME", "SearchEngine"));
        config.getMemberAttributeConfig().setAttribute("role", "benchmark");

        NetworkConfig network = config.getNetworkConfig();
        network.setPort(Integer.parseInt(System.getenv().getOrDefault("HZ_PORT", "5704")));
        network.setPortAutoIncrement(false);
        config.setProperty("hazelcast.wait.seconds.before.join", "0");

        String publicAddress = System.getenv("HZ_PUBLIC_ADDRESS");
        if (publicAddress != null && !publicAddress.isBlank()) {
            network.setPublicAddress(publicAddress);
        }

        JoinConfig join = network.getJoin();
        join.getAutoDetectionConfig().setEnabled(false);
        join.getMulticastConfig().setEnabled(false);
        join.getTcpIpConfig().setEnabled(true);
        String members = System.getenv().getOrDefault(
                "HZ_MEMBERS",
                "localhost:5701,localhost:5702,localhost:5703"
        );
        for (String member : members.split(",")) {
            join.getTcpIpConfig().addMember(member.trim());
        }
        return config;
    }

    private static void measureRecovery(
            HazelcastInstance hazelcast,
            long startTime,
            AtomicBoolean measuring
    ) {
        Thread worker = new Thread(() -> {
            try {
                while (!hazelcast.getPartitionService().isClusterSafe()) {
                    Thread.sleep(50);
                }
                long recoveryTimeMs = (System.nanoTime() - startTime) / 1_000_000;
                System.out.printf("RECOVERY TIME: %.3f s (%d ms)%n",
                        recoveryTimeMs / 1_000.0, recoveryTimeMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                measuring.set(false);
            }
        }, "recovery-time-measurement");
        worker.setDaemon(true);
        worker.start();
    }
}
