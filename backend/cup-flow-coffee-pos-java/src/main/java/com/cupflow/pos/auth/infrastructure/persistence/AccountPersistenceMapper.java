package com.cupflow.pos.auth.infrastructure.persistence;

import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
interface AccountPersistenceMapper {

    @Select("""
            SELECT id::text AS id, username, password_hash, display_name, status
            FROM accounts
            WHERE username = #{username}
            """)
    AccountRow findByUsername(String username);

    @Select("""
            SELECT id::text AS id, username, password_hash, display_name, status
            FROM accounts
            WHERE id = CAST(#{accountId} AS UUID)
            """)
    AccountRow findById(String accountId);

    @Select("""
            SELECT r.code
            FROM account_roles ar
            JOIN roles r ON r.id = ar.role_id
            WHERE ar.account_id = CAST(#{accountId} AS UUID)
            ORDER BY r.code
            """)
    List<String> findRoleCodes(String accountId);

    @Insert("""
            INSERT INTO accounts (id, username, password_hash, display_name, status)
            VALUES (
                CAST(#{id} AS UUID),
                #{username},
                #{passwordHash},
                #{displayName},
                #{status}
            )
            ON CONFLICT (username) DO NOTHING
            """)
    int insertIfAbsent(
            @Param("id") String id,
            @Param("username") String username,
            @Param("passwordHash") String passwordHash,
            @Param("displayName") String displayName,
            @Param("status") String status);

    @Insert("""
            INSERT INTO account_roles (account_id, role_id)
            SELECT CAST(#{accountId} AS UUID), r.id
            FROM roles r
            WHERE r.code = #{roleCode}
            ON CONFLICT (account_id, role_id) DO NOTHING
            """)
    int assignRoleIfAbsent(@Param("accountId") String accountId, @Param("roleCode") String roleCode);

    @Select("SELECT EXISTS (SELECT 1 FROM roles WHERE code = #{roleCode})")
    boolean roleExists(String roleCode);

    record AccountRow(String id, String username, String passwordHash, String displayName, String status) {}
}
