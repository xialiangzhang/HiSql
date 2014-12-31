/*
 * Copyright (c) 2014~2099, Zhang.XiaLiang (ZHANG.XL) All rights reserved.
 */
package org.hisql.exception;

/**
 * @author    ZHANG.XL
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
