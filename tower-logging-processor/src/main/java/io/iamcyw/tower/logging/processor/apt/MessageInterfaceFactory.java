package io.iamcyw.tower.logging.processor.apt;

import io.iamcyw.tower.logging.processor.model.MessageInterface;
import io.iamcyw.tower.logging.processor.model.MessageMethod;
import io.iamcyw.tower.logging.processor.util.ElementHelper;
import org.jboss.logging.BasicLogger;
import org.jboss.logging.annotations.MessageBundle;
import org.jboss.logging.annotations.MessageLogger;
import org.jboss.logging.annotations.ValidIdRange;
import org.jboss.logging.annotations.ValidIdRanges;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.Types;
import java.util.*;
import java.util.stream.Collectors;

import static io.iamcyw.tower.logging.processor.util.Objects.areEqual;

public final class MessageInterfaceFactory {
    private static final Object LOCK = new Object();
    private static volatile LoggerInterface LOGGER_INTERFACE;

    /**
     * Private constructor for factory.
     */
    private MessageInterfaceFactory() {
    }

    /**
     * Creates a message interface from the {@link javax.lang.model.element.TypeElement} specified by the {@code
     * interfaceElement} parameter.
     *
     * @param processingEnv        the annotation processing environment.
     * @param interfaceElement     the interface element to parse.
     * @param expressionProperties the properties used to resolve expressions
     *
     * @return a message interface for the interface element.
     */
    public static MessageInterface of(final ProcessingEnvironment processingEnv, final TypeElement interfaceElement,
                                      final Properties expressionProperties, final boolean addGeneratedAnnotation) {
        final Types types = processingEnv.getTypeUtils();
        if (types.isSameType(interfaceElement.asType(), ElementHelper.toType(processingEnv.getElementUtils(), BasicLogger.class))) {
            MessageInterface result = LOGGER_INTERFACE;
            if (result == null) {
                synchronized (LOCK) {
                    result = LOGGER_INTERFACE;
                    if (result == null) {
                        LOGGER_INTERFACE = LoggerInterface.of(processingEnv);
                        result = LOGGER_INTERFACE;
                    }
                }
            }
            return result;
        }
        final AptMessageInterface result = new AptMessageInterface(interfaceElement, processingEnv, expressionProperties, addGeneratedAnnotation);
        result.init();
        for (TypeMirror typeMirror : interfaceElement.getInterfaces()) {
            final MessageInterface extended = MessageInterfaceFactory.of(processingEnv, (TypeElement) types.asElement(typeMirror),
                                                                         expressionProperties, addGeneratedAnnotation);
            result.extendedInterfaces.add(extended);
            result.extendedInterfaces.addAll(extended.extendedInterfaces());
        }
        return result;
    }

    /**
     * Message interface implementation.
     */
    private static class AptMessageInterface extends AbstractClassType implements MessageInterface {
        private final TypeElement interfaceElement;
        private final Set<MessageInterface> extendedInterfaces;
        private final List<MessageMethod> messageMethods;
        private final List<ValidIdRange> validIdRanges;
        private final Properties expressionProperties;
        private final TypeElement generatedAnnotation;
        private String projectCode;
        private String packageName;
        private String simpleName;
        private String qualifiedName;
        private String fqcn;
        private int idLen;

        private AptMessageInterface(final TypeElement interfaceElement, final ProcessingEnvironment processingEnv,
                                    final Properties expressionProperties, final boolean addGeneratedAnnotation) {
            super(processingEnv, interfaceElement);
            this.interfaceElement = interfaceElement;
            this.expressionProperties = expressionProperties;
            this.messageMethods = new LinkedList<>();
            this.extendedInterfaces = new LinkedHashSet<>();
            if (ElementHelper.isAnnotatedWith(interfaceElement, ValidIdRanges.class)) {
                validIdRanges = Arrays.asList(interfaceElement.getAnnotation(ValidIdRanges.class).value());
            } else if (ElementHelper.isAnnotatedWith(interfaceElement, ValidIdRange.class)) {
                validIdRanges = Collections.singletonList(interfaceElement.getAnnotation(ValidIdRange.class));
            } else {
                validIdRanges = Collections.emptyList();
            }
            // Determine the type for the generated annotation
            TypeElement generatedAnnotation = null;
            if (addGeneratedAnnotation) {
                generatedAnnotation = processingEnv.getElementUtils().getTypeElement("javax.annotation.Generated");
                if (generatedAnnotation == null) {
                    // As of Java 9 the annotation has been moved to the javax.annotation.processing package
                    generatedAnnotation = processingEnv.getElementUtils().getTypeElement("javax.annotation.processing.Generated");
                }
            }
            this.generatedAnnotation = generatedAnnotation;
        }

        @Override
        public boolean extendsLoggerInterface() {
            return LOGGER_INTERFACE != null && extendedInterfaces.contains(LOGGER_INTERFACE);
        }

        @Override
        public String name() {
            return qualifiedName;
        }

        @Override
        public Set<MessageInterface> extendedInterfaces() {
            return Collections.unmodifiableSet(extendedInterfaces);
        }

        @Override
        public int hashCode() {
            return Objects.hash(qualifiedName);
        }

        @Override
        public Collection<MessageMethod> methods() {
            return messageMethods;
        }

        @Override
        public int compareTo(final MessageInterface o) {
            return this.name().compareTo(o.name());
        }

        @Override
        public String projectCode() {
            return projectCode;
        }

