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

package org.jetbrains.jet.codegen.optimization.variables

import org.jetbrains.org.objectweb.asm.tree.analysis.Value
import org.jetbrains.org.objectweb.asm.tree.analysis.Interpreter
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.tree.AbstractInsnNode
import org.jetbrains.org.objectweb.asm.Opcodes
import org.jetbrains.jet.codegen.optimization.common.OptimizationBasicInterpreter
import org.jetbrains.org.objectweb.asm.tree.analysis.BasicValue
import org.jetbrains.org.objectweb.asm.tree.InsnList
import java.util.BitSet
import org.jetbrains.org.objectweb.asm.tree.VarInsnNode
import org.jetbrains.org.objectweb.asm.tree.IincInsnNode

class ReachingDefinitionsValue private (storedValue: BasicValue) : BasicValue(storedValue.getType()) {
    class object {
        fun createByDefinitionIndex(definitionIndex: Int, storedValue: BasicValue): ReachingDefinitionsValue {
            val result = ReachingDefinitionsValue(storedValue)
            result.definitionIndices.add(definitionIndex)
            return result
        }
    }

    val definitionIndices: MutableSet<Int> = hashSetOf()
    var basicValue = storedValue
    {
        if (basicValue is ReachingDefinitionsValue) {
            basicValue = (basicValue as ReachingDefinitionsValue).basicValue
        }
    }

    fun merge(other: ReachingDefinitionsValue): ReachingDefinitionsValue {
        definitionIndices.addAll(other.definitionIndices)
        return this
    }

    override fun equals(other: Any?): Boolean {
        if (other !is ReachingDefinitionsValue) return false
        return definitionIndices == other.definitionIndices && basicValue == other.basicValue
    }

    fun getVarIndex(insns: InsnList): Int {
        val insn = insns[definitionIndices.first()]
        return when (insn) {
            is VarInsnNode -> insn.`var`
            is IincInsnNode -> insn.`var`
            else -> throw AssertionError("Unexpected var operation: ${insn.javaClass}")
        }
    }
}

public class VariablesCopyPropagationInterpreter(val insns: InsnList) : OptimizationBasicInterpreter() {
    override fun copyOperation(insn: AbstractInsnNode, value: BasicValue): BasicValue? {
        if (insn.isStoreOperation()) {
            return ReachingDefinitionsValue.createByDefinitionIndex(insns.indexOf(insn), value)
        }
        return super.copyOperation(insn, value)
    }

    override fun unaryOperation(insn: AbstractInsnNode, value: BasicValue): BasicValue? {
        if (insn.getOpcode() == Opcodes.IINC) {
            return ReachingDefinitionsValue.createByDefinitionIndex(insns.indexOf(insn), value)
        }
        return super.unaryOperation(insn, value)
    }

    override fun merge(v: BasicValue, w: BasicValue): BasicValue {
        if (v is ReachingDefinitionsValue && w is ReachingDefinitionsValue) {
            return if (v == w) v else v.merge(w)
        }
        if (v is ReachingDefinitionsValue) {
            return super.merge(v.basicValue, w)
        }
        if (w is ReachingDefinitionsValue) {
            return super.merge(v, w.basicValue)
        }

        return super.merge(v, w)
    }
}
