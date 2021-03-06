package au.com.gaiaresources.bdrs.controller.record.validator;

import java.util.Map;

import au.com.gaiaresources.bdrs.model.taxa.Attribute;
import au.com.gaiaresources.bdrs.service.property.PropertyService;

/**
 * Validates if an input is required and if it may be blank.
 */
public abstract class AbstractValidator implements Validator {
    
    public static final String REQUIRED_MESSAGE_KEY = "AbstractValidator.required";
    private static final String REQUIRED_MESSAGE = "This field is required.";
    
    private static final String BLANK_MESSAGE_KEY = "AbstractValidator.blank";
    private static final String BLANK_MESSAGE = "This cannot be blank.";
    
    
    /**
     * Used to limit the size of the string we can enter in the database.
     * The AttributeValue field is actually 
     */
    private static final int MAX_VALUE_LENGTH = 8191;
    
    private static final String VALUE_TOO_LONG_MESSAGE_KEY = "AbstractValidator.valueTooLong";
    private static final String VALUE_TOO_LONG_MESSAGE = "The length of the input is limited to %d characters.";
    
    protected PropertyService propertyService;
    protected boolean blank;
    protected boolean required;
    
    /**
     * Creates a new <code>AbstractValidator</code>.
     * 
     * @param propertyService used to access configurable messages displayed to the user.
     * @param required true if the input is mandatory, false otherwise.
     * @param blank true if the value can be an empty string, false otherwise.
     */
    public AbstractValidator(PropertyService propertyService, boolean required, boolean blank) {
        this.propertyService = propertyService;
        this.required = required;
        this.blank = blank;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean validate(Map<String, String[]> parameterMap, String key, Attribute attribute, Map<String, String> errorMap) {

        String value = getSingleParameter(parameterMap, key);        
        if(required && value == null) {
            errorMap.put(key, propertyService.getMessage(REQUIRED_MESSAGE_KEY, REQUIRED_MESSAGE));
        } else if(!blank && value.isEmpty()) {
            errorMap.put(key, propertyService.getMessage(BLANK_MESSAGE_KEY, BLANK_MESSAGE));
        } else if (value != null && value.length() > MAX_VALUE_LENGTH) {
            errorMap.put(key, String.format(propertyService.getMessage(VALUE_TOO_LONG_MESSAGE_KEY, VALUE_TOO_LONG_MESSAGE), MAX_VALUE_LENGTH));
        }
        
        return !errorMap.containsKey(key); 
    }
    
    /**
     * Returns the first value in the array specified by the <code>key</code> in
     * the parameter<code>parameterMap</code> if one exists, otherwise null.
     * 
     * @param parameterMap the map of data containing the specified key
     * @param key the key to be used to retrieve the array of values.
     * @return the first value in the array in the map or null.
     */
    protected String getSingleParameter(Map<String, String[]> parameterMap, String key) {
        String[] values = parameterMap.get(key);
        if(values == null) {
            return null;
        } else if(values.length == 0) {
            return null;
        } else {
            return values[0];
        }
    }

}
