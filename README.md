jooq-postgis-spatial
==================

 jOOQ data type converters and binders for PostGIS spatial database.
 Project inspired and uses some Hibernate Spatial classes to convert org.postgresql.util.PGobject geometry to JTS geometry.
 The similar project for MySQL spatial can be found here: [jooq-mysql-spatial](https://github.com/gquintana/jooq-mysql-spatial)

# jOOQ Codegen configuration (JTS Geometry types)

    <database>
        <customTypes>
            <customType>
                <name>Geometry</name>
                <type>com.vividsolutions.jts.geom.Geometry</type>
                <binding>net.dmitry.jooq.postgis.spatial.binding.JTSGeometryBinding</binding>
            </customType>
        </customTypes>
        <forcedTypes>
            <forcedType>
                <name>Geometry</name>
                <types>(geometry|GEOMETRY)</types>
            </forcedType>
        </forcedTypes>
    </database>

# jOOQ Codegen configuration (PostGIS Geometry types)

    <database>
        <customTypes>
            <customType>
                <name>Geometry</name>
                <type>com.vividsolutions.jts.geom.Geometry</type>
                <binding>net.dmitry.jooq.postgis.spatial.binding.PostgisGeometryBinding</binding>
            </customType>
        </customTypes>
        <forcedTypes>
            <forcedType>
                <name>Geometry</name>
                <types>(geometry|GEOMETRY)</types>
            </forcedType>
        </forcedTypes>
    </database>


# Writing JTS geometries

    Point point = JTS.getDefaultGeomFactory().createPoint(new Coordinate(...))
    dsl.insertInto(CITY, CITY.ID, CITY.NAME, CITY.GEOM)
            .values(1, "Lyon", point)
            .execute();

# Reading JTS geometries

    Point point = (Point) dsl.select(CITY.GEOM)
            .from(CITY)
            .where(CITY.ID.eq(1))
            .fetchOne(CITY.GEOM);


# Querying

Search cities located in France

    Polygon france = JTS.getDefaultGeomFactory().createPolygon(...)
    Result<CityRecord> cities = dsl.selectFrom(CITY)
            .where(MBRWithin(CITY.GEOM, france))
            .fetch();

Search to country containing Lyon

    Point lyon = JTS.getDefaultGeomFactory().createPoint(new Coordinate(...))
    Result<CountryRecord> countries = dsl.selectFrom(COUNTRY)
            .where(MBRContains(COUNTRY.GEOM, lyon))
            .fetch();

# TODO

- Define spatial functions like ST_Intersects , ST_CoveredBy, ST_Within etc.
