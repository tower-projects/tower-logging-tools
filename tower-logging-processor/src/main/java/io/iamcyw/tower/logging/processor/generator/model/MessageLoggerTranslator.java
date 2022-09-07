package io.iamcyw.tower.logging.processor.generator.model;

import io.iamcyw.tower.logging.processor.model.MessageInterface;
import io.iamcyw.tower.logging.processor.model.MessageMethod;

import javax.annotation.processing.ProcessingEnvironment;
import java.util.*;

public class MessageLoggerTranslator extends InterfaceModel {

    /**
     * The logger parameter name.
     */
    private static final String LOGGER_PARAMETER_NAME = "logger";


    MessageLoggerTranslator(final ProcessingEnvironment processingEnv, final MessageInterface messageInterface,
                            final String className, Map<String, Map<MessageMethod, String>> translations) {
        super(processingEnv, messageInterface, className, translations);
    }

}
