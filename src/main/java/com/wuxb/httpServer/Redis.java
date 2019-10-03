package com.wuxb.httpServer;

import com.wuxb.httpServer.util.Config;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

public class Redis {
	
	private static final String host = Config.get("redis.host");
	private static final int port = Integer.parseInt(Config.get("redis.port"));
	private static final String password = Config.get("redis.password");
	private static final int db = Integer.parseInt(Config.get("redis.db"));
	
	private static JedisPool jedisPool;
	
	static {
		JedisPoolConfig jedisPoolConfig = new JedisPoolConfig();
		jedisPoolConfig.setMaxTotal(500);
		jedisPoolConfig.setMaxIdle(10);
		jedisPoolConfig.setMaxWaitMillis(3000);
		if(password.isEmpty()) {
			jedisPool = new JedisPool(jedisPoolConfig, host, port, 3000);
		} else {
			jedisPool = new JedisPool(jedisPoolConfig, host, port, 3000, password);
		}
	}
	
	public static Jedis getConn() {
		Jedis jedis = jedisPool.getResource();
		if(db != 0) {
			jedis.select(db);
		}
		return jedis;
	}
	
}
