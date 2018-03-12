package org.tillerino.irc.server.compiler;

import static org.objectweb.asm.Opcodes.ACC_PRIVATE;
import static org.objectweb.asm.Opcodes.ALOAD;
import static org.objectweb.asm.Opcodes.ARETURN;
import static org.objectweb.asm.Opcodes.ASTORE;
import static org.objectweb.asm.Opcodes.CHECKCAST;
import static org.objectweb.asm.Opcodes.DUP;
import static org.objectweb.asm.Opcodes.GETFIELD;
import static org.objectweb.asm.Opcodes.GOTO;
import static org.objectweb.asm.Opcodes.IFNE;
import static org.objectweb.asm.Opcodes.INVOKEINTERFACE;
import static org.objectweb.asm.Opcodes.INVOKESPECIAL;
import static org.objectweb.asm.Opcodes.INVOKESTATIC;
import static org.objectweb.asm.Opcodes.INVOKEVIRTUAL;
import static org.objectweb.asm.Opcodes.NEW;
import static org.objectweb.asm.Opcodes.POP;
import static org.objectweb.asm.Opcodes.PUTFIELD;
import static org.objectweb.asm.Opcodes.RETURN;
import java.lang.reflect.AnnotatedParameterizedType;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang3.StringUtils;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.tillerino.irc.server.Connection;
import org.tillerino.irc.server.Response;
import org.tillerino.irc.server.util.IrcLineSplitter;

/**
 * Creates {@link CommandHandler} child classes to wrap methods annotated with {@link Handler}.
 * Arguments of the wrapped are created by methods annotated by {@link Finds}.
 */
public class CommandHandlerWriter {
  static final String HANDLE_SIGNATURE =
      Type.getMethodDescriptor(Stream.of(CommandHandler.class.getMethods())
          .filter(m -> m.getName().equals("handle")).findAny().get());

  static final Method GET_STRING;
  static final Method GET_STRING_LIST;
  static final Method GET_SEQ;
  static final Constructor<?> ARRAY_LIST_CONSTRUCTOR;
  static final Method ITERATOR;
  static final Method HAS_NEXT;
  static final Method NEXT;
  static final Method ADD;

