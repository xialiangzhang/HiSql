/*
 * Copyright (c) 2014~2099, Zhang.XiaLiang (ZHANG.XL) All rights reserved.
 */
package org.hisql;

/**
 * The database adapter
 *
 * @author    ZHANG.XL        
 */
public interface DbAdapter {
	
	/**
	 * Get the new query sql with paging
	 *
	 * @param   querySql - original sql for query
	 * @author  ZHANG.XL
	 */
	String getQuerySqlForPaging(String querySql);
	
	Integer[] getQueryArgsForPaging(int startRow, int records);
	
	/**
	 * Convert argument value for PreparedStatement
	 * 
	 * @author  ZHANG.XL
	 */
	Object convertArg(Object arg);
	
	/** 获取SQL语句中的日期赋值字符串  */
	public String getDateStrForSql(java.util.Date date);
}
