package net.dmitry.jooq.postgis.spatial.converter

import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.geom.GeometryFactory
import net.dmitry.jooq.postgis.spatial.jts.JTS
import net.dmitry.jooq.postgis.spatial.jts.mgeom.MCoordinate
import net.dmitry.jooq.postgis.spatial.jts.mgeom.MGeometry
import net.dmitry.jooq.postgis.spatial.jts.mgeom.MGeometryFactory
import org.jooq.Converter
import org.postgis.*

/**
 * @author Dmitry Zhuravlev
 * *         Date: 07.03.16
 */
class JTSGeometryConverter : Converter<Any, Geometry> {

    private val postgisGeometryConverter = PostgisGeometryConverter()

    override fun from(databaseObject: Any?): Geometry? = toJTS(postgisGeometryConverter.from(databaseObject))


    override fun to(userObject: Geometry?): Any? = if (userObject != null) toNative(userObject) else null

    override fun toType(): Class<Geometry>? = Geometry::class.java

    override fun fromType(): Class<Any>? = Any::class.java

    protected val geometryFactory: MGeometryFactory
        get() {
            return JTS.defaultGeomFactory
        }


    fun toJTS(databaseObject: Any?): Geometry? {
        var obj = databaseObject ?: return null
        // in some cases, Postgis returns not PGgeometry objects
        // but org.postgis.Geometry instances.
        // This has been observed when retrieving GeometryCollections
        // as the result of an SQL-operation such as Union.
        if (obj is org.postgis.Geometry) {
            obj = PGgeometry(obj)
        }

        when (obj) {
            is PGgeometry -> {
                val out: Geometry? = when (obj.geoType) {
                    org.postgis.Geometry.POINT -> convertPoint(obj.geometry as Point)
                    org.postgis.Geometry.LINESTRING -> convertLineString(
                            obj.geometry as LineString)
                    org.postgis.Geometry.POLYGON -> convertPolygon(obj.geometry as Polygon)
                    org.postgis.Geometry.MULTILINESTRING -> convertMultiLineString(
                            obj.geometry as MultiLineString)
                    org.postgis.Geometry.MULTIPOINT -> convertMultiPoint(
                            obj.geometry as MultiPoint)
                    org.postgis.Geometry.MULTIPOLYGON -> convertMultiPolygon(
                            obj.geometry as MultiPolygon)
                    org.postgis.Geometry.GEOMETRYCOLLECTION -> convertGeometryCollection(
                            obj.geometry as GeometryCollection)
                    else -> throw RuntimeException("Unknown type of PGgeometry")
                }
                out?.srid = obj.geometry.srid
                return out
            }
            is PGboxbase -> return convertBox(obj)
            else -> throw IllegalArgumentException(
                    "Can't convert object of type " + obj.javaClass.canonicalName)
        }

    }

    private fun convertBox(box: PGboxbase): Geometry {
        val ll = box.llb
        val ur = box.urt
        val ringCoords = if (box is PGbox2d) {
            arrayOf(Coordinate(ll.x, ll.y),
                    Coordinate(ur.x, ll.y),
                    Coordinate(ur.x, ur.y),
                    Coordinate(ll.x, ur.y),
                    Coordinate(ll.x, ll.y))
        } else {
            arrayOf(Coordinate(ll.x, ll.y, ll.z),
                    Coordinate(ur.x, ll.y, ll.z),
                    Coordinate(ur.x, ur.y, ur.z),
                    Coordinate(ll.x, ur.y, ur.z),
                    Coordinate(ll.x, ll.y, ll.z))
        }
        val shell = geometryFactory.createLinearRing(ringCoords)
        return geometryFactory.createPolygon(shell, null)
    }

    private fun convertGeometryCollection(collection: GeometryCollection): Geometry {
        val geometries = collection.geometries
        val jtsGeometries = Array(geometries.size) { i ->
            val ret = toJTS(collection.geometries[i])
            ret?.srid = 0
            ret!!
        }
        val jtsGCollection = geometryFactory.createGeometryCollection(jtsGeometries)
        return jtsGCollection
    }

    private fun convertMultiPolygon(pgMultiPolygon: MultiPolygon): Geometry {
        val polygons = Array(pgMultiPolygon.numPolygons()) { i ->
            val pgPolygon = pgMultiPolygon.getPolygon(i)
            convertPolygon(pgPolygon) as org.locationtech.jts.geom.Polygon
        }

        val out = geometryFactory.createMultiPolygon(polygons)
        return out
    }

    private fun convertMultiPoint(pgMultiPoint: MultiPoint): Geometry {
        val points = Array(pgMultiPoint.numPoints()) { i ->
            convertPoint(pgMultiPoint.getPoint(i))
        }
        val out = geometryFactory.createMultiPoint(points)
        out.srid = pgMultiPoint.srid
        return out
    }

