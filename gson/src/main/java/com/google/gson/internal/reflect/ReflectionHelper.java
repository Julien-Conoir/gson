/*
 * Copyright (C) 2021 Google Inc.
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

package com.google.gson.internal.reflect;

import com.google.gson.JsonIOException;
import com.google.gson.internal.GsonBuildConfig;
import com.google.gson.internal.TroubleshootingGuide;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Objects;

public class ReflectionHelper {

  private static final RecordHelper RECORD_HELPER;

  static {
    RecordHelper instance;
    try {
      // Try to construct the RecordSupportedHelper, if this fails, records are not supported on
      // this JVM.
      instance = new RecordSupportedHelper();
    } catch (ReflectiveOperationException e) {
      instance = new RecordNotSupportedHelper();
    }
    RECORD_HELPER = instance;
  }

  private ReflectionHelper() {}

  private static String getInaccessibleTroubleshootingSuffix(Exception e) {
    // Class was added in Java 9, therefore cannot use instanceof
    if (Objects.equals(e.getClass().getName(), "java.lang.reflect.InaccessibleObjectException")) {
      String message = e.getMessage();
      String troubleshootingId =
          message != null && message.contains("to module com.google.gson")
              ? "reflection-inaccessible-to-module-gson"
              : "reflection-inaccessible";
      return "\nSee " + TroubleshootingGuide.createUrl(troubleshootingId);
    }
    return "";
  }

  /**
   * Internal implementation of making an {@link AccessibleObject} accessible.
   *
   * @param object the object that {@link AccessibleObject#setAccessible(boolean)} should be called
   *     on.
   * @throws JsonIOException if making the object accessible fails
   */
  public static void makeAccessible(AccessibleObject object) throws JsonIOException {
    try {
      object.setAccessible(true);
    } catch (Exception exception) {
      String description = getAccessibleObjectDescription(object, false);
      throw new JsonIOException(
          "Failed making "
              + description
              + " accessible; either increase its visibility"
              + " or write a custom TypeAdapter for its declaring type."
              + getInaccessibleTroubleshootingSuffix(exception),
          exception);
    }
  }

  /**
   * Returns a short string describing the {@link AccessibleObject} in a human-readable way. The
   * result is normally shorter than {@link AccessibleObject#toString()} because it omits modifiers
   * (e.g. {@code final}) and uses simple names for constructor and method parameter types.
   *
   * @param object object to describe
   * @param uppercaseFirstLetter whether the first letter of the description should be uppercased
   */
  public static String getAccessibleObjectDescription(
      AccessibleObject object, boolean uppercaseFirstLetter) {
    String description;

    if (object instanceof Field) {
      description = "field '" + fieldToString((Field) object) + "'";
    } else if (object instanceof Method) {
      Method method = (Method) object;

      StringBuilder methodSignatureBuilder = new StringBuilder(method.getName());
      appendExecutableParameters(method, methodSignatureBuilder);
      String methodSignature = methodSignatureBuilder.toString();

      description = "method '" + method.getDeclaringClass().getName() + "#" + methodSignature + "'";
    } else if (object instanceof Constructor) {
      description = "constructor '" + constructorToString((Constructor<?>) object) + "'";
    } else {
      description = "<unknown AccessibleObject> " + object.toString();
    }

    if (uppercaseFirstLetter && Character.isLowerCase(description.charAt(0))) {
      description = Character.toUpperCase(description.charAt(0)) + description.substring(1);
    }
    return description;
  }

  /** Creates a string representation for a field, omitting modifiers and the field type. */
  public static String fieldToString(Field field) {
    return field.getDeclaringClass().getName() + "#" + field.getName();
  }

  /**
   * Creates a string representation for a constructor. E.g.: {@code java.lang.String(char[], int,
   * int)}
   */
  public static String constructorToString(Constructor<?> constructor) {
    StringBuilder stringBuilder = new StringBuilder(constructor.getDeclaringClass().getName());
    appendExecutableParameters(constructor, stringBuilder);

    return stringBuilder.toString();
  }

  // Ideally parameter type would be java.lang.reflect.Executable, but that was added in Java 8
  private static void appendExecutableParameters(
      AccessibleObject executable, StringBuilder stringBuilder) {
    stringBuilder.append('(');

    Class<?>[] parameters =
        (executable instanceof Method)
            ? ((Method) executable).getParameterTypes()
            : ((Constructor<?>) executable).getParameterTypes();
    for (int i = 0; i < parameters.length; i++) {
      if (i > 0) {
        stringBuilder.append(", ");
      }
      stringBuilder.append(parameters[i].getSimpleName());
    }

    stringBuilder.append(')');
  }

  public static boolean isStatic(Class<?> clazz) {
    return Modifier.isStatic(clazz.getModifiers());
  }

  /** Returns whether the class is anonymous or a non-static local class. */
  public static boolean isAnonymousOrNonStaticLocal(Class<?> clazz) {
    return !isStatic(clazz) && (clazz.isAnonymousClass() || clazz.isLocalClass());
  }

  /**
   * Tries making the constructor accessible, returning an exception message if this fails.
   *
   * @param constructor constructor to make accessible
   * @return exception message; {@code null} if successful, non-{@code null} if unsuccessful
   */
  public static String tryMakeAccessible(Constructor<?> constructor) {
    try {
      constructor.setAccessible(true);
      return null;
    } catch (Exception exception) {
      return "Failed making constructor '"
          + constructorToString(constructor)
          + "' accessible; either increase its visibility or write a custom InstanceCreator or"
          + " TypeAdapter for its declaring type: "
          // Include the message since it might contain more detailed information
          + exception.getMessage()
          + getInaccessibleTroubleshootingSuffix(exception);
    }
  }

  /** If records are supported on the JVM, this is equivalent to a call to Class.isRecord() */
  public static boolean isRecord(Class<?> raw) {
    return RECORD_HELPER.isRecord(raw);
  }

  public static String[] getRecordComponentNames(Class<?> raw) {
    return RECORD_HELPER.getRecordComponentNames(raw);
  }

  /** Looks up the record accessor method that corresponds to the given record field */
  public static Method getAccessor(Class<?> raw, Field field) {
    return RECORD_HELPER.getAccessor(raw, field);
  }

  public static <T> Constructor<T> getCanonicalRecordConstructor(Class<T> raw) {
    return RECORD_HELPER.getCanonicalRecordConstructor(raw);
  }

  public static RuntimeException createExceptionForUnexpectedIllegalAccess(
      IllegalAccessException exception) {
    throw new RecordException(
        "Unexpected IllegalAccessException occurred (Gson "
            + GsonBuildConfig.VERSION
            + "). Certain ReflectionAccessFilter features require Java >= 9 to work correctly. If"
            + " you are not using ReflectionAccessFilter, report this to the Gson maintainers.",
        exception);
  }
}
