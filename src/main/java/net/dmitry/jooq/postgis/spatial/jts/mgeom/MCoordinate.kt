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

/**
 * This coordinate class supports 4D coordinates, where the first 3 measures
 * (x,y,z) are coordinates in a 3 dimensional space (cartesian for example), and
 * the fourth is a measure value used for linear referencing. Note that the
 * measure value is independent of whether the (x,y,z) values are used. For
 * example, the z value can not be used while the measure value is used.
 *
 *
 * While this class extends the Coordinate class, it can be used seamlessly as a
 * substitute in the event that the Measure value is not used. In these cases
 * the Measure value shall simply be Double.NaN
 *
 * @see org.locationtech.jts.geom.Coordinate
 */
class MCoordinate : Coordinate {

    var m: Double = 0.toDouble()

    /**
     * Default constructor
     */
    constructor() : super() {
        this.m = java.lang.Double.NaN
    }

    constructor(x: Double, y: Double, z: Double, m: Double) : super(x, y, z) {
        this.m = m
    }

    constructor(x: Double, y: Double) : super(x, y) {
        m = java.lang.Double.NaN
    }

    constructor(coord: Coordinate) : super(coord) {
        m = if (coord is MCoordinate) {
            coord.m
        } else {
            java.lang.Double.NaN
        }
    }

    constructor(coord: MCoordinate) : super(coord) {
        m = coord.m
    }

    fun equals2DWithMeasure(other: Coordinate): Boolean {
        var result = this.equals2D(other)
        if (result) {
            val mc = convertCoordinate(other)
            result = java.lang.Double.compare(this.m, mc!!.m) == 0
        }
        return result
    }

    fun equals3DWithMeasure(other: Coordinate): Boolean {
        var result = this.equals3D(other)
        if (result) {
            val mc = convertCoordinate(other)
            result = java.lang.Double.compare(this.m, mc!!.m) == 0
        }
        return result
    }

    /*
	 * Default equality is now equality in 2D-plane. This is required to remain
	 * consistent with JTS.
	 *
	 * TODO:check whether this method is still needed.
	 *
	 * (non-Javadoc)
	 *
	 * @see org.locationtech.jts.geom.Coordinate#equals(java.lang.Object)
	 */
    override fun equals(other: Any?): Boolean {
        return if (other is Coordinate) {
            equals2D((other as Coordinate?)!!)
        } else {
            false
        }
    }

    override fun toString(): String {
        return "($x,$y,$z, m=$m)"
    }

    companion object {
        /**
         *
         */
        private val serialVersionUID = 1L

        /**
         * Converts a standard Coordinate instance to an MCoordinate instance. If
         * coordinate is already an instance of an MCoordinate, then it is simply
         * returned. In cases where it is converted, the measure value of the
         * coordinate is initialized to Double.NaN.
         *
         * @param coordinate The coordinate to be converted
         * @return an instance of MCoordinate corresponding to the
         * `coordinate` parameter
         */
        fun convertCoordinate(coordinate: Coordinate?): MCoordinate? {
            return if (coordinate == null) null else MCoordinate(coordinate)
        }

        /**
         * A convenience method for creating a MCoordinate instance where there are
         * only 2 coordinates and an lrs measure value. The z value of the
         * coordinate shall be set to Double.NaN
         *
         * @param x the x coordinate value
         * @param y the y coordinate value
         * @param m the lrs measure value
         * @return The constructed MCoordinate value
         */
        fun create2dWithMeasure(x: Double, y: Double, m: Double): MCoordinate {
            return MCoordinate(x, y, java.lang.Double.NaN, m)
        }

        /**
         * A convenience method for creating a MCoordinate instance where there are
         * only 2 coordinates and an lrs measure value. The z and m value of the
         * coordinate shall be set to Double.NaN
         *
         * @param x the x coordinate value
         * @param y the y coordinate value
         * @return The constructed MCoordinate value
         */
        fun create2d(x: Double, y: Double): MCoordinate {
            return MCoordinate(x, y, java.lang.Double.NaN, java.lang.Double.NaN)
        }

        /**
         * A convenience method for creating a MCoordinate instance where there are
         * 3 coordinates and an lrs measure value.
         *
         * @param x the x coordinate value
         * @param y the y coordinate value
         * @param z the z coordinate value
         * @param m the lrs measure value
         * @return The constructed MCoordinate value
         */
        fun create3dWithMeasure(x: Double, y: Double, z: Double,
                                m: Double): MCoordinate {
            return MCoordinate(x, y, z, m)
        }

        /**
         * A convenience method for creating a MCoordinate instance where there are
         * 3 coordinates but no lrs measure value. The m value of the coordinate
         * shall be set to Double.NaN
         *
         * @param x the x coordinate value
         * @param y the y coordinate value
         * @param z the z coordinate value
         * @return The constructed MCoordinate value
         */
        fun create3d(x: Double, y: Double, z: Double): MCoordinate {
            return MCoordinate(x, y, z, java.lang.Double.NaN)
        }
    }
}
