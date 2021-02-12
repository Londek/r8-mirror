// Copyright (c) 2020, the R8 project authors. Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

package com.android.tools.r8.missingclasses;

import com.android.tools.r8.CompilationFailedException;
import com.android.tools.r8.R8FullTestBuilder;
import com.android.tools.r8.TestDiagnosticMessages;
import com.android.tools.r8.TestParameters;
import com.android.tools.r8.references.MethodReference;
import com.android.tools.r8.references.Reference;
import com.google.common.collect.ImmutableList;
import org.junit.Test;

/** If a method definition refers to a missing class, then the method definition is to be blamed. */
public class MissingClassReferencedFromKeptMethodParameterTest extends MissingClassesTestBase {

  private static final MethodReference referencedFrom =
      Reference.method(
          Reference.classFromClass(Main.class),
          "get",
          ImmutableList.of(Reference.classFromClass(MissingClass.class)),
          null);

  public MissingClassReferencedFromKeptMethodParameterTest(TestParameters parameters) {
    super(parameters);
  }

  @Test(expected = CompilationFailedException.class)
  public void testNoRules() throws Exception {
    compileWithExpectedDiagnostics(
        Main.class,
        diagnostics -> inspectDiagnosticsWithNoRules(diagnostics, referencedFrom),
        this::addKeepMethodRule);
  }

  @Test
  public void testDontWarnMainClass() throws Exception {
    compileWithExpectedDiagnostics(
        Main.class,
        TestDiagnosticMessages::assertNoMessages,
        addDontWarn(Main.class).andThen(this::addKeepMethodRule));
  }

  @Test
  public void testDontWarnMissingClass() throws Exception {
    compileWithExpectedDiagnostics(
        Main.class,
        TestDiagnosticMessages::assertNoMessages,
        addDontWarn(MissingClass.class).andThen(this::addKeepMethodRule));
  }

  @Test
  public void testIgnoreWarnings() throws Exception {
    compileWithExpectedDiagnostics(
        Main.class,
        diagnostics -> inspectDiagnosticsWithIgnoreWarnings(diagnostics, referencedFrom),
        addIgnoreWarnings().andThen(this::addKeepMethodRule));
  }

  private void addKeepMethodRule(R8FullTestBuilder builder) {
    builder.addKeepRules(
        "-keep class " + Main.class.getTypeName() + " {",
        "  public static void get(" + MissingClass.class.getTypeName() + ");",
        "}");
  }

  static class Main {

    public static void main(String[] args) {}

    public static void get(MissingClass mc) {}
  }
}