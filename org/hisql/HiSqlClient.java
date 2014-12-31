/*
 * Copyright (c) 2014~2099, Zhang.XiaLiang (ZHANG.XL) All rights reserved.
 */
package org.hisql;

import java.util.List;
import java.util.Map;

/**
 * HiSQL is a simple and efficient ORM utility for operating DB SQL
 *
 * @author	ZHANG.XL
 */
public interface HiSqlClient {
	/**
	 * the flag for printing log, default false
	 * 
	 * @author  ZHANG.XL
	 */
	void setPrintLog(boolean printLog);
	
	/**
	 * Do query with SQL
	 *
	 * @param   sql - the SQL clause that include ? as binded variable
	 * @param	args - binded variables
	 * @param	returnClz - the class type for return value
	 * @author  ZHANG.XL
	 */
	<T> List<T> query(final String sql, final Object[] args, Class<T> returnClz,
			Integer startRow, Integer records);
	/**
	 * @author  ZHANG.XL
	 */
	<T> List<T> query(final String sql, final Object[] args, Class<T> returnClz);
	
	/**
	 * @author  ZHANG.XL
	 */
	<T> T queryForObject(final String sql, final Object[] args, Class<T> clazz);
	
	/**
	 * Execute sql for insert/update/delete operation
	 *
	 * @param   sql - SQL for insert, update or delete
	 * @return  affected record count
	 * @author  ZHANG.XL
	 */
	int execute(final String sql, final Object[] args);
	
	/**
	 * 获取连续num个问号（SQL语句中用）
	 * 
	 * 例如：getQuestionMarks(3) => "?,?,?"
	 *
	 * @param   问号的个数
	 * @throws  none
	 * @author  ZHANG.XL
	 */
	String getQuestionMarks(int num);
	
	/**
	 * Query one db entity by key value
	 * 
	 * @throws	SqlRuntimeException
	 * @author  ZHANG.XL
	 */
	<T extends DbEntity> T get(Class<T> entityClz, Number keyValue);
	
	/**
	 * Query multiple db entites by key values
	 * 
	 * @throws	SqlRuntimeException
	 * @author  ZHANG.XL
	 */
	<T extends DbEntity> List<T> get(Class<T> entityClz, 
			List<? extends Number> keyValues);
	
	/**
	 * Query multiple db entites by where conditions
	 * 
	 * @param	entityClz - entity class
	 * @param	whereArgMap - the arguments for where conditions
	 * @throws	SqlRuntimeException
	 * @author  ZHANG.XL
	 */
	<T extends DbEntity> List<T> get(Class<T> entityClz, 
			final Map<String, Object> whereArgMap, Integer startRow, Integer records);
	
	/**
	 * insert one db entity with all db fields
	 * 
	 * @return 	affected record count
	 * @throws	SqlRuntimeException
	 * @author	ZHANG.XL
	 */
	int insert(final DbEntity entity);

	/**
	 * Update one db entity with all db fields
	 * 
	 * @return 	affected record count
	 * @throws	SqlRuntimeException
	 * @author	ZHANG.XL
	 */
	int update(final DbEntity entity);
	
	/**
	 * Update one db entity with specified db fields
	 * 
	 * @return	affected record count
	 * @author  ZHANG.XL
	 */
	int update(final DbEntity entity, String[] updateFieldNames);
	
	/**
	 * Update one db entity with specified field-value map
	 * 
	 * @return	affected record count
	 * @author  ZHANG.XL
	 */
	int update(Class<? extends DbEntity> entityClz, final Number keyValue, 
			final Map<String, Object> fieldValueMap);
	
	/**
	 * Delete db entity by one key value
	 * 
	 * @author  ZHANG.XL
	 */
	int delete(Class<? extends DbEntity> entityClz, Number keyValue);
	
	/**
	 * Delete db entities by multiple key values
	 * 
	 * @author  ZHANG.XL
	 */
	int delete(Class<? extends DbEntity> entityClz, List<? extends Number> keyValueList);
	
	/**
	 * @author  ZHANG.XL
	 */
	String genInsertSqlWithValues(DbEntity entity);
	
	/**
	 * @author  ZHANG.XL
	 */
	String genInsertSqlWithValues(List<? extends DbEntity> entityList, String separator);
}
