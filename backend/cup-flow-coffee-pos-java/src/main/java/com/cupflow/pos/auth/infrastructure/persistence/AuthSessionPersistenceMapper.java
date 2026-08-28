package com.cupflow.pos.auth.infrastructure.persistence;

import java.time.Instant;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
interface AuthSessionPersistenceMapper {

    @Insert("""
            INSERT INTO auth_sessions (
                id, account_id, token_hash, created_at, last_activity_at,
                idle_expires_at, absolute_expires_at, updated_at
            ) VALUES (
                CAST(#{id} AS UUID), CAST(#{accountId} AS UUID), #{tokenHash}, #{createdAt}, #{lastActivityAt},
                #{idleExpiresAt}, #{absoluteExpiresAt}, #{createdAt}
            )
            """)
    int insert(
            @Param("id") String id,
            @Param("accountId") String accountId,
            @Param("tokenHash") String tokenHash,
            @Param("createdAt") Instant createdAt,
            @Param("lastActivityAt") Instant lastActivityAt,
            @Param("idleExpiresAt") Instant idleExpiresAt,
            @Param("absoluteExpiresAt") Instant absoluteExpiresAt);

    @Select("""
            SELECT
                id::text AS id,
                account_id::text AS account_id,
                token_hash,
                created_at,
                last_activity_at,
                idle_expires_at,
                absolute_expires_at
            FROM auth_sessions
            WHERE token_hash = #{tokenHash} AND revoked_at IS NULL
            """)
    SessionRow findActiveByTokenHash(String tokenHash);

    @Update("""
            UPDATE auth_sessions
            SET
                last_activity_at = GREATEST(last_activity_at, #{acceptedAt}),
                idle_expires_at = LEAST(
                    absolute_expires_at,
                    GREATEST(idle_expires_at, #{idleExpiresAt})
                ),
                updated_at = GREATEST(updated_at, #{acceptedAt}),
                version = version + 1
            WHERE token_hash = #{tokenHash}
              AND revoked_at IS NULL
              AND idle_expires_at > #{acceptedAt}
              AND absolute_expires_at > #{acceptedAt}
            """)
    int refreshActivity(
            @Param("tokenHash") String tokenHash,
            @Param("acceptedAt") Instant acceptedAt,
            @Param("idleExpiresAt") Instant idleExpiresAt);

    @Update("""
            UPDATE auth_sessions
            SET revoked_at = #{revokedAt}, revocation_reason = #{reason}, updated_at = #{revokedAt}, version = version + 1
            WHERE token_hash = #{tokenHash} AND revoked_at IS NULL
            """)
    int revokeByTokenHash(
            @Param("tokenHash") String tokenHash,
            @Param("revokedAt") Instant revokedAt,
            @Param("reason") String reason);

    record SessionRow(
            String id,
            String accountId,
            String tokenHash,
            Instant createdAt,
            Instant lastActivityAt,
            Instant idleExpiresAt,
            Instant absoluteExpiresAt) {}
}
