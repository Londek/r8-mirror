// Copyright (c) 2017, the R8 project authors. Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.
package com.android.tools.r8.cf.code;

import com.android.tools.r8.cf.CfPrinter;
import com.android.tools.r8.errors.Unreachable;
import com.android.tools.r8.ir.code.MemberType;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class CfArrayStore extends CfInstruction {

  private final MemberType type;

  public CfArrayStore(MemberType type) {
    this.type = type;
  }

  public MemberType getType() {
    return type;
  }

  private int getStoreType() {
    switch (type) {
      case OBJECT:
        return Opcodes.AASTORE;
      case BYTE:
      case BOOLEAN:
        return Opcodes.BASTORE;
      case CHAR:
        return Opcodes.CASTORE;
      case SHORT:
        return Opcodes.SASTORE;
      case INT:
        return Opcodes.IASTORE;
      case FLOAT:
        return Opcodes.FASTORE;
      case LONG:
        return Opcodes.LASTORE;
      case DOUBLE:
        return Opcodes.DASTORE;
      default:
        throw new Unreachable("Unexpected type " + type);
    }
  }

  @Override
  public void write(MethodVisitor visitor) {
    visitor.visitInsn(getStoreType());
  }

  @Override
  public void print(CfPrinter printer) {
    printer.print(this);
  }
}
