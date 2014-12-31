/*
 * Copyright (c) 2014~2099, Zhang.XiaLiang (ZHANG.XL) All rights reserved.
 */
package org.hisql.exception;

/**
 * 空表名异常
 *
 * @author	ZHANG.XL
 */
public class EmptyTableNameException extends RuntimeException {
	private static final long serialVersionUID = 2737532972164342913L;

	public EmptyTableNameException(Throwable e) {
		super(e);
	}
	
	public EmptyTableNameException(String msg) {
		super(msg);
	}
}
