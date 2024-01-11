/** This class represents the main entry point of the application. */
package iti.mail;

import org.apache.commons.cli.*;

import java.io.IOException;

import javax.mail.*;

public class Main {

    /** The version of the application. */
    static final String VERSION = "0.1";

    static final void showHelpAndExit(
            final HelpFormatter formatter, final Options options, final int exitCode) {
        formatter.printHelp(
                "Mail App",
                "\n- Simple terminal-based email management wizard\n\n",
                options,
                "\nVersion: " + VERSION + System.lineSeparator(),
                true);
        System.exit(exitCode);
        return;
    }

    /**
     * The main method of the application.
     *
     * @param args the command-line arguments
     */
    public static void main(String[] args) {
        MailClient mc = null;

        Options options = new Options();

        Option recipientOption = new Option("r", "recipient", true, "Email recipient");
        recipientOption.setRequired(false);

        Option ccOption = new Option("c", "cc", true, "Add CC recipients");
        ccOption.setRequired(false);
        options.addOption(ccOption);

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

        Option attachmentOption =
                new Option("a", "attachment", true, "Pass in zip file attachment");
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
                        "Search messages from oldest to newest (combined with -l and/or -f)");
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

        options.addOption(recipientOption);
        options.addOption(option);

        options.addOption("h", "help", false, "Show this help message");

        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();

        formatter.setWidth(110);
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
            showHelpAndExit(formatter, options, 1);
        }

        if (cmd.hasOption("h") || cmd.getOptions().length == 0 || cmd.getArgs().length > 0) {
            showHelpAndExit(formatter, options, 0);
        } else if (cmd.hasOption("r")) {
            if (cmd.hasOption("l") || cmd.hasOption("f")) {
                System.out.println(
                        "Error: If 'recipient' is present, neither 'filter' nor 'fetch' should be"
                                + " present.");
                showHelpAndExit(formatter, options, 1);
            }
        } else if (!(cmd.hasOption("f") || cmd.hasOption("l"))) {
            System.out.println(
                    "Error: If 'recipient' is not present, at least one of 'fetch' or 'filter' must"
                            + " be present.");
            showHelpAndExit(formatter, options, 1);
        }

        try {
            mc = new MailClient("mailserver.properties");

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
                            showHelpAndExit(formatter, options, 1);
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
                final String recipient = cmd.getOptionValue("recipient");
                final String cc = cmd.getOptionValue("cc", "");
                final String subject = cmd.getOptionValue("subject", "Test Subject");
                final String attachmentPath = cmd.getOptionValue("attachment");

                String messageText = cmd.getOptionValue("message");
                messageText = messageText == null ? FetchQuote.getQuote() : messageText;

                mc.send(recipient, subject, messageText, cc, attachmentPath);
            }
        } catch (AuthenticationFailedException af) {
            System.out.println("==> " + "(" + mc.getUsername() + ") " + af.getMessage());
            return;
        } catch (IOException | MessagingException ex) {
            ex.printStackTrace();
        } /* catch (ParseException pe) {
              System.out.println(
                      "==> ParseException raised "
                              + pe.getMessage().toLowerCase());
              System.exit(1);
              return;
          } */
    }
}
