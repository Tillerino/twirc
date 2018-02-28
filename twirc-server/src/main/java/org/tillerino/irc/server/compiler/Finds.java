package org.tillerino.irc.server.compiler;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a methods that can be used to find objects for a {@link CommandHandler}.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Finds {
  /**
   * The class which can be found by the annotated method. Must match the raw type of the return
   * value.
   */
  Class<?> value();
}
