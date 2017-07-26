package com.aws.file.uploader.legacy;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class FilePropertiesUtil {
	private static final Log logger = LogFactory.getLog(AmazonS3FileUploader.class);
	private static Properties prop = new Properties();

	public static final String DEFAULT_FILE_PART_SIZE = "default-file-part-size";
	public static final String MAX_FILE_SIZE="max-file-size";
	public static final String RESUME_UPLOAD_FILE_NAME="resume-upload-file";
	public static final String BUCKET_NAME="bucket-name";
	public static final String MAX_CONNECTIONS="max-conn";
	public static final String CONN_TIMEOUT="conn-timeout";
	public static final String SOCKET_TIMEOUT="socket-timeout";
	public static final String THREAD_POOL_SIZE="thread-pool-size";
	public static final String MULTIPART_THRESHOLD="multipart-threshold";
	public static final String MAIL_HOST="mail-host";
	
	private static InputStream input = null;
	public static final String FILE_PATH="src/main/resources/aws-configs.properties";
	

	public static Properties loadProperties() {
		try {
			input = new FileInputStream(FILE_PATH);
			// load a properties file
			prop.load(input);
		} catch (IOException ex) {
			logger.error("Cannot load aws-configs from the property file. please check the path",ex);
		} finally {
			if (input != null) {
				try {
					input.close();
				} catch (IOException e) {
					logger.error("Cannot close the input stream. please check the connection.",e);
				}
			}
		}
		return prop;
	}
}
