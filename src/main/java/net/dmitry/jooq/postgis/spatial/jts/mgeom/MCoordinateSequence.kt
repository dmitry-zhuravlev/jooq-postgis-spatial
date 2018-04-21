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
import org.locationtech.jts.geom.Envelope

import java.io.Serializable

/**
 * Implements the CoordinateSequence interface. In this implementation,
 * Coordinates returned by #toArray and #get are live -- parties that change
 * them are actually changing the MCoordinateSequence's underlying data.
 */
class MCoordinateSequence : CoordinateSequence, Serializable {

    private var coordinates: Array<MCoordinate>

    /**
     * Copy constructor -- simply aliases the input array, for better
     * performance.
     *
     * @param coordinates
     */
    constructor(coordinates: Array<MCoordinate>) {
        this.coordinates = coordinates
    }

    /**
     * Constructor that makes a copy of an array of Coordinates. Always makes a
     * copy of the input array, since the actual class of the Coordinates in the
     * input array may be different from MCoordinate.
     *
     * @param copyCoords
     */
    constructor(copyCoords: Array<Coordinate>) {
        coordinates = copy(copyCoords)
    }

    /**
     * Constructor that makes a copy of a CoordinateSequence.
     *
     * @param coordSeq
     */
    constructor(coordSeq: CoordinateSequence) {
        coordinates = copy(coordSeq)
    }

    /**
     * Constructs a sequence of a given size, populated with new
     * [MCoordinate]s.
     *
     * @param size the size of the sequence to create
     */
    constructor(size: Int) {
        coordinates = Array(size) { _ ->
            MCoordinate()
        }
    }

    /**
     * @see org.locationtech.jts.geom.CoordinateSequence.getDimension
     */
    override fun getDimension(): Int {
        return 4
    }

    override fun getCoordinate(i: Int): Coordinate {
        return coordinates[i]
    }

    /**
     * @see org.locationtech.jts.geom.CoordinateSequence.getCoordinateCopy
     */
    override fun getCoordinateCopy(index: Int): Coordinate {
        return Coordinate(coordinates[index])
    }

    /**
     * @see org.locationtech.jts.geom.CoordinateSequence.getCoordinate
     */
    override fun getCoordinate(index: Int, coord: Coordinate) {
        coord.x = coordinates[index].x
        coord.y = coordinates[index].y
    }

    /**
     * @see org.locationtech.jts.geom.CoordinateSequence.getX
     */
    override fun getX(index: Int): Double {
        return coordinates[index].x
    }

    /**
     * @see org.locationtech.jts.geom.CoordinateSequence.getY
     */
    override fun getY(index: Int): Double {
        return coordinates[index].y
    }

    /**
     * @return the measure value of the coordinate in the index
     */
    fun getM(index: Int): Double {
        return coordinates[index].m
    }

    /**
     * @see org.locationtech.jts.geom.CoordinateSequence.getOrdinate
     */
    override fun getOrdinate(index: Int, ordinateIndex: Int): Double {
        when (ordinateIndex) {
            CoordinateSequence.X -> return coordinates[index].x
            CoordinateSequence.Y -> return coordinates[index].y
            CoordinateSequence.Z -> return coordinates[index].z
            CoordinateSequence.M -> return coordinates[index].m
        }
        return java.lang.Double.NaN
    }

    /**
     * @see org.locationtech.jts.geom.CoordinateSequence.setOrdinate
     */
    override fun setOrdinate(index: Int, ordinateIndex: Int, value: Double) {
        when (ordinateIndex) {
            CoordinateSequence.X -> coordinates[index].x = value
            CoordinateSequence.Y -> coordinates[index].y = value
            CoordinateSequence.Z -> coordinates[index].z = value
            CoordinateSequence.M -> coordinates[index].m = value
            else -> throw IllegalArgumentException("invalid ordinateIndex")
        }
    }

    override fun clone(): Any {
        return copy()
    }

    override fun copy(): CoordinateSequence {
        val cloneCoordinates = Array(size()) { i ->
            coordinates[i].clone() as MCoordinate
        }

        return MCoordinateSequence(cloneCoordinates)
    }

    override fun size(): Int {
        return coordinates.size
    }

    override fun toCoordinateArray(): Array<out Coordinate>? {
        return coordinates
    }

    override fun expandEnvelope(env: Envelope): Envelope {
        coordinates.forEach { env.expandToInclude(it) }
        return env
    }

    override fun toString(): String {
        val strBuf = StringBuffer()
        strBuf.append("MCoordinateSequence [")
        for (i in coordinates.indices) {
            if (i > 0) {
                strBuf.append(", ")
            }
            strBuf.append(coordinates[i])
        }
        strBuf.append("]")
        return strBuf.toString()
    }

    companion object {
        /**
         *
         */
        private const val serialVersionUID = 1L

        fun copy(coordinates: Array<Coordinate>): Array<MCoordinate> {
            return Array(coordinates.size) { i ->
                MCoordinate(coordinates[i])
            }
        }

        fun copy(coordSeq: CoordinateSequence): Array<MCoordinate> {
            return Array(coordSeq.size()) { i ->
                MCoordinate(coordSeq.getCoordinate(i))
            }
        }
    }
}
