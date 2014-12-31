/*
 * Copyright (c) 2014~2099, Zhang.XiaLiang (ZHANG.XL) All rights reserved.
 */
package org.hisql.exception;

/**
 * 空主键名异常
 *
 * @author	ZHANG.XL
 */
public class EmptyKeyNameException extends RuntimeException {
	private static final long serialVersionUID = 4624523899148200066L;

	public EmptyKeyNameException(Throwable e) {
		super(e);
	}
	
	public EmptyKeyNameException(String msg) {
		super(msg);
	}
}
