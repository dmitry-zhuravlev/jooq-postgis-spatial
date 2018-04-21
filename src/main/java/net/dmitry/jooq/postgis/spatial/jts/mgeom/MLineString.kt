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
import org.locationtech.jts.geom.CoordinateArrays
import org.locationtech.jts.geom.CoordinateSequence
import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.LineSegment
import org.locationtech.jts.geom.LineString

import java.util.ArrayList

/**
 * An implementation of the LineString class with the addition that the
 * containing CoordinateSequence can carry measure. Note that this is not a
 * strict requirement of the class, and can interact with non-measure geometries
 * for JTS topological comparisons regardless.
 *
 * @author Karel Maesen
 */
class MLineString(points: CoordinateSequence, factory: GeometryFactory) : LineString(points, factory), MGeometry {
    override val factory: MGeometryFactory
        get() = super.factory as MGeometryFactory

    private var monotone = false

    private var strictMonotone = false


    /**
     * determine the direction of the measures w.r.t. the direction of the line
     *
     * @return MGeometry.NON_MONOTONE<BR></BR>
     * MGeometry.INCREASING<BR></BR>
     * MGeometry.DECREASING<BR></BR>
     * MGeometry.CONSTANT
     */
    val measureDirection: MGeometry.MeasureDirection
        get() {
            if (!this.monotone) {
                return MGeometry.MeasureDirection.NON_MONOTONE
            }
            val c1 = this.getCoordinateN(0) as MCoordinate
            val c2 = this
                    .getCoordinateN(this.numPoints - 1) as MCoordinate

            return if (c1.m < c2.m) {
                MGeometry.MeasureDirection.INCREASING
            } else if (c1.m > c2.m) {
                MGeometry.MeasureDirection.DECREASING
            } else {
                MGeometry.MeasureDirection.CONSTANT
            }
        }

    /**
     * @return the array with measure-values of the vertices
     */
    // return the measures of all vertices
    val measures: DoubleArray?
        get() {
            return if (!this.isEmpty) {
                val co = this.coordinates
                val a = DoubleArray(co.size)
                for (i in co.indices) {
                    a[i] = (co[i] as MCoordinate).m
                }
                a
            } else {
                null
            }
        }

    /**
     * Returns the measure length of the segment. This method assumes that the
     * length of the LineString is defined by the absolute value of (last
     * coordinate - first coordinate) in the CoordinateSequence. If either
     * measure is not defined or the CoordinateSequence contains no coordinates,
     * then Double.NaN is returned. If there is only 1 element in the
     * CoordinateSequence, then 0 is returned.
     *
     * @return The measure length of the LineString
     */
    val mLength: Double
        get() {
            if (coordinateSequence.size() == 0) {
                return java.lang.Double.NaN
            }
            if (coordinateSequence.size() == 1) {
                return 0.0
            } else {
                val lastIndex = coordinateSequence.size() - 1
                val begin = coordinateSequence.getOrdinate(
                        0,
                        CoordinateSequence.M
                )
                val end = coordinateSequence.getOrdinate(
                        lastIndex,
                        CoordinateSequence.M
                )
                return if (java.lang.Double.isNaN(begin) || java.lang.Double.isNaN(end))
                    java.lang.Double.NaN
                else
                    Math.abs(end - begin)
            }
        }

    init {
        determineMonotone()
    }

    override fun clone(): Any {
        val ls = super.copy()
        return MLineString(ls.coordinateSequence, super.factory)
    }

    /**
     * Calculates whether the measures in the CoordinateSequence are monotone
     * and strict monotone. The strict parameter indicates whether the
     * determination should apply the definition of "strict monotonicity" or
     * non-strict.
     */
    private fun determineMonotone() {
        this.monotone = true
        this.strictMonotone = true
        if (!this.isEmpty) {
            val m = this.measures
            // short circuit if the first value is NaN
            if (java.lang.Double.isNaN(m!![0])) {
                this.monotone = false
                this.strictMonotone = false
            } else {
                var result = 0
                var prevResult = 0
                var i = 1
                while (i < m.size && this.monotone) {
                    result = java.lang.Double.compare(m[i - 1], m[i])
                    this.monotone = !(result * prevResult < 0 || java.lang.Double
                            .isNaN(m[i]))
                    this.strictMonotone = this.strictMonotone and (this.monotone && result != 0)
                    prevResult = result
                    i++
                }
            }
        }
        // if not monotone, then certainly not strictly monotone
        assert(!(this.strictMonotone && !this.monotone))
    }

    override fun geometryChangedAction() {
        determineMonotone()
    }