    private fun convertMultiLineString(
            mlstr: MultiLineString): Geometry {
        val out = if (mlstr.haveMeasure) {
            geometryFactory.createMultiMLineString(Array(mlstr.numLines()) { i ->
                val coordinates = toJTSCoordinates(
                        mlstr.getLine(i).points)
                geometryFactory.createMLineString(coordinates)
            })
        } else {
            geometryFactory.createMultiLineString(Array<org.locationtech.jts.geom.LineString>(mlstr.numLines()) { i ->
                geometryFactory.createLineString(
                        toJTSCoordinates(mlstr.getLine(i).points))
            })
        }
        return out
    }

    private fun convertPolygon(
            polygon: Polygon): Geometry {
        val shell = geometryFactory.createLinearRing(
                toJTSCoordinates(polygon.getRing(0).points))
        return if (polygon.numRings() > 1) {
            val rings = arrayOfNulls<org.locationtech.jts.geom.LinearRing>(polygon.numRings() - 1)
            for (r in 1 until polygon.numRings()) {
                rings[r - 1] = geometryFactory.createLinearRing(
                        toJTSCoordinates(polygon.getRing(r).points))
            }
            geometryFactory.createPolygon(shell, rings)
        } else {
            geometryFactory.createPolygon(shell, null)
        }
    }

    private fun convertPoint(pnt: Point): org.locationtech.jts.geom.Point {
        val g = geometryFactory.createPoint(
                this.toJTSCoordinate(pnt))
        return g
    }

    private fun convertLineString(
            lineString: LineString): org.locationtech.jts.geom.LineString {
        return if (lineString.haveMeasure)
            geometryFactory.createMLineString(toJTSCoordinates(lineString.points))
        else
            geometryFactory.createLineString(
                    toJTSCoordinates(lineString.points))
    }

    private fun toJTSCoordinates(points: Array<Point>): Array<MCoordinate> {
        val coordinates = Array(points.size) { i ->
            this.toJTSCoordinate(points[i])
        }
        return coordinates
    }

    private fun toJTSCoordinate(point: Point): MCoordinate {
        return if (point.dimension == 2) {
            if (point.haveMeasure)
                MCoordinate.create2dWithMeasure(
                        point.getX(), point.getY(), point.getM())
            else
                MCoordinate.create2d(
                        point.getX(), point.getY())
        } else {
            if (point.haveMeasure)
                MCoordinate.create3dWithMeasure(
                        point.getX(), point.getY(), point.getZ(), point.getM())
            else
                MCoordinate.create3d(
                        point.getX(), point.getY(), point.getZ())
        }
    }


    /**
     * Converts a JTS `Geometry` to a native geometry object.

     * @param jtsGeometry    JTS Geometry to convert
     * *
     * @return native database geometry object corresponding to jtsGeometry.
     */
    protected fun toNative(jtsGeometry: Geometry): PGgeometry {
        var jtsGeom = jtsGeometry
        var geom: org.postgis.Geometry? = null
        jtsGeom = forceEmptyToGeometryCollection(jtsGeom)
        when (jtsGeom) {
            is org.locationtech.jts.geom.Point -> geom = convertJTSPoint(jtsGeom)
            is org.locationtech.jts.geom.LineString -> geom = convertJTSLineString(jtsGeom)
            is org.locationtech.jts.geom.MultiLineString -> geom = convertJTSMultiLineString(jtsGeom)
            is org.locationtech.jts.geom.Polygon -> geom = convertJTSPolygon(jtsGeom)
            is org.locationtech.jts.geom.MultiPoint -> geom = convertJTSMultiPoint(jtsGeom)
            is org.locationtech.jts.geom.MultiPolygon -> geom = convertJTSMultiPolygon(jtsGeom)
            is org.locationtech.jts.geom.GeometryCollection -> geom = convertJTSGeometryCollection(jtsGeom)
        }

        if (geom != null) {
            return PGgeometry(geom)
        } else {
            throw UnsupportedOperationException(
                    "Conversion of "
                            + jtsGeom.javaClass.simpleName
                            + " to PGgeometry not supported")
        }
    }


    //Postgis treats every empty geometry as an empty geometrycollection

    private fun forceEmptyToGeometryCollection(jtsGeometry: Geometry): Geometry {
        var forced = jtsGeometry
        if (forced.isEmpty) {
            var factory: GeometryFactory? = jtsGeometry.factory
            if (factory == null) {
                factory = JTS.defaultGeomFactory
            }
            forced = factory.createGeometryCollection(null)!!
            forced.setSRID(jtsGeometry.srid)
        }
        return forced
    }

    private fun convertJTSMultiPolygon(
            multiPolygon: org.locationtech.jts.geom.MultiPolygon): MultiPolygon {
        val pgPolygons = arrayOfNulls<Polygon>(multiPolygon.numGeometries)
        for (i in pgPolygons.indices) {
            pgPolygons[i] = convertJTSPolygon(
                    multiPolygon.getGeometryN(i) as org.locationtech.jts.geom.Polygon)
        }
        val mpg = MultiPolygon(pgPolygons)
        mpg.setSrid(multiPolygon.srid)
        return mpg
    }

