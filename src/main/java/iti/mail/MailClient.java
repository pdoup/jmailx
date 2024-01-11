/** This class represents a MailClient that can send/receive mails. */
package iti.mail;

import static org.jsoup.Jsoup.*;

import org.apache.commons.cli.*;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ArrayUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;
import java.util.Properties;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.mail.*;
import javax.mail.internet.AddressException;
import javax.mail.internet.ContentType;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.search.*;

class MailClient {

    final String username;
    final String password;
    final String sender;
    final Session session;
    final Properties prop;

    public static final int MAX_FILENAME_LENGTH = 150;

    /**
     * Constructs a new MailClient with the given configuration path.
     *
     * @param configPath the path to the configuration file
     * @throws IOException if an I/O error occurs
     */
    public MailClient(final String configPath) throws IOException {
        this.username = System.getenv("ITIMAIL");
        this.password = System.getenv("ITIPASS");
        if (this.username == null || this.password == null) {
            throw new RuntimeException(
                    "$ITIMAIL or $ITIPASS environment variables have not been set.");
        }

        this.prop = new Properties();

        final InputStream input = new FileInputStream(configPath);
        prop.load(input);

        if (input != null) {
            input.close();
        }

        this.sender = prop.getProperty("mail.proto.user");
        this.session =
                Session.getInstance(
                        prop,
                        new javax.mail.Authenticator() {
                            protected PasswordAuthentication getPasswordAuthentication() {
                                return new PasswordAuthentication(username, password);
                            }
                        });
    }

    /**
     * Returns the username of the mail client.
     *
     * @return the username of the mail client
     */
    public String getUsername() {
        return this.username;
    }

    /**
     * Sends an email with the given parameters.
     *
     * @param recipient the recipient of the email
     * @param subject the subject of the email
     * @param messageText the text of the message
     * @param carbonCopy the carbon copy of the email
     * @throws MessagingException if a messaging error occurs
     */
    public void send(
            final String recipient,
            final String subject,
            final String messageText,
            final String carbonCopy)
            throws MessagingException {
        send(recipient, subject, messageText, carbonCopy, null);
    }

