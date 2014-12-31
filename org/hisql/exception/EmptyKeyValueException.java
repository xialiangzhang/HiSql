/*
 * Copyright (c) 2014~2099, Zhang.XiaLiang (ZHANG.XL) All rights reserved.
 */
package org.hisql.exception;

/**
 * 空主键值异常
 *
 * @author	ZHANG.XL
 */
public class EmptyKeyValueException extends RuntimeException {
	private static final long serialVersionUID = 7148370762313660196L;

	public EmptyKeyValueException(Throwable e) {
		super(e);
	}
	
	public EmptyKeyValueException(String msg) {
		super(msg);
	}
}
