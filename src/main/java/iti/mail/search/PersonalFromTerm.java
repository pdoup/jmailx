package iti.mail.search;

import java.util.Arrays;
import java.util.Optional;

import javax.mail.Address;
import javax.mail.Message;
import javax.mail.internet.InternetAddress;
import javax.mail.search.AddressTerm;

public final class PersonalFromTerm extends AddressTerm {

    private static final long serialVersionUID = 5214730291502658669L;
    private final Optional<String> personalName;

    /**
     * Constructor
     *
     * @param address The Address to be compared
     */
    public PersonalFromTerm(Address address) {
        super(address);
        this.personalName = Optional.ofNullable(((InternetAddress) this.address).getPersonal());
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
     * The Personal Name (RFC822) based address comparator.
     *
     * @param msg The address comparison is applied to this Message
     * @return true if the comparison succeeds, otherwise false
     */
    @Override
    public boolean match(Message msg) {
        InternetAddress[] from = null;
        try {
            from =
                    Arrays.stream(msg.getFrom())
                            .map(InternetAddress.class::cast)
                            .toArray(InternetAddress[]::new);
        } catch (Exception e) {
            return false;
        }

        if (from == null || !personalName.isPresent()) {
            return false;
        }

        for (final InternetAddress internetAddress : from) {
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
        if (!(obj instanceof PersonalFromTerm)) {
            return false;
        }
        return super.equals(obj);
    }
}
