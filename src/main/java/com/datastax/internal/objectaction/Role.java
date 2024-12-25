package com.datastax.internal.objectaction;

public interface Role {

    String getName();

    boolean isSuper();

    boolean isLogin();

    /**
     * Represents the bit offsets used by Apache Cassandra for Permissions.
     */
    enum Permission {
        CREATE,
        ALTER,
        DROP,
        SELECT,
        MODIFY,
        AUTHORIZE,
        DESCRIBE
    }

    enum Resource {
        ALL_KEYSPACES,
        KEYSPACE,
        TABLE,
        ALL_ROLES
    }
}
