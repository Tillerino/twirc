package org.tillerino.irc.server.compiler;

import static java.lang.annotation.ElementType.TYPE_USE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.lang.reflect.AnnotatedType;
import org.junit.Test;

public class FindersTest {
  SimpleClass simple;

  @SomeQualifier
  SimpleClass notSoSimple;

  Finders finders = new Finders();

  @Retention(RUNTIME)
  @Target(TYPE_USE)
  @interface SomeQualifier {
    
  }

  @FoundBy(SimpleFinder.class)
  public static class SimpleClass {
    
  }

  public interface SimpleFinder {
    @Finds(SimpleClass.class)
    SimpleClass finds(String something);

    @Finds(SimpleClass.class)
    @SomeQualifier SimpleClass findsMoreComplicated(String something);
  }

  @Test
  public void testSimple() throws Exception {
    AnnotatedType type = FindersTest.class.getDeclaredField("simple").getAnnotatedType();
    assertTrue(finders.isFindable(type));
    assertEquals(SimpleFinder.class.getMethod("finds", String.class), finders.get(type));
  }

  @Test
  public void testWithTypeUse() throws Exception {
    AnnotatedType type = FindersTest.class.getDeclaredField("notSoSimple").getAnnotatedType();
    assertTrue(finders.isFindable(type));
    assertEquals(SimpleFinder.class.getMethod("findsMoreComplicated", String.class), finders.get(type));
  }
}
