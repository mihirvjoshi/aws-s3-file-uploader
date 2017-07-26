package com.aws.file.uploader.legacy;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.amazonaws.services.s3.model.ProgressEvent;
import com.amazonaws.services.s3.transfer.PersistableTransfer;
import com.amazonaws.services.s3.transfer.internal.S3ProgressListener;

class UploadProgressListener implements S3ProgressListener  {
	private static final Log logger = LogFactory.getLog(UploadProgressListener.class); 
	ExecutorService executor = Executors.newFixedThreadPool(1);
	 
	File file;
	int partNo;
	long partLength;

	UploadProgressListener(File file) {
		this.file = file;
	}

	@SuppressWarnings("unused")
	UploadProgressListener(File file, int partNo) {
		this(file, partNo, 0);
	}

	UploadProgressListener(File file, int partNo, long partLength) {
		this.file = file;
		this.partNo = partNo;
		this.partLength = partLength;
	}

	public void progressChanged(ProgressEvent progressEvent) {
		switch (progressEvent.getEventCode()) {
		case ProgressEvent.STARTED_EVENT_CODE:
			logger.info("Upload started for file " + "\"" + file.getName()
					+ "\"");
			break;
		case ProgressEvent.COMPLETED_EVENT_CODE:
			logger.info("Upload completed for file " + "\"" + file.getName()
					+ "\"" + ", " + file.length()
					+ " bytes data has been transferred");
			break;
		case ProgressEvent.FAILED_EVENT_CODE:
			logger.info("Upload failed for file " + "\"" + file.getName()
					+ "\"" + ", " + progressEvent.getBytesTransfered()
					+ " bytes data has been transferred");
			break;
		case ProgressEvent.CANCELED_EVENT_CODE:
			logger.info("Upload cancelled for file " + "\"" + file.getName()
					+ "\"" + ", " + progressEvent.getBytesTransfered()
					+ " bytes data has been transferred");
			break;
		case ProgressEvent.PART_STARTED_EVENT_CODE:
			logger.info("Upload started at " + partNo + ". part for file "
					+ "\"" + file.getName() + "\"");
			break;
		case ProgressEvent.PART_COMPLETED_EVENT_CODE:
			logger.info("Upload completed at "
					+ partNo
					+ ". part for file "
					+ "\""
					+ file.getName()
					+ "\""
					+ ", "
					+ (partLength > 0 ? partLength : progressEvent
							.getBytesTransfered())
					+ " bytes data has been transferred");
			break;
		case ProgressEvent.PART_FAILED_EVENT_CODE:
			logger.info("Upload failed at " + partNo + ". part for file "
					+ "\"" + file.getName() + "\"" + ", "
					+ progressEvent.getBytesTransfered()
					+ " bytes data has been transferred");
			break;
		}
	}

	@Override
	public void progressChanged(com.amazonaws.event.ProgressEvent progressEvent) {
		switch (progressEvent.getEventType()) {
		case TRANSFER_STARTED_EVENT:
			logger.info("Upload started for file " + "\"" + file.getName()
					+ "\"");
			break;
		case TRANSFER_COMPLETED_EVENT:
			logger.info("Upload completed for file " + "\"" + file.getName()
					+ "\"" + ", " + file.length()
					+ " bytes data has been transferred");
			break;
		case TRANSFER_FAILED_EVENT:
			logger.info("Upload failed for file " + "\"" + file.getName()
					+ "\"" + ", " + progressEvent.getBytesTransferred()
					+ " bytes data has been transferred");
			break;
		case TRANSFER_CANCELED_EVENT:
			logger.info("Upload cancelled for file " + "\"" + file.getName()
					+ "\"" + ", " + progressEvent.getBytesTransferred()
					+ " bytes data has been transferred");
			break;
		case TRANSFER_PART_STARTED_EVENT:
			logger.info("Upload started at " + partNo + ". part for file "
					+ "\"" + file.getName() + "\"");
			break;
		case TRANSFER_PART_COMPLETED_EVENT:
			logger.info("Upload completed at "
					+ partNo
					+ ". part for file "
					+ "\""
					+ file.getName()
					+ "\""
					+ ", "
					+ (partLength > 0 ? partLength : progressEvent
							.getBytesTransferred())
					+ " bytes data has been transferred");
			break;
		case TRANSFER_PART_FAILED_EVENT:
			logger.info("Upload failed at " + partNo + ". part for file "
					+ "\"" + file.getName() + "\"" + ", "
					+ progressEvent.getBytesTransferred()
					+ " bytes data has been transferred");
			break;
		}
	}

	@Override
	public void onPersistableTransfer(PersistableTransfer persistableTransfer) {
		executor.submit(new Runnable() {
			@Override
			public void run() {
				try {
					saveTransferState(persistableTransfer, "resume-upload");
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		});
	}
	
	public static void saveTransferState(
			PersistableTransfer persistableTransfer, String fileName)
			throws IOException {
		// Create a new file to store the information.
		File f = new File(fileName);
		if (!f.exists())
			f.createNewFile();
		FileOutputStream fos = new FileOutputStream(f);
		// Serialize the persistable transfer to the file.
		persistableTransfer.serialize(fos);
		fos.close();
	}

}
