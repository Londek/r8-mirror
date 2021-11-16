// Copyright (c) 2021, the R8 project authors. Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

package com.android.tools.r8.softverification;

public class TestNewInstance {

  public static String run() {
    if (System.currentTimeMillis() == 0) {
      new MissingClass();
    }
    if (System.currentTimeMillis() == 0) {
      new MissingClass();
    }
    String currentString = "foobar";
    for (int i = 0; i < 10; i++) {
      currentString = "foobar" + (i + currentString.length());
    }
    return currentString;
  }
}
