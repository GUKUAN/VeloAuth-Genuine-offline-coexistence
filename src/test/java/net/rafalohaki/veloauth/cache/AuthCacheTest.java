package net.rafalohaki.veloauth.cache;

import net.rafalohaki.veloauth.config.Settings;
import net.rafalohaki.veloauth.i18n.Messages;
import net.rafalohaki.veloauth.model.CachedAuthUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SuppressWarnings("java:S100")
class AuthCacheTest {

    @TempDir
    Path tempDir;

    private Messages messages;

    @BeforeEach
    void setUp() {
        messages = new Messages();
        messages.setLanguage("en");
    }

    @Test
    void testAddAuthorizedPlayer_ConcurrentAdds_DoesNotExceedMaxSize() throws Exception {
        int maxSize = 4;
        AuthCache authCache = createCache(maxSize, 16);

        try {
            for (int iteration = 0; iteration < 10; iteration++) {
                authCache.clearAll();
                int offset = iteration * 32;

                runConcurrently(32, index -> {
                    int playerIndex = offset + index;
                    UUID playerUuid = UUID.nameUUIDFromBytes(
                            ("authorized-player-" + playerIndex).getBytes(StandardCharsets.UTF_8)
                    );
                    authCache.addAuthorizedPlayer(
                            playerUuid,
                            new CachedAuthUser(
                                    playerUuid,
                                    "Player" + playerIndex,
                                    "10.0.1." + (playerIndex % 255),
                                    System.currentTimeMillis(),
                                    false,
                                    null
                            )
                    );
                });

                assertEquals(maxSize, authCache.getStats().authorizedPlayersCount());
            }
        } finally {
            authCache.shutdown();
        }
    }

    @Test
    void testAddPremiumPlayer_ConcurrentAdds_DoesNotExceedMaxPremiumCache() throws Exception {
        int maxPremiumCache = 3;
        AuthCache authCache = createCache(16, maxPremiumCache);

        try {
            for (int iteration = 0; iteration < 10; iteration++) {
                authCache.clearAll();
                int offset = iteration * 24;

                runConcurrently(24, index -> {
                    int playerIndex = offset + index;
                    authCache.addPremiumPlayer(
                            "PremiumPlayer" + playerIndex,
                            UUID.nameUUIDFromBytes(
                                    ("premium-player-" + playerIndex).getBytes(StandardCharsets.UTF_8)
                            )
                    );
                });

                assertEquals(maxPremiumCache, authCache.getStats().premiumCacheCount());
            }
        } finally {
            authCache.shutdown();
        }
    }

    @Test
    void testRemovePremiumPlayer_CaseInsensitiveRemoval_ClearsCachedEntry() {
        AuthCache authCache = createCache(16, 16);

        try {
            authCache.addPremiumPlayer("PremiumUser", UUID.randomUUID());

            assertTrue(authCache.findPremiumStatus("premiumuser").isPresent());

            authCache.removePremiumPlayer("premiumuser");

            assertTrue(authCache.findPremiumStatus("PremiumUser").isEmpty());
        } finally {
            authCache.shutdown();
        }
    }

    private AuthCache createCache(int maxSize, int maxPremiumCache) {
        Settings settings = new Settings(tempDir);
        return new AuthCache(
                new AuthCache.AuthCacheConfig(60, maxSize, 32, maxPremiumCache, 5, 5, 0, 60),
                settings,
                messages
        );
    }

    private static void runConcurrently(int taskCount, ThrowingIntConsumer action) throws Exception {
        CountDownLatch ready = new CountDownLatch(taskCount);
        CountDownLatch start = new CountDownLatch(1);

        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Future<?>> futures = new ArrayList<>(taskCount);
            for (int index = 0; index < taskCount; index++) {
                int currentIndex = index;
                futures.add(executor.submit(() -> {
                    ready.countDown();
                    assertTrue(start.await(5, TimeUnit.SECONDS));
                    action.accept(currentIndex);
                    return null;
                }));
            }

            assertTrue(ready.await(5, TimeUnit.SECONDS));
            start.countDown();

            for (Future<?> future : futures) {
                future.get(5, TimeUnit.SECONDS);
            }
        }
    }

    @FunctionalInterface
    private interface ThrowingIntConsumer {
        void accept(int index) throws Exception;
    }
}
