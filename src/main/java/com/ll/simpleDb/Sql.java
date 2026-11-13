package com.ll.simpleDb;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

public class Sql {
    private static final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    private final SimpleDb simpleDb;
    private final StringBuilder sqlBuilder = new StringBuilder();
    private final List<Object> params = new ArrayList<>();

    Sql(SimpleDb simpleDb) {
        this.simpleDb = simpleDb;
    }

    public Sql append(String sqlPart, Object... params) {
        if (!sqlBuilder.isEmpty()) sqlBuilder.append('\n');
        sqlBuilder.append(sqlPart);
        this.params.addAll(Arrays.asList(params));
        return this;
    }

    public Sql appendIn(String sqlPart, Object... params) {
        String placeholders = Arrays.stream(params)
                .map(p -> "?")
                .collect(Collectors.joining(", "));

        return append(sqlPart.replace("?", placeholders), params);
    }

    public long insert() {
        return execute(true, ps -> {
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                return keys.next() ? keys.getLong(1) : 0L;
            }
        });
    }

    public int update() {
        return execute(false, PreparedStatement::executeUpdate);
    }

    public int delete() {
        return update();
    }

    public List<Map<String, Object>> selectRows() {
        return execute(false, ps -> {
            try (ResultSet rs = ps.executeQuery()) {
                ResultSetMetaData meta = rs.getMetaData();
                int columnCount = meta.getColumnCount();
                List<Map<String, Object>> rows = new ArrayList<>();

                while (rs.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    for (int i = 1; i <= columnCount; i++) {
                        row.put(meta.getColumnLabel(i), rs.getObject(i));
                    }
                    rows.add(row);
                }

                return rows;
            }
        });
    }

    public Map<String, Object> selectRow() {
        List<Map<String, Object>> rows = selectRows();
        return rows.isEmpty() ? null : rows.getFirst();
    }

    public <T> List<T> selectRows(Class<T> cls) {
        return selectRows().stream()
                .map(row -> objectMapper.convertValue(row, cls))
                .toList();
    }

    public <T> T selectRow(Class<T> cls) {
        Map<String, Object> row = selectRow();
        return row == null ? null : objectMapper.convertValue(row, cls);
    }

    public LocalDateTime selectDatetime() {
        Object value = selectValue();
        if (value instanceof Timestamp timestamp) return timestamp.toLocalDateTime();
        return (LocalDateTime) value;
    }

    public Long selectLong() {
        return toLong(selectValue());
    }

    public List<Long> selectLongs() {
        return selectRows().stream()
                .map(row -> toLong(row.values().iterator().next()))
                .toList();
    }

    public String selectString() {
        Object value = selectValue();
        return value == null ? null : value.toString();
    }

    public Boolean selectBoolean() {
        Object value = selectValue();

        return switch (value) {
            case null -> null;
            case Boolean b -> b;
            case Number n -> n.longValue() != 0;
            default -> !value.toString().equals("0");
        };
    }

    private Object selectValue() {
        Map<String, Object> row = selectRow();
        return row == null ? null : row.values().iterator().next();
    }

    private static Long toLong(Object value) {
        if (value == null) return null;
        if (value instanceof Number n) return n.longValue();
        return Long.parseLong(value.toString());
    }

    @FunctionalInterface
    private interface StatementAction<R> {
        R apply(PreparedStatement ps) throws SQLException;
    }

    private <R> R execute(boolean returnGeneratedKeys, StatementAction<R> action) {
        String sql = sqlBuilder.toString();

        if (simpleDb.isDevMode()) {
            System.out.println("== rawSql ==\n" + sql + "\n== params ==\n" + params);
        }

        Connection connection = simpleDb.getConnection();
        int keyOption = returnGeneratedKeys ? Statement.RETURN_GENERATED_KEYS : Statement.NO_GENERATED_KEYS;

        try (PreparedStatement ps = connection.prepareStatement(sql, keyOption)) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            return action.apply(ps);
        } catch (SQLException e) {
            throw new RuntimeException("SQL 실행 실패: " + sql, e);
        }
    }
}
