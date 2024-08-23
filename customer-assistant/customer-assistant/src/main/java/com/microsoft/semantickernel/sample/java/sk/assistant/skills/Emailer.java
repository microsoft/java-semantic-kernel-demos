package com.microsoft.semantickernel.sample.java.sk.assistant.skills;

import com.microsoft.semantickernel.semanticfunctions.annotations.DefineKernelFunction;
import com.microsoft.semantickernel.semanticfunctions.annotations.KernelFunctionParameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A FAKE emailer skill that can get an email address for a user and send an email.
 */
public class Emailer {
    private static final Logger LOGGER = LoggerFactory.getLogger(Emailer.class);

    @DefineKernelFunction(name = "getEmailAddress", description = "Retrieves the email address for a given user", returnDescription = "The email address")
    public String getEmailAddress(
            @KernelFunctionParameter(description = "The name of the person to get an email address for", name = "name")
            String name) {
        // fake a lookup of an email address
        return "foo@example.com";
    }

    // Fake sending an email, just log it
    @DefineKernelFunction(name = "sendEmail", description = "Sends an email", returnDescription = "A message indicating if the email was sent")
    public String sendEmail(
            @KernelFunctionParameter(name = "subject", description = "The email subject") String subject,
            @KernelFunctionParameter(name = "message", description = "The message to email") String message,
            @KernelFunctionParameter(name = "emailAddress", description = "The emailAddress to send the message to") String emailAddress) {
        LOGGER.warn("================= Sending Email ====================");
        LOGGER.warn("To: %s%n".formatted(emailAddress));
        LOGGER.warn("Subject: %s%n".formatted(subject));
        LOGGER.warn("Message: %s%n".formatted(message));
        LOGGER.warn("====================================================");
        return "Message sent";
    }
}