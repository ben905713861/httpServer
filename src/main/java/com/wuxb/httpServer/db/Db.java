package com.wuxb.httpServer.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.wuxb.httpServer.db.ConnectionPool;
import com.wuxb.httpServer.util.Tools;

public class Db {

	private String table;
	private String field_sql = "*";
	private String join_sql = "";
	private String where_sql = "";
	private String groupBy_sql = "";
	private String having_sql = "";
	private String orderBy_sql = "";
	private String limit_sql = "";
	private List<Object> bindList = new LinkedList<Object>();
	
	private Db(String table) {
		this.table = table;
	}
	
	public static Db table(String table) {
		return new Db(table);
	}
	
	public Db where(Map<String, Object> whereMap) {
		if(whereMap == null || whereMap.size() == 0) {
			return this;
		}
		if(where_sql.isEmpty()) {
			where_sql = " WHERE 1=1 ";
		}
		for(Entry<String, Object> entrySet : whereMap.entrySet()) {
			String key = entrySet.getKey();
			Object value = entrySet.getValue();
			//空值
			if(value == null) {
				where_sql += " AND "+ key +" IS NULL ";
			}
			//通过数组携带特殊查询
			else if(value.getClass().isArray()) {
				Object[] queryArr = (Object[]) value;
				if(queryArr.length < 2) {
					System.err.println("where()方法子数组参数错误");
					continue;
				}
				String type = ((String) queryArr[0]).toUpperCase();
				switch(type) {
					case "LIKE":
					case "NOT LIKE":
						where_sql += " AND "+ key +" "+ type +" ? ";
						break;
					case "<":
					case "<=":
					case ">":
					case ">=":
					case "=":
					case "<>":
						where_sql += " AND "+ key + type +"? ";
						break;
					case "BETWEEN":
					case "NOT BETWEEN":
						Object[] between_query = (Object[]) queryArr[1];
						if(between_query.length != 2) {
							System.err.println("between查询条件长度只能是2个");
							continue;
						}
						where_sql += " AND "+ key +" "+ type +" ? AND ? ";
						break;
					case "IN":
					case "NOT IN":
						Object[] in_query = (Object[]) queryArr[1];
						if(in_query.length == 0) {
							System.err.println("in查询条件不得为空");
							continue;
						}
						where_sql += " AND "+ key +" "+ type +" (";
						for(int i = 0; i < in_query.length; i++) {
							where_sql += "?,";
						}
						where_sql = where_sql.substring(0, where_sql.length() -1);
						where_sql += ") ";
						break;
					default:
						System.err.println("查询类型"+ type +"不支持");
				}
			}
			//常规=
			else {
				where_sql += " AND "+ key +"=? ";
			}
		}
		bindList.addAll(whereMap.values());
		return this;
	}
	public Db where(String key, Object value) {
		if(where_sql.isEmpty()) {
			where_sql = " WHERE 1=1 ";
		}
		if(value == null) {
			where_sql += " AND " + key + " IS NULL ";
		} else if(value instanceof String && ((String) value).equals("NOT NULL")) {
			where_sql += " AND " + key + " IS NOT NULL ";
		} else {
			where_sql += " AND " + key + "=? ";
			bindList.add(value);
		}
		return this;
	}
	public Db where(String key, String type, Object value) {
		if(where_sql.isEmpty()) {
			where_sql = " WHERE 1=1 ";
		}
		type = type.toUpperCase();
		switch(type) {
			case "LIKE":
			case "NOT LIKE":
				where_sql += " AND "+ key +" "+ type +" ? ";
				bindList.add(value);
				break;
			case "<":
			case "<=":
			case ">":
			case ">=":
			case "=":
			case "<>":
				where_sql += " AND "+ key + type +"? ";
				bindList.add(value);
				break;
			case "BETWEEN":
			case "NOT BETWEEN":
				Object[] between_query = (Object[]) value;
				if(between_query.length != 2) {
					System.err.println("between查询条件长度只能是2个");
					break;
				}
				where_sql += " AND "+ key +" "+ type +" ? AND ? ";
				bindList.add(between_query[0]);
				bindList.add(between_query[1]);
				break;
			case "IN":
			case "NOT IN":
				Object[] in_query = (Object[]) value;
				if(in_query.length == 0) {
					System.err.println("in查询条件不得为空");
					break;
				}
				where_sql += " AND "+ key +" "+ type +" (";
				for(Object object : in_query) {
					where_sql += "?,";
					bindList.add(object);
				}
				where_sql = where_sql.substring(0, where_sql.length() -1);
				where_sql += ") ";
				break;
			default:
				System.err.println("查询类型"+ type +"不支持");
		}
		return this;
	}
	public Db where(String whereSql, Object[] bindValues) {
		if(where_sql.isEmpty()) {
			where_sql = " WHERE 1=1 ";
		}
		where_sql += " AND "+ whereSql;
		if(bindValues != null) {
			for(Object bindValue : bindValues) {
				bindList.add(bindValue);
			}
		}
		return this;
	}
	
