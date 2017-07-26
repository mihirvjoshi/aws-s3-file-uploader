package com.aws.file.uploader.legacy;

import java.util.Properties;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class MailUtil {
	private static final Log logger = LogFactory.getLog(MailUtil.class);
	
	public static void send(Properties prop, String to, String user, long fileSize, String url)
 throws MessagingException {
        prop.put("mail.smtp.host", prop.getProperty(FilePropertiesUtil.MAIL_HOST));    
        prop.put("mail.smtp.auth", "true");
        prop.put("mail.smtp.starttls.enable", "true");
        prop.put("mail.smtp.port", "587");
        
        Session session = Session.getDefaultInstance(prop,
				new javax.mail.Authenticator() {
					protected PasswordAuthentication getPasswordAuthentication() {
						return new PasswordAuthentication(prop.getProperty("user"), prop.getProperty("pwd"));
					}
				});

		MimeMessage message = new MimeMessage(session);
		message.setFrom(new InternetAddress(prop.getProperty("user")));
		message.addRecipient(Message.RecipientType.TO, new InternetAddress(to));
		message.setSubject("Ping");
		message.setText("Hello, File=>" + url + " with size "
				+ (fileSize / 1024 / 1024) + " MB, uploaded by " + user);

		// Send message
		Transport.send(message);
		logger.info("message sent successfully to...." + to);
	}

}
