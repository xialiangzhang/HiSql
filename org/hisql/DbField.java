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
 * The annotation for database field
 *
 * @author	ZHANG.XL        
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD})
@Inherited
public @interface DbField {
	/** field name in this table */
	String name() default "";
	
	/** whethere the value of this field can be null, default true */
	boolean nullable() default true;
}
