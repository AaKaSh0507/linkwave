package com.linkwave.app.service.presence;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.linkwave.app.domain.presence.PresenceMetadata;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
@org.springframework.context.annotation.Import(com.linkwave.app.config.TestRedisConfig.class)
@org.springframework.boot.autoconfigure.EnableAutoConfiguration(exclude = {
        org.springframework.boot.autoconfigure.session.SessionAutoConfiguration.class,
        org.springframework.session.data.redis.config.annotation.web.http.RedisHttpSessionConfiguration.class
})
class PresenceServiceTest {

    @Autowired
    private PresenceService presenceService;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String TEST_USER_1 = "+14155551234";
    private static final String TEST_USER_2 = "+14155555678";
    private static final String PRESENCE_KEY_PREFIX = "linkwave:presence:";

    @BeforeEach
    void setUp() {

        cleanupPresenceKeys();
    }

    @AfterEach
    void tearDown() {

        cleanupPresenceKeys();
    }

    @Test
    void testRecordHeartbeat_setsUserOnline() {

        presenceService.markOnline(TEST_USER_1);
        presenceService.recordHeartbeat(TEST_USER_1);

        assertThat(presenceService.isUserOnline(TEST_USER_1)).isTrue();

        String key = PRESENCE_KEY_PREFIX + TEST_USER_1;
        Boolean exists = redisTemplate.hasKey(key);
        assertThat(exists).isTrue();

        Long ttl = redisTemplate.getExpire(key, TimeUnit.SECONDS);
        assertThat(ttl).isGreaterThan(0).isLessThanOrEqualTo(75);
    }

    @Test
    void testTtlExpiration_marksUserOffline() {

        presenceService.markOnline(TEST_USER_1);
        assertThat(presenceService.isUserOnline(TEST_USER_1)).isTrue();

        String key = PRESENCE_KEY_PREFIX + TEST_USER_1;
        redisTemplate.expire(key, 2, TimeUnit.SECONDS);

        await().atMost(5, TimeUnit.SECONDS)
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> {
                    assertThat(presenceService.isUserOnline(TEST_USER_1)).isFalse();
                });

        Boolean exists = redisTemplate.hasKey(key);
        assertThat(exists).isFalse();
    }

    @Test
    void testMultiDevice_maintainsPresence() throws Exception {

        presenceService.markOnline(TEST_USER_1);
        assertThat(presenceService.isUserOnline(TEST_USER_1)).isTrue();

        PresenceMetadata metadata1 = presenceService.getPresenceMetadata(TEST_USER_1);
        assertThat(metadata1).isNotNull();
        assertThat(metadata1.getConnectionCount()).isEqualTo(1);

        presenceService.markOnline(TEST_USER_1);

        PresenceMetadata metadata2 = presenceService.getPresenceMetadata(TEST_USER_1);
        assertThat(metadata2).isNotNull();
        assertThat(metadata2.getConnectionCount()).isEqualTo(2);
        assertThat(presenceService.isUserOnline(TEST_USER_1)).isTrue();

        presenceService.markDisconnect(TEST_USER_1);

        PresenceMetadata metadata3 = presenceService.getPresenceMetadata(TEST_USER_1);
        assertThat(metadata3).isNotNull();
        assertThat(metadata3.getConnectionCount()).isEqualTo(1);
        assertThat(presenceService.isUserOnline(TEST_USER_1)).isTrue();

        presenceService.markDisconnect(TEST_USER_1);

        PresenceMetadata metadata4 = presenceService.getPresenceMetadata(TEST_USER_1);
        assertThat(metadata4).isNotNull();
        assertThat(metadata4.getConnectionCount()).isEqualTo(0);

        String key = PRESENCE_KEY_PREFIX + TEST_USER_1;
        redisTemplate.expire(key, 1, TimeUnit.SECONDS);

        await().atMost(3, TimeUnit.SECONDS)
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> {
                    assertThat(presenceService.isUserOnline(TEST_USER_1)).isFalse();
                });
    }

    @Test
    void testHeartbeat_refreshesTtl() throws Exception {

        presenceService.markOnline(TEST_USER_1);
        String key = PRESENCE_KEY_PREFIX + TEST_USER_1;

        redisTemplate.expire(key, 5, TimeUnit.SECONDS);
        Long initialTtl = redisTemplate.getExpire(key, TimeUnit.SECONDS);
        assertThat(initialTtl).isLessThanOrEqualTo(5);

        Thread.sleep(2000);
        presenceService.recordHeartbeat(TEST_USER_1);

        Long newTtl = redisTemplate.getExpire(key, TimeUnit.SECONDS);
        assertThat(newTtl).isGreaterThan(initialTtl);
        assertThat(newTtl).isGreaterThan(60);
    }

    @Test
    void testHeartbeat_rateLimiting() {

        presenceService.markOnline(TEST_USER_1);

        boolean firstResult = presenceService.recordHeartbeat(TEST_USER_1);
        assertThat(firstResult).isTrue();

        boolean secondResult = presenceService.recordHeartbeat(TEST_USER_1);

        assertThat(secondResult).isFalse();
    }

    @Test
    void testGetUsersPresence_bulkQuery() {

        presenceService.markOnline(TEST_USER_1);

        Map<String, Boolean> presenceMap = presenceService.getUsersPresence(
                List.of(TEST_USER_1, TEST_USER_2));

        assertThat(presenceMap).hasSize(2);
        assertThat(presenceMap.get(TEST_USER_1)).isTrue();
        assertThat(presenceMap.get(TEST_USER_2)).isFalse();
    }

    @Test
    void testGetLastSeen_returnsTimestamp() {

        Instant beforeOnline = Instant.now();
        presenceService.markOnline(TEST_USER_1);
        Instant afterOnline = Instant.now();

        Instant lastSeen = presenceService.getLastSeen(TEST_USER_1);

        assertThat(lastSeen).isNotNull();
        assertThat(lastSeen).isBetween(beforeOnline, afterOnline);
    }

    @Test
    void testGetLastSeen_offlineUser_returnsNull() {

        Instant lastSeen = presenceService.getLastSeen(TEST_USER_1);

        assertThat(lastSeen).isNull();
    }

    @Test
    void testMarkDisconnect_withoutPriorConnection_handlesGracefully() {

        presenceService.markDisconnect(TEST_USER_1);

        assertThat(presenceService.isUserOnline(TEST_USER_1)).isFalse();
    }

    @Test
    void testReconnect_withinTtlWindow_maintainsPresence() throws Exception {

        presenceService.markOnline(TEST_USER_1);
        assertThat(presenceService.isUserOnline(TEST_USER_1)).isTrue();

        presenceService.markDisconnect(TEST_USER_1);

        Thread.sleep(1000);
        presenceService.markOnline(TEST_USER_1);

        assertThat(presenceService.isUserOnline(TEST_USER_1)).isTrue();
    }

    private void cleanupPresenceKeys() {
        var keys = redisTemplate.keys(PRESENCE_KEY_PREFIX + "*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }
}
