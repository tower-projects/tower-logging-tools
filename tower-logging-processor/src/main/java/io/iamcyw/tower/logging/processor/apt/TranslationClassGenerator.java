package io.iamcyw.tower.logging.processor.apt;

import io.iamcyw.tower.logging.processor.generator.model.ClassModelFactory;
import io.iamcyw.tower.logging.processor.generator.model.InterfaceModel;
import io.iamcyw.tower.logging.processor.model.MessageInterface;
import io.iamcyw.tower.logging.processor.model.MessageMethod;
import io.iamcyw.tower.logging.processor.validation.FormatValidator;
import io.iamcyw.tower.logging.processor.validation.FormatValidatorFactory;
import io.iamcyw.tower.logging.processor.validation.StringFormatValidator;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageBundle;
import org.jboss.logging.annotations.MessageLogger;

import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.SupportedOptions;
import javax.lang.model.element.TypeElement;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import java.io.*;
import java.util.*;
import java.util.regex.Pattern;

import static io.iamcyw.tower.logging.processor.util.TranslationHelper.getTranslationClassNameSuffix;

@SupportedOptions(
        {TranslationClassGenerator.TRANSLATION_FILES_PATH_OPTION})
final class TranslationClassGenerator extends AbstractGenerator {
    private static final String TRANSLATION_FILE_EXTENSION_PATTERN = ".i18n_[a-z]*(_[A-Z]*){0,2}\\.properties";

    public static final String TRANSLATION_FILES_PATH_OPTION = "translationFilesPath";

    private final String translationFilesPath;


    /**
     * Construct an instance of the Translation
     * Class Generator.
     *
     * @param processingEnv the processing environment
     */
    public TranslationClassGenerator(final ProcessingEnvironment processingEnv) {
        super(processingEnv);
        Map<String, String> options = processingEnv.getOptions();
        this.translationFilesPath = options.get(TRANSLATION_FILES_PATH_OPTION);
    }

    @Override
    public void processTypeElement(final TypeElement annotation, final TypeElement element,
                                   final MessageInterface messageInterface) {
        try {
            final List<File> files = findTranslationFiles(messageInterface);
            final Map<File, Map<MessageMethod, String>> validTranslations = allInterfaceTranslations(messageInterface,
                                                                                                     files);
            generateSourceFileFor(messageInterface, validTranslations);
        } catch (IOException e) {
            logger().error(e, "Cannot read %s package files", messageInterface.packageName());
        }
    }

    private void generateSourceFileFor(final MessageInterface messageInterface,
                                       final Map<File, Map<MessageMethod, String>> translations) {

        final Map<String, Map<MessageMethod, String>> finalTranslations = new LinkedHashMap<>();
        for (File file : translations.keySet()) {
            finalTranslations.put(getTranslationClassNameSuffix(file.getName()),
                                  translations.get(file));
        }

        //Create source file
        final InterfaceModel classModel = ClassModelFactory.translation(processingEnv, messageInterface,
                                                                            finalTranslations);

        try {
            classModel.generateAndWrite();
        } catch (IllegalStateException | IOException e) {
            logger().error(e, "Cannot generate %s source file", classModel.qualifiedClassName());
        }
    }

    private Map<File, Map<MessageMethod, String>> allInterfaceTranslations(final MessageInterface messageInterface,
                                                                           final List<File> files) throws IOException {
        final Map<File, Map<MessageMethod, String>> validTranslations = new LinkedHashMap<>();
        for (MessageInterface superInterface : messageInterface.extendedInterfaces()) {
            validTranslations.putAll(allInterfaceTranslations(superInterface, findTranslationFiles(superInterface)));
        }
        if (files != null) {
            for (File file : files) {
                validTranslations.put(file, validateTranslationMessages(messageInterface, file));
            }
        }
        return validTranslations;
    }