	public Db order(String field, String orderType) {
		orderType = orderType.toUpperCase();
		if(!orderType.equals("ASC") && !orderType.equals("DESC")) {
			orderType = "ASC";
		}
		orderBy_sql = " ORDER BY "+ field +" "+ orderType +" ";
		return this;
	}
	
	public Db limit(int limit) {
		limit_sql = " LIMIT "+ limit;
		return this;
	}
	public Db limit(long offset, long limit) {
		return limit((int) offset, (int) limit);
	}
	public Db limit(int offset, int limit) {
		limit_sql = " LIMIT "+ offset +","+ limit;
		return this;
	}
	public Db page(long page, long limit) {
		return page((int) page, (int) limit);
	}
	public Db page(int page, int limit) {
		if(page < 1) {
			page = 1;
		}
		int offset = (page - 1) * limit;
		limit_sql = " LIMIT "+ offset +","+ limit;
		return this;
	}
	
	public Db field(String fields) {
		field_sql = fields;
		return this;
	}
	
	public Db join(String table, String link, String joinType) {
		join_sql += " "+ joinType + " JOIN "+ table +" ON "+ link +" ";
		return this;
	}
	
	public Db group(String field) {
		groupBy_sql = " GROUP BY "+ field +" ";
		return this;
	}
	
	public Db having(String query) {
		having_sql = " HAVING "+ query +" ";
		return this;
	}
	
	public List<Map<String, Object>> select() throws SQLException {
		String sql = "SELECT "+ field_sql +" FROM "+ table + join_sql + where_sql + groupBy_sql + having_sql + orderBy_sql + limit_sql;
		return query(sql, bindList);
	}
	
	public Map<String, Object> find() throws SQLException {
		String sql = "SELECT "+ field_sql +" FROM "+ table + join_sql + where_sql + groupBy_sql + having_sql + orderBy_sql + " LIMIT 1";
		List<Map<String, Object>> list = query(sql, bindList);
		if(list.size() == 0) {
			return null;
		}
		return list.get(0);
	}
	
	public List<Object> column(String field) throws SQLException {
		field(field);
		List<Map<String, Object>> rows = select();
		//获得纯净字段名
		if(field.lastIndexOf(" ") != -1) {
			field = field.substring(field.lastIndexOf(" ") + 1);
		}
		if(field.lastIndexOf(".") != -1) {
			field = field.substring(field.lastIndexOf(".") + 1);
		}
		List<Object> resList = new LinkedList<Object>();
		for(Map<String, Object> row : rows) {
			resList.add(row.get(field));
		}
		return resList;
	}
	
	public Object value(String field) throws SQLException {
		field(field);
		Map<String, Object> row = find();
		if(field.lastIndexOf(" ") != -1) {
			field = field.substring(field.lastIndexOf(" ") + 1);
		}
		if(field.lastIndexOf(".") != -1) {
			field = field.substring(field.lastIndexOf(".") + 1);
		}
		return row.get(field);
	}
	
	public int count() throws SQLException {
		Object count = value("COUNT(*) count");
		return ((Long) count).intValue();
	}
	
	public int insert(Map<String, Object> data) throws SQLException {
		String sql = "INSERT INTO "+ table +" (";
		String[] keys = data.keySet().toArray(new String[data.size()]);
		sql += Tools.array2String(",", keys);
		sql += ") VALUES (";
		for(Object value : data.values()) {
			if(value == null) {
				sql += "NULL,";
			} else {
				sql += "?,";
			}
		}
		sql = sql.substring(0, sql.length()-1);
		sql += ")";
		bindList.clear();
		bindList.addAll(data.values());
		return execute(sql, bindList);
	}
	
	public int insertAll(List<Map<String, Object>> dataList) throws SQLException {
		String sql = "INSERT INTO "+ table +" (";
		Set<String> fields = dataList.get(0).keySet();
		String[] keys = fields.toArray(new String[fields.size()]);
		sql += Tools.array2String(",", keys);
		sql += ") VALUES ";
		//数据位置
		for(Map<String, Object> data : dataList) {
			if(data.size() == 0) {
				continue;
			}
			String data_sql = "(";
			for(Object value : data.values()) {
				if(value == null) {
					data_sql += "NULL,";
				} else {
					data_sql += "?,";
				}
			}
			data_sql = data_sql.substring(0, data_sql.length()-1);
			data_sql += "),";
			sql += data_sql;
		}
		sql = sql.substring(0, sql.length()-1);
		//数据绑定
		bindList.clear();
		for(Map<String, Object> data : dataList) {
			bindList.addAll(data.values());
		}
		return execute(sql, bindList);
	}
	
