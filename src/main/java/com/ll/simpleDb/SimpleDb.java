package com.ll.simpleDb;

import lombok.Setter;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class SimpleDb {
    private final String url;
    private final String username;
    private final String password;

    @Setter
    private boolean devMode;

    // 스레드마다 별도의 커넥션을 사용한다.
    private final ThreadLocal<Connection> connectionHolder = new ThreadLocal<>();

    public SimpleDb(String host, String username, String password, String dbName) {
        this.url = "jdbc:mysql://%s:3306/%s?useUnicode=true&characterEncoding=utf8".formatted(host, dbName);
        this.username = username;
        this.password = password;
    }

    boolean isDevMode() {
        return devMode;
    }

    Connection getConnection() {
        try {
            Connection connection = connectionHolder.get();

            if (connection == null || connection.isClosed()) {
                connection = DriverManager.getConnection(url, username, password);
                connectionHolder.set(connection);
            }

            return connection;
        } catch (SQLException e) {
            throw new RuntimeException("DB 연결 실패: " + url, e);
        }
    }

    public int run(String sql, Object... params) {
        return genSql().append(sql, params).update();
    }

    public Sql genSql() {
        return new Sql(this);
    }

    public void startTransaction() {
        try {
            getConnection().setAutoCommit(false);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void commit() {
        try {
            Connection connection = getConnection();
            connection.commit();
            connection.setAutoCommit(true);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void rollback() {
        try {
            Connection connection = getConnection();
            connection.rollback();
            connection.setAutoCommit(true);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void close() {
        Connection connection = connectionHolder.get();
        connectionHolder.remove();

        if (connection == null) return;

        try {
            connection.close();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