    /**
     * Sends an email with the given parameters and an attachment.
     *
     * @param recipient the recipient of the email
     * @param subject the subject of the email
     * @param messageText the text of the message
     * @param carbonCopy the carbon copy of the email
     * @param attachmentPath the path to the attachment
     * @throws MessagingException if a messaging error occurs
     */
    public void send(
            final String recipient,
            final String subject,
            final String messageText,
            final String carbonCopy,
            final String attachmentPath)
            throws MessagingException {

        Message message = new MimeMessage(session);
        try {
            message.setFrom(
                    new InternetAddress(
                            username + "@" + this.prop.getProperty("mail.proto.domain"),
                            this.sender));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        final InternetAddress[] recipients = InternetAddress.parse(recipient);
        try {
            message.setRecipients(Message.RecipientType.TO, recipients);
        } catch (NullPointerException npe) {
            throw new AddressException("`To` address field cannot be empty");
        }

        if (!carbonCopy.isEmpty()) {
            message.setRecipients(Message.RecipientType.CC, InternetAddress.parse(carbonCopy));
        }
        message.setSubject(subject);

        Multipart multipart = new MimeMultipart();

        BodyPart messageBodyPart = new MimeBodyPart();

        final String messageContent = readFileOrString(messageText);
        messageBodyPart.setText(messageContent == null ? "Placeholder" : messageContent);
        multipart.addBodyPart(messageBodyPart);

        if (attachmentPath != null && !attachmentPath.isEmpty()) {
            File file = new File(attachmentPath);
            if (file.exists() && !file.isDirectory() && getFileExtension(file).equals("zip")) {
                messageBodyPart = new MimeBodyPart();
                DataSource source = new FileDataSource(attachmentPath);
                messageBodyPart.setDataHandler(new DataHandler(source));
                messageBodyPart.setFileName(file.getName());
                multipart.addBodyPart(messageBodyPart);
            } else {
                System.out.println("==> Invalid attachment. Please provide a valid zip file.");
                return;
            }
        }

        message.setContent(multipart, "text/plain");

        Transport.send(message);

        ZonedDateTime now = ZonedDateTime.now(ZoneId.systemDefault());
        DateTimeFormatter formatter = DateTimeFormatter.ISO_ZONED_DATE_TIME;

        System.out.println(
                "==> Mail sent to "
                        + Arrays.stream(recipients)
                                .map(InternetAddress::getAddress)
                                .map(addr -> "<" + addr + ">")
                                .collect(Collectors.joining(" "))
                        + " at "
                        + now.format(formatter));
    }

    /**
     * Reads the latest emails from the inbox.
     *
     * @param maxMessages the maximum number of messages to read
     * @param saveAttachments downloads any attachments found
     * @param reverseOrder list messages in reverse chronological order
     * @param reverseSearch search messages in reverse chronological order
     * @param folder the name of the folder to open
     * @throws MessagingException if a messaging error occurs
     * @throws IOException if an I/O error occurs
     */
    public void read(
            final int maxMessages,
            final boolean saveAttachments,
            final boolean reverseOrder,
            final boolean reverseSearch,
            final String folder)
            throws MessagingException, IOException {
        read(null, maxMessages, saveAttachments, reverseOrder, reverseSearch, folder);
    }

    /**
     * Reads the latest emails from the inbox.
     *
     * @param filter the messages to read
     * @param maxMessages the maximum number of messages to read
     * @param saveAttachments downloads any attachments found
     * @param reverseOrder list messages in reverse chronological order
     * @param reverseSearch search messages in reverse chronological order
     * @param folder the name of the folder to open
     * @throws MessagingException if a messaging error occurs
     * @throws IOException if an I/O error occurs
     */
    public void read(
            final String filter,
            final int maxMessages,
            final boolean saveAttachments,
            final boolean reverseOrder,
            final boolean reverseSearch,
            final String folder)
            throws MessagingException, IOException {
        final String ANSI_RESET = "\u001B[0m";
        final String ANSI_UNDERLINE = "\u001B[4m";

        final Store store = session.getStore(this.prop.getProperty("mail.proto.store_type"));
        store.connect();

        final String folderName =
                folder == null ? this.prop.getProperty("mail.proto.inbox_name") : folder.trim();
        final Folder inbox = store.getFolder(folderName);

        try {
            inbox.open(Folder.READ_ONLY);
        } catch (FolderNotFoundException fnf) {
            System.out.println(
                    "==> Folder \""
                            + fnf.getFolder().getName()
                            + "\" not found in store ("
                            + fnf.getFolder().getURLName()
                            + ")");
            store.close();
            return;
        }

        final int maxMessagesParsed = maxMessages > 0 ? maxMessages : Integer.MAX_VALUE;
        final int messageCount = inbox.getMessageCount();

        final int start = reverseSearch ? 1 : Math.max(1, messageCount - maxMessagesParsed + 1);
        final int end = reverseSearch ? Math.min(messageCount, maxMessagesParsed) : messageCount;

        System.out.println(
                "==> Found "
                        + messageCount
                        + " mail(s)"
                        + " in '"
                        + inbox.getName()
                        + "' folder ("
                        + inbox.getUnreadMessageCount()
                        + " unread)");

        int limit;
        Message[] messages = null;

        if (filter != null) {
            messages = inbox.search(MailFilter.parse(filter));
            final int searchSize = messages.length;
            limit = Math.min(maxMessagesParsed, messages.length);
            if (limit == maxMessagesParsed) {
                if (reverseSearch) {
                    messages = Arrays.copyOfRange(messages, 0, limit);
                } else {
                    messages =
                            Arrays.copyOfRange(messages, messages.length - limit, messages.length);
                }
            }
            System.out.println("==> Found " + searchSize + " mail(s) matching the criteria");
            System.out.println(
                    "==> Fetching "
                            + (reverseSearch ? "oldest" : "latest")
                            + " "
                            + limit
                            + " mail(s)");
        } else {
            messages = inbox.getMessages(start, end);
            limit = messages.length;
            System.out.println(
                    "==> Fetching "
                            + (reverseSearch ? "oldest" : "latest")
                            + " "
                            + (end - start + 1)
                            + " mail(s)");
        }

        if (messages.length == 0 || messages == null) {
            if (inbox.isOpen()) {
                inbox.close(false);
            }
            store.close();
            return;
        } else if (reverseOrder) {
            ArrayUtils.reverse(messages);
        } else {
            ;
        }

        for (int i = limit - 1; i >= 0; i--) {
            Message message = messages[i];
            System.out.println(
                    "------------------------------------------------------------------");
            System.out.printf("%16s: %d%n", ANSI_UNDERLINE + "Email No" + ANSI_RESET, limit - i);
            System.out.printf(
                    "%16s: %.1f %s%n",
                    ANSI_UNDERLINE + "Size" + ANSI_RESET,
                    message.getSize() >= 1024 * 1024
                            ? (double) message.getSize() / (1024 * 1024)
                            : (double) message.getSize() / 1024,
                    message.getSize() >= 1024 * 1024 ? "MB" : "kB");
            System.out.printf(
                    "%16s: %s%n", ANSI_UNDERLINE + "Subject" + ANSI_RESET, message.getSubject());
            System.out.printf(
                    "%16s: %s%n",
                    ANSI_UNDERLINE + "Sent" + ANSI_RESET,
                    new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss Z", Locale.getDefault())
                            .format(message.getSentDate()));

            System.out.printf(
                    "%16s: %s%n",
                    ANSI_UNDERLINE + "From" + ANSI_RESET,
                    Arrays.stream(message.getFrom())
                            .map(InternetAddress.class::cast)
                            .map(
                                    addr ->
                                            addr.getPersonal() != null
                                                    ? "<\""
                                                            + addr.getPersonal()
                                                            + "\" <"
                                                            + addr.getAddress()
                                                            + ">>"
                                                    : "<" + addr.getAddress() + ">")
                            .collect(Collectors.joining(" ")));

            System.out.printf(
                    "%16s: %s%n",
                    ANSI_UNDERLINE + "To" + ANSI_RESET,
                    (message.getRecipients(Message.RecipientType.TO) != null
                            ? Arrays.stream(message.getRecipients(Message.RecipientType.TO))
                                    .map(InternetAddress.class::cast)
                                    .map(
                                            addr ->
                                                    addr.getPersonal() != null
                                                            ? "<\""
                                                                    + addr.getPersonal()
                                                                    + "\" <"
                                                                    + addr.getAddress()
                                                                    + ">>"
                                                            : "<" + addr.getAddress() + ">")
                                    .collect(Collectors.joining(" "))
                            : "<\""
                                    + this.prop.getProperty("mail.proto.name")
                                    + "\" <"
                                    + this.username
                                    + "@"
                                    + this.prop.getProperty("mail.proto.domain")
                                    + ">>"));

            if (message.getRecipients(Message.RecipientType.CC) != null) {
                System.out.printf(
                        "%16s: %s%n",
                        ANSI_UNDERLINE + "Cc" + ANSI_RESET,
                        Arrays.stream(message.getRecipients(Message.RecipientType.CC))
                                .map(InternetAddress.class::cast)
                                .map(
                                        addr ->
                                                addr.getPersonal() != null
                                                        ? "<\""
                                                                + addr.getPersonal()
                                                                + "\" <"
                                                                + addr.getAddress()
                                                                + ">>"
                                                        : "<" + addr.getAddress() + ">")
                                .collect(Collectors.joining(" ")));
            }

            System.out.printf(
                    "%16s: %s%n",
                    ANSI_UNDERLINE + "Reply To" + ANSI_RESET,
                    Arrays.stream(message.getReplyTo())
                            .map(InternetAddress.class::cast)
                            .map(
                                    addr ->
                                            addr.getPersonal() != null
                                                    ? "<\""
                                                            + addr.getPersonal()
                                                            + "\" <"
                                                            + addr.getAddress()
                                                            + ">>"
                                                    : "<" + addr.getAddress() + ">")
                            .collect(Collectors.joining(" ")));

            System.out.println(
                    "------------------------------------------------------------------");

            String mailId =
                    UUID.randomUUID().toString()
                            + "_"
                            + ((InternetAddress) message.getFrom()[0])
                                    .getAddress()
                                    .replace("@", "_at_")
                            + "_"
                            + message.getSubject().trim().toLowerCase().replaceAll("\\s+", "_");
            mailId =
                    mailId.length() >= MAX_FILENAME_LENGTH
                            ? mailId.substring(0, MAX_FILENAME_LENGTH)
                            : mailId;

            final File mailDir = new File(System.getProperty("user.dir"), mailId);
            final ContentType contentType = new ContentType(message.getContentType());

            Object content = message.getContent();
            if (content instanceof MimeMultipart) {
                MimeMultipart multipart = (MimeMultipart) content;

                for (int j = 0; j < multipart.getCount(); j++) {
                    BodyPart bodyPart = multipart.getBodyPart(j);

                    if (bodyPart.isMimeType("text/plain")) {
                        System.out.println("Content: " + bodyPart.getContent());
                    } else if (bodyPart.isMimeType("text/html")) {
                        final String htmlContent = parse(bodyPart.getContent().toString()).text();
                        System.out.println("Content: " + htmlContent);
                    } else if (bodyPart.isMimeType("application/*")
                            || bodyPart.isMimeType("image/*")) {
                        System.out.println(
                                "Content: "
                                        + bodyPart.getContentType()
                                        + " ("
                                        + bodyPart.getDisposition()
                                        + ") "
                                        + String.format(
                                                "[%.1f %s]",
                                                bodyPart.getSize() >= 1024 * 1024
                                                        ? (double) bodyPart.getSize()
                                                                / (1024 * 1024)
                                                        : (double) bodyPart.getSize() / 1024,
                                                bodyPart.getSize() >= 1024 * 1024 ? "MB" : "kB"));

                        if (saveAttachments) {
                            if (!mailDir.exists()) {
                                mailDir.mkdirs();
                            }
                            String filename =
                                    Paths.get(bodyPart.getFileName()).getFileName().toString();
                            File f = new File(mailDir, filename);
                            InputStream is = bodyPart.getInputStream();

                            try (FileOutputStream fos = new FileOutputStream(f)) {
                                IOUtils.copy(is, fos);
                            }
                        }
                    } else if (bodyPart.isMimeType("multipart/alternative")) {
                        Object subContent = bodyPart.getContent();
                        if (subContent instanceof MimeMultipart) {
                            MimeMultipart subMultipart = (MimeMultipart) subContent;
                            for (int k = 0; k < subMultipart.getCount(); k++) {
                                BodyPart subBodyPart = subMultipart.getBodyPart(k);
                                if (subBodyPart.isMimeType("text/plain")) {
                                    System.out.println("Content: " + subBodyPart.getContent());
                                } else if (subBodyPart.isMimeType("text/html")) {
                                    final String subHtmlBodyContent =
                                            parse(subBodyPart.getContent().toString())
                                                    .body()
                                                    .text();
                                    System.out.println("Content: " + subHtmlBodyContent);
                                } else {
                                    System.out.println(
                                            "Content-Type: " + subBodyPart.getContentType());
                                }
                            }
                        } else {
                            System.out.println(
                                    "Inner Body Content-Type: " + bodyPart.getContentType());
                        }
                    } else {
                        System.out.println("Content-Type: " + bodyPart.getContentType());
                    }
                }
            } else if (contentType.getPrimaryType().equals("text")) {
                if (contentType.getSubType().equals("plain")) {
                    System.out.println("Content: " + message.getContent());
                } else if (contentType.getSubType().equals("html")) {
                    final String htmlContent = parse(message.getContent().toString()).text();
                    System.out.println("Content: " + htmlContent);
                } else {
                    System.out.println(
                            "Unable to print content-type: " + contentType.getBaseType());
                }
            } else {
                System.out.println(
                        "Unable to get the contents of message with Content-Type: "
                                + contentType.getBaseType());
            }
        }

        System.out.println("------------------------------------------------------------------");
        if (inbox.isOpen()) {
            inbox.close(false);
        }
        store.close();
    }

    /**
     * Returns the file extension of the given file.
     *
     * @param file the file to get the extension of
     * @return the file extension, or an empty string if the file has no extension
     */
    static String getFileExtension(File file) {
        String fileName = file.getName();
        if (fileName.lastIndexOf(".") != -1 && fileName.lastIndexOf(".") != 0)
            return fileName.substring(fileName.lastIndexOf(".") + 1);
        else return "";
    }

    /**
     * Reads the content of the file at the given path, or returns the input if the path does not
     * exist or is not a regular file.
     *
     * @param input the path to the file
     * @return the content of the file, or the input if the path does not exist or is not a regular
     *     file
     */
    static String readFileOrString(final String input) {
        final Path path = Paths.get(input);
        final StringBuilder sb = new StringBuilder();

        if (!Files.exists(path) || !Files.isRegularFile(path)) {
            return input;
        }

        try (BufferedReader reader = Files.newBufferedReader(path)) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
                sb.append(System.lineSeparator());
            }
        } catch (IOException e) {
            return null;
        }

