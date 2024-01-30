/** This class represents the main entry point of the application. */
package iti.mail;

import static java.net.URLDecoder.*;

import org.apache.commons.cli.*;
import org.apache.commons.io.FilenameUtils;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

import javax.mail.*;
import javax.mail.internet.AddressException;

public class Main {

    /** The default mail server confifuration file with the connection details */
    static final String confFile = "mailserver.properties";

    /**
     * Prints the help message and exits the program.
     *
     * @param name The name of the command line application.
     * @param formatter The HelpFormatter object used to print the help message.
     * @param options The Options object containing the command line options.
     * @param exitCode The exit code to use when terminating the program.
     */
    static final void showHelpAndExit(
            final String name,
            final HelpFormatter formatter,
            final Options options,
            final int exitCode) {
        formatter.printHelp(
                name,
                "\n- Simple terminal-based email management wizard\n\n",
                options,
                System.lineSeparator(),
                true);
        System.exit(exitCode);
        return;
    }

    /**
     * Retrieves the name and version of the current JAR file.
     *
     * @return The base name of the path of the JAR file or empty if encoding exception occurs.
     */
    static String getJarNameVersion() {
        try {
            final String path =
                    decode(
                            Main.class
                                    .getProtectionDomain()
                                    .getCodeSource()
                                    .getLocation()
                                    .getPath(),
                            StandardCharsets.UTF_8.name());
            return FilenameUtils.getBaseName(path);
        } catch (UnsupportedEncodingException uee) {
            return "javax-null";
        }
    }

    /**
     * Returns the absolute path of the properties file.
     *
     * @param filePath The path of the file as a string.
     * @return The absolute path of the properties file.
     * @throws IOException If the file does not exist, is a directory, or read permissions are
     *     missing. If the file does not exist, the method suggests supplying a configuration file
     *     as a command-line argument using the "-p" option or ensuring the presence of a
     *     "mailserver.properties" file in the same directory as the JAR file.
     */
    static String getAbsolutePropertiesPath(final String filePath) throws IOException {
        if (filePath != null) {
            final Path path = Paths.get(filePath);
            if (Files.exists(path)) {
                if (Files.isReadable(path) && !Files.isDirectory(path)) {
                    return path.toAbsolutePath().toString();
                } else {
                    throw new IOException(
                            "Properties file \""
                                    + path.getFileName()
                                    + "\" is a directory or read permissions are missing");
                }
            } else {
                throw new IOException(
                        "Properties file \""
                                + path.getFileName()
                                + "\" does not exist. Either supply a configuration file as a"
                                + " command-line argument using the \"-p\" option or ensure the"
                                + " presence of a \"mailserver.properties\" file in the same"
                                + " directory as the JAR file");
            }
        } else {
            return getAbsolutePropertiesPath(confFile);
        }
    }

