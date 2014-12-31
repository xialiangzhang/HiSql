/*
 * Copyright (c) 2014~2099, Zhang.XiaLiang (ZHANG.XL) All rights reserved.
 */
package org.hisql;

import org.hisql.exception.EmptyKeyNameException;
import org.hisql.exception.EmptyKeyValueException;
import org.hisql.exception.EmptyTableNameException;

/**
 * The interface for database entity
 * 
 * @author	ZHANG.XL
 */
public interface DbEntity {
	public String getTableName() throws EmptyTableNameException;
	public String getKeyName() throws EmptyKeyNameException;
	public <T extends Number> T getKeyValue() throws EmptyKeyValueException;
	public void setKeyValue(Number keyValue) throws EmptyKeyValueException;
}
