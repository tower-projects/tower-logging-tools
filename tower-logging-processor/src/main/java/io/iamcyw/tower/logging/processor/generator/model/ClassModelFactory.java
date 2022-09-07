package io.iamcyw.tower.logging.processor.generator.model;

import io.iamcyw.tower.logging.processor.model.MessageInterface;
import io.iamcyw.tower.logging.processor.model.MessageMethod;
import org.jboss.logging.annotations.MessageBundle;
import org.jboss.logging.annotations.MessageLogger;

import javax.annotation.processing.ProcessingEnvironment;
import java.util.Map;

import static io.iamcyw.tower.logging.processor.generator.model.ClassModelHelper.implementationClassName;

public class ClassModelFactory {

    public static InterfaceModel translation(final ProcessingEnvironment processingEnv, final MessageInterface messageInterface, final Map<String, Map<MessageMethod, String>>  translations) throws IllegalArgumentException {
        final String generatedClassName = implementationClassName(messageInterface, "i18n");
        if (messageInterface.isAnnotatedWith(MessageBundle.class)) {
            return new MessageBundleTranslator(processingEnv, messageInterface, generatedClassName, translations);
        }
        if (messageInterface.isAnnotatedWith(MessageLogger.class)) {
            return new MessageLoggerTranslator(processingEnv, messageInterface, generatedClassName, translations);
        }
        throw new IllegalArgumentException(String.format("Message interface %s is not a valid message logger or message bundle.", messageInterface));
    }

    /**
     * Creates an implementation code model from the message interface.
     *
     * @param processingEnv    the processing environment
     * @param messageInterface the message interface to implement
     *
     * @return the class model used to implement the interface.
     *
     * @throws IllegalArgumentException if interface is not annotated with {@link MessageBundle @MessageBundle} or {@link MessageLogger @MessageLogger}
     */
    public static ClassModel implementation(final ProcessingEnvironment processingEnv, final MessageInterface messageInterface) throws IllegalArgumentException {
        if (messageInterface.isAnnotatedWith(MessageBundle.class)) {
            return new MessageBundleImplementor(processingEnv, messageInterface);
        }
        if (messageInterface.isAnnotatedWith(MessageLogger.class)) {
            return new MessageLoggerImplementor(processingEnv, messageInterface);
        }
        throw new IllegalArgumentException(String.format("Message interface %s is not a valid message logger or message bundle.", messageInterface));
    }
}
