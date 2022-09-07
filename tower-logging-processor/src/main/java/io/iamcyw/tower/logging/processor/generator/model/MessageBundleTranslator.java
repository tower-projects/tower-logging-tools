package io.iamcyw.tower.logging.processor.generator.model;

import io.iamcyw.tower.logging.processor.model.MessageInterface;
import io.iamcyw.tower.logging.processor.model.MessageMethod;
import org.jboss.jdeparser.JClassDef;
import org.jboss.jdeparser.JMethodDef;

import javax.annotation.processing.ProcessingEnvironment;
import java.util.*;

public class MessageBundleTranslator extends InterfaceModel {

    /**
     * Construct a class model.
     *
     * @param processingEnv    the processing environment
     * @param messageInterface the message interface to implement.
     * @param className
     * @param superClassName   the super class used for the translation implementations.
     */
    MessageBundleTranslator(final ProcessingEnvironment processingEnv, final MessageInterface messageInterface,
                            final String className, Map<String, Map<MessageMethod, String>> translations) {
        super(processingEnv, messageInterface, className, translations);

    }

    @Override
    public JClassDef generateModel() throws IllegalStateException {
        JClassDef classDef = super.generateModel();



        return classDef;
    }

}
