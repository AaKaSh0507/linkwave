package com.linkwave.app;

import com.linkwave.app.util.DatabaseCleaner;
import com.linkwave.app.util.RedisCleaner;
import io.restassured.RestAssured;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(locations = "classpath:application-test.yml")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public abstract class FunctionalTestBase extends FunctionalTestContainers {

  @LocalServerPort protected int port;

  @Autowired protected JdbcTemplate jdbcTemplate;

  @Autowired(required = false)
  protected StringRedisTemplate stringRedisTemplate;

  @Autowired protected DatabaseCleaner databaseCleaner;

  @Autowired(required = false)
  protected RedisCleaner redisCleaner;

  @BeforeEach
  void setUp() {
    RestAssured.baseURI = "http://localhost";
    RestAssured.port = port;
    RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
    resetTestData();
  }

  @AfterEach
  void tearDown() {
    resetTestData();
  }

  protected void resetTestData() {
    databaseCleaner.cleanAllTables();
    if (redisCleaner != null) {
      redisCleaner.flushAll();
    }
  }

  protected void truncateAllTables() {
    databaseCleaner.cleanAllTables();
  }

  protected void clearRedis() {
    if (redisCleaner != null) {
      redisCleaner.flushAll();
    }
  }
}
