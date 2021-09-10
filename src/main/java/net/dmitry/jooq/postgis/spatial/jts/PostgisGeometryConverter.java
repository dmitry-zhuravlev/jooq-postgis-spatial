package net.dmitry.jooq.postgis.spatial.jts;

import net.dmitry.jooq.postgis.spatial.converter.JTSGeometryConverter;
import org.jetbrains.annotations.NotNull;
import org.jooq.Converter;
import org.locationtech.jts.geom.Geometry;
import org.postgis.PGgeometry;
import org.postgresql.util.PGobject;

import java.sql.SQLException;

public class PostgisGeometryConverter implements Converter<Object, Geometry> {
    @Override
    public Geometry from(Object o) {
        if (o == null) {
            return null;
        } else {
            try {
                org.postgis.Geometry g = new PGgeometry(o.toString()).getGeometry();
                return new JTSGeometryConverter().toJTS(g);
            } catch (SQLException throwables) {
                throwables.printStackTrace();
                return null;
            }
        }
    }

    @Override
    public Object to(Geometry geom) {
        if (geom == null) {
            return null;
        } else {
            PGobject p = new PGobject();
            p.setType("Geometry");
            try {
                p.setValue(geom.toText());
            } catch (SQLException throwables) {
                throwables.printStackTrace();
                return null;
            }
            return p;
        }
    }

    @Override
    public @NotNull
    Class<Object> fromType() {
        return Object.class;
    }

    @Override
    public @NotNull
    Class<Geometry> toType() {
        return Geometry.class;
    }
}
