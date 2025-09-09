package org.ab.sentinel.db;

import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;

import javax.sql.DataSource;

public final class JooqFactory {
    public static DSLContext create(DataSource ds) {
        return DSL.using(ds, SQLDialect.POSTGRES);
    }
}

