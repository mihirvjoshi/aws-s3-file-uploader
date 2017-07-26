package com.aws.file.uploader;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.AbortMultipartUploadRequest;
import com.amazonaws.services.s3.model.CompleteMultipartUploadRequest;
import com.amazonaws.services.s3.model.InitiateMultipartUploadRequest;
import com.amazonaws.services.s3.model.InitiateMultipartUploadResult;
import com.amazonaws.services.s3.model.PartETag;
import com.amazonaws.services.s3.model.UploadPartRequest;
import com.amazonaws.services.s3.transfer.TransferManager;
import com.aws.file.uploader.legacy.AmazonUtil;

public class AmazonS3FileUploadUtilWithSingleThread {
	private static final Logger logger = Logger.getLogger("AmazonS3Util");
	public static final long DEFAULT_FILE_PART_SIZE = 5 * 1024 * 1024; // 5MB
	public static long FILE_PART_SIZE = DEFAULT_FILE_PART_SIZE;

	private static AmazonS3 s3Client;
	private static TransferManager transferManager;

	public static void putObjectAsMultiPart(String bucketName, File file) {
		putObjectAsMultiPart(bucketName, file, FILE_PART_SIZE);
	}

	public static void putObjectAsMultiPart(String bucketName, File file, long partSize) {
		AmazonS3 s3Client = new AmazonS3Client(AmazonUtil.getAwsCredentials());
		// Create a list of UploadPartResponse objects. You get one of these for
		// each part upload.
		List<PartETag> partETags = new ArrayList<PartETag>();

		// Step 1: Initialize.
		InitiateMultipartUploadRequest initRequest = new InitiateMultipartUploadRequest(
		                                                    bucketName, file.getName());
		InitiateMultipartUploadResult initResponse = 
		                              s3Client.initiateMultipartUpload(initRequest);

		long contentLength = file.length();		
		try {
		    // Step 2: Upload parts.
		    long filePosition = 0;
		    for (int i = 1; filePosition < contentLength; i++) {
		        // Last part can be less than 5 MB. Adjust part size.
		    	partSize = Math.min(partSize, (contentLength - filePosition));
		    	
		        // Create request to upload a part.
		        UploadPartRequest uploadRequest = new UploadPartRequest()
		            .withBucketName(bucketName).withKey(file.getName())
		            .withUploadId(initResponse.getUploadId()).withPartNumber(i)
		            .withFileOffset(filePosition)
		            .withFile(file)
		            .withPartSize(partSize);

		        // Upload part and add response to our list.
		        partETags.add(s3Client.uploadPart(uploadRequest).getPartETag());

		        filePosition += partSize;
		    }

		    // Step 3: Complete.
		    CompleteMultipartUploadRequest compRequest = new 
		                CompleteMultipartUploadRequest(bucketName, 
		                							   file.getName(), 
		                                               initResponse.getUploadId(), 
		                                               partETags);

		    s3Client.completeMultipartUpload(compRequest);
		} catch (Exception e) {
			e.printStackTrace();
			logger.log(Level.SEVERE,"Unable to put object as multipart to Amazon S3 for file "+ file.getName(), e);
		    s3Client.abortMultipartUpload(new AbortMultipartUploadRequest(
		              bucketName, file.getName(), initResponse.getUploadId()));
		}	
		
	}
}