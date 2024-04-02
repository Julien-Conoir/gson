package com.google.gson.internal.reflect;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

public abstract class RecordHelper {

  abstract boolean isRecord(Class<?> clazz);

  abstract String[] getRecordComponentNames(Class<?> clazz);

  abstract <T> Constructor<T> getCanonicalRecordConstructor(Class<T> raw);

  public abstract Method getAccessor(Class<?> raw, Field field);
}
