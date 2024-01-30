# jmailx - CLI e-mail wizard in Java

## Installation
JRE version **1.8 or greater** is required. Unless you choose to build from source, running the standalone JAR file in the release section should suffice since all the requirements are bundled.

## Prerequisites
- Personal e-mail credentials (**Username** & **Password**) are expected to be set as environment variables, `$ITIMAIL` and `$ITIPASS` respectively.
- The [`mailserver.properties`](mailserver.properties) file contains important mail server configuration as well as other options (e.g., encoding, personal options) in the form of key-value pairs. These properties can be altered accordingly to match the target server specifications (e.g. protocol selection: *POP3*, *IMAP*). You may also create your very own custom properties file and pass it to the main program using the `-p` option, following the blueprint above. 

## Getting Started
The full list of options available is displayed below

Example: ```$ java -jar jmailx-0.1.jar --help```
```terminal
Usage: jmailx-0.1 [-r <arg>] [-c <arg>] [-b <arg>] [-s <arg>] [-m <arg>] [-a <arg>] [-d] [-f <arg>] [-e <arg>] [-o] [-i]
       [-l <arg>] [-p <arg>] [-v] [-h]

- Simple terminal-based email management wizard

  -r,--recipient=<arg>        Email recipients
  -c,--cc=<arg>               Add CC recipients
  -b,--bcc=<arg>              Add BCC recipients
  -s,--subject=<arg>          Email subject
  -m,--message=<arg>          Email message or path to text file with message body
  -a,--attachment=<arg>       Pass in attachment
  -d,--download               Download all file attachments (combined with -l and/or -f)
  -f,--filter=<arg>           Expression-based message filtering
  -e,--folder=<arg>           Name of folder to open
  -o,--from-oldest            Fetch messages from oldest to newest (combined with -l and/or -f)
  -i,--reverse                Reverse the order of messages displayed (combined with -l and/or -f)
  -l,--limit=<arg>            Limit the number of emails displayed to <N>. "all" fetches all messages from server
  -p,--properties=<arg>       Path to the properties file, default "mailserver.properties"

  -v,--version                Show program version
  -h,--help                   Show this help message
```
***
This utility offers 2 basic functionalities, **sending** e-mails and **viewing** e-mails received to your account.
***
### Sending an e-mail

This is the most basic use case of this utility, enabling the user to send an email to a single recipient e.g., test@mail.com with `subject="Hello"` and `message="World"`

```$ java -jar jmailx-0.1.jar -r test@mail.com -s 'Hello' -m 'World'```

_Multiple recipients_ can also be specified, either as a collection of comma-separated addresses or by repeating an option, e.g., issue the following command to specify that the e-mail has 2 recipients, 
test1@mail.com and test2@mail.com. 

```$ java -jar jmailx-0.1.jar -r test1@mail.com -r test2@mail.com -c test3@mail.com```

