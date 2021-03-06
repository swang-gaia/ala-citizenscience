package au.com.gaiaresources.bdrs.controller.attribute.formfield;

import au.com.gaiaresources.bdrs.controller.attribute.DisplayContext;

/**
 * The <code>FormField</code> binds an <code>Attribute</code> in an object that can be sorted by
 * weight for rendering by the view.
 */
public interface FormField extends Comparable<FormField> {

    /**
     * Returns the display weight of this form field. Lower weighted objects are
     * displayed first.
     * 
     * @return the display weight of this form field.
     */
    public int getWeight();

    /**
     * Returns true if this form field represents an <code>Attribute</code> from
     * a <code>Survey</code>, false otherwise.
     * 
     * @return true if this form field represents an Attribute
     * 
     */
    public boolean isAttributeFormField();

    /**
     * Returns true if this form field represents a bean property from a
     * <code>Record</code> false otherwise.
     * 
     * @return true if this form field represents a bean property from a
     *         <code>Record</code> false otherwise.
     */
    public boolean isPropertyFormField();

    /**
     * Returns true if this form field is one that cannot be entered and 
     * is only displayed on the form such as comments, horizontal rules, 
     * HTML, etc.
     * 
     * @return true if this form field is for display only, false otherwise.
     */
    public boolean isDisplayFormField();
    
    /**
     * Returns true if this form field represents a {@link TypedAttributeValueFormField} 
     * and the {@link AttributeScope} of the {@link Attribute} is a moderation type.
     * @return
     */
    public boolean isModerationFormField();

    /**
     * Returns true if this FormField should be visible in the supplied DisplayContext.
     * @param context the context to check the visibility in.
     * @return true if this FormField should be visible, false otherwise.
     */
    public boolean isVisible(DisplayContext context);
    
    /**
     * Gets a name that uniquely identifies this form field on the form.
     * @return Unique name within the form.
     */
    public String getName();
    
    /**
     * Gets a prefix for the id, name etc of form field.
     * @return
     */
    public String getPrefix();
    
    /**
     * Gets a string that categorizes the form field. E.g. taxongroupattr or censusmethodattr
     * @return
     */
    public String getCategory();
}
