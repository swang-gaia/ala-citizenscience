package au.com.gaiaresources.bdrs.controller.record;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import au.com.gaiaresources.bdrs.controller.attribute.formfield.RecordProperty;
import au.com.gaiaresources.bdrs.controller.record.validator.CoordXValidator;
import au.com.gaiaresources.bdrs.controller.record.validator.CoordYValidator;
import au.com.gaiaresources.bdrs.controller.record.validator.CrsValidator;
import au.com.gaiaresources.bdrs.controller.record.validator.DateValidator;
import au.com.gaiaresources.bdrs.controller.record.validator.DoubleRangeValidator;
import au.com.gaiaresources.bdrs.controller.record.validator.DoubleValidator;
import au.com.gaiaresources.bdrs.controller.record.validator.DynamicDateRangeValidator;
import au.com.gaiaresources.bdrs.controller.record.validator.DynamicIntRangeValidator;
import au.com.gaiaresources.bdrs.controller.record.validator.HistoricalDateValidator;
import au.com.gaiaresources.bdrs.controller.record.validator.HtmlValidator;
import au.com.gaiaresources.bdrs.controller.record.validator.IntRangeValidator;
import au.com.gaiaresources.bdrs.controller.record.validator.IntValidator;
import au.com.gaiaresources.bdrs.controller.record.validator.RegExpValidator;
import au.com.gaiaresources.bdrs.controller.record.validator.StringValidator;
import au.com.gaiaresources.bdrs.controller.record.validator.TaxonValidator;
import au.com.gaiaresources.bdrs.controller.record.validator.TimeValidator;
import au.com.gaiaresources.bdrs.controller.record.validator.Validator;
import au.com.gaiaresources.bdrs.controller.record.validator.WktValidator;
import au.com.gaiaresources.bdrs.model.survey.BdrsCoordReferenceSystem;
import au.com.gaiaresources.bdrs.model.survey.Survey;
import au.com.gaiaresources.bdrs.model.taxa.Attribute;
import au.com.gaiaresources.bdrs.model.taxa.TaxaDAO;
import au.com.gaiaresources.bdrs.model.taxa.TaxaService;
import au.com.gaiaresources.bdrs.service.property.PropertyService;

/**
 * The <code>RecordFormValidator</code> provides a single access point to 
 * validate POST parameters.
 */
public class RecordFormValidator {

    private Map<ValidationType, Validator> validatorMap;
    
    private Map<String, String> errorMap;

    private PropertyService propertyService;

    private TaxaDAO taxaDAO;
    
    private Survey survey;
    
    private TaxaService taxaService;
    
    /**
     * Creates a new <code>RecordFormValidator</code>.
     * @param propertyService used to access configurable messages displayed to the user.
     * @param taxaDAO used to determine if species names can be resolved.
     * @param survey defines what taxa are allowed. Can be null.
     */
    public RecordFormValidator(PropertyService propertyService, TaxaDAO taxaDAO, Survey survey, TaxaService taxaService) {
        
        this.propertyService = propertyService;
        this.taxaDAO = taxaDAO;
        this.survey = survey;
        this.taxaService = taxaService;
        // use treemap as we would like the order of the errors to be deterministic
        errorMap = new TreeMap<String, String>();
        createValidators();
    }
    