    private fun convertJTSMultiPoint(
            multiPoint: org.locationtech.jts.geom.MultiPoint): MultiPoint {
        val pgPoints = Array(multiPoint.numGeometries) { i ->
            convertJTSPoint(multiPoint.getGeometryN(i) as org.locationtech.jts.geom.Point)
        }
        val mp = MultiPoint(pgPoints)
        mp.setSrid(multiPoint.srid)
        return mp
    }

    private fun convertJTSPolygon(
            jtsPolygon: org.locationtech.jts.geom.Polygon): Polygon {
        val numRings = jtsPolygon.numInteriorRing
        val rings = arrayOfNulls<LinearRing>(numRings + 1)
        rings[0] = convertJTSLineStringToLinearRing(
                jtsPolygon.exteriorRing)
        for (i in 0 until numRings) {
            rings[i + 1] = convertJTSLineStringToLinearRing(
                    jtsPolygon.getInteriorRingN(i))
        }
        val polygon = Polygon(rings)
        polygon.setSrid(jtsPolygon.srid)
        return polygon
    }

    private fun convertJTSLineStringToLinearRing(
            lineString: org.locationtech.jts.geom.LineString): LinearRing {
        val lr = LinearRing(
                toPoints(
                        lineString.coordinates))
        lr.setSrid(lineString.srid)
        return lr
    }

    private fun convertJTSLineString(
            string: org.locationtech.jts.geom.LineString): LineString {
        val ls = LineString(
                toPoints(
                        string.coordinates))
        if (string is MGeometry) {
            ls.haveMeasure = true
        }
        ls.setSrid(string.srid)
        return ls
    }

    private fun convertJTSMultiLineString(
            string: org.locationtech.jts.geom.MultiLineString): MultiLineString {
        val lines = arrayOfNulls<LineString>(string.numGeometries)
        for (i in 0 until string.numGeometries) {
            lines[i] = LineString(
                    toPoints(
                            string.getGeometryN(
                                    i).coordinates))
        }
        val mls = MultiLineString(lines)
        if (string is MGeometry) {
            mls.haveMeasure = true
        }
        mls.setSrid(string.srid)
        return mls
    }

    private fun convertJTSPoint(point: org.locationtech.jts.geom.Point): Point {
        val pgPoint = Point()
        pgPoint.srid = point.srid
        pgPoint.x = point.x
        pgPoint.y = point.y
        val coordinate = point.coordinate
        if (java.lang.Double.isNaN(coordinate.z)) {
            pgPoint.dimension = 2
        } else {
            pgPoint.z = coordinate.z
            pgPoint.dimension = 3
        }
        pgPoint.haveMeasure = false
        if (coordinate is MCoordinate && !java.lang.Double.isNaN(coordinate.m)) {
            pgPoint.m = coordinate.m
            pgPoint.haveMeasure = true
        }
        return pgPoint
    }

    private fun convertJTSGeometryCollection(
            collection: org.locationtech.jts.geom.GeometryCollection): GeometryCollection {
        val pgCollections = Array(collection.numGeometries) { i ->
            val currentGeom = forceEmptyToGeometryCollection(collection.getGeometryN(i))
            when (currentGeom) {
                is org.locationtech.jts.geom.LineString -> convertJTSLineString(currentGeom)
                is org.locationtech.jts.geom.LinearRing -> convertJTSLineStringToLinearRing(currentGeom)
                is org.locationtech.jts.geom.MultiLineString -> convertJTSMultiLineString(currentGeom)
                is org.locationtech.jts.geom.MultiPoint -> convertJTSMultiPoint(currentGeom)
                is org.locationtech.jts.geom.MultiPolygon -> convertJTSMultiPolygon(currentGeom)
                is org.locationtech.jts.geom.Point -> convertJTSPoint(currentGeom)
                is org.locationtech.jts.geom.Polygon -> convertJTSPolygon(currentGeom)
                is org.locationtech.jts.geom.GeometryCollection -> convertJTSGeometryCollection(currentGeom)
                else -> {
                    throw UnsupportedOperationException("Unsupported geometry")
                }
            }
        }
        val gc = GeometryCollection(pgCollections)
        gc.setSrid(collection.srid)
        return gc
    }


    private fun toPoints(coordinates: Array<Coordinate>): Array<Point?> {
        val points = arrayOfNulls<Point>(coordinates.size)
        for (i in coordinates.indices) {
            val c = coordinates[i]
            val pt: Point
            pt = if (java.lang.Double.isNaN(c.z)) {
                Point(c.x, c.y)
            } else {
                Point(c.x, c.y, c.z)
            }
            if (c is MCoordinate) {
                val mc = c
                if (!java.lang.Double.isNaN(mc.m)) {
                    pt.setM(mc.m)
                }
            }
            points[i] = pt
        }
        return points
    }
}
