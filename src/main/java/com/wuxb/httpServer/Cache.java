package com.wuxb.httpServer;

import redis.clients.jedis.Jedis;

public class Cache {

	public static String get(String key) {
		Jedis jedis = Redis.getConn();
		String value = jedis.get(key);
		jedis.close();
		return value;
	}
	
	public static void set(String key, Object value) {
		Jedis jedis = Redis.getConn();
		jedis.set(key, value.toString());
		jedis.close();
	}
	
	public static void set(String key, Object value, int time) {
		Jedis jedis = Redis.getConn();
		jedis.setex(key, time, value.toString());
		jedis.close();
	}
	
	public static void delete(String key) {
		Jedis jedis = Redis.getConn();
		jedis.del(key);
		jedis.close();
	}
	
}
