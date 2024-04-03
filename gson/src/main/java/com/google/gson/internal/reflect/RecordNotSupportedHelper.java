package com.google.gson.internal.reflect;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/** Instance used when records are not supported */
public class RecordNotSupportedHelper extends RecordHelper {

  private static final String ERRORMSG = "Records are not supported on this JVM, this method should not be called";
  @Override
  boolean isRecord(Class<?> clazz) {
    return false;
  }

  @Override
  String[] getRecordComponentNames(Class<?> clazz) {
    throw new UnsupportedOperationException(ERRORMSG);
  }

  @Override
  <T> Constructor<T> getCanonicalRecordConstructor(Class<T> raw) {
    throw new UnsupportedOperationException(ERRORMSG);
  }

  @Override
  public Method getAccessor(Class<?> raw, Field field) {
    throw new UnsupportedOperationException(ERRORMSG);
  }
}