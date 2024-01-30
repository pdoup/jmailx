package iti.mail.search;

import java.util.Arrays;
import java.util.Optional;

import javax.mail.Address;
import javax.mail.Message;
import javax.mail.internet.InternetAddress;
import javax.mail.search.AddressTerm;

public final class PersonalRecipientTerm extends AddressTerm {

    /**
     * The recipient type.
     *
     * @serial
     */
    private Message.RecipientType type;

    private static final long serialVersionUID = 6548700653122680461L;
    private final Optional<String> personalName;

    /**
     * Constructor.
     *
     * @param type the recipient type
     * @param address the address to match for
     */
    public PersonalRecipientTerm(Message.RecipientType type, Address address) {
        super(address);
        this.type = type;
        this.personalName =
                Optional.ofNullable(InternetAddress.class.cast(this.address).getPersonal());
    }

    /**
     * Return the type of recipient to match with.
     *
     * @return the recipient type
     */
    public Message.RecipientType getRecipientType() {
        return type;
    }

    /**
     * This method returns the personal name.
     *
     * @return Optional containing the personal name if it exists, otherwise an empty Optional
     */
    public Optional<String> getPersonalName() {
        return personalName;
    }

    /**
     * The match method applied on Personal Names (RFC822).
     *
     * @param msg The address match is applied to this Message's recepient address
     * @return true if the match succeeds, otherwise false
     */
    @Override
    public boolean match(Message msg) {
        InternetAddress[] recipients = null;
        try {
            recipients =
                    Arrays.stream(msg.getRecipients(this.type))
                            .map(InternetAddress.class::cast)
                            .toArray(InternetAddress[]::new);
        } catch (Exception e) {
            return false;
        }

        if (recipients == null || !personalName.isPresent()) {
            return false;
        }

        for (final InternetAddress internetAddress : recipients) {
            final String personal = internetAddress.getPersonal();
            if (personal != null && personal.trim().equalsIgnoreCase(personalName.get().trim())) {
                return true;
            }
        }

        return false;
    }

    /** Equality comparison. */
    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof PersonalRecipientTerm)) {
            return false;
        }
        PersonalRecipientTerm rt = (PersonalRecipientTerm) obj;
        return rt.type.equals(this.type) && super.equals(obj);
    }

    /** Compute a hashCode for this object. */
    @Override
    public int hashCode() {
        return type.hashCode() + super.hashCode();
    }
}
