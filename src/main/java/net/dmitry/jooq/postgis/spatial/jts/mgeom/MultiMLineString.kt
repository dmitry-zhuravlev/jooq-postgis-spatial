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
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.MultiLineString

class MultiMLineString
/**
 * @param MlineStrings the `MLineString`s for this
 * `MultiMLineString`, or an
 * empty array to create the empty geometry. Elements may be
 * empty `LineString`s, but not `null`s.
 */
constructor (MlineStrings: Array<MLineString> = arrayOf(), val mGap: Double // difference in m between end of one part and
 ,
 factory: GeometryFactory) : MultiLineString(MlineStrings, factory), MGeometry {

    override val factory: MGeometryFactory
        get() = super.factory as MGeometryFactory

    private var monotone = false

    private var strictMonotone = false

    init {
        determineMonotone()
    }

    /**
     * TODO Improve this, and add more unit tests
     */
    private fun determineMonotone() {
        this.monotone = true
        this.strictMonotone = true
        if (this.isEmpty) {
            return
        }
        var mdir = MGeometry.MeasureDirection.CONSTANT
        for (i in this.geometries.indices) {
            val ml = this.geometries[0] as MLineString
            if (!ml.isEmpty) {
                mdir = ml.measureDirection
                break
            }
        }
        for (i in this.geometries.indices) {
            val ml = this.geometries[i] as MLineString
            if (ml.isEmpty) {
                continue
            }
            // check whether mlinestrings are all pointing in same direction,
            // and
            // are monotone
            if (!ml.isMonotone(false) || ml.measureDirection != mdir && ml
                            .measureDirection != MGeometry.MeasureDirection.CONSTANT) {
                this.monotone = false
                break
            }

            if (!ml.isMonotone(true) || ml.measureDirection != mdir) {
                this.strictMonotone = false
                break
            }

            // check whether the geometry measures do not overlap or
            // are inconsistent with previous parts
            if (i > 0) {
                val mlp = this.geometries[i - 1] as MLineString
                if (mdir == MGeometry.MeasureDirection.INCREASING) {
                    if (mlp.maxM > ml.minM) {
                        monotone = false
                    } else if (mlp.maxM >= ml.minM) {
                        strictMonotone = false
                    }
                } else {
                    if (mlp.minM < ml.maxM) {
                        monotone = false
                    } else if (mlp.minM <= ml.maxM) {
                        strictMonotone = false
                    }
                }

            }

        }
        if (!monotone) {
            this.strictMonotone = false
        }

    }

    override fun geometryChangedAction() {
        determineMonotone()
    }

    override fun getGeometryType(): String {
        return "MultiMLineString"
    }

    @Throws(MGeometryException::class)
    override fun getMatCoordinate(co: Coordinate, tolerance: Double): Double {

        if (!this.isMonotone(false)) {
            throw MGeometryException.MonotoneRequiredException()
        }

        var mval = java.lang.Double.NaN
        var dist = java.lang.Double.POSITIVE_INFINITY

        val p = super.factory.createPoint(co)

        // find points within tolerance for getMatCoordinate
        for (i in 0 until this.numGeometries) {
            val ml = this.getGeometryN(i) as MLineString
            // go to next MLineString if the input point is beyond tolerance
            if (ml.distance(p) > tolerance) {
                continue
            }

            val mc = ml.getClosestPoint(co, tolerance)
            if (mc != null) {
                val d = mc.distance(co)
                if (d <= tolerance && d < dist) {
                    dist = d
                    mval = mc.m
                }
            }
        }
        return mval
    }

    override fun clone(): Any {
        return super.copy()
    }

    override fun measureOnLength(keepBeginMeasure: Boolean) {
        var startM = 0.0
        for (i in 0 until this.numGeometries) {
            val ml = this.getGeometryN(i) as MLineString
            if (i == 0) {
                ml.measureOnLength(keepBeginMeasure)
            } else {
                ml.measureOnLength(false)
            }
            if (startM != 0.0) {
                ml.shiftMeasure(startM)
            }
            startM += ml.length + mGap
        }
        this.geometryChanged()
    }

    /*
		  * (non-Javadoc)
		  *
		  * @see org.hibernate.spatial.mgeom.MGeometry#getCoordinateAtM(double)
		  */

    @Throws(MGeometryException::class)
    override fun getCoordinateAtM(m: Double): Coordinate? {

        if (!this.isMonotone(false)) {
            throw MGeometryException.MonotoneRequiredException()
        }

        var c: Coordinate?
        for (i in 0 until this.numGeometries) {
            val mg = this.getGeometryN(i) as MGeometry
            c = mg.getCoordinateAtM(m)
            if (c != null) {
                return c
            }
        }
        return null
    }

    @Throws(MGeometryException::class)
    override fun getCoordinatesBetween(begin: Double, end: Double): Array<out CoordinateSequence> {

        if (!this.isMonotone(false)) {
            throw MGeometryException.MonotoneRequiredException()
        }

        if (this.isEmpty) {
            return arrayOf()
        }

        val ar = java.util.ArrayList<CoordinateSequence>()

        for (i in 0 until this.numGeometries) {
            val ml = this.getGeometryN(i) as MLineString
            for (cs in ml.getCoordinatesBetween(begin, end)) {
                if (cs.size() > 0) {
                    ar.add(cs)
                }
            }
        }
        return ar.toTypedArray()
    }

    /*
		  * (non-Javadoc)
		  *
		  * @see org.hibernate.spatial.mgeom.MGeometry#getMinM()
		  */

    override val minM: Double
        get() {
            var minM = java.lang.Double.POSITIVE_INFINITY
            for (i in 0 until this.numGeometries) {
                val ml = this.getGeometryN(i) as MLineString
                val d = ml.minM
                if (d < minM) {
                    minM = d
                }
            }
            return minM
        }

    /*
		  * (non-Javadoc)
		  *
		  * @see org.hibernate.spatial.mgeom.MGeometry#getMaxM()
		  */

    override val maxM: Double
        get() {
            var maxM = java.lang.Double.NEGATIVE_INFINITY
            for (i in 0 until this.numGeometries) {
                val ml = this.getGeometryN(i) as MLineString
                val d = ml.maxM
                if (d > maxM) {
                    maxM = d
                }
            }
            return maxM
        }

    /*
		  * (non-Javadoc)
		  *
		  * @see org.hibernate.spatial.mgeom.MGeometry#isMonotone()
		  */

    override fun isMonotone(strictMonotone: Boolean): Boolean {
        return if (strictMonotone) this.strictMonotone else monotone
    }

    override fun asGeometry(): Geometry {
        return this
    }

    companion object {

        /**
         *
         */
        private val serialVersionUID = 1L
    }
}