Notice that the CC option is also appended in this case, thus test3@mail.com will receive a carbon copy. The same principle applies if a user specifies the `cc` or `bcc` options, meaning that
multiple CC/BCC recipients can be added. Also, you might notice how neither a subject nor any message was specified. If the user omits to specify a subject, the _default_ `Subject` will be used instead. If the message body is missing or empty, a random quote is sent from the [Quotable API](https://api.quotable.io).  

The user can also specify any number of attachments to send along with the e-mail using the `-a` option, e.g., to attach a ZIP file (archive.zip). Naturally, the file attachment must point to a valid location. 
```$ java -jar jmailx-0.1.jar -r test1@mail.com -a archive.zip```

_Note_: If the message you want to send is relatively wordy, you can pass a text file in the `-m` option, e.g. ```$ java -jar <mailapp jar file>.jar -r test1@mail.com -m long_mail.txt```. The contents of the text file will be sent as the e-mail body.

***
### Listing e-mails
The most straightforward way to retrieve and display your e-mails from a folder can be done with the following command:

E.g. Issue the following command to fetch the latest 10 e-mails:

```$ java -jar jmailx-0.1.jar -l 10```

**E-mails are displayed one by one and a prompt is given if there is more than one left to view. Follow the onscreen instructions to jump to the next e-mail or exit.**

- Saving attachments found in e-mails can be performed by appending the `-d` option. Using this option creates a unique folder for each e-mail retrieved and stores any attachments inside. It's very common for e-mails to follow the
`text/html` mimetype; if that's the case, the content of the e-mail is treated as an HTML attachment which is also downloaded when the option is specified.
- Although the default folder to search for messages is 'INBOX' this option can be altered either via configuring the `mail.proto.inbox_name` property inside the `.properties` file or through the `-e` option.
- The order the messages are displayed can also be adjusted using the `-i` option. Selecting this option will result in displaying the message in reverse chronological order (from oldest to newest)
- The `-o` option is similar to `-i` (they can also be combined) since it also reverses the order, except that `-o` lists messages beginning from the oldest e-mails stored inside the folder in question. 

#### Filtering engine
The user can impose specific filters on messages that are going to be displayed using the `-f` or `--filter` option. This is achieved through a custom searching mechanism that filters certain terms, for instance, fetch all e-mails from folder that were sent after a certain date and also contain 'Meeting' as part of the subject. This process is translated to a simple expression-based filtering schema designed to combine multiple search terms. In a nutshell, the following terms can be used as search terms:

- **body**: finds any messages that contain the following string/phrase inside the message body
-  **subject**: finds any messages that contain the following string/phrase inside the subject
-  **from**: finds any messages sent from this address. The address can either be any RFC822 address or even a personal name. Passing a personal name, e.g. 'John Smith', will attempt to fetch any messages that were sent from 'John Smith' assuming the personal name exists as part of the address. This implementation simplifies the search operation for the user. Naturally, simple addresses such as test3@mail.com are supported and addresses containing both personal and address information in the following format, e.g., `John Smith <john.smith@domain.com>`, to eliminate any ambiguity.
-  **number**: finds the message associated with the provided number
-  **received**: finds any messages received at the exact timestamp. The timestamp format must be the following `"yyyy-MM-dd'T'HH.mm.ss"`. E.g., to get all e-mails sent at 10:03:21 AM on 2023/05/20 the string format is `2023-05-20T10.03.21`. The timestamp adheres to the local timezone settings.
-  **received_after**: finds any messages received after a timestamp. The date format is the same as `received` option.
-  **received_before**: finds any messages received before a timestamp. The date format is the same as `received` option.
-  **sent**: finds any messages received at the exact timestamp. This term is particularly useful when dealing with POP3 stores where the server does not store information regarding the date an e-mail was received. The functionality is the same as `received`.
-  **sent_after**: finds any messages received after a timestamp. Same as `received_after` option.
-  **sent_before**: finds any messages received before a timestamp. Same as `received_before` option.
-  **to**: finds any messages sent to the following recipient. The value is an address following the same protocol as the `from` term above.
-  **cc**: finds any messages that were CC'ed to the following address. The value is an address following the same protocol as the `from` term above.
-  **size_ge**: finds any messages that have a total size greater or equal to the following value. The value can be any real non-negative number followed by the unit (kb or mb). Example for e-mails greater than 100 kilobytes: `size_ge:100kb`.
-  **size_le**: finds any messages that have a total size less or equal to the following value. The value can be any real non-negative number followed by the unit (kb or mb). Example for e-mails greater than 2.5 megabytes: `size_ge:2.5mb`.
- **flags**: finds any messages with the following flag. Flags are used to tag messages. Two built-in flags can be specified as values, `seen` and `flagged`. Custom tags can also be used if they are supported by the mail server.

_Note_: Negating an option is possible by prepending `!` before the value, for instance, to fetch all messages **not** containing 'Work' in the subject, issue the following statement: `subject:!Work`

**Search terms can also be combined** using logical operators between them. To ensure 2 search terms coexist the user can join the options with a `+` symbol. Furthermore, to make sure either term is present the user can join the terms with the `|` symbol. The evaluation order is from left to right. For reasons of testing and clarity, a natural language translation mechanism was also developed that convert search strings to natural language. For example, consider the following complete search expression `"subject:hello+from:test@gmail.com|size_le:4kb"`. This is equivalent to `((subject contains "hello" and sender is "test@gmail.com") or size less than "4096" bytes)"`. For more examples and test cases visit the [test suite](./src/test/java/iti/mail) folder.  

**Search terms can be repeated** multiple times with different values using the logical operators introduced above. For example, you can enforce that an e-mail must have the following two CC recipients (test1@mail.com and test2@mail.com) in order to be shown by issuing the following expression: `"cc:test1@mail.com+cc:test2@mail.com"`. Similarly, you can experiment with other terms as well as logical operators that suit your needs.

This filter option `-f` can also be used in conjunction with the rest of the pertinent options, mainly: `-l`, `-o`, `-i`, `-d`, and `-e`.

## Build from scratch
You can also build this project and generate the JAR file from scratch using the provided [build.gradle](build.gradle) file as follows (after cloning the repository):

```sh
$ gradle clean build --no-daemon
```
***

## License
This project is licensed under the Apache License 2.0 - see the [LICENSE](LICENSE) file for details.
