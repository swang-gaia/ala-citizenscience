package au.com.gaiaresources.bdrs.controller.attribute;

import au.com.gaiaresources.bdrs.controller.AbstractController;
import au.com.gaiaresources.bdrs.controller.RenderController;
import au.com.gaiaresources.bdrs.controller.attribute.formfield.RecordProperty;
import au.com.gaiaresources.bdrs.controller.attribute.formfield.RecordPropertyType;
import au.com.gaiaresources.bdrs.controller.record.RecordFormValidator;
import au.com.gaiaresources.bdrs.controller.record.WebFormAttributeParser;
import au.com.gaiaresources.bdrs.controller.survey.SurveyBaseController;
import au.com.gaiaresources.bdrs.deserialization.record.AttributeParser;
import au.com.gaiaresources.bdrs.model.metadata.Metadata;
import au.com.gaiaresources.bdrs.model.metadata.MetadataDAO;
import au.com.gaiaresources.bdrs.model.method.CensusMethod;
import au.com.gaiaresources.bdrs.model.method.CensusMethodDAO;
import au.com.gaiaresources.bdrs.model.survey.Survey;
import au.com.gaiaresources.bdrs.model.survey.SurveyDAO;
import au.com.gaiaresources.bdrs.model.survey.SurveyFormRendererType;
import au.com.gaiaresources.bdrs.model.taxa.Attribute;
import au.com.gaiaresources.bdrs.model.taxa.AttributeDAO;
import au.com.gaiaresources.bdrs.model.taxa.AttributeType;
import au.com.gaiaresources.bdrs.model.taxa.TaxaDAO;
import au.com.gaiaresources.bdrs.model.taxa.TaxaService;
import au.com.gaiaresources.bdrs.model.threshold.ActionType;
import au.com.gaiaresources.bdrs.security.Role;
import au.com.gaiaresources.bdrs.service.content.ContentService;
import au.com.gaiaresources.bdrs.service.property.PropertyService;
import au.com.gaiaresources.bdrs.service.threshold.ThresholdService;
import au.com.gaiaresources.bdrs.servlet.BdrsWebConstants;
import au.com.gaiaresources.bdrs.servlet.view.PortalRedirectView;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.propertyeditors.CustomDateEditor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.ServletRequestDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import javax.annotation.security.RolesAllowed;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.ws.http.HTTPException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Map.Entry;

@RolesAllowed( {Role.POWERUSER,Role.SUPERVISOR,Role.ADMIN} )
@Controller
public class SurveyAttributeBaseController extends AbstractController {
    
    @SuppressWarnings("unused")
    private Logger log = Logger.getLogger(getClass());

    @Autowired
    private TaxaDAO taxaDAO;

    @Autowired
    private MetadataDAO metadataDAO;

    @Autowired
    private SurveyDAO surveyDAO;
    
    @Autowired
    private AttributeDAO attributeDAO;
    
    @Autowired
    private CensusMethodDAO cmDAO;
    
    @Autowired 
    private ThresholdService thresholdService;
    
    /**
     * Content access for moderation email settings.
     */
    @Autowired
    private ContentService contentService;
    
    @Autowired
    private PropertyService propertyService;
    
    @Autowired
    private TaxaService taxaService;
    
    private AttributeFormFieldFactory formFieldFactory = new AttributeFormFieldFactory();
    
    // will there be a 'standard taxonomic' (i.e. no census method) census method
    // provided in the contribute menu.
    public static final String PARAM_DEFAULT_CENSUS_METHOD_PROVIDED = "defaultCensusMethodProvided";
    public static final String PARAM_MODERATION_REQUIRED_EMAIL = "ownerToModeratorEmail";
    public static final String PARAM_MODERATION_PERFORMED_EMAIL = "moderatorToOwnerEmail";

