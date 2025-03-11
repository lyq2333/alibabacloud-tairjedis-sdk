package com.aliyun.tair.tests;

import java.util.HashSet;
import java.util.Set;

import io.valkey.HostAndPort;
import io.valkey.Jedis;
import io.valkey.JedisCluster;
import io.valkey.JedisPool;
import io.valkey.JedisPoolConfig;

public class TestBase {
    protected static final String HOST = "127.0.0.1";
    protected static final int PORT = 6379;
    protected static final int CLUSTER_PORT = 30001;

    protected static JedisPool jedisPool;
    protected static JedisCluster jedisCluster;

    static {
        try {
            jedisPool = new JedisPool(HOST, PORT);
            Set<HostAndPort> jedisClusterNodes = new HashSet<HostAndPort>();
            jedisClusterNodes.add(new HostAndPort(HOST, CLUSTER_PORT));
            jedisCluster = new JedisCluster(jedisClusterNodes);
        } catch (Exception e) {
            System.out.println(e.getCause().getMessage());
        }
    }

    protected static Jedis getJedis() {
        return new Jedis(HOST, PORT);
    }
}
