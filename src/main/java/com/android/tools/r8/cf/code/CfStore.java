// Copyright (c) 2017, the R8 project authors. Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.
package com.android.tools.r8.cf.code;

import com.android.tools.r8.cf.CfPrinter;
import com.android.tools.r8.errors.Unreachable;
import com.android.tools.r8.graph.DexItemFactory;
import com.android.tools.r8.graph.DexProgramClass;
import com.android.tools.r8.graph.GraphLens;
import com.android.tools.r8.graph.InitClassLens;
import com.android.tools.r8.graph.ProgramMethod;
import com.android.tools.r8.ir.code.ValueType;
import com.android.tools.r8.ir.conversion.CfSourceCode;
import com.android.tools.r8.ir.conversion.CfState;
import com.android.tools.r8.ir.conversion.CfState.Slot;
import com.android.tools.r8.ir.conversion.IRBuilder;
import com.android.tools.r8.ir.conversion.LensCodeRewriterUtils;
import com.android.tools.r8.ir.optimize.Inliner.ConstraintWithTarget;
import com.android.tools.r8.ir.optimize.InliningConstraints;
import com.android.tools.r8.naming.NamingLens;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class CfStore extends CfInstruction {

  private final int var;
  private final ValueType type;

  public CfStore(ValueType type, int var) {
    this.var = var;
    this.type = type;
  }

  private int getStoreType() {
    switch (type) {
      case OBJECT:
        return Opcodes.ASTORE;
      case INT:
        return Opcodes.ISTORE;
      case FLOAT:
        return Opcodes.FSTORE;
      case LONG:
        return Opcodes.LSTORE;
      case DOUBLE:
        return Opcodes.DSTORE;
      default:
        throw new Unreachable("Unexpected type " + type);
    }
  }

  @Override
  public CfStore asStore() {
    return this;
  }

  @Override
  public boolean isStore() {
    return true;
  }

  @Override
  public void write(
      ProgramMethod context,
      DexItemFactory dexItemFactory,
      GraphLens graphLens,
      InitClassLens initClassLens,
      NamingLens namingLens,
      LensCodeRewriterUtils rewriter,
      MethodVisitor visitor) {
    visitor.visitVarInsn(getStoreType(), var);
  }

  @Override
  public void print(CfPrinter printer) {
    printer.print(this);
  }

  public ValueType getType() {
    return type;
  }

  public int getLocalIndex() {
    return var;
  }

  @Override
  public void buildIR(IRBuilder builder, CfState state, CfSourceCode code) {
    Slot pop = state.pop();
    builder.addMove(type, state.write(var, pop).register, pop.register);
  }

  @Override
  public boolean emitsIR() {
    return false;
  }

  @Override
  public ConstraintWithTarget inliningConstraint(
      InliningConstraints inliningConstraints, DexProgramClass context) {
    return inliningConstraints.forStore();
  }
}
