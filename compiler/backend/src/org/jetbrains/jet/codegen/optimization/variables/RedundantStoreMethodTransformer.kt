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

import org.jetbrains.jet.codegen.optimization.transformer.MethodTransformer
import org.jetbrains.org.objectweb.asm.tree.MethodNode
import org.jetbrains.org.objectweb.asm.tree.VarInsnNode
import org.jetbrains.jet.codegen.optimization.common.isStoreOperation
import org.jetbrains.org.objectweb.asm.tree.InsnNode
import org.jetbrains.org.objectweb.asm.tree.AbstractInsnNode
import org.jetbrains.org.objectweb.asm.Opcodes
import org.jetbrains.jet.codegen.optimization.common.isLoadOperation
import org.jetbrains.org.objectweb.asm.tree.analysis.BasicValue
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.jet.codegen.optimization.common.OptimizationBasicInterpreter
import org.jetbrains.jet.codegen.optimization.common.getStackTop
import org.jetbrains.org.objectweb.asm.tree.analysis.Frame
import org.jetbrains.jet.codegen.optimization.common.InsnStream
import org.jetbrains.org.objectweb.asm.tree.JumpInsnNode
import org.jetbrains.org.objectweb.asm.tree.LabelNode
import org.jetbrains.org.objectweb.asm.tree.InsnList

public class RedundantStoreMethodTransformer : MethodTransformer() {
    override fun transform(internalClassName: String, methodNode: MethodNode) {
        val livenessFrames = analyzeLiveness(methodNode)
        val loadedFrames: Array<out Frame<BasicValue>>? = MethodTransformer.analyze(internalClassName, methodNode, LoadedValueInterpreter())
        val insnList = methodNode.instructions
        val insnsArray = insnList.toArray()

        for (index in insnsArray.indices) {
            val insn = insnsArray[index]
            if (insn.isStoreOperation() && insn is VarInsnNode) { 
                if (!livenessFrames[index + 1].isAlive(insn.`var`)) {
                    val storedValue = loadedFrames?.get(index)?.getStackTop()
                    if (storedValue is LoadedValue && isSafeToRemoveLoad(insn, storedValue, insnList)) {
                        insnList.remove(storedValue.dupInsnOrThis)
                        insnList.remove(insn)
                    }
                    else {
                        insnList.set(insn, insn.getAppropriatePopOperation())
                    }
                }
                else if (index + 2 < insnsArray.size() && !livenessFrames[index + 2].isAlive(insn.`var`) &&
                         insn.getNext().isLoadOperation()) {
                    // ASTORE 0
                    // ALOAD 0
                    // 0 is dead after
                    insnList.remove(insn.getNext())
                    insnList.remove(insn)
                }
            }
        }
    }
    
    fun isSafeToRemoveLoad(insn: AbstractInsnNode, loaded: LoadedValue, insnList: InsnList): Boolean {
        if (insnList.indexOf(loaded.insn) >= insnList.indexOf(insn)) return false
        return !InsnStream(loaded.insn, insn).any { it is JumpInsnNode || it is LabelNode }
    }
}

private class LoadedValueInterpreter : OptimizationBasicInterpreter() {
    override fun copyOperation(insn: AbstractInsnNode, value: BasicValue?): BasicValue? {
        if (value != null) {
            if (insn.isLoadOperation()) {
                return LoadedValue(insn, value)
            }
            if (insn.getOpcode() == Opcodes.DUP || insn.getOpcode() == Opcodes.DUP2) {
                (value as? LoadedValue)?.dupInsn = insn
                return LoadedValue(insn, value)
            }
        }

        return super.copyOperation(insn, value)
    }

    override fun merge(v: BasicValue, w: BasicValue): BasicValue {
        if (v is LoadedValue && v == w) {
           return v.merge(w as LoadedValue)
        }
        
        if (v is LoadedValue) {
            return v.basicValue
        }
        
        if (w is LoadedValue) {
            return w.basicValue
        }
        
        return super.merge(v, w)
    }
}

private class LoadedValue(val insn: AbstractInsnNode, val wrappedValue: BasicValue) : BasicValue(wrappedValue.getType()) {
    var dupInsn: AbstractInsnNode? = null
    val basicValue: BasicValue get() = (wrappedValue as? LoadedValue)?.basicValue ?: wrappedValue
    
    val dupInsnOrThis: AbstractInsnNode get() = dupInsn ?: insn

    override fun equals(other: Any?): Boolean {
        if (other !is LoadedValue) return false
        return insn == other.insn
    }
    
    fun merge(w: LoadedValue): LoadedValue {
        if (dupInsn == null) {
            dupInsn = w.dupInsn
        }
        
        if (w.dupInsn == null) {
            w.dupInsn = dupInsn
        }
        
        return this
    }
}

private fun VarInsnNode.getAppropriatePopOperation() =
    InsnNode(
            when (getOpcode()) {
                Opcodes.LSTORE, Opcodes.DSTORE -> Opcodes.POP2
                else -> Opcodes.POP
            }
    )
