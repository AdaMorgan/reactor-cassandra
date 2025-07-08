package com.github.utils;

import com.github.TestHelper;
import org.assertj.core.api.iterable.ThrowingExtractor;
import org.junit.jupiter.api.TestInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

public class SnapshotHandler
{
    private final TestInfo testInfo;
    private final Logger logger;

    public SnapshotHandler(TestInfo testInfo)
    {
        this.testInfo = testInfo;
        this.logger = LoggerFactory.getLogger(SnapshotHandler.class);
    }

    public void compareWithSnapshot(String actual, String suffix)
    {
        compareWithSnapshot(stream -> new String(TestHelper.readFully(stream), StandardCharsets.UTF_8), actual, suffix);
    }

    private void compareWithSnapshot(ThrowingExtractor<InputStream, String, Exception> reader, String actual, String suffix)
    {
        Class<?> currentClass = testInfo.getTestClass().orElseThrow(AssertionError::new);
        String filePath = getFilePath(suffix, "txt");

        try (InputStream stream = currentClass.getResourceAsStream(filePath))
        {
            assertThat(stream).as("Loading sample from resource file '%s'", filePath).isNotNull();
            assertThat(reader.apply(stream)).isEqualToNormalizingWhitespace(actual);
        }
        catch (IOException e)
        {
            throw new UncheckedIOException(e);
        }
        catch (AssertionError e)
        {
            try
            {
                updateOrCreateIfNecessary(actual, suffix, "txt");
            }
            catch (Exception exception)
            {
                e.addSuppressed(exception);
            }
            throw e;
        }
    }

    private void updateOrCreateIfNecessary(String actual, String suffix, String extension) throws IOException
    {
        if (System.getProperty("updateSnapshots") == null)
        {
            return;
        }

        Class<?> currentClass = testInfo.getTestClass().orElseThrow(AssertionError::new);
        String filePath = getFilePath(suffix, extension);

        String workingDirectory = System.getProperty("user.dir");
        String path = currentClass.getPackage().getName().replace(".", "/") + "/" + filePath;

        java.nio.file.Path fileLocation = java.nio.file.Paths.get(workingDirectory).resolve("src/test/resources").resolve(path);

        File file = fileLocation.toFile();
        if (!file.exists())
        {
            logger.info("Creating snapshot {}", file);
            file.getParentFile().mkdirs();
            assertThat(file.createNewFile()).isTrue();
        }

        try (FileWriter writer = new FileWriter(file))
        {
            try (BufferedWriter bufferedWriter = new BufferedWriter(writer))
            {
                logger.info("Updating snapshot {}", file);
                bufferedWriter.write(actual);
            }
        }
    }

    private String getFilePath(String suffix, String extension)
    {
        Class<?> currentClass = testInfo.getTestClass().orElseThrow(AssertionError::new);
        Method testMethod = testInfo.getTestMethod().orElseThrow(AssertionError::new);
        String fileName = currentClass.getSimpleName() + "/" + testMethod.getName();
        if (suffix != null && !suffix.isEmpty())
        {
            fileName += "_" + suffix;
        }
        fileName += "." + extension;
        return fileName;
    }
}
