package io.iamcyw.tower.logging.processor.generator.model;

import io.iamcyw.tower.logging.processor.model.MessageInterface;
import io.iamcyw.tower.logging.processor.model.MessageMethod;
import org.jboss.jdeparser.JCall;
import org.jboss.jdeparser.JClassDef;
import org.jboss.jdeparser.JMod;
import org.jboss.logging.annotations.MessageBundle;
import org.jboss.logging.annotations.MessageLogger;

import javax.annotation.processing.ProcessingEnvironment;
import java.util.LinkedHashSet;
import java.util.Set;

public class MessageBundleImplementor extends ImplementationClassModel {

    /**
     * Creates a new message bundle code model.
     *
     * @param processingEnv    the processing environment
     * @param messageInterface the message interface to implement.
     */
    public MessageBundleImplementor(final ProcessingEnvironment processingEnv, final MessageInterface messageInterface) {
        super(processingEnv, messageInterface);
    }

    @Override
    protected JClassDef generateModel() throws IllegalStateException {
        final JClassDef classDef = super.generateModel();
        // Add default constructor
        classDef.constructor(JMod.PROTECTED);
        createReadResolveMethod();
        final JCall localeGetter = createLocaleGetter(null, false);
        final Set<MessageMethod> messageMethods = new LinkedHashSet<>();
        messageMethods.addAll(messageInterface().methods());
        for (MessageInterface messageInterface : messageInterface().extendedInterfaces()) {
            if (messageInterface.isAnnotatedWith(MessageBundle.class) || messageInterface.isAnnotatedWith(MessageLogger.class)) {
                messageMethods.addAll(messageInterface.methods());
            }
        }
        // Process the method descriptors and add to the model before
        // writing.
        for (MessageMethod messageMethod : messageMethods) {
            createBundleMethod(classDef, localeGetter, messageMethod);
        }
        return classDef;
    }
}