    /**
     * Creates a surveyEditAttributes view.
     * @param request <code>HttpServletRequest</code> that contains a key called 'surveyId'.
     * @param response <code>HttpServletResponse</code> 
     * @return <code>ModelAndView</code> that contains a <code>Survey</code> and a List of type <code>AttributeFormField</code>.
     */
    @RolesAllowed({Role.ADMIN, Role.ROOT, Role.POWERUSER, Role.SUPERVISOR})
    @RequestMapping(value = "/bdrs/admin/survey/editAttributes.htm", method = RequestMethod.GET)
    public ModelAndView editSurveyAttributes(HttpServletRequest request, HttpServletResponse response) {
        Survey survey = getSurvey(request.getParameter(BdrsWebConstants.PARAM_SURVEY_ID));
        
        if (survey == null) {
            return SurveyBaseController.nullSurveyRedirect(getRequestContext());
        }
        
        // get the modified/newly create attributes that are in error
        List<Attribute> attributeList = new ArrayList<Attribute>();
        List<Attribute> failAttributeList = new ArrayList<Attribute>();

        getAndSaveAttributes(request, attributeList, failAttributeList);
        
        List<AttributeFormField> formFieldList = new ArrayList<AttributeFormField>();
        AttributeFormField field = null;
        for(Attribute attr : survey.getAttributes()) {
            int index = -1;
            if ((index = failAttributeList.indexOf(attr)) != -1) {
                // use the failed attribute instead as it has unsaved changes.
                field = formFieldFactory.createAttributeFormField(attributeDAO, failAttributeList.remove(index));
            } else {
                field = formFieldFactory.createAttributeFormField(attributeDAO, attr);
            }
            boolean isActiveThold = thresholdService.isActiveThresholdForAttribute(attr);
            field.setHasThreshold(isActiveThold);
            formFieldList.add(field);
        }
        
        // add the remaining new unsaved attributes
        for(Attribute attr : failAttributeList) {
            field = formFieldFactory.createAttributeFormField(attributeDAO, attr);
            boolean isActiveThold = thresholdService.isActiveThresholdForAttribute(attr);
            field.setHasThreshold(isActiveThold);
        }

        for(RecordPropertyType type : RecordPropertyType.values()) {
            RecordProperty recordProperty = new RecordProperty(survey, type, metadataDAO);
            field = formFieldFactory.createAttributeFormField(recordProperty);
            boolean isActiveThold = thresholdService.isActiveThresholdForRecordProperty(recordProperty);
            field.setHasThreshold(isActiveThold);
            formFieldList.add(field);
        }
        
        Collections.sort(formFieldList);
        
        ModelAndView mv = new ModelAndView("surveyEditAttributes");
        mv.addObject("survey", survey);
        mv.addObject("formFieldList", formFieldList);
        // add the email templates for moderation email settings
        mv.addObject("emailTemplates", contentService.getKeysStartingWith("email"));
        // add the settings for moderation emails
        Metadata md = survey.getMetadataByKey(Metadata.MODERATION_REQUIRED_EMAIL);
        mv.addObject(PARAM_MODERATION_REQUIRED_EMAIL, md == null ? ContentService.MODERATION_REQUIRED_EMAIL_KEY : md.getValue());
        md = survey.getMetadataByKey(Metadata.MODERATION_PERFORMED_EMAIL);
        mv.addObject(PARAM_MODERATION_PERFORMED_EMAIL, md == null ? ContentService.MODERATION_PERFORMED_EMAIL_KEY : md.getValue());
        return mv;
    }

