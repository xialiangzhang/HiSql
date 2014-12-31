/*
 * Copyright 2014-2099 the original author or authors.
 * 
 */
package org.hisql;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * The db adapter for MySql db
 *
 * @author	ZHANG.XL
 */
public class MySqlDbAdapter implements DbAdapter {

	/**
	 * @author  ZHANG.XL
	 */
	@Override
	public String getQuerySqlForPaging(String querySql) {
		return querySql + " LIMIT ?, ?";
	}
	
	/**
	 * @author  ZHANG.XL
	 */
	@Override
	public Integer[] getQueryArgsForPaging(int startRow, int records) {
		return new Integer[] {startRow - 1, records};
	}

	/**
	 * @author  ZHANG.XL
	 */
	@Override
	public Object convertArg(Object arg) {
		return arg;
	}

	/**
	 * @author  ZHANG.XL
	 */
	@Override
	public String getDateStrForSql(Date date) {
		String datestr = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(date);
		return String.format("('%s'", datestr);
	}
}
