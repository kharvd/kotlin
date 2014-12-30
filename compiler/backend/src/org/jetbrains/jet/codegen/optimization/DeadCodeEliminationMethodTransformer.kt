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

package org.jetbrains.jet.codegen.optimization

import org.jetbrains.jet.codegen.optimization.transformer.MethodTransformer
import org.jetbrains.org.objectweb.asm.tree.MethodNode
import org.jetbrains.jet.codegen.optimization.common.OptimizationBasicInterpreter
import org.jetbrains.org.objectweb.asm.tree.LabelNode
import org.jetbrains.jet.codegen.optimization.common.isMeaningful
import org.jetbrains.jet.codegen.optimization.common.InsnStream
import org.jetbrains.org.objectweb.asm.Opcodes
import org.jetbrains.org.objectweb.asm.tree.AbstractInsnNode


public class DeadCodeEliminationMethodTransformer : MethodTransformer() {
    override fun transform(internalClassName: String, methodNode: MethodNode) {
        val frames = MethodTransformer.analyze(internalClassName, methodNode, OptimizationBasicInterpreter())
        val insnList = methodNode.instructions
        val insnsArray = insnList.toArray()
        
        insnsArray.zip(frames).filter {
            it.second == null && it.first.isMeaningful
        }.forEach { insnList.remove(it.first) }
    }
}
