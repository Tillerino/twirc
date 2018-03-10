package org.tillerino.irc.server.compiler;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import org.tillerino.irc.server.Connection;

public class Finders {
  final Map<Object, Method> methods = new HashMap<>();

  public boolean isFindable(AnnotatedType type) {
    if (!(type.getType() instanceof Class)) {
      return false;
    }
    Class<?> cls = (Class<?>) type.getType();
    if (methods.containsKey(type)) {
      return true;
    }

    FoundBy annotation = cls.getAnnotation(FoundBy.class);
    if (annotation == null) {
      return false;
    }
    Class<?> finder = annotation.value();
    for (Method method : finder.getMethods()) {
      Finds findsAnnotation = method.getAnnotation(Finds.class);
      if (findsAnnotation == null || !findsAnnotation.value().equals(cls)) {
        continue;
      }
      Annotation[] returnedAnnotations = method.getAnnotatedReturnType().getAnnotations();
      Annotation[] expectedAnnotations = type.getAnnotations();
      if (!Arrays.equals(returnedAnnotations, expectedAnnotations)) {
        continue;
      }
      verifyAndPutMethod(type, finder, method);
      return true;
    }
    throw new RuntimeException(type + " claims to be found by " + finder.getSimpleName()
        + " but no such method can be found");
  }

  private void verifyAndPutMethod(Object type, Class<?> finder, Method method) {
    if (method.getParameterCount() < 1) {
      throw new RuntimeException(method + " does not take any arguments");
    }
    if (!method.getParameterTypes()[0].equals(String.class)
        && !method.getParameterTypes()[0].equals(CharSequence.class)) {
      throw new RuntimeException(method + " does not take a String argument");
    }
    if (method.getParameterCount() > 1 && !method.getParameterTypes()[1].equals(Connection.class)) {
      throw new RuntimeException(method + " takes too many arguments");
    }
    methods.put(type, method);
  }

  public Method get(AnnotatedType type) {
    if (!methods.containsKey(type)) {
      throw new IllegalStateException("shouldda asked first");
    }
    return methods.get(type);
  }
}