    /**
     * @param co        input coordinate in the neighbourhood of the MLineString
     * @param tolerance max. distance that co may be from this MLineString
     * @return an MCoordinate on this MLineString with appropriate M-value
     */
    @Throws(MGeometryException::class)
    fun getClosestPoint(co: Coordinate, tolerance: Double): MCoordinate? {
        if (!this.isMonotone(false)) {
            throw MGeometryException.MonotoneRequiredException()
        }

        if (!this.isEmpty) {
            val seg = LineSegment()
            val coAr = this.coordinates
            seg.p0 = coAr[0]
            var d: Double
            var projfact: Double
            var minDist = java.lang.Double.POSITIVE_INFINITY
            var mincp: MCoordinate? = null
            for (i in 1 until coAr.size) {
                seg.p1 = coAr[i]
                val cp = seg.closestPoint(co)
                d = cp.distance(co)
                if (d <= tolerance && d <= minDist) {
                    val testcp = MCoordinate(cp)
                    projfact = seg.projectionFactor(cp)
                    testcp.m = (coAr[i - 1] as MCoordinate).m + projfact * ((coAr[i] as MCoordinate).m - (coAr[i - 1] as MCoordinate).m)
                    if (d < minDist || testcp.m < mincp!!.m) {
                        mincp = testcp
                        minDist = d
                    }
                }
                seg.p0 = seg.p1
            }
            return if (minDist > tolerance) {
                null
            } else {
                mincp
            }
        } else {
            return null
        }
    }

    /*
		  * (non-Javadoc)
		  *
		  * @see org.hibernatespatial.mgeom.MGeometry#getCoordinateAtM(double)
		  */

    @Throws(MGeometryException::class)
    override fun getCoordinateAtM(m: Double): Coordinate? {
        if (!this.isMonotone(false)) {
            throw MGeometryException.MonotoneRequiredException()
        }
        if (this.isEmpty) {
            return null
        } else {
            val mval = this.measures
            val lb = minM
            val up = maxM

            if (m < lb || m > up) {
                return null
            } else {
                // determine linesegment that contains m;
                for (i in 1 until mval!!.size) {
                    if (mval[i - 1] <= m && m <= mval[i] || mval[i] <= m && m <= mval[i - 1]) {
                        val p0 = this
                                .getCoordinateN(i - 1) as MCoordinate
                        val p1 = this.getCoordinateN(i) as MCoordinate
                        // r indicates how far in this segment the M-values lies
                        val r = (m - mval[i - 1]) / (mval[i] - mval[i - 1])
                        val dx = r * (p1.x - p0.x)
                        val dy = r * (p1.y - p0.y)
                        val dz = r * (p1.z - p0.z)
                        return MCoordinate(
                                p0.x + dx, p0.y + dy,
                                p0.z + dz, m
                        )
                    }
                }
            }
        }
        return null
    }

    /*
		  * (non-Javadoc)
		  *
		  * @see org.locationtech.jts.geom.Geometry#getGeometryType()
		  */

    override fun getGeometryType(): String {
        return "MLineString"
    }

    /*
		  * (non-Javadoc)
		  *
		  * @see org.locationtech.jts.geom.Geometry#getMatCoordinate(org.locationtech.jts.geom.Coordinate,
		  *      double)
		  */

    @Throws(MGeometryException::class)
    override fun getMatCoordinate(c: Coordinate, tolerance: Double): Double {
        val mco = this.getClosestPoint(c, tolerance)
        return mco?.m ?: java.lang.Double.NaN
    }

    /**
     * get the measure of the specified coordinate
     *
     * @param n index of the coordinate
     * @return The measure of the coordinate. If the coordinate does not exists
     * it returns Double.NaN
     */
    fun getMatN(n: Int): Double {
        return (this.coordinates[n] as MCoordinate).m
    }

    /*
		  * (non-Javadoc)
		  *
		  * @see org.hibernate.spatial.mgeom.MGeometry##MGeometry#getMaxM()
		  */

    override val maxM: Double
        get() {
            if (this.isEmpty) {
                return java.lang.Double.NaN
            } else {
                val measures = this.measures

                return if (this.measureDirection == MGeometry.MeasureDirection.INCREASING) {
                    measures!![measures.size - 1]
                } else if (this.measureDirection == MGeometry.MeasureDirection.DECREASING || this.measureDirection == MGeometry.MeasureDirection.CONSTANT) {
                    measures!![0]
                } else {
                    var ma = java.lang.Double.NEGATIVE_INFINITY
                    for (i in measures!!.indices) {
                        if (ma < measures[i]) {
                            ma = measures[i]
                        }
                    }
                    ma
                }
            }
        }


