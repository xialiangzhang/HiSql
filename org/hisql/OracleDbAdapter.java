/*
 * Copyright 2014-2099 the original author or authors.
 * 
 */
package org.hisql;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * The db adapter for Oracle db
 *
 * @author    ZHANG.XL         
 */
public class OracleDbAdapter implements DbAdapter {
	/**
	 * @author  ZHANG.XL
	 */
	@Override
	public String getQuerySqlForPaging(String querySql) {
		StringBuilder sql = new StringBuilder();
		sql.append("SELECT * FROM ");
		sql.append(String.format("(SELECT A.*, ROWNUM RN FROM (%s) A WHERE ROWNUM<?) ", querySql));
		sql.append("WHERE RN>=?");
		
		return sql.toString();
	}
	
	/**
	 * @author  ZHANG.XL
	 */
	@Override
	public Integer[] getQueryArgsForPaging(int startRow, int records) {
		return new Integer[] {startRow + records, startRow};
	}
	
	/**
	 * @author  ZHANG.XL
	 */
	@Override
	public Object convertArg(Object arg) {
		if (arg instanceof java.util.Date) {
			return new Timestamp(((java.util.Date)arg).getTime());
		} else {
			return arg;
		}
	}
	
	/**
	 * @author  ZHANG.XL
	 */
	@Override
	public String getDateStrForSql(Date date) {
		String datestr = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(date);
		return String.format("to_date('%s','yyyy-mm-dd hh24:mi:ss')", datestr);
	}
}