    /**
     * Handles the saving of formfield and censusMethods attributes related to a Survey.
     * @param request <code>HttpServletRequest</code> that optionally contains a key called 'childCensusMethod'.
     * @param response <code>HttpServletResponse</code>
     * @param childCensusMethodList that gets populated by the <code>HttpServletRequest</code>.
     * @return <code>ModelAndView</code> with a <code>RedirectView</code>
     */
    @SuppressWarnings("unchecked")
    @RolesAllowed({Role.ADMIN, Role.ROOT, Role.POWERUSER, Role.SUPERVISOR})
    @RequestMapping(value = "/bdrs/admin/survey/editAttributes.htm", method = RequestMethod.POST)
    public ModelAndView submitSurveyAttributes(HttpServletRequest request, HttpServletResponse response,
            @RequestParam(value="childCensusMethod", required=false) int[] childCensusMethodList,
            @RequestParam(value = PARAM_DEFAULT_CENSUS_METHOD_PROVIDED, defaultValue="false") boolean defaultCensusMethodProvided,
            @RequestParam(value = PARAM_MODERATION_PERFORMED_EMAIL, required=false) String modPerformedEmailKey,
            @RequestParam(value = PARAM_MODERATION_REQUIRED_EMAIL, required=false) String modRequiredEmailKey) {
        
        Survey survey = getSurvey(request.getParameter(BdrsWebConstants.PARAM_SURVEY_ID));

        if (survey == null) {
            return SurveyBaseController.nullSurveyRedirect(getRequestContext());
        }
        
        Map<String, String[]> parameterMap = request.getParameterMap();
        for (RecordPropertyType type : RecordPropertyType.values()) {
            RecordProperty recordProperty = new RecordProperty(survey, type, metadataDAO);
            formFieldFactory.createAttributeFormField(recordProperty, parameterMap);
        }
        
        survey.setDefaultCensusMethodProvided(defaultCensusMethodProvided, metadataDAO);
        
        List<Attribute> attributeList = new ArrayList<Attribute>();
        List<Attribute> failAttributeList = new ArrayList<Attribute>();

        Map<String, String> errorMap = getAndSaveAttributes(request, attributeList, failAttributeList);

        survey.setAttributes(attributeList);

        // Update the form rendering type given the new criteria
        SurveyFormRendererType formRenderType = survey.getFormRendererType();
        if(formRenderType == null || !formRenderType.isEligible(survey)) {
            Metadata md = survey.setFormRendererType(SurveyFormRendererType.DEFAULT);
            metadataDAO.save(md);
        }

        updateModerationSettings(modPerformedEmailKey, modRequiredEmailKey, survey);

        // no child protection!
        List<CensusMethod> childList = new ArrayList<CensusMethod>();
        if (childCensusMethodList != null) {
            for (int cmId : childCensusMethodList) {
                CensusMethod child = cmDAO.get(cmId);
                childList.add(child);
            }
        }
        survey.setCensusMethods(childList);
        
        surveyDAO.save(survey);

        ModelAndView mv;
        if (errorMap.size() > 0) {
            // there was an error in one of the HTML attributes
            // send the error back and redirect to the current page
            for (Entry<String,String> error : errorMap.entrySet()) {
                getRequestContext().addMessage("bdrs.survey.attributes.parsingError", new Object[]{error.getKey(), error.getValue()});
            }
            mv = new ModelAndView(new PortalRedirectView("/bdrs/admin/survey/editAttributes.htm", true));
            mv.addObject(BdrsWebConstants.PARAM_SURVEY_ID, survey.getId());
            mv.addAllObjects(getFailedAttributeObjects(failAttributeList));
            return mv;
        }
        
        getRequestContext().addMessage("bdrs.survey.attributes.success", new Object[]{survey.getName()});
        
        if(request.getParameter("saveAndContinue") != null) {
            mv = new ModelAndView(new PortalRedirectView("/bdrs/admin/survey/editMap.htm", true));
            mv.addObject(BdrsWebConstants.PARAM_SURVEY_ID, survey.getId());
        }
        else if(request.getParameter("saveAndPreview") != null) {
            mv = new ModelAndView(new PortalRedirectView(RenderController.SURVEY_RENDER_REDIRECT_URL, true));
            mv.addObject(BdrsWebConstants.PARAM_SURVEY_ID, survey.getId());
            mv.addObject("preview", "preview");
        }
        else {
            mv = new ModelAndView(new PortalRedirectView("/bdrs/admin/survey/listing.htm", true));
        }
        return mv;
    }