        return sb.toString();
    }

    static class MailFilter {
        /**
         * Parses the given input string into a SearchTerm. The input string is split by the "|"
         * character to get "or" terms. Each "or" term is further processed to get "and" terms. The
         * method returns a SearchTerm that represents the logical OR of all "or" terms.
         *
         * @param input the input string to parse
         * @return a SearchTerm that represents the logical OR of all "or" terms
         * @throws IllegalArgumentException if the input string is invalid
         */
        public static SearchTerm parse(final String input) {
            String[] orTerms = input.split("\\|");
            return Arrays.stream(orTerms)
                    .map(MailFilter::parseAndTerm)
                    .reduce((term1, term2) -> new OrTerm(term1, term2))
                    .orElseThrow(() -> new IllegalArgumentException("Invalid input: " + input));
        }

        /**
         * Parses the given input string into a SearchTerm. The input string is split by the "+"
         * character to get "and" terms. Each "and" term is further processed to get a SearchTerm.
         * The method returns a SearchTerm that represents the logical AND of all "and" terms.
         *
         * @param input the input string to parse
         * @return a SearchTerm that represents the logical AND of all "and" terms
         * @throws IllegalArgumentException if the input string is invalid
         */
        static SearchTerm parseAndTerm(final String input) {
            String[] andTerms = input.split("\\+");
            return Arrays.stream(andTerms)
                    .map(MailFilter::parseTerm)
                    .reduce((term1, term2) -> new AndTerm(term1, term2))
                    .orElseThrow(() -> new IllegalArgumentException("Invalid input: " + input));
        }

        /**
         * Parses the given input string into a SearchTerm. The input string is split by the ":"
         * character to get a field and its value. The method returns a SearchTerm based on the
         * field and its value.
         *
         * @param input the input string to parse
         * @return a SearchTerm based on the field and its value
         * @throws IllegalArgumentException if the input string is invalid
         */
        static SearchTerm parseTerm(final String input) {
            String[] parts = input.split(":");
            if (parts.length != 2) {
                throw new IllegalArgumentException("Invalid term: " + input);
            }

            SearchTerm st = null;
            final String field = parts[0];

            final boolean negateValue = parts[1].startsWith("!") ? true : false;
            final String value = negateValue ? parts[1].substring(1) : parts[1];

            try {
                switch (field) {
                    case "body":
                        st = new BodyTerm(value);
                        break;
                    case "subject":
                        st = new SubjectTerm(value);
                        break;
                    case "from":
                        st = new FromTerm(new InternetAddress(value));
                        break;
                    case "number":
                        st = new MessageNumberTerm(Integer.parseInt(value));
                        break;
                    case "received":
                        st = new ReceivedDateTerm(DateTerm.EQ, prepareZonedDate(value));
                        break;
                    case "received_after":
                        st = new ReceivedDateTerm(DateTerm.GE, prepareZonedDate(value));
                        break;
                    case "received_before":
                        st = new ReceivedDateTerm(DateTerm.LE, prepareZonedDate(value));
                        break;
                    case "sent":
                        st = new SentDateTerm(DateTerm.EQ, prepareZonedDate(value));
                        break;
                    case "sent_after":
                        st = new SentDateTerm(DateTerm.GE, prepareZonedDate(value));
                        break;
                    case "sent_before":
                        st = new SentDateTerm(DateTerm.LE, prepareZonedDate(value));
                        break;
                    case "to":
                        st =
                                new RecipientTerm(
                                        Message.RecipientType.TO, new InternetAddress(value));
                        break;
                    case "cc":
                        st =
                                new RecipientTerm(
                                        Message.RecipientType.CC, new InternetAddress(value));
                        break;
                    case "bcc":
                        st =
                                new RecipientTerm(
                                        Message.RecipientType.BCC, new InternetAddress(value));
                        break;
                    case "size_ge":
                        st = new SizeTerm(SizeTerm.GE, fileToBytes(value));
                        break;
                    case "size_le":
                        st = new SizeTerm(SizeTerm.LE, fileToBytes(value));
                        break;
                    case "flag":
                        Flags flag = null;
                        switch (value) {
                            case "seen":
                                flag = new Flags(Flags.Flag.SEEN);
                                break;
                            case "flagged":
                                flag = new Flags(Flags.Flag.FLAGGED);
                                break;
                            default:
                                flag = new Flags(value);
                        }

                        st = new FlagTerm(flag, negateValue ? false : true);
                        return st;
                    default:
                        throw new IllegalArgumentException("Unknown field: " + field);
                }
            } catch (AddressException | NumberFormatException e) {
                e.printStackTrace();
            }

            return negateValue ? new NotTerm(st) : st;
        }

        /**
         * Converts a string representing a date and time into a Date object. The string is expected
         * to be in the format "yyyy-MM-dd'T'HH.mm.ss". The date and time are interpreted as being
         * in the "Europe/Athens" time zone.
         *
         * @param dt the input string to parse
         * @return a Date object representing the date and time
         */
        static Date prepareZonedDate(final String dt) {
            final DateTimeFormatter formatter =
                    DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH.mm.ss");
            final LocalDateTime localDateTime = LocalDateTime.parse(dt, formatter);
            final ZonedDateTime zonedDateTime =
                    ZonedDateTime.of(localDateTime, ZoneId.systemDefault());

            return Date.from(zonedDateTime.toInstant());
        }

        /**
         * Converts a string representing a file size into an integer representing the size in
         * bytes. The string is expected to end with either "kb" or "mb", which are interpreted as
         * kilobytes and megabytes, respectively.
         *
         * @param fileSize the input string to parse
         * @return the file size in bytes
         * @throws IllegalArgumentException if the input string is invalid
         */
        static int fileToBytes(final String fileSize) {
            if (fileSize.length() < 3) {
                throw new IllegalArgumentException("Invalid file size format");
            }

            final String size = fileSize.substring(0, fileSize.length() - 2);
            final String unit = fileSize.substring(fileSize.length() - 2).toLowerCase();

            try {
                final float sizeInBytes = Float.parseFloat(size);

                switch (unit) {
                    case "kb":
                        return Math.round(sizeInBytes * 1024);
                    case "mb":
                        return Math.round(sizeInBytes * 1024 * 1024);
                    default:
                        throw new IllegalArgumentException(
                                "Invalid unit. Only 'kb' and 'mb' are supported.");
                }
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid number format in file size", e);
            }
        }

        /**
         * Converts a SearchTerm into a string representation. The string representation depends on
         * the type of the SearchTerm.
         *
         * @param term the SearchTerm to convert
         * @return a string representation of the SearchTerm
         */
        public static String toString(SearchTerm term) {
            if (term instanceof AndTerm) {
                AndTerm andTerm = (AndTerm) term;
                return "("
                        + toString(andTerm.getTerms()[0])
                        + " and "
                        + toString(andTerm.getTerms()[1])
                        + ")";
            } else if (term instanceof OrTerm) {
                OrTerm orTerm = (OrTerm) term;
                return "("
                        + toString(orTerm.getTerms()[0])
                        + " or "
                        + toString(orTerm.getTerms()[1])
                        + ")";
            } else if (term instanceof SubjectTerm) {
                SubjectTerm subjectTerm = (SubjectTerm) term;
                return "subject contains \"" + subjectTerm.getPattern() + "\"";
            } else if (term instanceof BodyTerm) {
                BodyTerm bodyTerm = (BodyTerm) term;
                return "body contains \"" + bodyTerm.getPattern() + "\"";
            } else if (term instanceof FromTerm) {
                FromTerm fromTerm = (FromTerm) term;
                return "sender is \"" + fromTerm.getAddress().toString() + "\"";
            } else if (term instanceof MessageNumberTerm) {
                MessageNumberTerm messageNumberTerm = (MessageNumberTerm) term;
                return "message number is \"" + messageNumberTerm.getNumber() + "\"";
            } else if (term instanceof ReceivedDateTerm) {
                ReceivedDateTerm recvDateTerm = (ReceivedDateTerm) term;
                switch (recvDateTerm.getComparison()) {
                    case DateTerm.EQ:
                        return "received date is \"" + recvDateTerm.getDate() + "\"";
                    case DateTerm.LE:
                        return "received before date \"" + recvDateTerm.getDate() + "\"";
                    case DateTerm.GE:
                        return "received after date \"" + recvDateTerm.getDate() + "\"";
                    default:
                        return "";
                }
            } else if (term instanceof SentDateTerm) {
                SentDateTerm sentDateTerm = (SentDateTerm) term;
                switch (sentDateTerm.getComparison()) {
                    case DateTerm.EQ:
                        return "sent date is \"" + sentDateTerm.getDate() + "\"";
                    case DateTerm.LE:
                        return "sent before date \"" + sentDateTerm.getDate() + "\"";
                    case DateTerm.GE:
                        return "sent after date \"" + sentDateTerm.getDate() + "\"";
                    default:
                        return "";
                }
            } else if (term instanceof RecipientTerm) {
                RecipientTerm recipientTerm = (RecipientTerm) term;
                final String recipientType = recipientTerm.getRecipientType().toString();
                switch (recipientType) {
                    case "To":
                        return "recipient is \"" + recipientTerm.getAddress().toString() + "\"";
                    case "Cc":
                        return "cc sent to \"" + recipientTerm.getAddress().toString() + "\"";
                    case "Bcc":
                        return "bcc sent to \"" + recipientTerm.getAddress().toString() + "\"";
                    default:
                        return "";
                }
            } else if (term instanceof SizeTerm) {
                SizeTerm sizeTerm = (SizeTerm) term;
                return "size "
                        + (sizeTerm.getComparison() == SizeTerm.GE
                                ? "greater than \""
                                : "less than \"")
                        + sizeTerm.getNumber()
                        + "\" bytes";
            } else if (term instanceof NotTerm) {
                NotTerm notTerm = (NotTerm) term;
                return "not " + toString(notTerm.getTerm());
            } else if (term instanceof FlagTerm) {
                FlagTerm flagTerm = (FlagTerm) term;
                return "flag \""
                        + flagTerm.getFlags().toString().replaceAll("\\\\", "")
                        + "\""
                        + (flagTerm.getTestSet() ? " set" : " not set");
            } else {
                throw new IllegalArgumentException("Unknown term: " + term);
            }
        }
    }
}
