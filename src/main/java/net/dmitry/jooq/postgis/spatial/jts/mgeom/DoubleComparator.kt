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

/**
 * This utility class is used to testsuite-suite doubles for equality
 *
 * @author Didier H. Besset
 *
 * Adapted from "Object-oriented implementation of
 * numerical methods"
 */
//TODO: This class should be removed.
object DoubleComparator {

    private val radix : Int
        get() {
            var radix = 0
            var a = 1.0
            var tmp1: Double
            var tmp2: Double
            do {
                a += a
                tmp1 = a + 1.0
                tmp2 = tmp1 - a
            } while (tmp2 - 1.0 != 0.0)
            var b = 1.0
            while (radix == 0) {
                b += b
                tmp1 = a + b
                radix = (tmp1 - a).toInt()
            }
            return radix
        }

    private val machinePrecision : Double
        get() {
            val floatingRadix = radix.toDouble()
            val inverseRadix = 1.0 / floatingRadix
            var machinePrecision = 1.0
            var tmp = 1.0 + machinePrecision
            while (tmp - 1.0 != 0.0) {
                machinePrecision *= inverseRadix
                tmp = 1.0 + machinePrecision
            }
            return machinePrecision
        }

    private val defaultNumericalPrecision = Math
            .sqrt(machinePrecision)

    @JvmOverloads
    fun equals(a: Double, b: Double, precision: Double = defaultNumericalPrecision): Boolean {
        val norm = Math.max(Math.abs(a), Math.abs(b))
        val result = norm < precision || Math.abs(a - b) < precision * norm
        return result || java.lang.Double.isNaN(a) && java.lang.Double.isNaN(b)
    }

    @JvmStatic
    fun main(args: Array<String>) {
        println("Machine precision = $machinePrecision")
        println("Radix = $radix")
        println(
                "default numerical precision = $defaultNumericalPrecision"
        )
    }
}
