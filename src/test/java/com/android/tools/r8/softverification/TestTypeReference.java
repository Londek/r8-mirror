// Copyright (c) 2021, the R8 project authors. Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

package com.android.tools.r8.softverification;

public class TestTypeReference {

  public static String run() {
    return run(null);
  }

  public static String test(MissingClass missingClass) {
    return missingClass == null ? "nobar" : null;
  }

  public static String run(MissingClass missingClass) {
    String currentString = missingClass == null ? "foobar" : test(missingClass);
    currentString = missingClass == null ? currentString : test(missingClass);
    for (int i = 0; i < 10; i++) {
      currentString = "foobar" + (i + currentString.length());
    }
    return currentString;
  }
}
