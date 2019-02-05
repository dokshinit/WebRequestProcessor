/*
 * Copyright (c) 2016. Aleksey Nikolaevich Dokshin. All right reserved.
 * Contacts: dant.it@gmail.com, dokshin@list.ru.
 */

package app;

import javax.activation.DataHandler;
import javax.activation.FileDataSource;
import javax.mail.*;
import javax.mail.internet.*;
import java.io.File;
import java.io.UnsupportedEncodingException;
import java.util.Properties;

/**
 * Класс для рассылки автоматических сообщений.
 *
 * @author Aleksey Dokshin <dant.it@gmail.com> (07.04.16).
 */
public class ReportsMailer {

    private final String smtphost;
    private final String username;
    private final String password;

    private final InternetAddress fromAddress;

    private final Properties properties;
    private final Session session;


    public ReportsMailer(String smtphost, String username, String password) throws AddressException {
        this.smtphost = smtphost;
        this.username = username;
        this.password = password;
        this.properties = new Properties();

        this.fromAddress = new InternetAddress(username);

        // Setup mail server
        properties.setProperty("mail.smtp.host", smtphost);
        properties.setProperty("mail.smtp.ssl.enable", "true");
        properties.setProperty("mail.smtp.ssl.trust", "*");
        properties.setProperty("mail.smtp.socketFactory.port", "465"); // SSL порт.
        properties.setProperty("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
        properties.setProperty("mail.smtp.auth", "true");

        session = Session.getInstance(properties, new Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(ReportsMailer.this.username, ReportsMailer.this.password);
            }
        });
    }

    public String smtpHost() {
        return smtphost;
    }

    public MimeMessage newMessage() throws MessagingException {
        MimeMessage message = new MimeMessage(session);
        message.setFrom(fromAddress);
        return message;
    }

    public void sendMail(String frompersonal, String toaddr, String[] ccaddr, String subject, String text,
                         String... attachments) throws MessagingException, UnsupportedEncodingException {
        fromAddress.setPersonal(frompersonal);
        MimeMessage message = newMessage();
        message.addRecipient(Message.RecipientType.TO, new InternetAddress(toaddr));
        if (ccaddr != null && ccaddr.length > 0) {
            InternetAddress[] cca = new InternetAddress[ccaddr.length];
            for (int i = 0; i < ccaddr.length; i++) cca[i] = new InternetAddress(ccaddr[i]);
            message.addRecipients(Message.RecipientType.CC, cca);
        }
        message.setSubject(subject);

        if (attachments.length == 0) {
            message.setText(text);
        } else {
            Multipart multipart = new MimeMultipart();
            MimeBodyPart messageBodyPart = new MimeBodyPart();
            messageBodyPart.setText(text);
            multipart.addBodyPart(messageBodyPart);

            for (String file : attachments) {
                int pos = file.lastIndexOf(File.separator);
                String name = pos == -1 ? file : file.substring(pos + 1);

                messageBodyPart = new MimeBodyPart();
                messageBodyPart.setDataHandler(new DataHandler(new FileDataSource(file)));
                messageBodyPart.setFileName(name);
                messageBodyPart.setDisposition("attachment");
                multipart.addBodyPart(messageBodyPart);
            }
            message.setContent(multipart);
        }

        // Send message
        Transport.send(message);
    }
}
