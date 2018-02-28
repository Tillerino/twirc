package org.tillerino.irc.server.compiler;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
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

    if (!cls.isAnnotationPresent(FoundBy.class)) {
      return false;
    }
    Class<?> finder = cls.getAnnotation(FoundBy.class).value();
    Optional<Method> method =
        Stream.of(finder.getMethods()).filter(m -> m.isAnnotationPresent(Finds.class))
            .filter(m -> m.getAnnotation(Finds.class).value().equals(cls))
            .filter(m -> {
              Annotation[] returnedAnnotations = m.getAnnotatedReturnType().getAnnotations();
              Annotation[] expectedAnnotations = type.getAnnotations();
              return Arrays.equals(returnedAnnotations, expectedAnnotations);
            })
            .findFirst();
    verifyAndPutMethod(type, finder, method);
    return true;
  }

  private void verifyAndPutMethod(Object type, Class<?> finder, Optional<Method> method) {
    if (!method.isPresent()) {
      throw new RuntimeException(type + " claims to be found by "
          + finder.getSimpleName() + " but no such method can be found");
    }
    if (method.get().getParameterCount() < 1) {
      throw new RuntimeException(method.get() + " does not take any arguments");
    }
    if (!method.get().getParameterTypes()[0].equals(String.class) && !method.get().getParameterTypes()[0].equals(CharSequence.class)) {
      throw new RuntimeException(method.get() + " does not take a String argument");
    }
    if (method.get().getParameterCount() > 1 && !method.get().getParameterTypes()[1].equals(Connection.class)) {
      throw new RuntimeException(method.get() + " takes too many arguments");
    }
    methods.put(type, method.get());
  }

  public Method get(AnnotatedType type) {
    return methods.get(type);
  }
}
