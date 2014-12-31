/*
 * Copyright (c) 2014~2099, Zhang.XiaLiang (ZHANG.XL) All rights reserved.
 */
package org.hisql;

import java.lang.reflect.Field;

import org.hisql.annotation.DbField;
import org.hisql.annotation.DbTable;
import org.hisql.exception.EmptyKeyValueException;
import org.hisql.exception.EmptyTableNameException;


/**
 * <pre>
 * The abstract class for database entity
 * 
 * The inherited classes shoule append two types of annotation:
 * 1) Annotation @{@link DbTable} for class
 * 2) Annotation @{@link DbField} or @{@link org.hisql.annotation.NotDbField} for field
 * </pre>
 * 
 * @author	ZHANG.XL
 */
public abstract class AbstractDbEntity implements DbEntity {
	
	/**
	 * Get the table name of the DbEntity class
	 *
	 * @param   entityClz - the class type for the entity
	 * @return  table name
	 * @throws	EmptyTableNameException
	 * @author  ZHANG.XL
	 */
	public static <T extends DbEntity> String getTableName(Class<T> entityClz) 
			throws EmptyTableNameException {
		DbTable tblAnnoation = entityClz.getAnnotation(DbTable.class);
		if (tblAnnoation == null) {
			throw new RuntimeException(entityClz.getSimpleName() 
					+  " class needs adding annotation @DbTable");
		}
		String tblName = tblAnnoation.tableName();
		if (tblName == null || tblName.trim().length() == 0) {
			throw new EmptyTableNameException(
				"Not found table name: the annotation @DbTable needs setting \"tableName\"" 
				+ " for " + entityClz.getSimpleName());
		}
		return tblName.trim();
	}
	
	/**
	 * Get the table name of the DbEntity object
	 *
	 * @return  table name
	 * @throws	EmptyTableNameException
	 * @author  ZHANG.XL
	 */
	@Override
	public String getTableName() throws EmptyTableNameException {
		return getTableName(this.getClass());
	}
	
	/**
	 * Get the primary key name of the DbEntity class
	 *
	 * @param   entityClz - the class type for the entity
	 * @return  primary key name
	 * @author  ZHANG.XL
	 */
	public static <T extends DbEntity> String getKeyName(Class<T> entityClz) {
		DbTable tblAnnoation = entityClz.getAnnotation(DbTable.class);
		if (tblAnnoation == null) {
			throw new RuntimeException(entityClz.getSimpleName() 
					+  " class needs adding annotation @DbTable");
		}
		String keyName = tblAnnoation.keyName();
		if (keyName == null || keyName.trim().length() == 0) {
			throw new RuntimeException("The annotation @DbTable needs setting \"keyName\""
				+ "for " + entityClz.getSimpleName());
		}
		return keyName.trim();
	}
	
	/**
	 * Get the primary key name of the DbEntity object
	 *
	 * @author  ZHANG.XL
	 */
	@Override
	public String getKeyName() {
		return getKeyName(this.getClass());
	}
	
	/**
	 * Get the primary key value of the DbEntity object
	 *
	 * @return  the primary key value
	 * @throws  RuntimeException
	 * @author  ZHANG.XL
	 */
	@SuppressWarnings("unchecked")
	public static <T extends Number> T getKeyValue(final DbEntity entity) 
			throws EmptyKeyValueException {
		try {
			Field keyField = entity.getClass().getField(getKeyName(entity.getClass()));
			Object keyValue = keyField.get(entity);
			
			if (keyValue instanceof Integer) {
				return (T) new Integer(((Integer) keyValue).intValue());
			} else if (keyValue instanceof Long) {
				return (T) new Long(((Long) keyValue).longValue());
			} else if (keyValue instanceof Short) {
				return (T) new Short(((Short) keyValue).shortValue());
			} else if (keyValue == null) {
				throw new EmptyKeyValueException("The key value cannot be empty for the object of "
					+ entity.getClass().getSimpleName());
			} else { 
				throw new Exception("Do not support the key type: " 
						+ keyValue.getClass().getSimpleName());
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * Get the primary key value of the DbEntity object
	 *
	 * @return  the primary key value
	 * @throws  RuntimeException
	 * @author  ZHANG.XL
	 */
	@Override
	public <T extends Number> T getKeyValue() throws EmptyKeyValueException {
		return getKeyValue(this);
	}
	
	/**
	 * Set key value for the db entity
	 *
	 * @throws  RuntimeException
	 * @author  ZHANG.XL
	 */
	public static void setKeyValue(DbEntity entity, Number keyValue) {
		if (entity == null) return;
		if (keyValue == null) {
			throw new EmptyKeyValueException("The key value of " + entity.getClass()
					+ "cannot be empty");
		}
		try {
			Class<? extends DbEntity> entityClz = entity.getClass();
			Field keyField = entityClz.getField(getKeyName(entityClz));
			keyField.set(entity, keyValue);
		} catch (Exception e) {
			throw new RuntimeException(e); 
		}
	}
	
	/**
	 * Set key value for this entity
	 *
	 * @throws  RuntimeException
	 * @author  ZHANG.XL
	 */
	@Override
	public void setKeyValue(Number keyValue) {
		setKeyValue(this, keyValue);
	}
}
