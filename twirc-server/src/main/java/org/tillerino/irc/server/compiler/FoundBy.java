package org.tillerino.irc.server.compiler;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Points out the class which finds the annotated class for a {@link CommandHandler}.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface FoundBy {
  /**
   * Class that has a method annotated with {@link Finds}.
   */
  Class<?> value();
}
