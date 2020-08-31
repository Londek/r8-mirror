// Copyright (c) 2020, the R8 project authors. Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

package com.android.tools.r8.shaking.horizontalclassmerging;

import static com.android.tools.r8.utils.codeinspector.Matchers.isPresent;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsNot.not;

import com.android.tools.r8.NeverClassInline;
import com.android.tools.r8.NeverInline;
import com.android.tools.r8.TestBase;
import com.android.tools.r8.TestParameters;
import com.android.tools.r8.utils.BooleanUtils;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class RemapMethodTest extends TestBase {
  private final TestParameters parameters;
  private final boolean enableHorizontalClassMerging;

  public RemapMethodTest(TestParameters parameters, boolean enableHorizontalClassMerging) {
    this.parameters = parameters;
    this.enableHorizontalClassMerging = enableHorizontalClassMerging;
  }

  @Parameterized.Parameters(name = "{0}, horizontalClassMerging:{1}")
  public static List<Object[]> data() {
    return buildParameters(
        getTestParameters().withAllRuntimesAndApiLevels().build(), BooleanUtils.values());
  }

  @Test
  public void testR8() throws Exception {
    testForR8(parameters.getBackend())
        .addInnerClasses(this.getClass())
        .addKeepMainRule(Main.class)
        .addOptionsModification(
            options -> {
              options.enableHorizontalClassMerging = enableHorizontalClassMerging;
              options.enableVerticalClassMerging = false;
            })
        .enableInliningAnnotations()
        .enableNeverClassInliningAnnotations()
        .setMinApi(parameters.getApiLevel())
        .compile()
        // .run(parameters.getRuntime(), Main.class)
        // .assertSuccessWithOutputLines("foo", "foo", "bar", "bar")
        .inspect(
            codeInspector -> {
              assertThat(codeInspector.clazz(A.class), isPresent());
              assertThat(codeInspector.clazz(C.class), isPresent());
              if (enableHorizontalClassMerging) {
                assertThat(codeInspector.clazz(B.class), not(isPresent()));
                assertThat(codeInspector.clazz(D.class), not(isPresent()));
                // TODO(b/165517236): Explicitly check classes have been merged.
              } else {
                assertThat(codeInspector.clazz(B.class), isPresent());
                assertThat(codeInspector.clazz(D.class), isPresent());
              }
            });
  }

  @NeverClassInline
  public static class A {
    @NeverInline
    public void foo() {
      System.out.println("foo");
    }
  }

  @NeverClassInline
  public static class B {
    // TODO(b/164924717): remove non overlapping constructor requirement
    public B(String s) {}

    @NeverInline
    public void bar(D d) {
      d.bar();
    }
  }

  @NeverClassInline
  public static class Other {
    String field;

    public Other() {
      field = "";
    }
  }

  @NeverClassInline
  public static class C extends Other {
    @NeverInline
    public void foo() {
      System.out.println("foo");
    }
  }

  @NeverClassInline
  public static class D extends Other {
    public D(String s) {
      System.out.println(s);
    }

    @NeverInline
    public void bar() {
      System.out.println("bar");
    }
  }

  public static class Main {
    public static void main(String[] args) {
      A a = new A();
      a.foo();
      B b = new B("bar");
      C c = new C();
      c.foo();
      D d = new D("bar");
      b.bar(d);
    }
  }
}
