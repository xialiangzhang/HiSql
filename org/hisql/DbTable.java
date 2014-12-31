/*
 * Copyright 2014-2099 the original author or authors.
 * 
 */
package org.hisql;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The annotation for database table
 *
 * @author	ZHANG.XL      
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
@Inherited
public @interface DbTable {
	/** table name in database */
	String tableName();
	
	/** primary key of this table */
	String keyName();
}
