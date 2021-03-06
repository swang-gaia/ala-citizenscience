package au.com.gaiaresources.bdrs.service.threshold.actionhandler;

import au.com.gaiaresources.bdrs.model.user.User;
import org.apache.log4j.Logger;
import org.hibernate.Session;

import au.com.gaiaresources.bdrs.service.property.PropertyService;
import au.com.gaiaresources.bdrs.service.threshold.ActionHandler;
import au.com.gaiaresources.bdrs.service.threshold.ComplexTypeOperator;
import au.com.gaiaresources.bdrs.email.EmailService;
import au.com.gaiaresources.bdrs.model.threshold.Action;
import au.com.gaiaresources.bdrs.model.threshold.Condition;
import au.com.gaiaresources.bdrs.model.threshold.Threshold;

/**
 * Sends a generated email message to the address specified in
 * {@link Action#getValue()} indicating that the threshold conditions have been
 * met.
 */
public class EmailActionHandler implements ActionHandler {

    @SuppressWarnings("unused")
    private Logger log = Logger.getLogger(getClass());

    protected EmailService emailService;
    protected PropertyService propertyService;

    private static final String SIMPLE_PROPERTY_TEMPLATE = " %s \'%s\'%n";
    private static final String COMPLEX_PROPERTY_TEMPLATE = " %s %s \'%s\' %s %s \'%s\'%n";

    /**
     * Creates a new <code>EmailActionHandler</code>
     * @param emailService the email service to use when sending emails.
     * @param propertyService provides the email subject line to use.
     */
    public EmailActionHandler(EmailService emailService,
            PropertyService propertyService) {
        this.emailService = emailService;
        this.propertyService = propertyService;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void executeAction(Session sesh, Threshold threshold, Object entity,
            Action action) throws ClassNotFoundException {

        String entityDisplayName = String.format(propertyService.getMessage(entity.getClass().getCanonicalName(), entity.getClass().getName()));
        String subject = String.format(propertyService.getMessage("action.handler.email.subject.template"), entityDisplayName);

        StringBuilder builder = new StringBuilder();

        if (entity instanceof User) {
            User u = (User)entity;
            // No need for null checks. These fields are non nullable
            // in the User model object.
            builder.append(String.format("First Name : %s%n", u.getFirstName()));
            builder.append(String.format("Last Name : %s%n", u.getLastName()));
            builder.append(String.format("Login : %s%n", u.getName()));
            builder.append(String.format("Email : %s%n%n", u.getEmailAddress()));
        }

        for (Condition condition : threshold.getConditions()) {

            builder.append(condition.getPropertyPath());
            if (condition.isSimplePropertyType()) {
                builder.append(String.format(SIMPLE_PROPERTY_TEMPLATE, condition.getValueOperator().getDisplayText(), condition.getValue()));
            } else {
                ComplexTypeOperator operator = condition.getComplexTypeOperator();
                builder.append(String.format(COMPLEX_PROPERTY_TEMPLATE, operator.getKeyLabel(), condition.getKeyOperator().getDisplayText(), condition.getKey(), operator.getValueLabel(), condition.getValueOperator().getDisplayText(), condition.getValue()));
            }
        }

        String message = String.format(propertyService.getMessage("action.handler.email.message.template"), entityDisplayName, builder.toString());
        emailService.sendMessage(action.getValue(), null, subject, message);
    }
}
