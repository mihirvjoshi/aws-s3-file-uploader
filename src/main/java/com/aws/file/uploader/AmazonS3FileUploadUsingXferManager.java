package com.aws.file.uploader;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.ClientConfiguration;
import com.amazonaws.Protocol;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.transfer.PauseResult;
import com.amazonaws.services.s3.transfer.PersistableTransfer;
import com.amazonaws.services.s3.transfer.PersistableUpload;
import com.amazonaws.services.s3.transfer.TransferManager;
import com.amazonaws.services.s3.transfer.TransferManagerBuilder;
import com.amazonaws.services.s3.transfer.TransferProgress;
import com.amazonaws.services.s3.transfer.Upload;

public class AmazonS3FileUploadUsingXferManager {

	private static final Logger logger = Logger.getLogger("AmazonS3Util");

	public static final long DEFAULT_FILE_PART_SIZE = 5 * 1024 * 1024; // 5MB
	public static long FILE_PART_SIZE = DEFAULT_FILE_PART_SIZE;

	private static AmazonS3 s3Client;
	private static TransferManager transferManager;

	static {
		init();
	}

	AmazonS3FileUploadUsingXferManager() {}

	private static void init() {
		ClientConfiguration clientConfiguration = new ClientConfiguration();
		clientConfiguration.setMaxConnections(5000);
		clientConfiguration.setConnectionTimeout(5*1000);
		clientConfiguration.setSocketTimeout(5*1000);
		clientConfiguration.setProtocol(Protocol.HTTP);
		
		s3Client = AmazonS3ClientBuilder.standard()
				.withCredentials(AmazonUtil.getAwsCredentials())
				.withRegion(Regions.US_EAST_1).withForceGlobalBucketAccessEnabled(true) 
				.withClientConfiguration(clientConfiguration).build();
		transferManager = TransferManagerBuilder.standard()
				.withExecutorFactory(() -> Executors.newFixedThreadPool(100))
				.withMinimumUploadPartSize(DEFAULT_FILE_PART_SIZE)
				.withMultipartCopyThreshold(DEFAULT_FILE_PART_SIZE)
				.withS3Client(s3Client).build();
	}

	public static void putObjectAsMultiPart(String bucketName, File file) {
		putObjectAsMultiPart(bucketName, file, FILE_PART_SIZE);
	}

	public static void putObjectAsMultiPart(String bucketName, File file, long partSize) {
/*		try {
			resumeMultipartObject() ;
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
*/		Upload myUpload = transferManager.upload(bucketName, file.getName(), file);
		PutObjectRequest putRequest = new PutObjectRequest(bucketName, file.getName(),file);
		transferManager.upload(putRequest, new UploadProgressListener(file));
		 
		// You can poll your transfer's status to check its progress
/*		while (myUpload.isDone() == false) {
		       System.out.println("Transfer: " + myUpload.getDescription());
		       System.out.println("  - State: " + myUpload.getState());
		       System.out.println("  - Progress: "
		                       + myUpload.getProgress().getBytesTransferred());
		}
*/		 
		// Or you can block the current thread and wait for your transfer to
		// to complete. If the transfer fails, this method will throw an
		// AmazonClientException or AmazonServiceException detailing the reason.
		try {
			myUpload.waitForCompletion();
		} catch (AmazonServiceException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (AmazonClientException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		 
		// After the upload is complete, call shutdownNow to release the resources.
		transferManager.shutdownNow();
	}
	
	public boolean pauseMultipartObject(Upload myUpload) throws IOException, InterruptedException{
		// Sleep until data transferred to Amazon S3 is less than 20 MB.
		long MB = 1024 * 1024;
		TransferProgress progress = myUpload.getProgress();
		while( progress.getBytesTransferred() < 20*MB ) Thread.sleep(2000);

		// Initiate a pause with forceCancelTransfer as true. 
		// This cancels the upload if the upload cannot be paused.
		boolean forceCancel = true;
		PauseResult<PersistableUpload> pauseResult = myUpload.tryPause(forceCancel);	
		
		if(pauseResult!=null && pauseResult.getPauseStatus()!=null){
			return pauseResult.getPauseStatus().isPaused();
		}
		
		// Retrieve the persistable upload from the pause result.
		PersistableUpload persistableUpload = pauseResult.getInfoToResume();

		// Create a new file to store the information.
		File f = new File("resume-upload");
		if( !f.exists() ) f.createNewFile();
		FileOutputStream fos = new FileOutputStream(f);

		// Serialize the persistable upload to the file.
		persistableUpload.serialize(fos);
		fos.close();
		
		return forceCancel;
	}

	public static void resumeMultipartObject() throws IOException {
		String fileName = "resume-upload";
		File f = new File(fileName);
		if (f.exists()) {
			FileInputStream fis = new FileInputStream(f);
			// Deserialize PersistableUpload information from disk.
			PersistableUpload persistableUpload = PersistableTransfer
					.deserializeFrom(fis);
			// Call resumeUpload with PersistableUpload.
			transferManager.resumeUpload(persistableUpload);
			fis.close();
		}
	}
}
