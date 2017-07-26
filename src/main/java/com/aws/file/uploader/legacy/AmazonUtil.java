package com.aws.file.uploader.legacy;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.PropertiesCredentials;

public class AmazonUtil {  	   
    private static final Log logger = LogFactory.getLog(AmazonUtil.class);
    private static final String AWS_CREDENTIALS_CONFIG_FILE_PATH = "src/main/resources/aws-credentials.properties";
    private static AWSCredentials awsCredentials;  
      
	static {
		init();
	}  
      
    private AmazonUtil() {               
    }  
      
	private static void init() {
		try {
			InputStream credentialsAsStream = new FileInputStream(AWS_CREDENTIALS_CONFIG_FILE_PATH);
			awsCredentials = new PropertiesCredentials(credentialsAsStream);
		} catch (IOException e) {
			 logger.error("Unable to initialize AWS Credentials from " + AWS_CREDENTIALS_CONFIG_FILE_PATH);
		}
	}  
      
	public static AWSCredentials getAwsCredentials() {
		return awsCredentials;
	}  
      
}  
