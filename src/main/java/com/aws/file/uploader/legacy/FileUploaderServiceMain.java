package com.aws.file.uploader.legacy;


public class FileUploaderServiceMain {

	public static void main(String[] args) {
		AmazonS3FileUploader util = new AmazonS3FileUploader();
//		File file = new File("H:\\Tools\\BigDeta\\Case Study\\Emirates Test Case\\carriers.csv");
//		File file = new File("H:\\Tools\\BigDeta\\Case Study\\Emirates Test Case\\2007.csv");
//		File file = new File("H:\\Tools\\BigDeta\\Case Study\\Emirates Test Case\\2007.csv.bz2");
		util.putObjectAsMultiPart("H:\\Tools\\BigDeta\\Case Study\\Emirates Test Case\\carriers.csv", "mihir.beit@gmail.com", "Mihir Joshi", "professionalmihir@gmail.com");		
	}
}
