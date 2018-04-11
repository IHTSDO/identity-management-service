package org.ihtsdo.otf.im.service;

import org.apache.commons.lang.CharEncoding;
import org.ihtsdo.otf.im.domain.IHTSDOUser;
import org.ihtsdo.otf.im.domain.WritableUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.core.env.Environment;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring4.SpringTemplateEngine;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.mail.internet.MimeMessage;
import java.util.Locale;

/**
 * Service for sending e-mails.
 * <p/>
 * <p>
 * We use the @Async annotation to send e-mails asynchronously.
 * </p>
 */
@Service
public class MailService {

	private final Logger log = LoggerFactory.getLogger(MailService.class);

	@Inject
	private Environment env;

	@Inject
	private JavaMailSenderImpl javaMailSender;

	@Inject
	private MessageSource messageSource;

	@Inject
	private SpringTemplateEngine templateEngine;

	/**
	 * System default email address that sends the e-mails.
	 */
	private String from;

	@PostConstruct
	public void init() {
		this.from = env.getProperty("spring.mail.from");
	}

	@Async
	public void sendEmail(String to, String subject, String content, boolean isMultipart, boolean isHtml) {
		log.debug("Send e-mail[multipart '{}' and html '{}'] to '{}' with subject '{}' and content={}",
				isMultipart, isHtml, to, subject, content);

		// Prepare message using a Spring helper
		MimeMessage mimeMessage = javaMailSender.createMimeMessage();
		try {
			MimeMessageHelper message = new MimeMessageHelper(mimeMessage, isMultipart, CharEncoding.UTF_8);
			message.setTo(to);
			message.setFrom(from);
			message.setSubject(subject);
			message.setText(content, isHtml);
			javaMailSender.send(mimeMessage);
			log.debug("Sent e-mail to User '{}'", to);
		} catch (Exception e) {
			log.warn("E-mail could not be sent to user '{}', exception is: {}", to, e.getMessage());
		}
	}

	@Async
	public void sendActivationEmail(IHTSDOUser user, String baseUrl) {
		log.debug("Sending activation e-mail to '{}' with chosen lang '{}'", user.getEmailAddress(), user.getLangKey());
		Locale locale = Locale.forLanguageTag(StringUtils.isEmpty(user.getLangKey()) ? "en" : user.getLangKey());
		Context context = new Context(locale);
		context.setVariable("user", user);
		context.setVariable("baseUrl", baseUrl);
		String content = templateEngine.process("activationEmail", context);
		String subject = messageSource.getMessage("email.activation.title", null, locale);
		sendEmail(user.getEmailAddress(), subject, content, false, true);
	}

	@Async
	public void sendActivationEmail(WritableUser user, String baseUrl) {
		log.debug("Sending activation e-mail to '{}' with chosen lang '{}'", user.getEmailAddress(), user.getLangKey());
		Context ctx = getMailContext(user, baseUrl);

		String content = templateEngine.process("activationEmail", ctx);
		String subject = messageSource.getMessage("email.activation.title", null, ctx.getLocale());
		sendEmail(user.getEmailAddress(), subject, content, false, true);
	}

	@Async
	public void sendPasswordResetEmail(WritableUser user, String baseUrl) {

		log.debug("Sending password reset e-mail to '{}' with chosen lang '{}'", user.getEmailAddress(), user.getLangKey());
		Context ctx = getMailContext(user, baseUrl);

		String content = templateEngine.process("passwordResetEmail", ctx);
		String subject = messageSource.getMessage("email.pwd.reset.title", null, ctx.getLocale());
		sendEmail(user.getEmailAddress(), subject, content, false, true);
	}

	@Async
	public void sendPasswordResetSuccessEmail(WritableUser user, String baseUrl) {

		log.debug("Sending password reset success e-mail to '{}' with chosen lang '{}'", user.getEmailAddress(), user.getLangKey());
		Context ctx = getMailContext(user, baseUrl);

		String content = templateEngine.process("passwordResetSuccessEmail", ctx);
		String subject = messageSource.getMessage("email.pwd.reset.success.title", null, ctx.getLocale());
		sendEmail(user.getEmailAddress(), subject, content, false, true);
	}

	private Context getMailContext(WritableUser user, String baseUrl) {

		Locale locale = Locale.forLanguageTag(StringUtils.isEmpty(user.getLangKey()) ? "en" : user.getLangKey());
		Context context = new Context(locale);
		context.setVariable("user", user);
		context.setVariable("baseUrl", baseUrl);
		return context;
	}
}
