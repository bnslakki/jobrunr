package org.jobrunr.storage.nosql.redis;

import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import org.jobrunr.jobs.mappers.JobMapper;
import org.jobrunr.storage.StorageProvider;
import org.jobrunr.storage.StorageProviderTest;
import org.jobrunr.utils.mapper.jackson.JacksonJsonMapper;
import org.junit.jupiter.api.AfterAll;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.jobrunr.utils.resilience.RateLimiter.Builder.rateLimit;

@Testcontainers
public class LettuceRedisStorageProviderTest extends StorageProviderTest {

    @Container
    private static final GenericContainer redisContainer = new GenericContainer("redis").withExposedPorts(6379);

    private static RedisClient redisClient;

    @Override
    protected void cleanup() {
        RedisClient redisClient = getRedisClient();
        try (StatefulRedisConnection<String, String> connection = redisClient.connect()) {
            RedisCommands<String, String> commands = connection.sync();
            commands.flushall();
        }
    }

    @Override
    protected StorageProvider getStorageProvider() {
        final LettuceRedisStorageProvider lettuceRedisStorageProvider = new LettuceRedisStorageProvider(getRedisClient(), rateLimit().withoutLimits());
        lettuceRedisStorageProvider.setJobMapper(new JobMapper(new JacksonJsonMapper()));
        return lettuceRedisStorageProvider;
    }

    @AfterAll
    public static void shutdownRedisClient() {
        getRedisClient().shutdown();
    }

    private static RedisClient getRedisClient() {
        if (redisClient == null) {
            redisClient = RedisClient.create(RedisURI.create(redisContainer.getContainerIpAddress(), redisContainer.getMappedPort(6379)));
        }
        return redisClient;
    }
}
