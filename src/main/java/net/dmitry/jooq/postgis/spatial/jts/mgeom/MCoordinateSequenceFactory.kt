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
import org.locationtech.jts.geom.CoordinateSequenceFactory

import java.io.Serializable

/**
 * Creates MCoordinateSequenceFactory internally represented as an array of
 * [MCoordinate]s.
 */
object MCoordinateSequenceFactory : CoordinateSequenceFactory, Serializable {

    /**
     * Returns an MCoordinateSequence based on the given array -- the array is
     * used directly if it is an instance of MCoordinate[]; otherwise it is
     * copied.
     */
    override fun create(coordinates: Array<Coordinate>): CoordinateSequence {
        return MCoordinateSequence(
                    coordinates as Array<MCoordinate>
        )
    }

    override fun create(coordSeq: CoordinateSequence): CoordinateSequence {
        return MCoordinateSequence(coordSeq)
    }

    /**
     * Creates a MCoordinateSequence instance initialized to the size parameter.
     * Note that the dimension argument is ignored.
     *
     * @see org.locationtech.jts.geom.CoordinateSequenceFactory.create
     */
    override fun create(size: Int, dimension: Int): CoordinateSequence {
        return MCoordinateSequence(size)
    }

}
