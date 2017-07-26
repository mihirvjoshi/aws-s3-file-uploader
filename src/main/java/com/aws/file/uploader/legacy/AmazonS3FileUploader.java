package com.aws.file.uploader.legacy;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.time.Instant;
import java.util.Date;
import java.util.Properties;
import java.util.concurrent.Executors;

import javax.mail.MessagingException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.ClientConfiguration;
import com.amazonaws.HttpMethod;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.transfer.PauseResult;
import com.amazonaws.services.s3.transfer.PersistableTransfer;
import com.amazonaws.services.s3.transfer.PersistableUpload;
import com.amazonaws.services.s3.transfer.TransferManager;
import com.amazonaws.services.s3.transfer.TransferManagerConfiguration;
import com.amazonaws.services.s3.transfer.TransferProgress;
import com.amazonaws.services.s3.transfer.Upload;

public class AmazonS3FileUploader {
	private static final Log logger = LogFactory.getLog(AmazonS3FileUploader.class);

	private static final long FILE_SIZE_UNIT_IN_MB = 1024*1024;
	private static long DEFAULT_FILE_PART_SIZE ; // 5MB
	private static long FILE_PART_SIZE;
	private static Date expiration = new Date();
	private static AmazonS3 s3Client;
	private static TransferManager transferManager;
	private static Properties prop;

	static {
		prop = FilePropertiesUtil.loadProperties();
		System.out.println(prop.get(FilePropertiesUtil.DEFAULT_FILE_PART_SIZE));
		DEFAULT_FILE_PART_SIZE = Long.valueOf(prop.getProperty(FilePropertiesUtil.DEFAULT_FILE_PART_SIZE)) * FILE_SIZE_UNIT_IN_MB;
		FILE_PART_SIZE = DEFAULT_FILE_PART_SIZE;
		init();
	}

	AmazonS3FileUploader() {}

	private static void init() {
		ClientConfiguration clientConfiguration = new ClientConfiguration();
		clientConfiguration.setMaxConnections(Integer.valueOf(prop.getProperty(FilePropertiesUtil.MAX_CONNECTIONS)));
		clientConfiguration.setConnectionTimeout(Integer.valueOf(prop.getProperty(FilePropertiesUtil.CONN_TIMEOUT)));
		clientConfiguration.setSocketTimeout(Integer.valueOf(prop.getProperty(FilePropertiesUtil.SOCKET_TIMEOUT)));
		s3Client = new AmazonS3Client(AmazonUtil.getAwsCredentials(), clientConfiguration);
		transferManager = new TransferManager(s3Client, Executors.newFixedThreadPool(Integer.valueOf(prop.getProperty(FilePropertiesUtil.THREAD_POOL_SIZE))));
		TransferManagerConfiguration config = new TransferManagerConfiguration();
		config.setMultipartUploadThreshold(DEFAULT_FILE_PART_SIZE);
		config.setMinimumUploadPartSize(DEFAULT_FILE_PART_SIZE);
		transferManager.setConfiguration(config);
	}

	public static void putObjectAsMultiPart(String filePath, String userMailId, String userName, String accountantMailId) {
		Upload upload = null;
		File file;
		String bucketName = prop.getProperty(FilePropertiesUtil.BUCKET_NAME);
		if(filePath!=null){
			file = new File(filePath);
		} else {
			file = new File(FilePropertiesUtil.RESUME_UPLOAD_FILE_NAME);
		}
		UploadProgressListener progressListener=new UploadProgressListener(file);
		if (file.exists() && file.getName()!=null && file.getName().contains(FilePropertiesUtil.RESUME_UPLOAD_FILE_NAME)) {
			try {
				FileInputStream fis = new FileInputStream(file);
				// Deserialize PersistableUpload information from disk.
				PersistableUpload persistableUpload = PersistableTransfer.deserializeFrom(fis);
				// Call resumeUpload with PersistableUpload.
				upload = transferManager.resumeUpload(persistableUpload);
				upload.addProgressListener(progressListener);
				fis.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		} else {
			PutObjectRequest putRequest = new PutObjectRequest(bucketName, file.getName(), file);
			upload = transferManager.upload(putRequest, progressListener);
		}

		try {
			upload.waitForCompletion();
			URL fileURL = generateFileURL(bucketName, file);
			MailUtil.send(prop, accountantMailId, userName, upload.getProgress().getBytesTransferred(), fileURL.toString());
		} catch (AmazonServiceException e) {
            logger.error("Unable to put object as multipart to Amazon S3 for file :" + file.getName(), e);  
            transferManager.abortMultipartUploads(bucketName, Date.from(Instant.now()));
		} catch (AmazonClientException e) {
            logger.error("Unable to update the content due to communication failure :" + file.getName(), e);  
            transferManager.abortMultipartUploads(bucketName, Date.from(Instant.now()));
		} catch (InterruptedException e) {
            logger.error("Unable to update the content due to interrupt:" + file.getName(), e);  
            transferManager.abortMultipartUploads(bucketName, Date.from(Instant.now()));
		} catch (MessagingException e) {
            logger.error("Unable to send the mail for the file:" + file.getName() + " to the receipient" + accountantMailId, e);  
		} finally{
			transferManager.shutdownNow();
		}
	}

	private static URL generateFileURL(String bucketName, File file) {
		GeneratePresignedUrlRequest generatePresignedUrlRequest = new GeneratePresignedUrlRequest(bucketName, file.getName());
		generatePresignedUrlRequest.setMethod(HttpMethod.GET); // Default.
		generatePresignedUrlRequest.setExpiration(getExpiryTime());

		URL fileURL = s3Client.generatePresignedUrl(generatePresignedUrlRequest);
		logger.info("File upload completed. File URL is "+ fileURL.toString());
		return fileURL;
	}
	
	private static Date getExpiryTime(){
		long milliSeconds = expiration.getTime();
		milliSeconds += 1000 * 60 * 60; // Add 1 hour.
		expiration.setTime(milliSeconds);
		return expiration;
	}

	public boolean pauseMultipartObject(Upload myUpload) throws IOException, InterruptedException{
		// Sleep until data transferred to Amazon S3 is less than 20 MB.
		TransferProgress progress = myUpload.getProgress();
		while( progress.getBytesTransferred() < 20*FILE_SIZE_UNIT_IN_MB ) Thread.sleep(2000);

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
		File f = new File(FilePropertiesUtil.RESUME_UPLOAD_FILE_NAME);
		if( !f.exists() ) f.createNewFile();
		FileOutputStream fos = new FileOutputStream(f);

		// Serialize the persistable upload to the file.
		persistableUpload.serialize(fos);
		fos.close();
		
		return forceCancel;
	}

	public static void resumeMultipartObject() throws IOException {
		File f = new File(FilePropertiesUtil.RESUME_UPLOAD_FILE_NAME);
		if (f.exists()) {
			FileInputStream fis = new FileInputStream(f);
			// Deserialize PersistableUpload information from disk.
			PersistableUpload persistableUpload = PersistableTransfer.deserializeFrom(fis);
			// Call resumeUpload with PersistableUpload.
			Upload upload = transferManager.resumeUpload(persistableUpload);
			fis.close();
		} 
	}
}