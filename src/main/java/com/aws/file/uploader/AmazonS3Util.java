package com.aws.file.uploader;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.amazonaws.ClientConfiguration;
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

public class AmazonS3Util {
	private static final Logger logger = Logger.getLogger("AmazonS3Util");

	public static final long DEFAULT_FILE_PART_SIZE = 5 * 1024 * 1024; // 5MB
	public static long FILE_PART_SIZE = DEFAULT_FILE_PART_SIZE;

	private static AmazonS3 s3Client;
	private static TransferManager transferManager;

	static {
		init();
	}

	AmazonS3Util() {}

	private static void init() {
	  ClientConfiguration clientConfiguration = new ClientConfiguration();
	  clientConfiguration.setMaxConnections(5000);
	  clientConfiguration.setConnectionTimeout(2000000);
	  clientConfiguration.setSocketTimeout(100);
  	  s3Client = new AmazonS3Client(AmazonUtil.getAwsCredentials());
  	  transferManager = new TransferManager();
	}

	public static void putObjectAsMultiPart(String bucketName, File file) {
		putObjectAsMultiPart(bucketName, file, FILE_PART_SIZE);
	}

	public static void putObjectAsMultiPart(String bucketName, File file, long partSize) {
		List<PartETag> partETags = new ArrayList<PartETag>();
		List<MultiPartFileUploader> uploaderPartList = new ArrayList<MultiPartFileUploader>();
		// Step 1: Initialize.
		InitiateMultipartUploadRequest initRequest = new InitiateMultipartUploadRequest(bucketName, file.getName());
		InitiateMultipartUploadResult initResponse = s3Client.initiateMultipartUpload(initRequest);
		long contentLength = file.length();

		try {
			// Step 2: Upload parts.
			long filePosition = 0;
			for (int i = 1; filePosition < contentLength; i++) {
				// Last part can be less than part size. Adjust part size.
				partSize = Math.min(partSize, (contentLength - filePosition));

				// Create request to upload a part.
				UploadPartRequest uploadRequest = new UploadPartRequest()
						.withBucketName(bucketName).withKey(file.getName())
						.withUploadId(initResponse.getUploadId())
						.withPartNumber(i).withFileOffset(filePosition)
						.withFile(file).withPartSize(partSize);
				
//				uploadRequest.setProgressListener(new UploadProgressListener(file, i, partSize));
				
				// Upload part and add response to our list.
				// MultiPartFileUploader uploader = new
				// MultiPartFileUploader(uploadRequest);
				MultiPartFileUploader filePart = new MultiPartFileUploader(uploadRequest, s3Client);
				uploaderPartList.add(filePart);
				filePart.upload();				

				filePosition += partSize;
			}

			for (MultiPartFileUploader filePart : uploaderPartList) {
				filePart.join();
				partETags.add(filePart.getPartETag());
			}

			// Step 3: complete.
			CompleteMultipartUploadRequest compRequest = new CompleteMultipartUploadRequest(bucketName, file.getName(), initResponse.getUploadId(), partETags);
			s3Client.completeMultipartUpload(compRequest);
		} catch (Throwable t) {
			logger.log(Level.SEVERE,"Unable to put object as multipart to Amazon S3 for file "+ file.getName(), t);
			s3Client.abortMultipartUpload(new AbortMultipartUploadRequest(bucketName, file.getName(), initResponse.getUploadId()));
		}
	}

}