    /**
     * Updates the email settings for moderation attributes and enables commenting on the Survey if the Survey is
     * able to trigger the MODERATION_HISTORY threshold action.
     *
     * @param modPerformedEmailKey the value of the parameter containing the moderation performed email key.
     * @param modRequiredEmailKey the value of the parameter containing the moderation required email key.
     * @param survey the survey to update moderation settings for.
     */
    private void updateModerationSettings(String modPerformedEmailKey, String modRequiredEmailKey, Survey survey) {
        // update the moderation email keys if specified
        updateModerationSettings(survey, modPerformedEmailKey, ContentService.MODERATION_PERFORMED_EMAIL_KEY, Metadata.MODERATION_PERFORMED_EMAIL);
        updateModerationSettings(survey, modRequiredEmailKey, ContentService.MODERATION_REQUIRED_EMAIL_KEY, Metadata.MODERATION_REQUIRED_EMAIL);

        // Check if the survey is able to trigger a moderation history action, and if so automatically enable
        // commenting.
        if (thresholdService.canTriggerAction(ActionType.MODERATION_HISTORY, survey.getAttributes())) {
            survey.setRecordCommentsEnabled(true, metadataDAO);
        }
    }


    /**
     * Updates the email settings for moderation attributes.  Creates a {@link Metadata} value for metadataKey
     * with value paramValue if the paramValue is specified and is not equal to the default content, specified
     * by contentKey. If it matches the default, any existing {@link Metadata} for metadataKey will be removed.
     *
     * @param survey The survey that contains the metadata
     * @param paramValue The value of the metadata to set
     * @param contentKey The default content key
     * @param metadataKey The key for the metadata
     */
    private void updateModerationSettings(Survey survey, String paramValue, String contentKey, String metadataKey) {
        // if it is not null and is not the default key, set a metadata for the value
        if (paramValue != null && !paramValue.equals(contentKey)) {
            Metadata md = survey.addMetadata(metadataKey, paramValue);
            metadataDAO.save(md);
        } else if (paramValue == null || paramValue.equals(contentKey)) {
            // delete the existing metadata if it is reset to the default
            Metadata md = survey.getMetadataByKey(metadataKey);
            if (md != null) {
                survey.removeMetadata(md);
                metadataDAO.save(md);
                metadataDAO.delete(md);
            }
        }
    }

    /**
     * Fake the form field creation by creating the attribute form fields manually here.
     * @param failAttributeList
     * @return
     */
    private Map<String, ?> getFailedAttributeObjects(
            List<Attribute> failAttributeList) {
        Map<String, Object> returnAtts = new HashMap<String, Object>();
        int index = 0;
        for (Attribute attribute : failAttributeList) {
            if (attribute.getId() != null) {
                int pk = attribute.getId();
                returnAtts.put("attribute", pk);
                returnAtts.put("name_"+pk, attribute.getName());
                returnAtts.put("description_"+pk, attribute.getDescription());
                returnAtts.put("typeCode_"+pk, attribute.getTypeCode());
                returnAtts.put("required_"+pk, attribute.isRequired());
                returnAtts.put("tag_"+pk, attribute.isTag());
                returnAtts.put("scope_"+pk, attribute.getScope());
                returnAtts.put("weight_"+pk, attribute.getWeight());
                // set the options
                returnAtts.put("option_"+pk, StringUtils.join(attribute.getOptions().toArray(), ","));
            } else {
                returnAtts.put("add_attribute", index);
                returnAtts.put("add_name_"+index, attribute.getName());
                returnAtts.put("add_description_"+index, attribute.getDescription());
                returnAtts.put("add_typeCode_"+index, attribute.getTypeCode());
                returnAtts.put("add_required_"+index, attribute.isRequired());
                returnAtts.put("add_tag_"+index, attribute.isTag());
                returnAtts.put("add_scope_"+index, attribute.getScope());
                returnAtts.put("add_weight_"+index, attribute.getWeight());
                // set the options
                returnAtts.put("add_option_"+index, StringUtils.join(attribute.getOptions().toArray(), ","));
            }
        }
        
        return returnAtts;
    }
    
