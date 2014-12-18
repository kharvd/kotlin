/*
 * Copyright 2010-2014 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.jet.codegen.optimization.common

import org.jetbrains.org.objectweb.asm.tree.analysis.BasicValue
import org.jetbrains.org.objectweb.asm.Type
import kotlin.platform.platformStatic


class OptimizationBasicValue(asmType: Type?) : BasicValue(asmType) {
    class object {
        platformStatic val UNINITIALIZED_VALUE = OptimizationBasicValue(null)

        platformStatic val INT_VALUE = OptimizationBasicValue(Type.INT_TYPE)
        platformStatic val FLOAT_VALUE = OptimizationBasicValue(Type.FLOAT_TYPE)
        platformStatic val LONG_VALUE = OptimizationBasicValue(Type.LONG_TYPE)
        platformStatic val DOUBLE_VALUE = OptimizationBasicValue(Type.DOUBLE_TYPE)
        platformStatic val REFERENCE_VALUE = OptimizationBasicValue(Type.getObjectType("java/lang/Object"))

        platformStatic val BOOLEAN_VALUE = OptimizationBasicValue(Type.BOOLEAN_TYPE)
        platformStatic val CHAR_VALUE = OptimizationBasicValue(Type.CHAR_TYPE)
        platformStatic val BYTE_VALUE = OptimizationBasicValue(Type.BYTE_TYPE)
        platformStatic val SHORT_VALUE = OptimizationBasicValue(Type.SHORT_TYPE)
    }
    override fun equals(other: Any?): Boolean {
        if (other == null || javaClass != other.javaClass) {
            return false
        }
        return super.equals(other)
    }
}
