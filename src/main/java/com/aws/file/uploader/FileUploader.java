package com.aws.file.uploader;

import java.io.File;

public class FileUploader {

	public static void main(String[] args) {
//		AmazonS3Util util = new AmazonS3Util();
//		AmazonS3FileUploadUtil util = new AmazonS3FileUploadUtil();
		AmazonS3FileUploadUsingXferManager util = new AmazonS3FileUploadUsingXferManager();
		File file = new File("H:\\Tools\\BigDeta\\Case Study\\Emirates Test Case\\SEED_DATA_SQL.sql");
//		File file = new File("H:\\Tools\\BigDeta\\Case Study\\Emirates Test Case\\2007.csv");
		util.putObjectAsMultiPart("intuit-account-data-1", file);		
	}
}