    @SuppressWarnings("unchecked")
    private Map<String, String> getAndSaveAttributes(HttpServletRequest request,
            List<Attribute> attributeList, List<Attribute> failAttributeList) {
        // First look for any Attributes that may have been updated
        // All attributes have a hidden input called 'attribute'
        Attribute attr;
        boolean isValid = true;
        AttributeParser parser = new WebFormAttributeParser(taxaDAO);
        RecordFormValidator validator = new RecordFormValidator(propertyService, taxaDAO, null, taxaService);
        if(request.getParameterValues("attribute") != null) {
            for(String rawAttrPk : request.getParameterValues("attribute")) {
                if(rawAttrPk != null && !rawAttrPk.isEmpty()) {
                    attr = taxaDAO.getAttribute(Integer.parseInt(rawAttrPk));
                    AttributeFormField formField = formFieldFactory.createAttributeFormField(attributeDAO, cmDAO, attr, request.getParameterMap());
                    Attribute newAttr = ((AttributeInstanceFormField)formField).getAttribute();
                    // check that the attribute is valid if it is an HTML type attribute
                    // don't validate HTML_NO_VALIDATION types
                    if (newAttr.getType().equals(AttributeType.HTML) ||
                            newAttr.getType().equals(AttributeType.HTML_COMMENT)) {
                        isValid &= parser.validate(validator, "description_"+rawAttrPk, null, newAttr, request.getParameterMap(), null);
                    }
                    
                    if (AttributeType.HTML_HORIZONTAL_RULE.equals(newAttr.getType()) || !StringUtils.isEmpty(request.getParameter("name_"+rawAttrPk))) {
                        // everything EXCEPT horizontal rule needs a valid name field.
                        // if name field doesn't exist, ignore
                        if (isValid) {
                            attr = (Attribute) formField.save();
                            attributeList.add(attr);
                        } else {
                            failAttributeList.add(newAttr);
                        }
                    }
                }
            }
        }

        // Create new Attributes
        if(request.getParameter("add_attribute") != null) {
            for(String rawIndex : request.getParameterValues("add_attribute")) {
                if(rawIndex != null && !rawIndex.isEmpty()) {
                    int index = Integer.parseInt(rawIndex);
                    AttributeFormField formField = formFieldFactory.createAttributeFormField(attributeDAO, cmDAO, index, request.getParameterMap());
                    Attribute newAttr = ((AttributeInstanceFormField)formField).getAttribute();
                    // check that the attribute is valid if it is an HTML type attribute
                    // don't validate HTML_NO_VALIDATION types
                    if (newAttr.getType().equals(AttributeType.HTML) ||
                            newAttr.getType().equals(AttributeType.HTML_COMMENT)) {
                        isValid &= parser.validate(validator, "add_description_"+rawIndex, null, newAttr, request.getParameterMap(), null);
                    }
                    if (AttributeType.HTML_HORIZONTAL_RULE.equals(newAttr.getType()) || !StringUtils.isEmpty(request.getParameter("add_name_"+rawIndex))) {
                        // everything EXCEPT horizontal rule needs a valid name field.
                        // if name field doesn't exist, ignore
                        if (isValid) {
                            attributeList.add((Attribute)formField.save());
                        } else {
                            failAttributeList.add(newAttr);
                        }
                    }
                }
            }
        }
        
        return validator.getErrorMap();
    }

    private Survey getSurvey(String rawSurveyId) {
        if(rawSurveyId == null){
            // Do not know which survey to deal with. Bail out.
            throw new HTTPException(HttpServletResponse.SC_NOT_FOUND);
        }
        return surveyDAO.getSurvey(Integer.parseInt(rawSurveyId));
    }

    @InitBinder
    public void initBinder(HttpServletRequest request, ServletRequestDataBinder binder) {
        DateFormat dateFormat = new SimpleDateFormat("dd MMM yyyy");
        dateFormat.setLenient(false);
        binder.registerCustomEditor(Date.class, new CustomDateEditor(
                dateFormat, true));
    }
}
