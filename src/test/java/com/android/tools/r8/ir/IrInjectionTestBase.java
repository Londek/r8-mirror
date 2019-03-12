// Copyright (c) 2017, the R8 project authors. Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.
package com.android.tools.r8.ir;

import com.android.tools.r8.ToolHelper;
import com.android.tools.r8.dex.ApplicationReader;
import com.android.tools.r8.graph.AppInfo;
import com.android.tools.r8.graph.AppView;
import com.android.tools.r8.graph.DexApplication;
import com.android.tools.r8.graph.DexEncodedMethod;
import com.android.tools.r8.ir.code.BasicBlock;
import com.android.tools.r8.ir.code.IRCode;
import com.android.tools.r8.ir.code.Instruction;
import com.android.tools.r8.ir.code.InstructionListIterator;
import com.android.tools.r8.ir.code.ValueNumberGenerator;
import com.android.tools.r8.ir.conversion.IRConverter;
import com.android.tools.r8.origin.Origin;
import com.android.tools.r8.shaking.MainDexClasses;
import com.android.tools.r8.shaking.RootSetBuilder.RootSet;
import com.android.tools.r8.smali.SmaliBuilder;
import com.android.tools.r8.smali.SmaliBuilder.MethodSignature;
import com.android.tools.r8.smali.SmaliTestBase;
import com.android.tools.r8.utils.AndroidApp;
import com.android.tools.r8.utils.AndroidAppConsumers;
import com.android.tools.r8.utils.InternalOptions;
import com.android.tools.r8.utils.Timing;
import com.android.tools.r8.utils.codeinspector.CodeInspector;
import com.android.tools.r8.utils.codeinspector.MethodSubject;
import java.io.IOException;
import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.ExecutionException;
import org.antlr.runtime.RecognitionException;

public class IrInjectionTestBase extends SmaliTestBase {

  protected DexApplication buildApplication(SmaliBuilder builder, InternalOptions options) {
    try {
      return buildApplication(
          AndroidApp.builder().addDexProgramData(builder.compile(), Origin.unknown()).build(),
          options);
    } catch (IOException | RecognitionException | ExecutionException e) {
      throw new RuntimeException(e);
    }
  }

  protected DexApplication buildApplication(AndroidApp input, InternalOptions options) {
    try {
      options.itemFactory.resetSortedIndices();
      return new ApplicationReader(input, options, new Timing("IrInjectionTest")).read();
    } catch (IOException | ExecutionException e) {
      throw new RuntimeException(e);
    }
  }

  protected MethodSubject getMethodSubject(DexApplication application, MethodSignature signature) {
    return getMethodSubject(
        application,
        signature.clazz,
        signature.returnType,
        signature.name,
        signature.parameterTypes);
  }

  protected MethodSubject getMethodSubject(
      DexApplication application,
      String className,
      String returnType,
      String methodName,
      List<String> parameters) {
    CodeInspector inspector = new CodeInspector(application);
    return getMethodSubject(inspector, className, returnType, methodName, parameters);
  }

  public class TestApplication {

    public final DexApplication application;
    public final AppView<? extends AppInfo> appView;
    public final RootSet rootSet;

    public final DexEncodedMethod method;
    public final IRCode code;
    public final List<IRCode> additionalCode;
    public final AndroidAppConsumers consumers;

    public final ValueNumberGenerator valueNumberGenerator = new ValueNumberGenerator();

    public TestApplication(AppView<? extends AppInfo> appView, MethodSubject method) {
      this(appView, null, method, null);
    }

    public TestApplication(
        AppView<? extends AppInfo> appView,
        RootSet rootSet,
        MethodSubject method,
        List<IRCode> additionalCode) {
      this.application = appView.appInfo().app;
      this.appView = appView;
      this.rootSet = rootSet;
      this.method = method.getMethod();
      this.code = method.buildIR(appView.dexItemFactory());
      this.additionalCode = additionalCode;
      this.consumers = new AndroidAppConsumers(appView.options());
    }

    public int countArgumentInstructions() {
      int count = 0;
      ListIterator<Instruction> iterator = code.entryBlock().listIterator();
      while (iterator.next().isArgument()) {
        count++;
      }
      return count;
    }

    public InstructionListIterator listIteratorAt(BasicBlock block, int index) {
      InstructionListIterator iterator = block.listIterator();
      for (int i = 0; i < index; i++) {
        iterator.next();
      }
      return iterator;
    }

    private AndroidApp writeDex(DexApplication application, InternalOptions options) {
      try {
        ToolHelper.writeApplication(application, options);
        options.signalFinishedToConsumers();
        return consumers.build();
      } catch (ExecutionException e) {
        throw new RuntimeException(e);
      }
    }

    public String run() throws IOException {
      Timing timing = new Timing(getClass().getSimpleName());
      IRConverter converter = new IRConverter(appView, timing, null, MainDexClasses.NONE, rootSet);
      converter.replaceCodeForTesting(method, code);
      AndroidApp app = writeDex(application, appView.options());
      return runOnArtRaw(app, DEFAULT_MAIN_CLASS_NAME).stdout;
    }
  }
}
