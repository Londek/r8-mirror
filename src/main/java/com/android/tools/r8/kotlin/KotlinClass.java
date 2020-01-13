// Copyright (c) 2018, the R8 project authors. Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

package com.android.tools.r8.kotlin;

import static com.android.tools.r8.kotlin.Kotlin.addKotlinPrefix;
import static com.android.tools.r8.kotlin.KotlinMetadataSynthesizer.isExtension;
import static com.android.tools.r8.kotlin.KotlinMetadataSynthesizer.toKmType;
import static com.android.tools.r8.kotlin.KotlinMetadataSynthesizer.toRenamedKmConstructor;
import static com.android.tools.r8.kotlin.KotlinMetadataSynthesizer.toRenamedKmFunction;
import static com.android.tools.r8.kotlin.KotlinMetadataSynthesizer.toRenamedKmFunctionAsExtension;
import static com.android.tools.r8.kotlin.KotlinMetadataSynthesizer.toRenamedKmType;

import com.android.tools.r8.graph.AppView;
import com.android.tools.r8.graph.DexClass;
import com.android.tools.r8.graph.DexEncodedMethod;
import com.android.tools.r8.graph.DexType;
import com.android.tools.r8.naming.NamingLens;
import com.android.tools.r8.shaking.AppInfoWithLiveness;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import kotlinx.metadata.KmClass;
import kotlinx.metadata.KmConstructor;
import kotlinx.metadata.KmFunction;
import kotlinx.metadata.KmProperty;
import kotlinx.metadata.KmType;
import kotlinx.metadata.jvm.KotlinClassHeader;
import kotlinx.metadata.jvm.KotlinClassMetadata;

public class KotlinClass extends KotlinInfo<KotlinClassMetadata.Class> {

  private KmClass kmClass;

  static KotlinClass fromKotlinClassMetadata(
      KotlinClassMetadata kotlinClassMetadata, DexClass clazz) {
    assert kotlinClassMetadata instanceof KotlinClassMetadata.Class;
    KotlinClassMetadata.Class kClass = (KotlinClassMetadata.Class) kotlinClassMetadata;
    return new KotlinClass(kClass, clazz);
  }

  private KotlinClass(KotlinClassMetadata.Class metadata, DexClass clazz) {
    super(metadata, clazz);
  }

  @Override
  void processMetadata() {
    assert !isProcessed;
    isProcessed = true;
    kmClass = metadata.toKmClass();
  }

  @Override
  void rewrite(AppView<AppInfoWithLiveness> appView, NamingLens lens) {
    List<KmType> superTypes = kmClass.getSupertypes();
    superTypes.clear();
    for (DexType itfType : clazz.interfaces.values) {
      KmType kmType = toRenamedKmType(itfType, appView, lens);
      if (kmType != null) {
        superTypes.add(kmType);
      }
    }
    assert clazz.superType != null;
    if (clazz.superType != appView.dexItemFactory().objectType) {
      KmType kmTypeForSupertype = toRenamedKmType(clazz.superType, appView, lens);
      if (kmTypeForSupertype != null) {
        superTypes.add(kmTypeForSupertype);
      }
    } else if (clazz.isInterface()) {
      superTypes.add(toKmType(addKotlinPrefix("Any;")));
    }

    if (!appView.options().enableKotlinMetadataRewriting) {
      return;
    }
    List<KmConstructor> constructors = kmClass.getConstructors();
    List<KmConstructor> originalConstructors = new ArrayList<>(constructors);
    constructors.clear();
    for (Map.Entry<DexEncodedMethod, KmConstructor> entry :
        clazz.kotlinConstructors(originalConstructors, appView).entrySet()) {
      KmConstructor constructor =
          toRenamedKmConstructor(entry.getKey(), entry.getValue(), appView, lens);
      if (constructor != null) {
        constructors.add(constructor);
      }
    }

    List<KmFunction> functions = kmClass.getFunctions();
    List<KmFunction> originalFunctions =
        functions.stream()
            .filter(kmFunction -> !isExtension(kmFunction))
            .collect(Collectors.toList());
    List<KmFunction> originalExtensions =
        functions.stream()
            .filter(KotlinMetadataSynthesizer::isExtension)
            .collect(Collectors.toList());
    functions.clear();

    List<KmProperty> properties = kmClass.getProperties();
    for (DexEncodedMethod method : clazz.kotlinFunctions(originalFunctions, properties, appView)) {
      KmFunction function = toRenamedKmFunction(method, null, appView, lens);
      if (function != null) {
        functions.add(function);
      }
    }
    for (Map.Entry<DexEncodedMethod, KmFunction> entry :
        clazz.kotlinExtensions(originalExtensions, appView).entrySet()) {
      KmFunction extension =
          toRenamedKmFunctionAsExtension(entry.getKey(), entry.getValue(), appView, lens);
      if (extension != null) {
        functions.add(extension);
      }
    }
  }

  @Override
  KotlinClassHeader createHeader() {
    KotlinClassMetadata.Class.Writer writer = new KotlinClassMetadata.Class.Writer();
    kmClass.accept(writer);
    return writer.write().getHeader();
  }

  @Override
  public Kind getKind() {
    return Kind.Class;
  }

  @Override
  public boolean isClass() {
    return true;
  }

  @Override
  public KotlinClass asClass() {
    return this;
  }

}
