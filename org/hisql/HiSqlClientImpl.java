/*
 * Copyright 2014-2099 the original author or authors.
 * 
 */
package org.hisql;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang.StringEscapeUtils;
import org.hisql.annotation.DbField;
import org.hisql.annotation.NotDbField;
import org.hisql.exception.SqlRuntimeException;

/**
 * @author	ZHANG.XL
 */
public class HiSqlClientImpl implements HiSqlClient {
	private DbAdapter dbAdapter;
	private ConnectionManager conMng;
	private Map<String, String> sqlCache;
	private boolean printLog = false;
	
	public HiSqlClientImpl(DbVersion dbVersion, ConnectionManager conMng) throws SQLException {
		createDbAdapter(dbVersion);
		this.conMng = conMng;
		this.sqlCache = new HashMap<String, String>();
	}
	
	public void setPrintLog(boolean _printLog) {
		this.printLog = _printLog;
	}
	
	/** 
	 * Create a new instance of DbAdapter 
	 */
	private void createDbAdapter(DbVersion dbVersion) {
		if (dbVersion == DbVersion.Oracle) {
			this.dbAdapter = new OracleDbAdapter();
		} else if (dbVersion == DbVersion.Mysql) {
			this.dbAdapter = new MySqlDbAdapter();
		}
	}
	