        private void init() {
            final MessageMethodBuilder builder = MessageMethodBuilder.create(processingEnv, expressionProperties)
                                                                     .add(getMessageMethods(interfaceElement));
            final Collection<MessageMethod> m = builder.build();
            this.messageMethods.addAll(m);
            final MessageBundle messageBundle = interfaceElement.getAnnotation(MessageBundle.class);
            final MessageLogger messageLogger = interfaceElement.getAnnotation(MessageLogger.class);
            if (messageBundle != null) {
                projectCode = messageBundle.projectCode();
                idLen = messageBundle.length();
            } else if (messageLogger != null) {
                projectCode = messageLogger.projectCode();
                idLen = messageLogger.length();
            } else {
                throw new ProcessingException(interfaceElement, "Interface is not annotated with @MessageBundle or @MessageLogger");
            }
            qualifiedName = elements.getBinaryName(interfaceElement).toString();
            final int lastDot = qualifiedName.lastIndexOf(".");
            if (lastDot > 0) {
                packageName = qualifiedName.substring(0, lastDot);
                simpleName = qualifiedName.substring(lastDot + 1);
            } else {
                packageName = null;
                simpleName = qualifiedName;
            }
            // Get the FQCN
            final TypeElement loggingClass = ElementHelper.getClassAnnotationValue(interfaceElement, MessageLogger.class, "loggingClass");
            if (loggingClass != null) {
                final String value = loggingClass.getQualifiedName().toString();
                if (!value.equals(Void.class.getName())) {
                    fqcn = value;
                }
            }
        }

        @Override
        public String packageName() {
            return packageName;
        }

        @Override
        public String getComment() {
            return elements.getDocComment(interfaceElement);
        }

        @Override
        public String simpleName() {
            return simpleName;
        }

        @Override
        public String loggingFQCN() {
            return fqcn;
        }

        @Override
        public List<ValidIdRange> validIdRanges() {
            return validIdRanges;
        }

        @Override
        public int getIdLength() {
            return idLen;
        }


        @Override
        public TypeElement getDelegate() {
            return interfaceElement;
        }

        @Override
        public TypeElement generatedAnnotation() {
            return generatedAnnotation;
        }

        @Override
        public Properties expressionProperties() {
            return expressionProperties;
        }

        @Override
        public boolean equals(final Object obj) {
            if (obj == this) {
                return true;
            }
            if (!(obj instanceof AptMessageInterface)) {
                return false;
            }
            final AptMessageInterface other = (AptMessageInterface) obj;
            return areEqual(name(), other.name());
        }


        @Override
        public String toString() {
            return io.iamcyw.tower.logging.processor.util.Objects.ToStringBuilder.of(this).add(qualifiedName).toString();
        }


    }

    private static class LoggerInterface extends AbstractClassType implements MessageInterface {
        private final TypeElement loggerInterface;
        private final Set<MessageMethod> messageMethods;

        private LoggerInterface(final ProcessingEnvironment processingEnv, final TypeElement loggerInterface) {
            super(processingEnv, loggerInterface.asType());
            messageMethods = new LinkedHashSet<>();
            this.loggerInterface = loggerInterface;
        }

        private void init() {
            final MessageMethodBuilder builder = MessageMethodBuilder.create(processingEnv)
                                                                     .add(getMessageMethods(loggerInterface));
            final Collection<MessageMethod> m = builder.build();
            this.messageMethods.addAll(m);
        }

        static LoggerInterface of(final ProcessingEnvironment processingEnv) {
            final LoggerInterface result = new LoggerInterface(processingEnv, ElementHelper.toTypeElement(processingEnv, BasicLogger.class));
            result.init();
            return result;
        }

        @Override
        public boolean extendsLoggerInterface() {
            return false;
        }

        @Override
        public Set<MessageInterface> extendedInterfaces() {
            return Collections.emptySet();
        }

        @Override
        public Collection<MessageMethod> methods() {
            return messageMethods;
        }

        @Override
        public String projectCode() {
            return null;
        }

        @Override
        public String name() {
            return BasicLogger.class.getName();
        }

        @Override
        public String packageName() {
            return BasicLogger.class.getPackage().getName();
        }

        @Override
        public String simpleName() {
            return BasicLogger.class.getSimpleName();
        }

        @Override
        public String loggingFQCN() {
            return null;
        }

        @Override
        public List<ValidIdRange> validIdRanges() {
            return Collections.emptyList();
        }

        @Override
        public int getIdLength() {
            return -1;
        }

        @Override
        public int hashCode() {
            return io.iamcyw.tower.logging.processor.util.Objects.HashCodeBuilder.builder().add(name()).toHashCode();
        }

        @Override
        public boolean equals(final Object obj) {
            if (obj == this) {
                return true;
            }
            if (!(obj instanceof AptMessageInterface)) {
                return false;
            }
            final AptMessageInterface other = (AptMessageInterface) obj;
            return areEqual(name(), other.name());
        }

        @Override
        public int compareTo(final MessageInterface o) {
            return this.name().compareTo(o.name());
        }

        @Override
        public String toString() {
            return io.iamcyw.tower.logging.processor.util.Objects.ToStringBuilder.of(this).add(name()).toString();
        }

        @Override
        public String getComment() {
            return elements.getDocComment(loggerInterface);
        }

        @Override
        public TypeElement getDelegate() {
            return loggerInterface;
        }
    }

    private static Collection<ExecutableElement> getMessageMethods(final TypeElement intf) {
        return ElementFilter.methodsIn(intf.getEnclosedElements())
                            .stream()
                            .filter(method -> !method.isDefault() && !method.getModifiers().contains(Modifier.STATIC))
                            .collect(Collectors.toList());
    }
}
