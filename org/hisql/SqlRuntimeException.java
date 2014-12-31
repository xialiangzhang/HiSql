/*
 * Copyright 2014-2099 the original author or authors.
 * 
 */
package org.hisql;

/**
 *
 * @author    ZHANG.XL
 * @version   1.0, 2014年8月22日         
 */
public class SqlRuntimeException extends RuntimeException {
	private static final long serialVersionUID = -4719362690949159378L;

	public SqlRuntimeException(Throwable e) {
		super(e);
	}
	
	public SqlRuntimeException(String msg) {
		super(msg);
	}
}
