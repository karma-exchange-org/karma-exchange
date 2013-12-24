package org.karmaexchange.util;

import java.io.UnsupportedEncodingException;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.karmaexchange.dao.User;
import org.karmaexchange.dao.User.RegisteredEmail;



public class MailUtil {

	public static void sendMail(User fromUser, User toUser,String subject, String body)
	{
		sendMail(getPrimaryEmailForUser(fromUser), getUserName(fromUser),
			getPrimaryEmailForUser(toUser), getUserName(toUser), subject, body);
	}

	private static void sendMail(String fromEmail, String fromName, String toEmail, String toName, String subject, String body)
	{
		Session session = Session.getDefaultInstance(null);


		try {
		    Message msg = new MimeMessage(session);
		    msg.setFrom(new InternetAddress(fromEmail, fromName));
		    msg.addRecipient(Message.RecipientType.TO,
		     new InternetAddress(toEmail, toName));
		    msg.setSubject(subject);
		    msg.setText(body);
		    Transport.send(msg);

		} catch (AddressException e) {
		    // ...
		} catch (MessagingException e) {
		    // ...
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private static String getUserName(User user) {
		return user.getFirstName() + " " + user.getLastName();
	}

	public static String getPrimaryEmailForUser(User userObj){

		for (RegisteredEmail registeredEmail : userObj.getRegisteredEmails()) {
		      if (registeredEmail.isPrimary()) {
		    	  return registeredEmail.getEmail();
		      }
		}
		return null;
	}


}