    /**
     * Copies the coordinates of the specified array that fall between fromM and toM to a CoordinateSubSequence.
     *
     *
     * The CoordinateSubSequence also contains the array indices of the first and last coordinate in firstIndex, resp.
     * lastIndex. If there are no coordinates between fromM and toM, then firstIndex will contain -1, and lastIndex
     * will point to the coordinate that is close to fromM or toM.
     *
     *
     * This function expects that fromM is less than or equal to toM, and that the coordinates in the array are
     * sorted monotonic w.r.t. to their m-values.
     *
     * @param mcoordinates
     * @param fromM
     * @param toM
     * @param direction    INCREASING or DECREASING
     * @return a CoordinateSubSequence containing the coordinates between fromM and toM
     */
    private fun copyCoordinatesBetween(mcoordinates: Array<MCoordinate>, fromM: Double, toM: Double, direction: MGeometry.MeasureDirection): CoordinateSubSequence {
        val sseq = CoordinateSubSequence()
        sseq.firstIndex = -1
        sseq.lastIndex = -1
        for (i in mcoordinates.indices) {
            val m = mcoordinates[i].m

            if (m in fromM..toM) {
                sseq.vertices.add(mcoordinates[i])
                if (sseq.firstIndex == -1) {
                    sseq.firstIndex = i
                }
            }
            if (direction == MGeometry.MeasureDirection.INCREASING) {
                if (m > toM) {
                    break
                }
                sseq.lastIndex = i
            } else {
                if (m < fromM) {
                    break
                }
                sseq.lastIndex = i
            }

        }
        return sseq
    }

    /**
     * Interpolates a coordinate between mco1, mco2, based on the measured value m
     */
    private fun interpolate(mco1: MCoordinate, mco2: MCoordinate, m: Double): MCoordinate {
        var mco1 = mco1
        var mco2 = mco2
        if (mco1.m > mco2.m) {
            val h = mco1
            mco1 = mco2
            mco2 = h
        }

        if (m < mco1.m || m > mco2.m) {
            throw IllegalArgumentException("Internal Error: m-value not in interval mco1.m/mco2.m")
        }

        val r = (m - mco1.m) / (mco2.m - mco1.m)
        val interpolated = MCoordinate(
                mco1.x + r * (mco2.x - mco1.x),
                mco1.y + r * (mco2.y - mco1.y),
                mco1.z + r * (mco2.z - mco1.z),
                m
        )
        this.precisionModel.makePrecise(interpolated)
        return interpolated
    }


    @Throws(MGeometryException::class)
    override fun getCoordinatesBetween(fromM: Double, toM: Double): Array<out CoordinateSequence> {
        if (!this.isMonotone(false)) {
            throw MGeometryException.MonotoneRequiredException()
        }

        if (fromM > toM) {
            return getCoordinatesBetween(toM, fromM)
        }

        val mc: MCoordinateSequence
        if (!isOverlapping(fromM, toM)) {
            mc = MCoordinateSequence(arrayOf<MCoordinate>())
        } else {
            val mcoordinates = this.coordinates as Array<MCoordinate>
            val subsequence = copyCoordinatesBetween(
                    mcoordinates,
                    fromM,
                    toM,
                    this.measureDirection
            )
            addInterpolatedEndPoints(fromM, toM, mcoordinates, subsequence)
            val ra = subsequence.vertices.toTypedArray()
            mc = MCoordinateSequence(ra)
        }
        return arrayOf(mc)
    }

    private fun isOverlapping(fromM: Double, toM: Double): Boolean {
        if (this.isEmpty) {
            return false
        }
        //WARNING: this assumes a monotonic increasing or decreasing measures
        val beginCo = this.getCoordinateN(0) as MCoordinate
        val endCo = this.getCoordinateN(this.numPoints - 1) as MCoordinate
        return !(Math.min(fromM, toM) > Math.max(beginCo.m, endCo.m) || Math.max(fromM, toM) < Math.min(beginCo.m, endCo.m))
    }

