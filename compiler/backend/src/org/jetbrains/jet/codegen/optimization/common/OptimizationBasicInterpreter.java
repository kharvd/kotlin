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

package org.jetbrains.jet.codegen.optimization.common;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.org.objectweb.asm.Handle;
import org.jetbrains.org.objectweb.asm.Opcodes;
import org.jetbrains.org.objectweb.asm.Type;
import org.jetbrains.org.objectweb.asm.tree.*;
import org.jetbrains.org.objectweb.asm.tree.analysis.AnalyzerException;
import org.jetbrains.org.objectweb.asm.tree.analysis.BasicInterpreter;
import org.jetbrains.org.objectweb.asm.tree.analysis.BasicValue;

public class OptimizationBasicInterpreter extends BasicInterpreter {

    @Override
    @Nullable
    public BasicValue newValue(@Nullable Type type) {
        if (type == null) {
            return OptimizationBasicValue.UNINITIALIZED_VALUE;
        }

        switch (type.getSort()) {
            case Type.VOID:
                return null;
            case Type.BOOLEAN:
                return OptimizationBasicValue.BOOLEAN_VALUE;
            case Type.CHAR:
                return OptimizationBasicValue.CHAR_VALUE;
            case Type.BYTE:
                return OptimizationBasicValue.BYTE_VALUE;
            case Type.SHORT:
                return OptimizationBasicValue.SHORT_VALUE;
            case Type.INT:
                return OptimizationBasicValue.INT_VALUE;
            case Type.FLOAT:
                return OptimizationBasicValue.FLOAT_VALUE;
            case Type.LONG:
                return OptimizationBasicValue.LONG_VALUE;
            case Type.DOUBLE:
                return OptimizationBasicValue.DOUBLE_VALUE;
            case Type.ARRAY:
                return OptimizationBasicValue.REFERENCE_VALUE;
            case Type.OBJECT:
                return new OptimizationBasicValue(type);
            default:
                throw new Error("Internal error");
        }
    }

    @Override
    public BasicValue newOperation(@NotNull AbstractInsnNode insn) throws AnalyzerException {
        if (insn.getOpcode() == Opcodes.LDC) {
            Object cst = ((LdcInsnNode) insn).cst;

            if (cst instanceof Long) {
                return OptimizationBasicValue.LONG_VALUE;
            }

            if (cst instanceof Boolean ||
                cst instanceof Integer ||
                cst instanceof Short ||
                cst instanceof Byte ||
                cst instanceof Character) {
                return OptimizationBasicValue.INT_VALUE;
            }

            if (cst instanceof Float) {
                return OptimizationBasicValue.FLOAT_VALUE;
            }

            if (cst instanceof Double) {
                return OptimizationBasicValue.DOUBLE_VALUE;
            }
        }

        switch (insn.getOpcode()) {
            case ACONST_NULL:
                return newValue(Type.getObjectType("null"));
            case ICONST_M1:
            case ICONST_0:
            case ICONST_1:
            case ICONST_2:
            case ICONST_3:
            case ICONST_4:
            case ICONST_5:
                return OptimizationBasicValue.INT_VALUE;
            case LCONST_0:
            case LCONST_1:
                return OptimizationBasicValue.LONG_VALUE;
            case FCONST_0:
            case FCONST_1:
            case FCONST_2:
                return OptimizationBasicValue.FLOAT_VALUE;
            case DCONST_0:
            case DCONST_1:
                return OptimizationBasicValue.DOUBLE_VALUE;
            case BIPUSH:
            case SIPUSH:
                return OptimizationBasicValue.INT_VALUE;
            case LDC:
                Object cst = ((LdcInsnNode) insn).cst;
                if (cst instanceof Integer) {
                    return OptimizationBasicValue.INT_VALUE;
                } else if (cst instanceof Float) {
                    return OptimizationBasicValue.FLOAT_VALUE;
                } else if (cst instanceof Long) {
                    return OptimizationBasicValue.LONG_VALUE;
                } else if (cst instanceof Double) {
                    return OptimizationBasicValue.DOUBLE_VALUE;
                } else if (cst instanceof String) {
                    return newValue(Type.getObjectType("java/lang/String"));
                } else if (cst instanceof Type) {
                    int sort = ((Type) cst).getSort();
                    if (sort == Type.OBJECT || sort == Type.ARRAY) {
                        return newValue(Type.getObjectType("java/lang/Class"));
                    } else if (sort == Type.METHOD) {
                        return newValue(Type.getObjectType("java/lang/invoke/MethodType"));
                    } else {
                        throw new IllegalArgumentException("Illegal LDC constant " + cst);
                    }
                } else if (cst instanceof Handle) {
                    return newValue(Type.getObjectType("java/lang/invoke/MethodHandle"));
                } else {
                    throw new IllegalArgumentException("Illegal LDC constant " + cst);
                }
            case GETSTATIC:
                return newValue(Type.getType(((FieldInsnNode) insn).desc));
            case NEW:
                return newValue(Type.getObjectType(((TypeInsnNode) insn).desc));
            default:
                throw new Error("Internal error.");
        }
    }

