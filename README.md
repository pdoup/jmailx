# jmail
## CLI application for basic e-mail management in Java

## Usage

The JAR file packages all the requirements in a single executable (Java version **1.8 or greater** is required).

### Prerequisites
- E-mail credentials (**Username** & **Password**) are expected to be set in the form of environment variables, `$ITIMAIL` and `$ITIPASS` respectively.
- The [`mailserver.properties`](mailserver.properties) file contains important mail server configuration properties and must be in the same location as the entry-point JAR file. The properties can be altered accordingly to match the target server specifications (e.g. protocol selection: *POP3*, *IMAP*).

Example: ```$ java -jar <mailapp jar file>.jar --help```
```terminal
Usage: Mail App [-c <arg>] [-s <arg>] [-m <arg>] [-a <arg>] [-d] [-f <arg>] [-e <arg>] [-o] [-i] [-r <arg>]
       [-l <arg>] [-h]

- Simple terminal-based email management wizard

  -c,--cc=<arg>               Add CC recipients
  -s,--subject=<arg>          Email subject
  -m,--message=<arg>          Email message or path to text file with message body
  -a,--attachment=<arg>       Pass in zip file attachment
  -d,--download               Download all file attachments (combined with -l and/or -f)
  -f,--filter=<arg>           Expression-based message filtering
  -e,--folder=<arg>           Name of folder to open
  -o,--from-oldest            Search messages from oldest to newest (combined with -l and/or -f)
  -i,--reverse                Reverse the order of messages displayed (combined with -l and/or -f)
  -r,--recipient=<arg>        Email recipient
  -l,--limit=<arg>            Limit the number of emails displayed to <N>. "all" fetches all messages from
                              server
  -h,--help                   Show this help message

Version: 0.1
```
***
### Sending an E-mail
Issue the following command to send an email to `test@mail.com` with `subject="Hello"` and `message="World"`

```$ java -jar <mailapp jar file>.jar -r test@mail.com -s 'Hello' -m 'World'```
***
### Reading E-mails
Retrieving and displaying e-mails from a folder can be done with the following command:

E.g. Issue the following command to fetch the latest 10 e-mails:

```$ java -jar <mailapp jar file>.jar -l 10```
***
### Reading E-mails with custom filters
E-mail filtering is also available through a custom filtering interface. The full list of filtering option is available at [?].

E.g. Issue the following command to filter any e-mails sent from `example@mail.com` and these e-mails were sent after `2023-10-01 10:00:00` or subject contains the phrase `'hello'`:

```$ java -jar <mailapp jar file>.jar -f 'from:example@mail.com+sent_after:2023-10-01T10.00.00|subject:hello'```
***

## Build from scratch
You can also build this project from scratch with the provided [build.gradle](build.gradle) file as follows:

```sh
$ gradle clean build --no-daemon
```
***
## License

This project is licensed under the Apache License 2.0 - see the [LICENSE](LICENSE) file for details.
