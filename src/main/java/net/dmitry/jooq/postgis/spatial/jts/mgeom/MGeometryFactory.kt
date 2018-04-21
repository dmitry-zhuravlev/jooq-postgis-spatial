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

import org.locationtech.jts.geom.CoordinateSequence
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.PrecisionModel

/**
 * Extension of the GeometryFactory for constructing Geometries with Measure
 * support.
 *
 * @see org.locationtech.jts.geom.GeometryFactory
 */
class MGeometryFactory : GeometryFactory {

    @JvmOverloads constructor(precisionModel: PrecisionModel = PrecisionModel(), SRID: Int = 0,
                              coordinateSequenceFactory: MCoordinateSequenceFactory = MCoordinateSequenceFactory) : super(precisionModel, SRID, coordinateSequenceFactory)

    /**
     * Constructs a MLineString using the given Coordinates; a null or empty
     * array will create an empty MLineString.
     *
     * @param coordinates array of MCoordinate defining this geometry's vertices
     * @return An instance of MLineString containing the coordinates
     * @see .createLineString
     */
    fun createMLineString(coordinates: Array<MCoordinate>?): MLineString {
        return createMLineString(
                if (coordinates != null)
                    coordinateSequenceFactory
                            .create(coordinates)
                else
                    null
        )
    }

    fun createMultiMLineString(mlines: Array<MLineString>,
                               mGap: Double): MultiMLineString {
        return MultiMLineString(mlines, mGap, this)
    }

    fun createMultiMLineString(mlines: Array<MLineString>): MultiMLineString {
        return MultiMLineString(mlines, 0.0, this)
    }

    /**
     * Creates a MLineString using the given CoordinateSequence; a null or empty
     * CoordinateSequence will create an empty MLineString.
     *
     * @param coordinates a CoordinateSequence possibly empty, or null
     * @return An MLineString instance based on the `coordinates`
     * @see .createLineString
     */
    fun createMLineString(coordinates: CoordinateSequence?): MLineString {
        return MLineString(coordinates!!, this)
    }

}
