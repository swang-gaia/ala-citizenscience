package au.com.gaiaresources.bdrs.controller.attribute;

import java.util.Map;

import au.com.gaiaresources.bdrs.controller.attribute.formfield.RecordProperty;
import au.com.gaiaresources.bdrs.model.method.CensusMethodDAO;
import au.com.gaiaresources.bdrs.model.taxa.Attribute;
import au.com.gaiaresources.bdrs.model.taxa.AttributeDAO;

/**
 * <p>
 * The <code>AttributeFormFieldFactory</code> is a one-stop-shop for the
 * creation of all types of <code>AttributeFormFields</code>.
 * </p>
 * The primary purpose of <code>AttributeFormField</code> is represent the
 * ordering of an <code>Attribute</code> or <code>Record</code> property on a
 * record ordering form. The <code>AttributeFormFieldFactory</code> provides an
 * abstracted means of instantiating the appropriate form field type that in
 * turn, provides an abstracted means of managing the weight of
 * <code>Attributes</code> or <code>Record</code> properties via
 * <code>Metadata</code>.
 * 
 * <p>
 * This factory abstracts the specific implementation of
 * <code>AttributeFormField</code> and performs any necessary configuration
 * before returning the field.
 * </p>
 */
public class AttributeFormFieldFactory {

    /**
     * Creates a new <code>AttributeFormFieldFactory</code>.
     */
    public AttributeFormFieldFactory() {
    }

    /**
     * Creates a new form field for a previously saved <code>Attribute</code>.
     * 
     * @param dao
     *            the database object to use when saving the attribute.
     * @param attribute
     *            the attribute where the form field value is stored.
     * @return an <code>AttributeFormField</code>.
     * @see AttributeInstanceFormField
     */
    public AttributeFormField createAttributeFormField(AttributeDAO dao,
            Attribute attribute) {
        return new AttributeInstanceFormField(dao, attribute);
    }

    /**
     * Creates a new, blank form field that is used for adding new
     * <code>Attributes</code>.
     * 
     * Note: You cannot invoke {@link AttributeFormField#save()} on fields
     * returned by this invocation because not all parameters required to
     * construct a valid Attribute (such as description and type) are available.
     * To create a new Attribute see
     * {@link #createAttributeFormField(AttributeDAO, int, Map)}
     * 
     * @param index
     *            the index of this field. The first added field is 1, the
     *            second is 2 and so on.
     * @return an <code>AttributeFormField</code>.
     * @see AttributeInstanceFormField
     */
    public AttributeFormField createAttributeFormField(int index) {
        return new AttributeInstanceFormField(index);
    }

    /**
     * Creates and populates a new <code>Attribute</code>.
     * 
     * @param dao
     *            the database object to use when saving the attribute.
     * @param index
     *            the index of this field. The first added field is 1, the
     *            second is 2 and so on.
     * @param parameterMap
     *            the map of POST parameters that the form field will utilise to
     *            populate the <code>Attribute</code> that is created.
     * @return an <code>AttributeFormField</code>.
     * @see AttributeInstanceFormField
     */
    public AttributeFormField createAttributeFormField(AttributeDAO dao, CensusMethodDAO cmDAO,
            int index, Map<String, String[]> parameterMap) {
        return new AttributeInstanceFormField(dao, cmDAO, index, parameterMap);
    }

    /**
     * Updates the specified <code>Attribute</code> using the POST parameters
     * provided.
     * 
     * @param dao
     *            the database object to use when saving the attribute.
     * @param cmDAO
     *            the dao to use for looking up census methods for CM attribute types
     * @param attribute
     *            the <code>Attribute</code> that shall be updated.
     * @param parameterMap
     *            the map of POST parameters that the form field will utilise to
     *            populate the <code>Attribute</code> that is created.
     * @return an <code>AttributeFormField</code>.
     * @see AttributeInstanceFormField
     */
    public AttributeFormField createAttributeFormField(AttributeDAO dao, CensusMethodDAO cmDAO, 
            Attribute attribute, Map<String, String[]> parameterMap) {
        return new AttributeInstanceFormField(dao, cmDAO, attribute, parameterMap);
    }
    
    /**
     * Creates a new form field representing a <code>Record</code> property.
     * This will create and save a new Metadata instance if an instance for the
     * specified property does not already exist.
     * 
     * @param recordProperty 
     *            the <code>RecordProperty</code> that has access to the <code>RecordPropertySetting</code>
     * @return an <code>AttributeFormField</code>.
     * @see RecordPropertyAttributeFormField
     */
    public AttributeFormField createAttributeFormField(RecordProperty recordProperty) {
    	return new RecordPropertyAttributeFormField(recordProperty);
    }

    /**
     * Updates the record property sorting weight metadata from the specified
     * POST parameter map.
     * 
	 * @param recordProperty 
     *            the <code>RecordProperty</code> that has access to the <code>RecordPropertySetting</code>
     * @param parameterMap
     *            the map of POST parameters that the form field will utilise to
     *            populate the <code>Metadata</code> that is created.
     * @return an <code>AttributeFormField</code>.
     * @see RecordPropertyAttributeFormField
     */
    public AttributeFormField createAttributeFormField(RecordProperty recordProperty,
            Map<String, String[]> parameterMap) {
        return new RecordPropertyAttributeFormField(recordProperty,
                parameterMap);
    }
}
