package io.iamcyw.tower.logging.processor.apt;

import io.iamcyw.tower.logging.processor.generator.model.ClassModel;
import io.iamcyw.tower.logging.processor.generator.model.ClassModelFactory;
import io.iamcyw.tower.logging.processor.model.MessageInterface;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.TypeElement;
import java.io.IOException;
import java.util.Map;

public class ImplementationClassGenerator extends AbstractGenerator {

    private static final String LOGGING_VERSION = "loggingVersion";
    /**
     * Constructs a new processor.
     *
     * @param processingEnv the processing environment.
     */
    public ImplementationClassGenerator(ProcessingEnvironment processingEnv) {
        super(processingEnv);
        final Map<String, String> options = processingEnv.getOptions();
        if (options.containsKey(LOGGING_VERSION)) {
            logger().warn(null, "The option %s has been deprecated and is no longer used.", LOGGING_VERSION);
        }
    }

    @Override
    public void processTypeElement(TypeElement annotation, TypeElement element, MessageInterface messageInterface) {
        try {
            final ClassModel classModel = ClassModelFactory.implementation(processingEnv, messageInterface);
            classModel.generateAndWrite();
        } catch (IllegalStateException | IOException e) {
            logger().error(element, e);
        }
    }

}
