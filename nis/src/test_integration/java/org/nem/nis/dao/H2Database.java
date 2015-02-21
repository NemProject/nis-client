package org.nem.nis.dao;

import org.nem.core.utils.ExceptionUtils;

import java.sql.*;

public class H2Database implements AutoCloseable {
	private static final String JDBC_DRIVER = "org.h2.Driver";
	private final Connection conn;

	public H2Database() {
		this.conn = ExceptionUtils.propagate(() -> {
			Class.forName(JDBC_DRIVER);
			return DriverManager.getConnection(getDbPath(), "", "");
		});
	}

	private String getDbPath() {
		return String.format("jdbc:h2:%s/nem/nis/data/test", System.getProperty("user.home"));
	}

	@Override
	public void close() throws Exception {
		ExceptionUtils.propagateVoid(() -> {
			if (null != this.conn) {
				this.conn.close();
			}
		});
	}

	public ResultSet executeQuery(final String sql) {
		try {
			Statement stmt = this.conn.createStatement();
			return stmt.executeQuery(sql);
		} catch (SQLException e) {
			throw new RuntimeException("h2 query failed.");
		}
	}
}
