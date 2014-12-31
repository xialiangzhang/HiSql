/*
 * Copyright (c) 2014~2099, Zhang.XiaLiang (ZHANG.XL) All rights reserved.
 */
package org.hisql;

/**
 * Database version
 *
 * @author	ZHANG.XL       
 */
public enum DbVersion {
	Oracle(1), Mysql(2);
	
	private final int value;

	DbVersion(int value) {
        this.value = value;
    }
    
    public int getValue() {
        return value;
    }
}
