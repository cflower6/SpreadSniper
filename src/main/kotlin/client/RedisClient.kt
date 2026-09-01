package client

import io.lettuce.core.RedisClient as LettuceRedisClient
import io.lettuce.core.SetArgs
import io.lettuce.core.api.StatefulRedisConnection
import kotlinx.coroutines.future.await

class RedisClient(
    redisUrl: String
) {
    private val client: LettuceRedisClient =
        LettuceRedisClient.create(redisUrl)

    private val connection: StatefulRedisConnection<String, String> =
        client.connect()

    private val async =
        connection.async()

    suspend fun set(
        key: String,
        value: String,
        ttlSeconds: Long
    ) {
        val args =
            SetArgs.Builder.ex(ttlSeconds)

        async
            .set(key, value, args)
            .toCompletableFuture()
            .await()
    }

    suspend fun get(
        key: String
    ): String? {
        return async
            .get(key)
            .toCompletableFuture()
            .await()
    }

    suspend fun setIfAbsent(
        key: String,
        value: String,
        ttlSeconds: Long
    ): Boolean {
        val args =
            SetArgs.Builder
                .nx()
                .ex(ttlSeconds)

        val result =
            async
                .set(key, value, args)
                .toCompletableFuture()
                .await()

        return result == "OK"
    }

    fun close() {
        connection.close()
        client.shutdown()
    }
}