    private fun addInterpolatedEndPoints(fromM: Double, toM: Double, mcoordinates: Array<MCoordinate>, subsequence: CoordinateSubSequence) {

        val increasing = this.measureDirection == MGeometry.MeasureDirection.INCREASING
        val fM: Double
        val lM: Double
        if (increasing) {
            fM = fromM
            lM = toM
        } else {
            fM = toM
            lM = fromM
        }

        if (subsequence.firstIndex == -1) {
            val fi = interpolate(
                    mcoordinates[subsequence.lastIndex],
                    mcoordinates[subsequence.lastIndex + 1],
                    fM
            )
            subsequence.vertices.add(fi)
            val li = interpolate(
                    mcoordinates[subsequence.lastIndex],
                    mcoordinates[subsequence.lastIndex + 1],
                    lM
            )
            subsequence.vertices.add(li)
        } else {
            //interpolate a first vertex if necessary
            if (subsequence.firstIndex > 0 && (increasing && mcoordinates[subsequence.firstIndex].m > fromM || !increasing && mcoordinates[subsequence.firstIndex].m < toM)) {
                val fi = interpolate(
                        mcoordinates[subsequence.firstIndex - 1],
                        mcoordinates[subsequence.firstIndex],
                        fM
                )
                subsequence.vertices.add(0, fi)
            }
            //interpolate a last vertex if necessary
            if (subsequence.lastIndex < mcoordinates.size - 1 && (increasing && mcoordinates[subsequence.lastIndex].m < toM || !increasing && mcoordinates[subsequence.lastIndex].m > fromM)) {
                val li = interpolate(
                        mcoordinates[subsequence.lastIndex],
                        mcoordinates[subsequence.lastIndex + 1],
                        lM
                )
                subsequence.vertices.add(li)
            }
        }
    }

    private fun inverse(mcoordinates: Array<MCoordinate>): Array<MCoordinate> {
        for (i in 0 until mcoordinates.size / 2) {
            val h = mcoordinates[i]
            mcoordinates[i] = mcoordinates[mcoordinates.size - 1 - i]
            mcoordinates[mcoordinates.size - 1 - i] = h
        }
        return mcoordinates
    }

    override val minM: Double
        get() {

            if (this.isEmpty) {
                return java.lang.Double.NaN
            } else {
                val a = this.measures
                if (this.measureDirection == MGeometry.MeasureDirection.INCREASING) {
                    return a!![0]
                } else if (this.measureDirection == MGeometry.MeasureDirection.DECREASING || this.measureDirection == MGeometry.MeasureDirection.CONSTANT) {
                    return a!![a.size - 1]
                } else {

                    var ma = java.lang.Double.POSITIVE_INFINITY
                    for (i in a!!.indices) {
                        if (ma > a[i]) {
                            ma = a[i]
                        }
                    }
                    return ma
                }
            }
        }

    /**
     * Assigns the first coordinate in the CoordinateSequence to the
     * `beginMeasure` and the last coordinate in the
     * CoordinateSequence to the `endMeasure`. Measure values for
     * intermediate coordinates are then interpolated proportionally based on
     * their 2d offset of the overall 2d length of the LineString.
     *
     *
     * If the beginMeasure and endMeasure values are equal it is assumed that
     * all intermediate coordinates shall be the same value.
     *
     * @param beginMeasure Measure value for first coordinate
     * @param endMeasure   Measure value for last coordinate
     */
    fun interpolate(beginMeasure: Double, endMeasure: Double) {
        if (this.isEmpty) {
            return
        }
        // interpolate with first vertex = beginMeasure; last vertex =
        // endMeasure
        val coordinates = this.coordinates
        val length = this.length
        val mLength = endMeasure - beginMeasure
        var d = 0.0
        val continuous = DoubleComparator.equals(beginMeasure, endMeasure)
        var m = beginMeasure
        var prevCoord = MCoordinate.convertCoordinate(coordinates[0])
        prevCoord!!.m = m
        var curCoord: MCoordinate?
        for (i in 1 until coordinates.size) {
            curCoord = MCoordinate.convertCoordinate(coordinates[i])
            if (continuous) {
                curCoord!!.m = beginMeasure
            } else {
                d += curCoord!!.distance(prevCoord!!)
                m = beginMeasure + d / length * mLength
                curCoord.m = m
                prevCoord = curCoord
            }
        }
        this.geometryChanged()
        assert(this.isMonotone(false)) { "interpolate function should always leave MGeometry monotone" }
    }

    /**
     * Indicates whether the MLineString has monotone increasing or decreasing
     * M-values
     *
     * @return `true if MLineString is empty or M-values are increasing (NaN) values, false otherwise`
     */
    override fun isMonotone(strict: Boolean): Boolean {
        return if (strict) this.strictMonotone else this.monotone
    }

    override fun asGeometry(): Geometry {
        return this
    }

    // TODO get clear on function and implications of normalize
    // public void normalize(){
    //
    // }