	/**
	 * Do query with SQL
	 *
	 * @param   sql - the SQL clause that include ? as binded variable
	 * @param	args - binded variables
	 * @param	returnClz - the class type for return value
	 * @author  ZHANG.XL
	 */
	public <T> List<T> query(final String sql, final Object[] args, Class<T> returnClz,
			Integer startRow, Integer records) {
		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			con = conMng.getConnection();
			ps = getPreparedStatementForQuery(con, sql, args, startRow, records);
			rs = ps.executeQuery();
			List<T> returnList = new ArrayList<T>();
			ResultSetMetaData tsmd = rs.getMetaData();
			int columnCnt = tsmd.getColumnCount();
			boolean isJavaSystemClass = isJavaSysClass(returnClz);
			while (rs.next()) {
				if (isJavaSystemClass) {
					returnList.add(castObject(returnClz, rs.getObject(1)));
				} else {
					Map<String, Object> retmap = new HashMap<String, Object>(columnCnt);
					for (int i = 1; i <= columnCnt; i++) {
						retmap.put(tsmd.getColumnName(i).toLowerCase(), rs.getObject(i));
					}
					returnList.add(createObjectByMap(returnClz, retmap));
				}
			}
			return returnList;
		} catch (Exception e) {
			System.out.println(String.format("Sql: ", sql));
			throw new SqlRuntimeException(e);
		} finally {
			try {
				if (rs != null) rs.close();
				if (ps != null) ps.close();
			} catch (SQLException e) {
				throw new SqlRuntimeException(e);
			}
			conMng.releaseConnection(con);
		}
	}
	
	/**
	 * @author  ZHANG.XL
	 */
	private PreparedStatement getPreparedStatementForQuery(Connection con, final String sql, 
			final Object[] args, Integer startRow, Integer records) throws Exception {
		if (startRow == null || records == null) {
			return this.getPreparedStatement(con, sql, args);
		} else {
			String newSql = this.dbAdapter.getQuerySqlForPaging(sql);
			Object[] newArgs = null;
			if (args != null) {
				newArgs = Arrays.copyOf(args, args.length + 2);
				Object[] pageArgs = this.dbAdapter.getQueryArgsForPaging(startRow, records);
				newArgs[args.length] = pageArgs[0];
				newArgs[args.length + 1] = pageArgs[1];
			} else {
				newArgs = this.dbAdapter.getQueryArgsForPaging(startRow, records);
			}
			return this.getPreparedStatement(con, newSql, newArgs);
		}
	}
	
	/**
	 * @author  ZHANG.XL
	 */
	public <T> List<T> query(final String sql, final Object[] args, Class<T> returnClz) {
		return this.query(sql, args, returnClz, null, null);
	}
	
	/**
	 * @author  ZHANG.XL
	 */
	public <T> T queryForObject(final String sql, final Object[] args, Class<T> clazz) {
		List<T> retList = query(sql, args, clazz, null, null);
		if (retList != null && retList.size() > 0) {
			return retList.get(0);
		} else {
			return null;
		}
	}
	
	/**
	 * @author  ZHANG.XL
	 */
	private <T> T createObjectByMap(
			Class<T> entityClz, Map<String, Object> retmap) throws Exception {
		if (retmap == null || retmap.isEmpty() || isJavaSysClass(entityClz)) {
			return null;
		}
		
		T entity = entityClz.newInstance();
		Field[] fields = entityClz.getFields();
		for (Field field : fields) {
			if (!retmap.containsKey(field.getName())) continue;
			Class<?> fieldType = field.getType();
			if (fieldType.isArray()) continue;
			
			Object fieldVal = retmap.get(field.getName());
			if ((fieldVal instanceof Number) && (fieldType != Boolean.class)) {
				field.set(entity, this.castNumber(fieldType, fieldVal));
			} else if (fieldType == java.util.Date.class) {
				if (fieldVal instanceof Timestamp) {
					java.util.Date date = new java.util.Date(((Timestamp) fieldVal).getTime());
					field.set(entity, date);
				} else if (fieldVal instanceof String) {
					java.util.Date date = 
							new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(fieldVal.toString());
					field.set(entity, date);
				}
			} else if (fieldType == Boolean.class) {
				if (fieldVal instanceof Number) {
					field.set(entity, ((Number) fieldVal).intValue() == 0 ? false : true);
				}
			} else {
				field.set(entity, fieldVal);
			}
		}
		
		return entity;
	}

	/**
	 * 对象值转型（将val转型为targetClz）
	 * 
	 * @param	targetClz - 转型的目标类型
	 * @param	val - 需要转型的对象值
	 * @author  ZHANG.XL
	 */
	@SuppressWarnings("unchecked")
	private <T> T castObject(Class<T> targetClz, Object val) {
		if (targetClz == String.class) {
			return (T) ((val != null) ? val.toString() : null); 
		} else if (val instanceof Number) {
			return castNumber(targetClz, val);
		} else if (val instanceof Timestamp) {
			java.util.Date date = new java.util.Date(((Timestamp) val).getTime());
			return (T) date;
		} else {
			return (T) val;
		}
	}
	
	/**
	 * @author ZHANG.XL
	 */
	@SuppressWarnings("unchecked")
	private <T> T castNumber(Class<T> numType, Object val) {
		double dnum = 0;
		if (val instanceof Number) {
			dnum = ((Number) val).doubleValue();
		} else if (val instanceof BigDecimal) {
			dnum = ((BigDecimal) val).doubleValue();
		} else {
			return null;
		}
		
		if (numType == Long.class || numType == Long.TYPE) {
			return (T) new Long((long) dnum);
		} else if (numType == Integer.class || numType == Integer.TYPE) {
			return (T) new Integer((int) dnum);
		} else if (numType == Float.class || numType == Float.TYPE) {
			return (T) new Float(dnum);
		} else if (numType == Double.class) {
			return (T) new Double(dnum);
		} else if (numType == Short.class || numType == Short.TYPE) {
			return (T) new Short((short) dnum);
		} else if (numType == Byte.class) {
			return (T) new Byte((byte) dnum);
		}
		return null;
	}
	
	/**
	 * Execute sql for insert/update/delete operation
	 *
	 * @param   sql - SQL for insert, update or delete
	 * @return  affected record count
	 * @author  ZHANG.XL
	 */
	public int execute(final String sql, final Object[] args) {
		Connection con = null;
		try {
			con = conMng.getConnection();
			PreparedStatement ps = getPreparedStatement(con, sql, args);
			int cnt = ps.executeUpdate();
			ps.close();
			return cnt;
		} catch (Exception e) {
			System.out.println(sql);
			throw new SqlRuntimeException(e);
		} finally {
			conMng.releaseConnection(con);
		}
	}
	
	/**
	 * @author  ZHANG.XL
	 */
	private PreparedStatement getPreparedStatement(Connection con, 
			final String sql, final Object[] args) throws SQLException {
		PreparedStatement ps = con.prepareStatement(sql);
		if (args != null) {
			for (int i = 1; i <= args.length; i++) {
				ps.setObject(i, dbAdapter.convertArg(args[i - 1]));
			}
		}
		if (this.printLog) {
			printLog(sql, args);
		}
		return ps;
	}
	
	/**
	 * @author  ZHANG.XL
	 */
	private void printLog(String sql, Object[] args) {
		System.out.println(String.format("SQL Clause: %s", sql));
		System.out.print("SQL Parameters: [");
		if (args != null) {
			StringBuilder paraStr = new StringBuilder();
			for (Object obj : args) {
				if (obj != null) {
					if (obj instanceof Number)
						paraStr.append(obj.toString());
					else if (obj instanceof Boolean)
						paraStr.append(((Boolean) obj).booleanValue() ? 1 : 0);
					else
						paraStr.append("'").append(obj.toString()).append("'");
				} else {
					paraStr.append("null");
				}
				paraStr.append(",");
			}
			if (paraStr.length() > 0) {
				paraStr.setLength(paraStr.length() - 1);
			}
			System.out.print(paraStr.toString());
		}
		System.out.println("]");
	}
	
	/**
	 * 获取连续num个问号（SQL语句中用）
	 * 
	 * 例如：getQuestionMarks(3) => "?,?,?"
	 *
	 * @param   问号的个数
	 * @throws  none
	 * @author  ZHANG.XL
	 * @date	2014-3-18
	 */
	public String getQuestionMarks(int num) {
		if (num < 1) return null;
		char[] chs = new char[num * 2];
		for (int i = 0; i < chs.length; i++) {
			chs[i] = '?';
			chs[++i] = ',';
		}
		return new String(chs, 0, num * 2 - 1);
	}
	
	/**
	 * Query one db entity by key value
	 * 
	 * @throws	SqlRuntimeException
	 * @author  ZHANG.XL
	 */
	public <T extends DbEntity> T get(Class<T> entityClz, Number keyValue)  {
		try {
			if (keyValue == null) {
				throw new Exception("Key value cannot be empty.");
			}
			final String tblName = AbstractDbEntity.getTableName(entityClz);
			final String keyName = AbstractDbEntity.getKeyName(entityClz);
			String sql = String.format("SELECT * FROM %s WHERE %s=?", 
				tblName, keyName);
			return this.queryForObject(sql, new Object[]{keyValue}, entityClz);
		} catch (Exception e) {
			throw new SqlRuntimeException(e);
		}
	}
	
	/**
	 * Query multiple db entites by key values
	 * 
	 * @throws	SqlRuntimeException
	 * @author  ZHANG.XL
	 */
	public <T extends DbEntity> List<T> get(Class<T> entityClz, 
			List<? extends Number> keyValues)  {
		try {
			if (keyValues == null || keyValues.size() == 0) {
				throw new Exception("Key values cannot be empty.");
			}
			final String tblName = AbstractDbEntity.getTableName(entityClz);
			final String keyName = AbstractDbEntity.getKeyName(entityClz);
			String sql = String.format("SELECT * FROM %s WHERE %s IN (%s)", 
				tblName, keyName, getQuestionMarks(keyValues.size()));
			return this.query(sql, keyValues.toArray(), entityClz);
		} catch (Exception e) {
			throw new SqlRuntimeException(e);
		}
	}
	
	/**
	 * Query multiple db entites by where conditions
	 * 
	 * @param	entityClz - entity class
	 * @param	whereArgMap - the arguments for where conditions
	 * @throws	SqlRuntimeException
	 * @author  ZHANG.XL
	 */
	public <T extends DbEntity> List<T> get(Class<T> entityClz, 
			final Map<String, Object> whereArgMap, Integer startRow, Integer records) {
		if (whereArgMap == null || whereArgMap.size() == 0) 
			return Collections.emptyList();
		
		try {
			final String tblName = AbstractDbEntity.getTableName(entityClz);
			Object[] args = new Object[whereArgMap.size()];
			StringBuilder sql = new StringBuilder();
			sql.append("SELECT * FROM ").append(tblName).append(" WHERE ");
			Iterator<Entry<String, Object>> itr = whereArgMap.entrySet().iterator();
			int i = 0;
			final String andOpt = " AND ";
			while (itr.hasNext()) {
				Entry<String, Object> entry = itr.next();
				sql.append(entry.getKey()).append("=?").append(andOpt);
				args[i++] = entry.getValue();
			}
			sql.setLength(sql.length() - andOpt.length());
			return this.query(sql.toString(), args, entityClz, startRow, records);
		} catch (Exception e) {
			throw new SqlRuntimeException(e);
		}
	}
	
	/**
	 * insert one db entity with all db fields
	 * 
	 * @return 	affected record count
	 * @throws	SqlRuntimeException
	 * @author	ZHANG.XL
	 */
	public int insert(final DbEntity entity) {
		if (entity != null) {
			try {
				return this.execute(getInsertSql(entity.getClass()), getInsertArgs(entity));
			} catch (Exception e) {
				throw new SqlRuntimeException(e);
			}
		} else {
			throw new IllegalArgumentException("db entity cannot be null");
		}
	}
	
	/**
	 * @author  ZHANG.XL
	 */
	private String getInsertSql(Class<? extends DbEntity> entityClz) {
		final String sqlKey = entityClz.getName() + "_insert";
		if (this.sqlCache.containsKey(sqlKey)) {
			return sqlCache.get(sqlKey);
		}
		
		StringBuilder fieldNameSql = new StringBuilder();
		String dbFieldName = null;
		int dbFieldCnt = 0;
		Field[] fields = entityClz.getFields();
		for (Field field : fields) {
			dbFieldName = getDbFieldName(field);
			if (dbFieldName != null) {
				fieldNameSql.append(dbFieldName).append(",");
				dbFieldCnt++;
			}
		}
		if (fieldNameSql.length() > 0) {
			final String tblName = AbstractDbEntity.getTableName(entityClz);
			fieldNameSql.setLength(fieldNameSql.length() - 1);
			String sql = String.format("INSERT INTO %s (%s) VALUES (%s)", 
					tblName, fieldNameSql.toString(), this.getQuestionMarks(dbFieldCnt));
			sqlCache.put(sqlKey, sql);
			return sql;
		} else {
			throw new SqlRuntimeException("not found db field in " + entityClz.getName());
		}
	}
	
	/**
	 * @author  ZHANG.XL
	 */
	private Object[] getInsertArgs(DbEntity entity) throws Exception {
		Field[] fields = entity.getClass().getFields();
		List<Object> argList = new ArrayList<Object>(fields.length);
		for (Field field : fields) {
			if (field.getAnnotation(NotDbField.class) == null) {
				argList.add(field.get(entity));
			}
		}
		return argList.toArray();
	}
	
	/**
	 * Get db field name by the specified"field". 
	 * If the "field" is not db field, then return null. 
	 * 
	 * @author  ZHANG.XL
	 */
	private String getDbFieldName(final Field field) {
		NotDbField notDbField = field.getAnnotation(NotDbField.class);
		if (notDbField != null) {
			return null;
		}
		DbField dbField = field.getAnnotation(DbField.class);
		if (dbField != null) {
			return (dbField.name().isEmpty()) ? field.getName() : dbField.name();
		} else {
			return field.getName();
		}
	}
	
	/**
	 * Update one db entity with all db fields
	 * 
	 * @return 	affected record count
	 * @throws	SqlRuntimeException
	 * @author	ZHANG.XL
	 */
	public int update(final DbEntity entity) {
		if (entity != null) {
			try {
				return this.execute(getUpdateSql(entity.getClass()), getUpdateArgs(entity));
			} catch (Exception e) {
				throw new SqlRuntimeException(e);
			}
		} else {
			throw new IllegalArgumentException("db entity cannot be null");
		}
	}
	
	/**
	 * @author  ZHANG.XL
	 */
	private String getUpdateSql(Class<? extends DbEntity> entityClz) {
		final String sqlKey = entityClz.getName() + "_update";
		if (this.sqlCache.containsKey(sqlKey)) {
			return sqlCache.get(sqlKey);
		}
		
		StringBuilder updateSql = new StringBuilder();
		String dbFieldName = null;
		Field[] fields = entityClz.getFields();
		for (Field field : fields) {
			dbFieldName = getDbFieldName(field);
			if (dbFieldName != null) {
				updateSql.append(dbFieldName).append("=?,");
			}
		}
		if (updateSql.length() > 0) {
			final String tblName = AbstractDbEntity.getTableName(entityClz);
			final String keyName = AbstractDbEntity.getKeyName(entityClz);
			updateSql.setLength(updateSql.length() - 1);
			String sql = String.format("UPDATE %s SET %s WHERE %s=?", 
					tblName, updateSql.toString(), keyName);
			sqlCache.put(sqlKey, sql);
			return sql;
		} else {
			throw new SqlRuntimeException("not found db field in " + entityClz.getName());
		}
	}
	
	/**
	 * @author  ZHANG.XL
	 */
	private Object[] getUpdateArgs(DbEntity entity) throws Exception {
		Field[] fields = entity.getClass().getFields();
		List<Object> argList = new ArrayList<Object>(fields.length);
		for (Field field : fields) {
			if (field.getAnnotation(NotDbField.class) == null) {
				argList.add(field.get(entity));
			}
		}
		argList.add(entity.getKeyValue());
		return argList.toArray();
	}
	
	/**
	 * Update one db entity with specified db fields
	 * 
	 * @return	affected record count
	 * @author  ZHANG.XL
	 */
	public int update(final DbEntity entity, String[] updateFieldNames) {
		if (entity == null) {
			throw new IllegalArgumentException("db entity cannot be null");
		}
		if (updateFieldNames == null || updateFieldNames.length == 0) {
			throw new IllegalArgumentException("updateFieldNames cannot be empty");
		}
		
		List<Object> argList = new ArrayList<Object>(updateFieldNames.length + 1);
		try {
			Class<? extends DbEntity> entityClz = entity.getClass();
			final String keyName = entity.getKeyName();
			StringBuilder updateSql = new StringBuilder();
			for (String fieldName : updateFieldNames) {
				if (!fieldName.equalsIgnoreCase(keyName)) {
					updateSql.append(fieldName).append("=?,");
					Object arg = entityClz.getField(fieldName).get(entity);
					argList.add(arg);
				}
			}
			if (updateSql.length() > 0) {
				argList.add(entity.getKeyValue());
				updateSql.setLength(updateSql.length() - 1);
				String sql = String.format("UPDATE %s SET %s WHERE %s=?", 
						entity.getTableName(), updateSql.toString(), keyName);
				return this.execute(sql.toString(), argList.toArray());
			} else {
				throw new Exception("not found the updated field of this entity " + entityClz);
			}
		} catch (Exception e) {
			throw new SqlRuntimeException(e);
		}
	}
	
	/**
	 * Update one db entity with specified field-value map
	 * 
	 * @return	affected record count
	 * @author  ZHANG.XL
	 */
	public int update(Class<? extends DbEntity> entityClz, final Number keyValue, 
			final Map<String, Object> fieldValueMap) {
		if (entityClz == null || fieldValueMap == null || fieldValueMap.size() == 0) {
			return 0;
		}
		if (keyValue == null) {
			throw new IllegalArgumentException("key value cannot be null when updating "
					+ entityClz.getName());
		}
		
		StringBuilder updateSql = new StringBuilder();
		int i = 0;
		Object[] args = new Object[fieldValueMap.size() + 1];
		Iterator<Entry<String, Object>> itr = fieldValueMap.entrySet().iterator();
		while (itr.hasNext()) {
			Entry<String, Object> entry = itr.next();
			updateSql.append(entry.getKey()).append("=?,");
			args[i++] = entry.getValue();
		}
		final String tblName = AbstractDbEntity.getTableName(entityClz);
		final String keyName = AbstractDbEntity.getKeyName(entityClz);
		updateSql.setLength(updateSql.length() - 1);
		args[i] = keyValue;
		String sql = String.format("UPDATE %s SET %s WHERE %s=?", 
				tblName, updateSql.toString(), keyName);
		return this.execute(sql, args);
	}
	
	/**
	 * Delete db entity by one key value
	 * 
	 * @author  ZHANG.XL
	 */
	public int delete(Class<? extends DbEntity> entityClz, Number keyValue) {
		return this.execute(getDeleteSql(entityClz), new Object[]{keyValue});
	}
	
	/**
	 * @author  ZHANG.XL
	 */
	private String getDeleteSql(Class<? extends DbEntity> entityClz) {
		final String sqlKey = entityClz.getName() + "_delete";
		if (this.sqlCache.containsKey(sqlKey)) {
			return this.sqlCache.get(sqlKey);
		}
		
		final String tblName = AbstractDbEntity.getTableName(entityClz);
		final String keyName = AbstractDbEntity.getKeyName(entityClz);
		String sql = String.format("DELETE FROM %s WHERE %s=?", tblName, keyName);
		this.sqlCache.put(sqlKey, sql);
		return sql;
	}
	
	/**
	 * Delete db entities by multiple key values
	 * 
	 * @author  ZHANG.XL
	 */
	public int delete(Class<? extends DbEntity> entityClz, List<? extends Number> keyValueList) {
		if (keyValueList == null || keyValueList.size() == 0) {
			return 0;
		}
		
		final String tblName = AbstractDbEntity.getTableName(entityClz);
		final String keyName = AbstractDbEntity.getKeyName(entityClz);
		String sql = String.format("DELETE FROM %s WHERE %s IN (%s)", 
			tblName, keyName, this.getQuestionMarks(keyValueList.size()));
		return this.execute(sql, keyValueList.toArray());
	}
	
	/**
	 * @author  ZHANG.XL
	 */
	public static boolean isJavaSysClass(Class<?> clz) {  
	    return (clz != null && clz.getClassLoader() == null);  
	}

	/**
	 * @author  ZHANG.XL
	 */
	@Override
	public String genInsertSqlWithValues(DbEntity entity) {
		StringBuilder fieldNameSql = new StringBuilder();
		StringBuilder fieldValueSql = new StringBuilder();
		String dbFieldName = null;
		Class<? extends DbEntity> entityClz = entity.getClass();
		Field[] fields = entityClz.getFields();
		try {
			for (Field field : fields) {
				dbFieldName = getDbFieldName(field);
				if (dbFieldName != null) {
					fieldNameSql.append(dbFieldName).append(",");
					fieldValueSql.append(getColumnValueInSql(field.get(entity))).append(",");
				}
			}
		} catch (Exception e) {
			throw new SqlRuntimeException(e.getMessage());
		}
		if (fieldNameSql.length() > 0) {
			final String tblName = AbstractDbEntity.getTableName(entityClz);
			fieldNameSql.setLength(fieldNameSql.length() - 1);
			fieldValueSql.setLength(fieldValueSql.length() - 1);
			String sql = String.format("INSERT INTO %s (%s) VALUES (%s)", 
					tblName, fieldNameSql.toString(), fieldValueSql.toString());
			return sql;
		} else {
			throw new SqlRuntimeException("not found db field in " + entityClz.getName());
		}
	}
	
	/**
	 * @author  ZHANG.XL
	 */
	@Override
	public String genInsertSqlWithValues(List<? extends DbEntity> entityList, String separator) {
		if (entityList == null || entityList.size() == 0) 
			return null;
		
		StringBuilder fieldNameSql = new StringBuilder();
		String dbFieldName = null;
		Class<? extends DbEntity> entityClz = entityList.get(0).getClass();
		Field[] fields = entityClz.getFields();
		List<Integer> dbFieldIdxList = new ArrayList<Integer>(fields.length);
		int i = 0;
		try {
			for (Field field : fields) {
				dbFieldName = getDbFieldName(field);
				if (dbFieldName != null) {
					fieldNameSql.append(dbFieldName).append(",");
					dbFieldIdxList.add(i);
				}
				i++;
			}
			if (fieldNameSql.length() > 0) {
				StringBuilder fieldValueSql = new StringBuilder();
				final String tblName = AbstractDbEntity.getTableName(entityClz);
				fieldNameSql.setLength(fieldNameSql.length() - 1);
				String baseInsertSql = String.format("INSERT INTO %s (%s) ", 
						tblName, fieldNameSql.toString());
				StringBuilder sql = new StringBuilder();
				for (DbEntity entity : entityList) {
					fieldValueSql.setLength(0);
					for (Integer fieldIdx : dbFieldIdxList) {
						fieldValueSql.append(getColumnValueInSql(fields[fieldIdx].get(entity)));
						fieldValueSql.append(",");
					}
					fieldValueSql.setLength(fieldValueSql.length() - 1);
					sql.append(baseInsertSql).append(String.format(" VALUES (%s)", fieldValueSql));
					sql.append(separator);
				}
				return sql.toString();
			} else {
				throw new SqlRuntimeException("not found db field in " + entityClz.getName());
			}
		} catch (Exception e) {
			throw new SqlRuntimeException(e.getMessage());
		}
	}
	
	/**
	 * @author  ZHANG.XL
	 */
	private String getColumnValueInSql(Object fieldVal) {
		if (fieldVal instanceof String || fieldVal instanceof Character) {
			return String.format("'%s'", escapeSql(fieldVal.toString()));
		} else if (fieldVal instanceof Number) {
			return fieldVal.toString();
		} else if (fieldVal instanceof Boolean) {
			boolean boolVal = ((Boolean) fieldVal).booleanValue();
			return boolVal ? "1" : "0";
		} else if (fieldVal instanceof java.util.Date) {
			return dbAdapter.getDateStrForSql((java.util.Date) fieldVal);
		} else if (fieldVal == null) {
			return "null";
		}
		return fieldVal.toString();
	}
	
	/**
	 * @author  ZHANG.XL
	 */
	private static String escapeSql(String str) {
		return StringEscapeUtils.escapeSql(str);
	}
}
