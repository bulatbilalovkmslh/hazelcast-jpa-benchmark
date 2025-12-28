package org.example.config;

import com.hazelcast.config.Config;
import com.hazelcast.config.EvictionConfig;
import com.hazelcast.config.EvictionPolicy;
import com.hazelcast.config.JoinConfig;
import com.hazelcast.config.MapConfig;
import com.hazelcast.config.MaxSizePolicy;
import com.hazelcast.config.NetworkConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class HazelcastConfig {

    @Bean
    public Config hazelcastInstanceConfig() {
        Config config = new Config();

        config.setClusterName("hazelcast-jpa-benchmark");

        MapConfig customerCacheMap = new MapConfig("customer-cache");
        customerCacheMap.setTimeToLiveSeconds(60);

        EvictionConfig evictionConfig = new EvictionConfig()
                .setEvictionPolicy(EvictionPolicy.LRU)
                .setMaxSizePolicy(MaxSizePolicy.PER_NODE)
                .setSize(100);

        customerCacheMap.setEvictionConfig(evictionConfig);

        config.addMapConfig(customerCacheMap);

        NetworkConfig networkConfig = config.getNetworkConfig();
        networkConfig.setPort(5701).setPortAutoIncrement(true);

        JoinConfig join = networkConfig.getJoin();
        join.getMulticastConfig().setEnabled(false);

        join.getTcpIpConfig()
                .setEnabled(true)
                .addMember("127.0.0.1:5701")
                .addMember("127.0.0.1:5702")
                .addMember("127.0.0.1:5703");

        return config;
    }
}
