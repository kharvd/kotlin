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
import org.jetbrains.org.objectweb.asm.tree.analysis.Frame
import org.jetbrains.org.objectweb.asm.tree.analysis.BasicValue
import org.jetbrains.org.objectweb.asm.tree.InsnList
import org.jetbrains.org.objectweb.asm.tree.VarInsnNode
import org.jetbrains.org.objectweb.asm.Opcodes

private class VariablesCopyPropagationResolver(val frames: Array<Frame<BasicValue?>?>, val insns: InsnList) {
    // defined only for instruction indices where variable definition happens
    // if insns[i] is STORE of variable j, then finalDefinitionVarIndex[i] will contain
    // propagated definition index that may be used instead of usages of i'th definition
    // Example
    // 0: ICONST_0
    // 1: ISTORE_0
    // 2: ILOAD_0
    // 3: ISTORE_1
    // 4: ILOAD_1
    // 5: ISTORE_2
    // result will be: {-1, 1, -1, 1, -1, 1}
    // So any further usages of variables 0, 1 or 2 they may be replaced by definition having index 1
    // if it's reaching for them
    private val propagatedReachingDefinitions = arrayOfNulls<ReachingDefinitionsValue>(frames.size())

    fun getPropagatedDefinitions(definitions: ReachingDefinitionsValue): ReachingDefinitionsValue {
        if (definitions.definitionIndices.size() > 1) return definitions
        var definitionIndex = definitions.definitionIndices.first()
        if (propagatedReachingDefinitions[definitionIndex] != null) return propagatedReachingDefinitions[definitionIndex]!!

        propagatedReachingDefinitions[definitionIndex] = definitions

        if (insns[definitionIndex].getOpcode() == Opcodes.IINC) return definitions

        var currentDefinitionIndex = definitionIndex
        val storedValue = frames[currentDefinitionIndex]?.getStackTop() ?: return definitions

        if (storedValue !is ReachingDefinitionsValue) {
            return definitions
        }

        val result = getPropagatedDefinitions(storedValue)
        propagatedReachingDefinitions[definitionIndex] = result
        return result
    }
}

class VariablesCopyPropagationMethodTransformer : MethodTransformer() {
    override fun transform(internalClassName: String, methodNode: MethodNode) {
        val frames = MethodTransformer.analyze(
                internalClassName, methodNode, VariablesCopyPropagationInterpreter(methodNode.instructions)
        )

        val varsDefinitionResolver = VariablesCopyPropagationResolver(frames, methodNode.instructions)
        val insnsArray = methodNode.instructions.toArray()

        val insnsToReplace = arrayListOf<Pair<VarInsnNode, VarInsnNode>>()

        for ((insn, frame) in insnsArray.zip(frames)) {
            if (insn.isLoadOperation() && frame != null) {
                val varIndex = (insn as VarInsnNode).`var`
                val storedValue = frame.getLocal(varIndex)
                if (storedValue !is ReachingDefinitionsValue) continue

                val propagatedDefinitions = varsDefinitionResolver.getPropagatedDefinitions(storedValue)
                val candidateVarIndex = propagatedDefinitions.getVarIndex(methodNode.instructions)
                val candidateReachingValue = frame.getLocal(candidateVarIndex)

                // if we still have the same reaching definition as propagated
                if (candidateVarIndex != varIndex && candidateReachingValue is ReachingDefinitionsValue &&
                    candidateReachingValue == propagatedDefinitions) {
                    insnsToReplace.add(Pair(insn, VarInsnNode(insn.getOpcode(), candidateVarIndex)))
                }
            }
        }

        for ((from, to) in insnsToReplace) {
            methodNode.instructions.set(from, to)
        }

    }
}
