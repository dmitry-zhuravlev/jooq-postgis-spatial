package net.dmitry.jooq.postgis.spatial.jts;

import org.jooq.*;
import org.jooq.impl.DSL;
import org.locationtech.jts.geom.Geometry;
//import org.postgis.Geometry;

import java.sql.SQLException;

public class PostgisGeometryBinding implements Binding<Object, Geometry> {
    private final PostgisGeometryConverter geometryConverter = new PostgisGeometryConverter();


    public Converter<Object, Geometry> converter() {
        return geometryConverter;
    }

    @Override
    public void sql(BindingSQLContext<Geometry> ctx) throws SQLException {
        ctx.render().visit(DSL.sql("?::geometry"));
    }

    @Override
    public void register(BindingRegisterContext<Geometry> ctx) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void set(BindingSetStatementContext<Geometry> ctx) throws SQLException {
        ctx.statement().setObject(ctx.index(),  ctx.convert(converter()).value());
    }

    @Override
    public void set(BindingSetSQLOutputContext<Geometry> ctx) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void get(BindingGetResultSetContext<Geometry> ctx) throws SQLException {
        ctx.convert(converter()).value(ctx.resultSet().getObject(ctx.index()));
    }

    @Override
    public void get(BindingGetStatementContext<Geometry> ctx) throws SQLException {
        ctx.convert(converter()).value(ctx.statement().getObject(ctx.index()));
    }

    @Override
    public void get(BindingGetSQLInputContext<Geometry> ctx) throws SQLException {
        throw new UnsupportedOperationException();
    }


}