    private void createValidators() {
        validatorMap = new HashMap<ValidationType, Validator>();
        validatorMap.put(ValidationType.PRIMARYKEY, new IntRangeValidator(propertyService, true, false, 0, Integer.MAX_VALUE));  
        
        validatorMap.put(ValidationType.STRING, new StringValidator(propertyService, false, true));
        validatorMap.put(ValidationType.REQUIRED_BLANKABLE_STRING, new StringValidator(propertyService, true, true));
        validatorMap.put(ValidationType.REQUIRED_NONBLANK_STRING, new StringValidator(propertyService, true, false));
        
        validatorMap.put(ValidationType.HTML, new HtmlValidator(propertyService, false, true));

        validatorMap.put(ValidationType.BARCODE, new RegExpValidator(propertyService, false, true));
        validatorMap.put(ValidationType.REQUIRED_BARCODE, new RegExpValidator(propertyService, true, false));
        validatorMap.put(ValidationType.REGEX, new RegExpValidator(propertyService, false, true));
        validatorMap.put(ValidationType.REQUIRED_REGEX, new RegExpValidator(propertyService, true, false));
        
        validatorMap.put(ValidationType.INTEGER, new IntValidator(propertyService, false, true));
        validatorMap.put(ValidationType.REQUIRED_INTEGER, new IntValidator(propertyService, true, false));
        
        validatorMap.put(ValidationType.INTEGER_RANGE, new DynamicIntRangeValidator(propertyService, false, true));
        validatorMap.put(ValidationType.REQUIRED_INTEGER_RANGE, new DynamicIntRangeValidator(propertyService, true, false));
        
        validatorMap.put(ValidationType.REQUIRED_POSITIVE_INT, new IntRangeValidator(propertyService, true, false));
        // less than, not less than or equal to.
        validatorMap.put(ValidationType.REQUIRED_POSITIVE_LESSTHAN, new IntRangeValidator(propertyService, true, false, 1000000-1));
        validatorMap.put(ValidationType.POSITIVE_LESSTHAN, new IntRangeValidator(propertyService, false, true, 1000000-1));
        
        validatorMap.put(ValidationType.DOUBLE, new DoubleValidator(propertyService, false, true));
        validatorMap.put(ValidationType.REQUIRED_DOUBLE, new DoubleValidator(propertyService, true, false));
        validatorMap.put(ValidationType.REQUIRED_DEG_LONGITUDE, new DoubleRangeValidator(propertyService, true, false, -180, 180));
        validatorMap.put(ValidationType.REQUIRED_DEG_LATITUDE, new DoubleRangeValidator(propertyService, true, false, -90, 90));
        validatorMap.put(ValidationType.DEG_LONGITUDE, new DoubleRangeValidator(propertyService, false, true, -180, 180));
        validatorMap.put(ValidationType.DEG_LATITUDE, new DoubleRangeValidator(propertyService, false, true, -90, 90));
        
        if (survey != null) {
        	setCrs(survey.getMap().getCrs());
        }
        
        validatorMap.put(ValidationType.DATE, new DateValidator(propertyService, false, true));
        validatorMap.put(ValidationType.REQUIRED_DATE, new DateValidator(propertyService, true, false));
        validatorMap.put(ValidationType.REQUIRED_HISTORICAL_DATE, new HistoricalDateValidator(propertyService, true, false));
        validatorMap.put(ValidationType.BLANKABLE_HISTORICAL_DATE, new HistoricalDateValidator(propertyService, false, true));
        validatorMap.put(ValidationType.DATE_WITHIN_RANGE, new DynamicDateRangeValidator(propertyService, false, true));
        validatorMap.put(ValidationType.REQUIRED_DATE_WITHIN_RANGE, new DynamicDateRangeValidator(propertyService, true, false));
        validatorMap.put(ValidationType.REQUIRED_TIME, new TimeValidator(propertyService, true, false));
        validatorMap.put(ValidationType.TIME, new TimeValidator(propertyService, false, true));
        
        validatorMap.put(ValidationType.REQUIRED_TAXON, new TaxonValidator(propertyService, true, false, taxaDAO, survey, taxaService));
        validatorMap.put(ValidationType.TAXON, new TaxonValidator(propertyService, false, true, taxaDAO, survey, taxaService));
        
        validatorMap.put(ValidationType.REQUIRED_CRS, new CrsValidator(propertyService, true, false));
    }
    
    /**
     * Returns a mapping of validation error messages. Messages are keyed against
     * the input name that was validated.
     * @return mapping of validation error messages.
     */
    public Map<String, String> getErrorMap() {
        return errorMap;
    }

    /**
     * Validates the first value in the array for the specified <code>key</code>
     * in the <code>parameterMap</code>. The <code>ValidationType</code> indicates
     * the kind of validation to apply. If the input is invalid, error messages
     * are stored in an internal map that is accessible via {@link #getErrorMap()}. 
     * 
     * @param parameterMap the POST dictionary of values. The key of the map 
     * is the input name and the value array is the input values. Note that only
     * the first input value gets validated.
     * @param type the kind of validation to apply to the input.
     * @param key the name of the input to validate.
     * @param attribute attached to the value
     * @return true if the input was valid, false otherwise.
     */
    
    public boolean validate(Map<String, String[]> parameterMap, ValidationType type, String key, Attribute attribute) {
        Validator v = validatorMap.get(type);
        if(v == null) {
            throw new IllegalArgumentException("Cannot find validator for: "+type);
        } else {
            return v.validate(parameterMap, key, attribute, errorMap);
        }
    }
    /**
     * Validates the first value in the array for the specified <code>key</code>
     * in the <code>parameterMap</code>. The <code>ValidationType</code> indicates
     * the kind of validation to apply. If the input is invalid, error messages
     * are stored in an internal map that is accessible via {@link #getErrorMap()}. 
     * 
     * @param parameterMap the POST dictionary of values. The key of the map 
     * is the input name and the value array is the input values. Note that only
     * the first input value gets validated.
     * @param type the kind of validation to apply to the input.
     * @param key the name of the input to validate.
     * @param attribute attached to the value
     * @param recordProperty a RecordProperty to be assessed
     * @return true if the input was valid and not hidden, false otherwise.
     */
	public boolean validate(Map<String, String[]> parameterMap,
			ValidationType type, String key, Attribute attribute,
			RecordProperty recordProperty) {
		if (recordProperty.isHidden()) {
			//The record property is hidden so we cannot validate it.
			return true;
		} else {
			// Now we really do the validation
			return this.validate(parameterMap, type, key, attribute);
		}
	}
	
	/**
	 * Set the expected coordinate reference system to be used when validating fields for 
	 * the current record. This is initially set to the survey's crs but we can re-set it to
	 * whatever we require.
	 */
	public void setCrs(BdrsCoordReferenceSystem crs) {
		if (crs == null) {
			throw new IllegalArgumentException("BdrsCoordRefernceSystem cannot be null");
		}
    	validatorMap.put(ValidationType.COORD_X, new CoordXValidator(propertyService, false, true, crs));
        validatorMap.put(ValidationType.REQUIRED_COORD_X, new CoordXValidator(propertyService, true, false, crs));
        validatorMap.put(ValidationType.COORD_Y, new CoordYValidator(propertyService, false, true, crs));
        validatorMap.put(ValidationType.REQUIRED_COORD_Y, new CoordYValidator(propertyService, true, false, crs));	
        validatorMap.put(ValidationType.REQUIRED_WKT, new WktValidator(propertyService, true, false, crs));
	}
}


