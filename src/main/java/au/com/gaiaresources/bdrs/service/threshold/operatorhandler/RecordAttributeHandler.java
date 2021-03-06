package au.com.gaiaresources.bdrs.service.threshold.operatorhandler;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

import org.apache.log4j.Logger;
import org.hibernate.Session;

import au.com.gaiaresources.bdrs.service.threshold.ConditionOperatorHandler;
import au.com.gaiaresources.bdrs.service.threshold.OperatorHandler;
import au.com.gaiaresources.bdrs.model.taxa.AttributeValue;
import au.com.gaiaresources.bdrs.model.taxa.TypedAttributeValue;
import au.com.gaiaresources.bdrs.model.threshold.Condition;

/**
 * Checks that at least one {@link AttributeValue} from an iterable of
 * {@link AttributeValue} objects matches the {@link Condition}. This handler
 * is attribute type aware and will appropriately convert the attribute
 * value before comparison.
 */
public class RecordAttributeHandler implements OperatorHandler {

    @SuppressWarnings("unused")
    private Logger log = Logger.getLogger(getClass());

    @Override
    public boolean match(Session sesh,
            ConditionOperatorHandler conditionOperatorHandler, Object entity,
            Condition condition) throws IllegalArgumentException,
            IllegalAccessException, InvocationTargetException,
            ClassNotFoundException {
        
        @SuppressWarnings("unchecked")
        List<Object> properties = condition.getPropertiesForPath(entity);
        if (properties == null || properties.isEmpty()) {
            return false;
        }
        
        String expectedKey = condition.getKey();
        boolean returnValue = false;
        for (Object property : properties) {
            Iterable<AttributeValue> attributes = (Iterable<AttributeValue>) property;
            if (attributes == null) {
                return false;
            }
            
            for (TypedAttributeValue recAttr : attributes) {
            
                String actualKey = recAttr.getAttribute().getName();
                boolean match = false;
                
                boolean isKeyMatch = conditionOperatorHandler.match(condition.getKeyOperator(), actualKey, expectedKey); 
                // If we have not previously found a match and 
                // this is an attribute that matches our key, check out the value.
                if (!returnValue && isKeyMatch) {
                    switch (recAttr.getAttribute().getType()) {
                    case STRING:
                    case STRING_AUTOCOMPLETE:
                    case TEXT:
                    case STRING_WITH_VALID_VALUES:
                    case IMAGE:
                    case FILE:
                    case AUDIO:
                    case VIDEO:
                    case BARCODE:
                    case REGEX:
                    case TIME:
                        match = conditionOperatorHandler.match(condition.getValueOperator(), recAttr.getStringValue(), condition.stringValue());
                        break;
                    case INTEGER:
                    case INTEGER_WITH_RANGE:
                        match = conditionOperatorHandler.match(condition.getValueOperator(), recAttr.getNumericValue().intValue(), condition.intValue());
                        break;
                    case SINGLE_CHECKBOX:
                    	match = conditionOperatorHandler.match(condition.getValueOperator(), recAttr.getBooleanValue(), condition.booleanValue());
                    	break;
                    case MULTI_CHECKBOX:
                    	match = conditionOperatorHandler.match(condition.getValueOperator(), recAttr.getMultiCheckboxValue(), condition.stringArrayValue());
                    	break;
                    case MULTI_SELECT:
                    	match = conditionOperatorHandler.match(condition.getValueOperator(), recAttr.getMultiSelectValue(), condition.stringArrayValue());
                    	break;
                    case DECIMAL:
                        match = conditionOperatorHandler.match(condition.getValueOperator(), recAttr.getNumericValue().doubleValue(), condition.doubleValue());
                        break;
                    case DATE:
                        match = conditionOperatorHandler.match(condition.getValueOperator(), recAttr.getDateValue(), condition.dateValue());
                        break;
                    case HTML:
                    case HTML_NO_VALIDATION:
                    case HTML_COMMENT:
                    case HTML_HORIZONTAL_RULE:
                        // HTML attributes will never have an attribute value because they are display only
                        match = false;
                        break;
                    case SPECIES:
                    	match = conditionOperatorHandler.match(condition.getValueOperator(), recAttr.getStringValue(), condition.stringValue());
                    	break;
                    case CENSUS_METHOD_ROW:
                    case CENSUS_METHOD_COL:
                        // census method attributes never match conditions
                        match = false;
                        break;
                    default:
                        log.warn(String.format("Unknown attribute type found %s Match is false.", recAttr.getAttribute().getType().toString()));
                        match = false;
                        break;
                    }
                }
                returnValue = returnValue || match;
            }
        }
        return returnValue;
    }
}