    /**
     * Returns only the valid translations message corresponding
     * to the declared {@link MessageMethod} methods in the
     * {@link MessageBundle} or {@link MessageLogger}
     * interface.
     *
     * @param messageInterface the message interface.
     * @param file             the translation file
     * @return the valid translations messages
     */
    private Map<MessageMethod, String> validateTranslationMessages(final MessageInterface messageInterface,
                                                                   final File file) {
        Map<MessageMethod, String> validTranslations = new LinkedHashMap<>();

        try {

            //Load translations
            Properties translations = new Properties();
            translations.load(new InputStreamReader(new FileInputStream(file), "utf-8"));
            final Set<MessageMethod> messageMethods = new LinkedHashSet<>();
            messageMethods.addAll(messageInterface.methods());
            for (MessageInterface msgIntf : messageInterface.extendedInterfaces()) {
                if (msgIntf.isAnnotatedWith(MessageBundle.class) || msgIntf.isAnnotatedWith(MessageLogger.class)) {
                    messageMethods.addAll(msgIntf.methods());
                }
            }
            for (MessageMethod messageMethod : messageMethods) {
                final String key = messageMethod.translationKey();
                if (translations.containsKey(key)) {
                    final String translationMessage = translations.getProperty(key);
                    if (!translationMessage.trim()
                                           .isEmpty()) {
                        final FormatValidator validator = getValidatorFor(messageMethod, translationMessage);
                        if (validator.isValid()) {
                            if (validator.argumentCount() == messageMethod.formatParameterCount()) {
                                validTranslations.put(messageMethod, translationMessage);
                            } else {
                                logger().warn(messageMethod,
                                              "The parameter count for the format (%d) and the number of format parameters (%d) do not match.",
                                              validator.argumentCount(), messageMethod.formatParameterCount());
                            }
                        } else {
                            logger().warn(messageMethod, "%s Resource Bundle: %s", validator.summaryMessage(),
                                          file.getAbsolutePath());
                        }
                    } else {
                        logger().warn(messageMethod,
                                      "The translation message with key %s is ignored because value is empty or contains only whitespace",
                                      key);
                    }

                } else {
                    logger().warn(messageMethod,
                                  "The translation message with key %s have no corresponding messageMethod.", key);
                }
            }

        } catch (IOException e) {
            logger().error(e, "Cannot read the %s translation file", file.getName());
        }

        return validTranslations;
    }

    private static FormatValidator getValidatorFor(final MessageMethod messageMethod, final String translationMessage) {
        FormatValidator result = FormatValidatorFactory.create(messageMethod.message()
                                                                            .format(), translationMessage);
        if (result.isValid()) {
            if (messageMethod.message()
                             .format() == Message.Format.PRINTF) {
                result = StringFormatValidator.withTranslation(messageMethod.message()
                                                                            .value(), translationMessage);
            }
        }
        return result;
    }

    private List<File> findTranslationFiles(final MessageInterface messageInterface) throws IOException {
        final String packageName = messageInterface.packageName();
        final String interfaceName = messageInterface.simpleName();

        final String classTranslationFilesPath;

        //User defined
        if (translationFilesPath != null) {
            classTranslationFilesPath = translationFilesPath;

            //By default use the class output folder
        } else {
            FileObject fObj = processingEnv.getFiler()
                                           .getResource(StandardLocation.CLASS_OUTPUT, packageName, interfaceName);
            classTranslationFilesPath = fObj.toUri()
                                            .getPath()
                                            .replace(interfaceName, "");
        }
        final List<File> result;
        File[] files = new File(classTranslationFilesPath).listFiles(new TranslationFileFilter(interfaceName));
        if (files == null) {
            result = Collections.emptyList();
        } else {
            result = Arrays.asList(files);
            result.sort((o1, o2) -> {
                int result1 = o1.getAbsolutePath()
                                .compareTo(o2.getAbsolutePath());
                result1 = (result1 != 0 ? result1 : Integer.signum(o1.getName()
                                                                     .length() - o2.getName()
                                                                                .length()));
                return result1;
            });
        }
        return result;

    }

    /**
     * Translation file Filter.
     */
    private class TranslationFileFilter implements FilenameFilter {

        private final String className;

        /**
         * The property file filter.
         *
         * @param className the class that have i18n property file
         */
        public TranslationFileFilter(final String className) {
            this.className = className;
        }

        @Override
        public boolean accept(final File dir, final String name) {

            boolean isGenerated = false;
                    // name.endsWith(TranslationFileGenerator.GENERATED_FILE_EXTENSION);
            boolean isTranslationFile = name.matches(Pattern.quote(className) + TRANSLATION_FILE_EXTENSION_PATTERN);

            return !isGenerated && isTranslationFile;
        }

    }

}
