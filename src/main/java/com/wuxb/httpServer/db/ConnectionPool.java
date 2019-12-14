package com.wuxb.httpServer.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;
import java.util.Vector;

import com.wuxb.httpServer.util.Config;

public class ConnectionPool {
	
	private static final int MIN_CONN_NUM = 5; //线程池最小连接数
	private static final int MAX_CONN_NUM = 10; //线程池最大连接数
	private static final String driver = Config.get("database.driver");
	private static final String url = Config.get("database.url");
	private static final String username = Config.get("database.username");
	private static final String password = Config.get("database.password");
	//线程池
	public static List<Connection> connections = new Vector<Connection>();
	private static ThreadLocal<Connection> conWrapper = new ThreadLocal<Connection>();
	//锁
	private static Object getcConnLock = new Object();
	private static Object releaseLock = new Object();
	
	static {
		try {
			Class.forName(driver);
			for(int i = 0; i < MIN_CONN_NUM; i++) {
				connections.add(createConnection());
			}
			keepConnection();
		} catch (ClassNotFoundException e1) {
			e1.printStackTrace();
		} catch (SQLException e1) {
			e1.printStackTrace();
		}
	}
	
	private ConnectionPool() {
		
	}
	
	//创建连接
	private static Connection createConnection() throws SQLException {
		return DriverManager.getConnection(url, username, password);
	}
	
	//心跳轮询来保持连接
	private static void keepConnection() {
		new Thread(new Runnable() {
			@Override
			public void run() {
				while(true) {
					try {
						//每20秒轮询一次，每次检查一个连接
						Thread.sleep(3000);
					} catch(Exception e) {
						e.printStackTrace();
						continue;
					}
					
					Connection connection = null;
					try {
						connection = getConnection();
						connection.prepareStatement("SELECT 1").executeQuery();
						releaseConnection();
					} catch (SQLException e) {
						try {
							if(connection != null) {
								connection.close();
							}
						} catch (SQLException e1) {
							e1.printStackTrace();
						}
					}
				}
			}
		}).start();
	}
	
	//输出连接
	public static Connection getConnection() throws SQLException {
		Connection connection = conWrapper.get();
		if(connection != null && !connection.isClosed()) {
			return connection;
		}
		synchronized(getcConnLock) {
			if(connections.size() == 0) {
				connection = createConnection();
			} else {
				connection = connections.remove(0);
				if(connection.isClosed()) {
					connection = createConnection();
				}
			}
			conWrapper.set(connection);
			return connection;
		}
	}
	
	//回收连接
	public static void releaseConnection() throws SQLException {
		Connection connection = conWrapper.get();
		if(connection == null || connection.isClosed()) {
			return;
		}
		conWrapper.remove();
		//同一时刻只能释放一个资源
		synchronized(releaseLock) {
			if(connections.size() >= MAX_CONN_NUM) {
				connection.close();
				return;
			}
			if(!connection.getAutoCommit()) {
				connection.setAutoCommit(true);
			}
			connections.add(connection);
		}
	}
	
}
