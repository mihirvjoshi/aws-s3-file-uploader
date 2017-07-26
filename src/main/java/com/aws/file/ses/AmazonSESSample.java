package com.aws.file.ses;

import java.io.IOException;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.Protocol;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailService;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailServiceClientBuilder;
import com.amazonaws.services.simpleemail.model.Body;
import com.amazonaws.services.simpleemail.model.Content;
import com.amazonaws.services.simpleemail.model.Destination;
import com.amazonaws.services.simpleemail.model.Message;
import com.amazonaws.services.simpleemail.model.SendEmailRequest;
import com.aws.file.uploader.AmazonUtil;

public class AmazonSESSample {

    // Replace with your "From" address. This address must be verified.
    static final String FROM = "mihir.beit@gmail.com";

    // Replace with a "To" address. If your account is still in the
    // sandbox, this address must be verified.
    static final String TO = "professionalmihir@gmail.com";

    static final String BODY = "This email was sent through Amazon SES using the AWS SDK for Java.";
    static final String SUBJECT = "Amazon SES test (AWS SDK for Java)";

    public static void main(String[] args) throws IOException {

        // Construct an object to contain the recipient address.
        Destination destination = new Destination().withToAddresses(new String[] { TO });

        // Create the subject and body of the message.
        Content subject = new Content().withData(SUBJECT);
        Content textBody = new Content().withData(BODY);
        Body body = new Body().withText(textBody);

        // Create a message with the specified subject and body.
        Message message = new Message().withSubject(subject).withBody(body);

        // Assemble the email.
        SendEmailRequest request = new SendEmailRequest().withSource(FROM)
                .withDestination(destination).withMessage(message);

        try {
            System.out.println("Attempting to send an email through Amazon SES using the AWS SDK for Java.");

            ClientConfiguration clientConfiguration = new ClientConfiguration();
    		clientConfiguration.setMaxConnections(5000);
    		clientConfiguration.setConnectionTimeout(5*1000);
    		clientConfiguration.setSocketTimeout(5*1000);
    		clientConfiguration.setProtocol(Protocol.HTTP);
            AmazonSimpleEmailService client = AmazonSimpleEmailServiceClientBuilder.standard()
            		.withCredentials(AmazonUtil.getAwsCredentials())
            		.withClientConfiguration(clientConfiguration)
                    .withRegion(Regions.US_WEST_2).build();

            // Send the email.
            client.sendEmail(request);
            System.out.println("Email sent!");
        } catch (Exception ex) {
            System.out.println("The email was not sent.");
            System.out.println("Error message: " + ex.getMessage());
        }
    }
}