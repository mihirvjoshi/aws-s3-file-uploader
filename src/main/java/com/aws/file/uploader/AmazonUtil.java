package com.aws.file.uploader;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Logger;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.PropertiesCredentials;

public class AmazonUtil {

	private static final Logger logger = Logger.getLogger("AmazonUtil");

	private static final String AWS_CREDENTIALS_CONFIG_FILE_PATH = "H:\\Tools\\Dev Tools\\workspace\\aws-s3-file-uploader\\aws-s3-file-uploader\\src\\main\\resources\\aws-credentials.properties";

	private static AWSCredentialsProvider awsCredentials;

	static {
		init();
	}

	private AmazonUtil() {

	}

	private static void init() {
		try {
			InputStream credentialsAsStream = new FileInputStream(AWS_CREDENTIALS_CONFIG_FILE_PATH);
			AWSCredentials credentials = new PropertiesCredentials(credentialsAsStream);
			awsCredentials = new AWSStaticCredentialsProvider(credentials);
		} catch (IOException e) {
			e.printStackTrace();
			// logger.error("Unable to initialize AWS Credentials from " +
			// AWS_CREDENTIALS_CONFIG_FILE_PATH);
		}
	}

	public static AWSCredentialsProvider getAwsCredentials() {
		return awsCredentials;
	}

}