  static {
    try {
      GET_STRING = IrcLineSplitter.class.getMethod("getString");
      GET_STRING_LIST = IrcLineSplitter.class.getMethod("getList");
      GET_SEQ = IrcLineSplitter.class.getMethod("getSeq");
      ARRAY_LIST_CONSTRUCTOR = ArrayList.class.getConstructor();
      ITERATOR = Iterable.class.getMethod("iterator");
      HAS_NEXT = Iterator.class.getMethod("hasNext");
      NEXT = Iterator.class.getMethod("next");
      ADD = List.class.getMethod("add", Object.class);
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private final Finders finders;
  private final Class<? extends Object> handlerClass;
  private final Method method;
  private final boolean isStatic;
  private final String generatedClassName;

  Map<Class<?>, Integer> fieldsIndex = new LinkedHashMap<>();

  public CommandHandlerWriter(Finders finders, Class<? extends Object> handlerClass, Method method) {
    this.finders = finders;
    this.handlerClass = handlerClass;
    this.method = method;
    if (!method.getReturnType().equals(Response.class)) {
      throw new IllegalArgumentException("Handler " + method + " must return " + Response.class);
    }
    isStatic = Modifier.isStatic(method.getModifiers());
    generatedClassName = getCorrectNestedClassName(handlerClass) + "$"
        + StringUtils.capitalize(method.getName()) + "Handler";
  }

  static String getCorrectNestedClassName(Class<?> clazz) {
    if (clazz.getEnclosingClass() != null) {
      return getCorrectNestedClassName(clazz.getEnclosingClass()) + "$" + clazz.getSimpleName();
    }
    String canonicalName = clazz.getCanonicalName();
    if (canonicalName == null) {
      throw new RuntimeException();
    }
    return canonicalName.replace('.', '/');
  }

  public byte[] write() {
    ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
    cw.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC | Opcodes.ACC_SUPER, generatedClassName, null,
        Type.getInternalName(CommandHandler.class), null);
    writeFields(cw);
    writeConstructor(cw);
    writeHandlerMethod(cw);
    cw.visitEnd();
    return cw.toByteArray();
  }

  private void writeFields(ClassWriter cw) {
    if (!isStatic) {
      writeField(cw, handlerClass);
    }
    for (Parameter parameter : method.getParameters()) {
      AnnotatedType annotatedType = parameter.getAnnotatedType();
      if (finders.isFindable(annotatedType)) {
        writeField(cw, finders.get(annotatedType).getDeclaringClass());
      }
      if (annotatedType instanceof AnnotatedParameterizedType) {
        writeField(cw, finders.get(getListNestedClass((AnnotatedParameterizedType) annotatedType))
            .getDeclaringClass());
      }
    }
  }

  private void writeField(ClassWriter cw, Class<?> cls) {
    fieldsIndex.computeIfAbsent(cls, type -> {
      String fieldName = type.getSimpleName() + (fieldsIndex.size() + 1);
      cw.visitField(ACC_PRIVATE, fieldName, Type.getDescriptor(type), null, null);
      return fieldsIndex.size() + 1;
    });
  }

  private String getFieldName(Class<?> cls) {
    Integer fieldIndex = fieldsIndex.get(cls);
    if (fieldIndex == null) {
      throw new RuntimeException("Don't have a field for " + cls);
    }
    return cls.getSimpleName() + fieldIndex;
  }

  private void writeConstructor(ClassWriter cw) {
    String desc = fieldsIndex.keySet().stream().map(Type::getDescriptor)
        .collect(Collectors.joining("", "(", ")V"));
    MethodVisitor visitor = cw.visitMethod(Opcodes.ACC_PUBLIC, "<init>", desc, null, null);
    visitor.visitCode();
    visitor.visitVarInsn(ALOAD, 0);
    visitor.visitMethodInsn(INVOKESPECIAL, Type.getInternalName(CommandHandler.class), "<init>",
        "()V", false);
    fieldsIndex.keySet().stream().forEach(type -> {
      visitor.visitVarInsn(ALOAD, 0);
      visitor.visitVarInsn(ALOAD, fieldsIndex.get(type));
      visitor.visitFieldInsn(PUTFIELD, generatedClassName, getFieldName(type),
          Type.getDescriptor(type));
    });
    visitor.visitInsn(RETURN);
    visitor.visitMaxs(0, 0);
    visitor.visitFrame(0, 0, new Object[0], 0, new Object[0]);
    visitor.visitEnd();
  }

  private void writeHandlerMethod(ClassWriter cw) {
    MethodVisitor visitor =
        cw.visitMethod(Opcodes.ACC_PUBLIC, "handle", HANDLE_SIGNATURE, null, null);
    visitor.visitCode();
    if (!isStatic) {
      loadField(visitor, handlerClass);
    }
    for (Parameter parameter : method.getParameters()) {
      loadParameter(visitor, parameter.getAnnotatedType());
    }
    invoke(visitor, method);
    visitor.visitInsn(ARETURN);
    visitor.visitMaxs(0, 0);
    visitor.visitFrame(0, 0, new Object[0], 0, new Object[0]);
    visitor.visitEnd();
  }

  private void invoke(MethodVisitor handle, Method method) {
    String desc = Type.getMethodDescriptor(method);
    handle.visitMethodInsn(
        Modifier.isStatic(method.getModifiers()) ? INVOKESTATIC
            : method.getDeclaringClass().isInterface() ? INVOKEINTERFACE : INVOKEVIRTUAL,
        Type.getInternalName(method.getDeclaringClass()), method.getName(), desc,
        method.getDeclaringClass().isInterface());
  }

  private void invoke(MethodVisitor handle, Constructor<?> constructor) {
    handle.visitMethodInsn(INVOKESPECIAL, Type.getInternalName(constructor.getDeclaringClass()),
        "<init>", Type.getConstructorDescriptor(constructor), false);
  }

  private void loadField(MethodVisitor handle, Class<?> type) {
    handle.visitVarInsn(ALOAD, 0);
    handle.visitFieldInsn(GETFIELD, generatedClassName, getFieldName(type),
        Type.getDescriptor(type));
  }

  private void loadParameter(MethodVisitor visitor, AnnotatedType type) {
    if (type.getType().equals(Connection.class)) {
      visitor.visitVarInsn(ALOAD, 1);
      return;
    }
    if (type.getType().equals(String.class)) {
      visitor.visitVarInsn(ALOAD, 2);
      invoke(visitor, GET_STRING);
      return;
    }
    if (type.getType().equals(CharSequence.class)) {
      visitor.visitVarInsn(ALOAD, 2);
      invoke(visitor, GET_SEQ);
      return;
    }
    if (finders.isFindable(type)) {
      Method loaderMethod = finders.get(type);
      loadField(visitor, loaderMethod.getDeclaringClass());
      loadParameter(visitor, loaderMethod.getParameters()[0].getAnnotatedType());
      invoke(visitor, loaderMethod);
      return;
    }
    if (type instanceof AnnotatedParameterizedType) {
      AnnotatedType nestedType = getListNestedClass((AnnotatedParameterizedType) type);
      visitor.visitVarInsn(ALOAD, 2);
      invoke(visitor, GET_STRING_LIST);
      invoke(visitor, ITERATOR);
      visitor.visitVarInsn(ASTORE, 3); // iterator
      visitor.visitTypeInsn(NEW, Type.getInternalName(ArrayList.class));
      visitor.visitInsn(DUP);
      invoke(visitor, ARRAY_LIST_CONSTRUCTOR);
      visitor.visitVarInsn(ASTORE, 4); // target

      Label check = new Label();
      visitor.visitJumpInsn(GOTO, check);
      Label next = new Label();
      visitor.visitLabel(next); // list.add(field.apply(iterator.next()));
      visitor.visitVarInsn(ALOAD, 4);
      Method loaderMethod = finders.get(nestedType);
      loadField(visitor, loaderMethod.getDeclaringClass());
      visitor.visitVarInsn(ALOAD, 3);
      invoke(visitor, NEXT);
      visitor.visitTypeInsn(CHECKCAST, Type.getInternalName(String.class));
      invoke(visitor, loaderMethod);
      invoke(visitor, ADD);
      visitor.visitInsn(POP);
      visitor.visitLabel(check);
      visitor.visitVarInsn(ALOAD, 3);
      invoke(visitor, HAS_NEXT);
      visitor.visitJumpInsn(IFNE, next);
      visitor.visitVarInsn(ALOAD, 4);
      return;
    }
    throw new IllegalArgumentException(type.toString());
  }

  private AnnotatedType getListNestedClass(AnnotatedParameterizedType type) {
    java.lang.reflect.Type baseType = type.getType();
    if (!(baseType instanceof ParameterizedType)) {
      throw new RuntimeException();
    }
    if (!((ParameterizedType) baseType).getRawType().equals(List.class)) {
      throw new RuntimeException("Can only convert lists");
    }
    AnnotatedType nestedType = type.getAnnotatedActualTypeArguments()[0];
    if (!finders.isFindable(nestedType)) {
      throw new RuntimeException("Cannot load " + nestedType);
    }
    return nestedType;
  }
}