    @Override
    public BasicValue unaryOperation(@NotNull AbstractInsnNode insn, @NotNull BasicValue value) throws AnalyzerException {
        switch (insn.getOpcode()) {
            case INEG:
            case IINC:
            case L2I:
            case F2I:
            case D2I:
            case I2B:
            case I2C:
            case I2S:
                return OptimizationBasicValue.INT_VALUE;
            case FNEG:
            case I2F:
            case L2F:
            case D2F:
                return OptimizationBasicValue.FLOAT_VALUE;
            case LNEG:
            case I2L:
            case F2L:
            case D2L:
                return OptimizationBasicValue.LONG_VALUE;
            case DNEG:
            case I2D:
            case L2D:
            case F2D:
                return OptimizationBasicValue.DOUBLE_VALUE;
            case IFEQ:
            case IFNE:
            case IFLT:
            case IFGE:
            case IFGT:
            case IFLE:
            case TABLESWITCH:
            case LOOKUPSWITCH:
            case IRETURN:
            case LRETURN:
            case FRETURN:
            case DRETURN:
            case ARETURN:
            case PUTSTATIC:
                return null;
            case GETFIELD:
                return newValue(Type.getType(((FieldInsnNode) insn).desc));
            case NEWARRAY:
                switch (((IntInsnNode) insn).operand) {
                    case T_BOOLEAN:
                        return newValue(Type.getType("[Z"));
                    case T_CHAR:
                        return newValue(Type.getType("[C"));
                    case T_BYTE:
                        return newValue(Type.getType("[B"));
                    case T_SHORT:
                        return newValue(Type.getType("[S"));
                    case T_INT:
                        return newValue(Type.getType("[I"));
                    case T_FLOAT:
                        return newValue(Type.getType("[F"));
                    case T_DOUBLE:
                        return newValue(Type.getType("[D"));
                    case T_LONG:
                        return newValue(Type.getType("[J"));
                    default:
                        throw new AnalyzerException(insn, "Invalid array type");
                }
            case ANEWARRAY:
                String desc = ((TypeInsnNode) insn).desc;
                return newValue(Type.getType("[" + Type.getObjectType(desc)));
            case ARRAYLENGTH:
                return OptimizationBasicValue.INT_VALUE;
            case ATHROW:
                return null;
            case CHECKCAST:
                desc = ((TypeInsnNode) insn).desc;
                return newValue(Type.getObjectType(desc));
            case INSTANCEOF:
                return OptimizationBasicValue.INT_VALUE;
            case MONITORENTER:
            case MONITOREXIT:
            case IFNULL:
            case IFNONNULL:
                return null;
            default:
                throw new Error("Internal error.");
        }
    }

    @Override
    public BasicValue binaryOperation(
            @NotNull AbstractInsnNode insn, @NotNull BasicValue value1, @NotNull BasicValue value2
    ) throws AnalyzerException {
        switch (insn.getOpcode()) {
            case IALOAD:
            case BALOAD:
            case CALOAD:
            case SALOAD:
            case IADD:
            case ISUB:
            case IMUL:
            case IDIV:
            case IREM:
            case ISHL:
            case ISHR:
            case IUSHR:
            case IAND:
            case IOR:
            case IXOR:
                return OptimizationBasicValue.INT_VALUE;
            case FALOAD:
            case FADD:
            case FSUB:
            case FMUL:
            case FDIV:
            case FREM:
                return OptimizationBasicValue.FLOAT_VALUE;
            case LALOAD:
            case LADD:
            case LSUB:
            case LMUL:
            case LDIV:
            case LREM:
            case LSHL:
            case LSHR:
            case LUSHR:
            case LAND:
            case LOR:
            case LXOR:
                return OptimizationBasicValue.LONG_VALUE;
            case DALOAD:
            case DADD:
            case DSUB:
            case DMUL:
            case DDIV:
            case DREM:
                return OptimizationBasicValue.DOUBLE_VALUE;
            case AALOAD:
                return OptimizationBasicValue.REFERENCE_VALUE;
            case LCMP:
            case FCMPL:
            case FCMPG:
            case DCMPL:
            case DCMPG:
                return OptimizationBasicValue.INT_VALUE;
            case IF_ICMPEQ:
            case IF_ICMPNE:
            case IF_ICMPLT:
            case IF_ICMPGE:
            case IF_ICMPGT:
            case IF_ICMPLE:
            case IF_ACMPEQ:
            case IF_ACMPNE:
            case PUTFIELD:
                return null;
            default:
                throw new Error("Internal error.");
        }
    }

    @NotNull
    @Override
    public BasicValue merge(
            @NotNull BasicValue v, @NotNull BasicValue w
    ) {
        if (!v.equals(w)) {
            if (v == OptimizationBasicValue.UNINITIALIZED_VALUE || w == OptimizationBasicValue.UNINITIALIZED_VALUE) {
                return OptimizationBasicValue.UNINITIALIZED_VALUE;
            }

            // if merge of two references then `lub` is java/lang/Object
            // arrays also are BasicValues with reference type's
            if (v.getType().getSort() == Type.OBJECT && w.getType().getSort() == Type.OBJECT) {
                return OptimizationBasicValue.REFERENCE_VALUE;
            }

            assert v.getType().getSort() != Type.ARRAY && w.getType().getSort() != Type.ARRAY : "There should not be arrays";

            // if merge of something can be stored in int var (int, char, boolean, byte, character)
            if (v.getType().getOpcode(Opcodes.ISTORE) == Opcodes.ISTORE &&
                w.getType().getOpcode(Opcodes.ISTORE) == Opcodes.ISTORE) {
                return OptimizationBasicValue.INT_VALUE;
            }

            return OptimizationBasicValue.UNINITIALIZED_VALUE;
        }
        return v;
    }
}
