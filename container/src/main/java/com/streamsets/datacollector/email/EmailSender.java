/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.streamsets.datacollector.email;


import com.streamsets.datacollector.util.Configuration;
import com.streamsets.datacollector.util.ContainerError;
import com.streamsets.datacollector.util.PipelineException;

import javax.inject.Inject;
import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public class EmailSender {
  public static final String MAIL_CONFIGS_PREFIX = "mail.";

  public static final String EMAIL_SMTP_USER = "xmail.username";
  public static final String EMAIL_SMTP_PASS = "xmail.password";
  public static final String EMAIL_SMTP_FROM = "xmail.from.address";

  private final Properties javaMailProps;
  private final String user;
  private final String password;
  private final String from;
  private final boolean auth;
  private Session session;

  @Inject
  public EmailSender(Configuration conf) {
    javaMailProps = createJavaMailSessionProperties(conf.getSubSetConfiguration(MAIL_CONFIGS_PREFIX));
    String protocol = javaMailProps.getProperty("mail.transport.protocol", "smtp");
    if (!protocol.equals("smtp") && !protocol.equals("smtps")) {

    }
    auth = Boolean.parseBoolean(javaMailProps.getProperty("mail." + protocol + ".auth"));
    user = conf.get(EMAIL_SMTP_USER, "");
    password = conf.get(EMAIL_SMTP_PASS, "");
    from = conf.get(EMAIL_SMTP_FROM, "sdc@localhost");
  }

  private Properties getDefaultsJavaMailSessionProperties() {
    Properties properties = new Properties();
    properties.setProperty("mail.smtp.host", "localhost");
    properties.setProperty("mail.smtp.port", "25");
    properties.setProperty("mail.smtp.auth", "false");
    properties.setProperty("mail.smtp.starttls.enable", "false");
    properties.setProperty("mail.smtps.host", "localhost");
    properties.setProperty("mail.smtps.port", "465");
    properties.setProperty("mail.smtps.auth", "false");
    return properties;
  }

  private Properties createJavaMailSessionProperties(Configuration conf) {
    Properties properties = new Properties(getDefaultsJavaMailSessionProperties());
    for (Map.Entry<String, String> entry : conf.getValues().entrySet()) {
      properties.setProperty(entry.getKey(), entry.getValue().trim());
    }
    return properties;
  }

  private InternetAddress toAddress(String email) throws AddressException {
    return new InternetAddress(email.trim());
  }

  private List<InternetAddress> toAddress(List<String> emails) throws AddressException {
    List<InternetAddress> list = new ArrayList<>(emails.size());
    for (String email : emails) {
      list.add(toAddress(email));
    }
    return list;
  }

  private Session createSession() {
    Session session;
    if (!auth) {
      session = Session.getInstance(javaMailProps);
    } else {
      session = Session.getInstance(javaMailProps, new Authenticator() {
        @Override
        protected PasswordAuthentication getPasswordAuthentication() {
          return new PasswordAuthentication(user, password);
        }
      });
    }
    return session;
  }

  public void send(List<String> addresses, String subject, String body) throws PipelineException {
    try {
      session = (session == null) ? createSession() : session;
      Message message = new MimeMessage(session);
      InternetAddress fromAddr = toAddress(from);
      message.setFrom(fromAddr);
      List<InternetAddress> toAddrs = toAddress(addresses);
      message.addRecipients(Message.RecipientType.TO, toAddrs.toArray(new InternetAddress[toAddrs.size()]));
      message.setSubject(subject);
      message.setContent(body, "text/html; charset=UTF-8");
      Transport.send(message);
    } catch (Exception ex) {
      session = null;
      throw new PipelineException(ContainerError.CONTAINER_0500, ex.toString(), ex);
    }
  }

}