	public int update(Map<String, Object> data) throws SQLException {
		String sql = "UPDATE "+ table +" SET ";
		for(Entry<String, Object> entry : data.entrySet()) {
			String key = entry.getKey();
			Object value = entry.getValue();
			if(value == null) {
				sql += key +"=NULL,";
			} else {
				sql += key +"=?,";
			}
		}
		sql = sql.substring(0, sql.length()-1);
		sql += where_sql + limit_sql;
		bindList.addAll(0, data.values());
		return execute(sql, bindList);
	}
	
	public int delete() throws SQLException {
		String sql = "DELETE FROM "+ table + where_sql + limit_sql;
		return execute(sql, bindList);
	}
	
	public static void begin() throws SQLException {
		ConnectionPool.getConnection().setAutoCommit(false);
	}
	
	public static void commit() throws SQLException {
		Connection connection = ConnectionPool.getConnection();
		connection.commit();
		connection.setAutoCommit(true);
		ConnectionPool.releaseConnection();
	}
	
	public static void rollback() throws SQLException {
		Connection connection = ConnectionPool.getConnection();
		connection.rollback();
		connection.setAutoCommit(true);
		ConnectionPool.releaseConnection();
	}
	
	public static List<Map<String, Object>> query(String sql,List<Object> bindList) throws SQLException {
		Connection connection = null;
		try {
			connection = ConnectionPool.getConnection();
			List<Map<String, Object>> resList = new LinkedList<Map<String, Object>>();
			PreparedStatement pstmt = connection.prepareStatement(sql);//创建预编译语句对象
			bindValues(pstmt, bindList);
			ResultSet resultSet = pstmt.executeQuery();//执行预编译语句
			ResultSetMetaData md = resultSet.getMetaData();
			//获取字段名
			int columnCount = md.getColumnCount();
			String columns[] = new String[columnCount];
			for(int i = 1; i <= columnCount; i++) {
				columns[i-1] = md.getColumnLabel(i);//获取字段别名（如果有）,没有别名则返回原名
			}
			//存储结果
			while(resultSet.next()) {
				Map<String, Object> rowData = new LinkedHashMap<String, Object>();
				for(int i = 1; i <= columnCount; i++) {
					rowData.put(columns[i-1], resultSet.getObject(i));
				}
				resList.add(rowData);
			}
			pstmt.close();
			resultSet.close();
			return resList;
		} catch(SQLException e) {
			throw new SQLException(e.getMessage());
		} finally {
			//连接池回收
			if(connection != null && connection.getAutoCommit()) {
				ConnectionPool.releaseConnection();
			}
		}
	}
	
	public static int execute(String sql, List<Object> bindList) throws SQLException {
		Connection connection = null;
		try {
			connection = ConnectionPool.getConnection();
			PreparedStatement pstmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);//创建预编译语句对象
			bindValues(pstmt, bindList);
			int res = pstmt.executeUpdate();//执行预编译语句
			//返回新增主键（如果是inser语句）
			ResultSet rs = pstmt.getGeneratedKeys();
			if(rs.next()) {
				res = (int) rs.getLong(1);
			}
			pstmt.close();
			return res;
		} catch(SQLException e) {
			throw new SQLException(e.getMessage());
		} finally {
			//连接池回收
			if(connection != null && connection.getAutoCommit()) {
				ConnectionPool.releaseConnection();
			}
		}
	}
	
	private static void bindValues(PreparedStatement pstmt, List<Object> bindList) throws SQLException {
		if(bindList == null || bindList.size() == 0) {
			return;
		}
		//整理
		LinkedList<Object> newBindList = new LinkedList<Object>();
		for(Object data : bindList) {
			if(data == null) {
				continue;
			}
			if(data instanceof Object[]) {
				Object[] queryObjects = (Object[]) data;
				if(queryObjects[1] instanceof Object[]) {
					Object[] queryValues = (Object[]) queryObjects[1];
					for(Object value : queryValues) {
						newBindList.add(value);
					}
				} else {
					newBindList.add(queryObjects[1]);
				}
			} else {
				newBindList.add(data);
			}
		}
		bindList = newBindList;
		//赋值
		int i = 1;
		for(Object data : bindList) {
			if(data instanceof String) {
				pstmt.setString(i, (String) data);
			}
			else if(data instanceof Integer) {
				pstmt.setInt(i, (int) data);
			}
			else if(data instanceof Long) {
				pstmt.setLong(i, (long) data);
			}
			else if(data instanceof Short) {
				pstmt.setShort(i, (short) data);
			}
			else if(data instanceof Byte) {
				pstmt.setByte(i, (byte) data);
			}
			else if(data instanceof Float) {
				pstmt.setFloat(i, (float) data);
			}
			else if(data instanceof Double) {
				pstmt.setDouble(i, (double) data);
			}
			else if(data instanceof Boolean) {
				pstmt.setBoolean(i, (boolean) data);
			}
			else {
				throw new SQLException("输入数据必须是数字或者字符串类型");
			}
			i++;
		}
	}
	
}
