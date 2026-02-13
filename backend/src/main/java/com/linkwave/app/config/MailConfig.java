package com.linkwave.app.config;

import com.linkwave.app.config.auth.EmailConfig;
import java.util.Properties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

@Configuration
public class MailConfig {

  private final EmailConfig emailConfig;

  public MailConfig(EmailConfig emailConfig) {
    this.emailConfig = emailConfig;
  }

  @Bean
  public JavaMailSender javaMailSender() {
    JavaMailSenderImpl mailSender = new JavaMailSenderImpl();

    mailSender.setHost(emailConfig.getHost());
    mailSender.setPort(emailConfig.getPort());
    mailSender.setUsername(emailConfig.getUsername());
    mailSender.setPassword(emailConfig.getPassword());

    Properties props = mailSender.getJavaMailProperties();
    props.put("mail.transport.protocol", "smtp");
    boolean hasCredentials =
        emailConfig.getUsername() != null && !emailConfig.getUsername().isBlank();
    props.put("mail.smtp.auth", String.valueOf(hasCredentials));
    props.put("mail.smtp.starttls.enable", String.valueOf(emailConfig.isTlsEnabled()));
    props.put("mail.smtp.starttls.required", String.valueOf(emailConfig.isTlsEnabled()));
    props.put("mail.smtp.connectiontimeout", "5000");
    props.put("mail.smtp.timeout", "5000");
    props.put("mail.smtp.writetimeout", "5000");
    props.put("mail.debug", "false");

    return mailSender;
  }
}
