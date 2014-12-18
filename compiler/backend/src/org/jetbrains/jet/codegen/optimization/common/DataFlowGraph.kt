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

import org.jetbrains.org.objectweb.asm.tree.InsnList
import java.util.ArrayList
import kotlin.platform.platformStatic
import org.jetbrains.org.objectweb.asm.tree.MethodNode
import org.jetbrains.org.objectweb.asm.tree.TryCatchBlockNode
import org.jetbrains.org.objectweb.asm.tree.LabelNode
import org.jetbrains.org.objectweb.asm.tree.JumpInsnNode
import org.jetbrains.org.objectweb.asm.Opcodes
import org.jetbrains.org.objectweb.asm.tree.AbstractInsnNode
import org.jetbrains.org.objectweb.asm.tree.LookupSwitchInsnNode
import org.jetbrains.org.objectweb.asm.tree.TableSwitchInsnNode


class DataFlowGraph private (private val insns: InsnList) {
     val edges: Array<MutableList<Int>> = Array(insns.size()) { arrayListOf<Int>() }
     
     class object {
          platformStatic fun build(node: MethodNode): DataFlowGraph {
               val graph = DataFlowGraph(node.instructions)
               val exceptionHandlers = buildExceptionHandlers(node)
               
               val insns = node.instructions
               
               for (i in 0..insns.size() - 1) {
                    val insn = insns.get(i)
                    if (insn.isReturnOperation()) continue
                    val opcode = insn.getOpcode()
                    val next: AbstractInsnNode? = insn.getNext()
                    
                    if (insn is JumpInsnNode) {
                         if (opcode != Opcodes.GOTO) {
                              graph.addEdge(insn, next)
                         }
                         graph.addEdge(insn, insn.label)
                    }
                    else if (insn is LookupSwitchInsnNode) {
                         graph.addEdge(insn, insn.dflt)
                         for (label in insn.labels) {
                              graph.addEdge(insn, label)
                         }
                    }
                    else if (insn is TableSwitchInsnNode) {
                         graph.addEdge(insn, insn.dflt)
                         for (label in insn.labels) {
                              graph.addEdge(insn, label)
                         }
                    }
                    else if (opcode != Opcodes.ATHROW) {
                         graph.addEdge(insn, next)                         
                    }
                    
                    val currentExceptionHandlers = exceptionHandlers[i] ?: continue
                    
                    for (handler in currentExceptionHandlers) {
                         graph.addEdge(insn, handler)
                    }
               }
               
               return graph
          }
     }
     
     private fun addEdge(from: AbstractInsnNode, to: AbstractInsnNode?) {
          if (to != null) {
               edges[insns.indexOf(from)].add(insns.indexOf(to))
          }
     }
     
     public fun getSuccessorsIndices(insn: AbstractInsnNode): List<Int> = edges[insns.indexOf(insn)]
     
}

private fun buildExceptionHandlers(node: MethodNode): Array<out List<LabelNode>?> {
     val insns = node.instructions
     val handlers = arrayOfNulls<MutableList<LabelNode>>(insns.size())
     
     for (tcb in node.tryCatchBlocks) {
          for (index in insns.indexOf(tcb.start)..insns.indexOf(tcb.end) - 1) {
               val current: MutableList<LabelNode>
               if (handlers[index] == null) {
                    current = arrayListOf()
                    handlers[index] = current
               } else {
                    current = handlers[index]!!
               }
               
               current.add(tcb.handler)
          }
     }
     
     return handlers
}
