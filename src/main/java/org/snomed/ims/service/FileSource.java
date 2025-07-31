package org.snomed.ims.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.time.Instant;
import java.util.Properties;

public class FileSource {

	private final File file;
	private Instant lastModified;
	private final Logger logger = LoggerFactory.getLogger(getClass());

	public FileSource(File directory, String filename) {
		file = new File(directory, filename);
		if (!file.isFile()) {
			logger.error("{} file does not exist in the working directory. Please create it here: {}",
					filename, file.getAbsolutePath());
			System.exit(1);
		}
	}

	public boolean hasChanged() throws IOException {
		return !Files.getLastModifiedTime(file.toPath()).toInstant().equals(lastModified);
	}

	public Properties readProperties() throws IOException {
		try (InputStream resourceAsStream = new FileInputStream(file)) {
			Properties properties = new Properties();
			properties.load(resourceAsStream);
			lastModified = Files.getLastModifiedTime(file.toPath()).toInstant();
			return properties;
		} catch (IOException e) {
			throw new IOException("Failed to read %s properties file.".formatted(file.getAbsolutePath()), e);
		}
	}

}
