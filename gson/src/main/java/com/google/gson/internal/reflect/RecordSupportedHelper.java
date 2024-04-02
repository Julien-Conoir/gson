package com.google.gson.internal.reflect;

import com.google.gson.internal.GsonBuildConfig;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/** Instance used when records are supported */
public class RecordSupportedHelper extends RecordHelper {

  private final Method isRecord;
  private final Method getRecordComponents;
  private final Method getName;
  private final Method getType;

  protected RecordSupportedHelper() throws NoSuchMethodException, ClassNotFoundException {
    isRecord = Class.class.getMethod("isRecord");
    getRecordComponents = Class.class.getMethod("getRecordComponents");
    Class<?> classRecordComponent = Class.forName("java.lang.reflect.RecordComponent");
    getName = classRecordComponent.getMethod("getName");
    getType = classRecordComponent.getMethod("getType");
  }

  private static RuntimeException createExceptionForRecordReflectionException(
      ReflectiveOperationException exception) {
    throw new RuntimeException(
        "Unexpected ReflectiveOperationException occurred"
            + " (Gson "
            + GsonBuildConfig.VERSION
            + ")."
            + " To support Java records, reflection is utilized to read out information"
            + " about records. All these invocations happens after it is established"
            + " that records exist in the JVM. This exception is unexpected behavior.",
        exception);
  }

  @Override
  boolean isRecord(Class<?> raw) {
    try {
      return (boolean) isRecord.invoke(raw);
    } catch (ReflectiveOperationException e) {
      throw createExceptionForRecordReflectionException(e);
    }
  }

  @Override
  public String[] getRecordComponentNames(Class<?> raw) {
    try {
      Object[] recordComponents = (Object[]) getRecordComponents.invoke(raw);
      String[] componentNames = new String[recordComponents.length];
      for (int i = 0; i < recordComponents.length; i++) {
        componentNames[i] = (String) getName.invoke(recordComponents[i]);
      }
      return componentNames;
    } catch (ReflectiveOperationException e) {
      throw createExceptionForRecordReflectionException(e);
    }
  }

  @Override
  public <T> Constructor<T> getCanonicalRecordConstructor(Class<T> raw) {
    try {
      Object[] recordComponents = (Object[]) getRecordComponents.invoke(raw);
      Class<?>[] recordComponentTypes = new Class<?>[recordComponents.length];
      for (int i = 0; i < recordComponents.length; i++) {
        recordComponentTypes[i] = (Class<?>) getType.invoke(recordComponents[i]);
      }
      // Uses getDeclaredConstructor because implicit constructor has same visibility as record
      // and might therefore not be public
      return raw.getDeclaredConstructor(recordComponentTypes);
    } catch (ReflectiveOperationException e) {
      throw createExceptionForRecordReflectionException(e);
    }
  }

  @Override
  public Method getAccessor(Class<?> raw, Field field) {
    try {
      // Records consists of record components, each with a unique name, a corresponding field and
      // accessor method with the same name. Ref.:
      // https://docs.oracle.com/javase/specs/jls/se17/html/jls-8.html#jls-8.10.3
      return raw.getMethod(field.getName());
    } catch (ReflectiveOperationException e) {
      throw createExceptionForRecordReflectionException(e);
    }
  }
}
