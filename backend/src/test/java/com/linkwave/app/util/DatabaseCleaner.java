package com.linkwave.app.util;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class DatabaseCleaner {

  private final JdbcTemplate jdbcTemplate;

  public DatabaseCleaner(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  public void cleanAllTables() {
    List<String> tables =
        jdbcTemplate.queryForList(
            "SELECT tablename FROM pg_tables WHERE schemaname = 'public'", String.class);

    if (tables == null || tables.isEmpty()) {
      return;
    }

    String tablesToTruncate =
        tables.stream()
            .filter(Objects::nonNull)
            .map(table -> "\"" + table + "\"")
            .collect(Collectors.joining(", "));

    if (!tablesToTruncate.isBlank()) {
      jdbcTemplate.execute("TRUNCATE TABLE " + tablesToTruncate + " RESTART IDENTITY CASCADE");
    }
  }
}
