/*
 * Copyright (c) 2014~2099, Zhang.XiaLiang (ZHANG.XL) All rights reserved.
 */
package org.hisql;

import java.sql.Connection;

/**
 * The interface of connection manangement
 *
 * @author	ZHANG.XL
 */
public interface ConnectionManager {
	Connection getConnection();
	void releaseConnection(Connection con);
}