    override fun measureOnLength(keepBeginMeasure: Boolean) {

        val co = this.coordinates
        if (!this.isEmpty) {
            var d = 0.0
            var pco = co[0] as MCoordinate
            if (!keepBeginMeasure || java.lang.Double.isNaN(pco.m)) {
                pco.m = 0.0
            }
            var mco: MCoordinate
            for (i in 1 until co.size) {
                mco = co[i] as MCoordinate
                d += mco.distance(pco)
                mco.m = d
                pco = mco
            }
            this.geometryChanged()
        }
    }

    /**
     * This method reverses the measures assigned to the Coordinates in the
     * CoordinateSequence without modifying the positional (x,y,z) values.
     */
    fun reverseMeasures() {
        if (!this.isEmpty) {
            val m = this.measures
            val coar = this.coordinates as Array<MCoordinate>
            var nv: Double
            for (i in m!!.indices) {
                nv = m[m.size - 1 - i]
                coar[i].m = nv
            }
            this.geometryChanged()
        }
    }

    fun setMeasureAtIndex(index: Int, m: Double) {
        coordinateSequence.setOrdinate(index, CoordinateSequence.M, m)
        this.geometryChanged()
    }

    /**
     * Shift all measures by the amount parameter. A negative amount shall
     * subtract the amount from the measure. Note that this can make for
     * negative measures.
     *
     * @param amount the positive or negative amount by which to shift the measures
     * in the CoordinateSequence.
     */
    fun shiftMeasure(amount: Double) {
        val coordinates = this.coordinates
        var mco: MCoordinate
        if (!this.isEmpty) {
            for (i in coordinates.indices) {
                mco = coordinates[i] as MCoordinate
                mco.m = mco.m + amount
            }
        }
        this.geometryChanged()
    }

    /*
		  * (non-Javadoc)
		  *
		  * @see java.lang.Object#toString()
		  */

    override fun toString(): String {
        val ar = this.coordinates
        val buf = StringBuffer(ar.size * 17 * 3)
        for (i in ar.indices) {
            buf.append(ar[i].x)
            buf.append(" ")
            buf.append(ar[i].y)
            buf.append(" ")
            buf.append((ar[i] as MCoordinate).m)
            buf.append("\n")
        }
        return buf.toString()
    }

    @Throws(MGeometryException::class)
    fun unionM(l: MLineString): MLineString {

        if (!this.monotone || !l.monotone) {
            throw MGeometryException.MonotoneRequiredException()
        }
        val linecoar = l.coordinates
        if (l.measureDirection == MGeometry.MeasureDirection.DECREASING) {
            CoordinateArrays.reverse(linecoar)
        }
        val thiscoar = this.coordinates
        if (this.measureDirection == MGeometry.MeasureDirection.DECREASING) {
            CoordinateArrays.reverse(thiscoar)
        }

        // either the last coordinate in thiscoar equals the first in linecoar;
        // or the last in linecoar equals the first in thiscoar;
        val lasttco = thiscoar[thiscoar.size - 1] as MCoordinate
        val firsttco = thiscoar[0] as MCoordinate
        val lastlco = linecoar[linecoar.size - 1] as MCoordinate
        val firstlco = linecoar[0] as MCoordinate

        val newcoar = arrayOfNulls<MCoordinate>(thiscoar.size + linecoar.size - 1)
        if (lasttco.equals2D(firstlco) && DoubleComparator.equals(lasttco.m, firstlco.m)) {
            System.arraycopy(thiscoar, 0, newcoar, 0, thiscoar.size)
            System.arraycopy(
                    linecoar, 1, newcoar, thiscoar.size,
                    linecoar.size - 1
            )
        } else if (lastlco.equals2D(firsttco) && DoubleComparator.equals(lastlco.m, firsttco.m)) {
            System.arraycopy(linecoar, 0, newcoar, 0, linecoar.size)
            System.arraycopy(
                    thiscoar, 1, newcoar, linecoar.size,
                    thiscoar.size - 1
            )
        } else {
            throw MGeometryException.UnionOnDisjointLineStrException()
        }

        val mcs = super.factory
                .coordinateSequenceFactory.create(newcoar)
        val returnmlinestring = MLineString(mcs, super.factory)
        assert(returnmlinestring.isMonotone(false)) { "new unionM-ed MLineString is not monotone" }
        return returnmlinestring
    }

    internal class CoordinateSubSequence {
        var firstIndex: Int = 0
        var lastIndex: Int = 0
        var vertices = ArrayList<MCoordinate>()
    }

    companion object {

        /**
         *
         */
        private val serialVersionUID = 1L
    }
}
