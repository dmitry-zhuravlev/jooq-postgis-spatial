/*
 * This file is part of Hibernate Spatial, an extension to the
 *  hibernate ORM solution for spatial (geographic) data.
 *
 *  Copyright © 2007-2012 Geovise BVBA
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package net.dmitry.jooq.postgis.spatial.jts.mgeom

import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.CoordinateSequence
import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.geom.Point

import java.util.ArrayList

object EventLocator {

    /**
     * Returns the point on the specified MGeometry where its measure equals the specified position.
     *
     * @return a Point Geometry
     * @throws MGeometryException
     */
    @Throws(MGeometryException::class)
    fun getPointGeometry(lrs: MGeometry, position: Double): Point {
        val c = lrs.getCoordinateAtM(position)
        val pnt = lrs.factory.createPoint(c)
        copySRID(lrs.asGeometry(), pnt)
        return pnt
    }

    @Throws(MGeometryException::class)
    fun getLinearGeometry(lrs: MGeometry,
                          begin: Double, end: Double): MultiMLineString {
        val factory = lrs.factory
        val cs = lrs.getCoordinatesBetween(begin, end)
        val linestrings = cs.filter { it.size() >= 2 }.map { factory.createMLineString(it) }
        val result = factory.createMultiMLineString(linestrings.toTypedArray())
        copySRID(lrs.asGeometry(), result.asGeometry())
        return result
    }

    fun copySRID(source: Geometry, target: Geometry) {
        target.srid = source.srid
    }

}
