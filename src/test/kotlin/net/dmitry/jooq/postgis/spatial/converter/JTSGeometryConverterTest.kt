package net.dmitry.jooq.postgis.spatial.converter

import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.Geometry
import net.dmitry.jooq.postgis.spatial.jts.JTS
import org.junit.Assert.assertTrue
import org.junit.Test
import org.postgresql.util.PGobject

/**
 * @author Dmitry Zhuravlev
 * *         Date: 08.03.16
 */
class JTSGeometryConverterTest {

    val jtsGeometryConverter = JTSGeometryConverter()

    @Test
    fun testFrom() {
        val pGobject = PGobject().apply {
            type = "geography"
            value = "0101000020E6100000304CA60A460D4140BE9F1A2FDD0C4E40"
        }
        val converted = jtsGeometryConverter.from(pGobject)
        assertTrue(converted is Geometry)
    }

    @Test
    fun testTo() {
        val x = 34.1037
        val y = 60.1005
        val jtsPoint = JTS.defaultGeomFactory.createPoint(Coordinate(x, y))
        val convertedBack = jtsGeometryConverter.to(jtsPoint)
        assertTrue(convertedBack is org.postgis.PGgeometry
                && convertedBack.geometry.getPoint(0).x == x && convertedBack.geometry.getPoint(0).y == y)
    }
}