    /**
     * The main method of the application.
     *
     * @param args the command-line arguments
     */
    public static void main(String[] args) {
        MailClient mc = null;
        Options options = new Options();

        final String jarName = getJarNameVersion();

        Option recipientOption = new Option("r", "recipient", true, "Email recipients");
        recipientOption.setRequired(false);
        options.addOption(recipientOption);

        Option ccOption = new Option("c", "cc", true, "Add CC recipients");
        ccOption.setRequired(false);
        options.addOption(ccOption);

        Option bccOption = new Option("b", "bcc", true, "Add BCC recipients");
        bccOption.setRequired(false);
        options.addOption(bccOption);

        Option subjectOption = new Option("s", "subject", true, "Email subject");
        subjectOption.setRequired(false);
        options.addOption(subjectOption);

        Option messageOption =
                new Option(
                        "m",
                        "message",
                        true,
                        "Email message or path to text file with message body");
        messageOption.setRequired(false);
        options.addOption(messageOption);

        Option attachmentOption = new Option("a", "attachment", true, "Pass in attachment");
        attachmentOption.setRequired(false);
        options.addOption(attachmentOption);

        Option saveOption =
                new Option(
                        "d",
                        "download",
                        false,
                        "Download all file attachments (combined with -l and/or -f)");
        saveOption.setRequired(false);
        options.addOption(saveOption);

        Option filterOption = new Option("f", "filter", true, "Expression-based message filtering");
        filterOption.setRequired(false);
        options.addOption(filterOption);

        Option folderOption = new Option("e", "folder", true, "Name of folder to open");
        folderOption.setRequired(false);
        options.addOption(folderOption);

        Option old2newOption =
                new Option(
                        "o",
                        "from-oldest",
                        false,
                        "Fetch messages from oldest to newest (combined with -l and/or -f)");
        old2newOption.setRequired(false);
        options.addOption(old2newOption);

        Option reverseOption =
                new Option(
                        "i",
                        "reverse",
                        false,
                        "Reverse the order of messages displayed (combined with -l and/or -f)");
        reverseOption.setRequired(false);
        options.addOption(reverseOption);

        Option option =
                Option.builder("l")
                        .desc(
                                "Limit the number of emails displayed to <N>. \"all\" fetches all"
                                        + " messages from server")
                        .longOpt("limit")
                        .hasArg()
                        .type(Number.class)
                        .build();
        options.addOption(option);

        Option propertiesOption =
                new Option(
                        "p",
                        "properties",
                        true,
                        "Path to the properties file, default \""
                                + confFile
                                + "\""
                                + System.lineSeparator());
        propertiesOption.setRequired(false);
        options.addOption(propertiesOption);

        options.addOption("v", "version", false, "Show program version");
        options.addOption("h", "help", false, "Show this help message");

        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();

        formatter.setWidth(120);
        formatter.setDescPadding(7);
        formatter.setLeftPadding(2);
        formatter.setSyntaxPrefix("Usage: ");
        formatter.setNewLine(System.lineSeparator());
        formatter.setOptPrefix("-");
        formatter.setLongOptPrefix("--");
        formatter.setLongOptSeparator("=");
        formatter.setArgName("arg");
        formatter.setOptionComparator(null);

        CommandLine cmd = null;
        try {
            cmd = parser.parse(options, args, true);
        } catch (ParseException e) {
            System.out.println(e.getMessage());
            showHelpAndExit(jarName, formatter, options, 1);
        }

        if (cmd.hasOption("h")
                || cmd.hasOption("v")
                || cmd.getOptions().length == 0
                || cmd.getArgs().length > 0) {
            if (cmd.hasOption("v") && !cmd.hasOption("h")) {
                System.out.println(jarName);
                return;
            } else {
                showHelpAndExit(jarName, formatter, options, 0);
            }
        } else if (cmd.hasOption("r")) {
            if (cmd.hasOption("l") || cmd.hasOption("f")) {
                System.out.println(
                        "Error: If 'recipient' is present, neither 'filter' nor 'fetch' should be"
                                + " present.");
                showHelpAndExit(jarName, formatter, options, 1);
            }
        } else if (!(cmd.hasOption("f") || cmd.hasOption("l"))) {
            System.out.println(
                    "Error: If 'recipient' is not present, at least one of 'fetch' or 'filter' must"
                            + " be present.");
            showHelpAndExit(jarName, formatter, options, 1);
        }

        try {
            mc = new MailClient(getAbsolutePropertiesPath(cmd.getOptionValue(propertiesOption)));

            if (cmd.hasOption("l") || cmd.hasOption("f")) {
                int fetchNumber = Integer.MIN_VALUE;
                if (cmd.hasOption("l")) {
                    try {
                        fetchNumber = ((Number) cmd.getParsedOptionValue("l")).intValue();
                    } catch (ParseException pe) {
                        final String parseError = pe.getMessage();
                        final String parseErrorString =
                                parseError.substring(
                                        parseError.indexOf("\"") + 1, parseError.lastIndexOf("\""));
                        if (!parseErrorString.equalsIgnoreCase("all")) {
                            System.out.println("Invalid message limit " + parseErrorString);
                            showHelpAndExit(jarName, formatter, options, 1);
                        }
                    }
                }
                final String mailFilter = cmd.getOptionValue("f");
                final boolean saveAttachments = cmd.hasOption("d") ? true : false;
                final boolean reverseOrder = cmd.hasOption("i") ? true : false;
                final boolean old2new = cmd.hasOption("o") ? true : false;
                final String folderName = cmd.getOptionValue("e");

                mc.read(
                        mailFilter,
                        fetchNumber,
                        saveAttachments,
                        reverseOrder,
                        old2new,
                        folderName);
            } else {
                final String recipients = String.join(",", cmd.getOptionValues(recipientOption));

                final String cc =
                        Optional.ofNullable(cmd.getOptionValues("cc"))
                                .map(addr -> String.join(",", addr))
                                .orElse("");

                final String bcc =
                        Optional.ofNullable(cmd.getOptionValues(bccOption))
                                .map(addr -> String.join(",", addr))
                                .orElse("");

                final String subject = cmd.getOptionValue("subject", "Subject");

                final String attachments =
                        Optional.ofNullable(cmd.getOptionValues("attachment"))
                                .map(attc -> String.join(",", attc))
                                .orElse(null);

                String messageText = cmd.getOptionValue("message");
                messageText = messageText == null ? FetchQuote.getQuote() : messageText;

                mc.send(recipients, subject, messageText, cc, bcc, attachments);
            }
        } catch (AuthenticationFailedException af) {
            System.out.println("==> " + "(" + mc.getUsername() + ") " + af.getMessage());
            return;
        } catch (IllegalStateException ise) {
            System.out.println("==> " + ise.getMessage());
            return;
        } catch (AddressException ae) {
            System.out.printf("==> %s: [%s]%n", ae.getLocalizedMessage(), ae.getRef());
            return;
        } catch (IOException ioe) {
            System.out.println("==> IOError: " + ioe.getLocalizedMessage());
            return;
        } catch (NoSuchProviderException nsp) {
            System.out.println(
                    "==> Error in properties file: " + "[" + nsp.getLocalizedMessage() + "]");
            return;
        } catch (IllegalArgumentException iae) {
            System.out.println("==> " + iae.getLocalizedMessage());
            return;
        } catch (MessagingException ex) {
            ex.printStackTrace();
        }
    }
}
