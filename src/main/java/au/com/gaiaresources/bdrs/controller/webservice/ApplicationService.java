package au.com.gaiaresources.bdrs.controller.webservice;

import au.com.gaiaresources.bdrs.controller.AbstractController;
import au.com.gaiaresources.bdrs.controller.attribute.formfield.RecordProperty;
import au.com.gaiaresources.bdrs.controller.attribute.formfield.RecordPropertyType;
import au.com.gaiaresources.bdrs.db.impl.PersistentImpl;
import au.com.gaiaresources.bdrs.file.FileService;
import au.com.gaiaresources.bdrs.json.JSONArray;
import au.com.gaiaresources.bdrs.json.JSONException;
import au.com.gaiaresources.bdrs.json.JSONObject;
import au.com.gaiaresources.bdrs.model.file.ManagedFile;
import au.com.gaiaresources.bdrs.model.file.ManagedFileDAO;
import au.com.gaiaresources.bdrs.model.location.Location;
import au.com.gaiaresources.bdrs.model.location.LocationDAO;
import au.com.gaiaresources.bdrs.model.metadata.Metadata;
import au.com.gaiaresources.bdrs.model.metadata.MetadataDAO;
import au.com.gaiaresources.bdrs.model.method.CensusMethod;
import au.com.gaiaresources.bdrs.model.method.CensusMethodDAO;
import au.com.gaiaresources.bdrs.model.record.Record;
import au.com.gaiaresources.bdrs.model.record.RecordDAO;
import au.com.gaiaresources.bdrs.model.record.RecordGroup;
import au.com.gaiaresources.bdrs.model.record.RecordGroupDAO;
import au.com.gaiaresources.bdrs.model.survey.Survey;
import au.com.gaiaresources.bdrs.model.survey.SurveyDAO;
import au.com.gaiaresources.bdrs.model.taxa.*;
import au.com.gaiaresources.bdrs.model.user.User;
import au.com.gaiaresources.bdrs.model.user.UserDAO;
import au.com.gaiaresources.bdrs.security.UserDetails;
import au.com.gaiaresources.bdrs.service.survey.SurveyImportExportService;
import au.com.gaiaresources.bdrs.servlet.BdrsWebConstants;
import au.com.gaiaresources.bdrs.servlet.RequestContext;
import au.com.gaiaresources.bdrs.servlet.RequestContextHolder;
import au.com.gaiaresources.bdrs.util.SpatialUtil;
import au.com.gaiaresources.bdrs.util.SpatialUtilFactory;
import javassist.scopedpool.SoftValueHashMap;
import org.apache.commons.beanutils.PropertyUtils;
import org.apache.log4j.Logger;
import org.codehaus.plexus.util.StringUtils;
import org.hibernate.FlushMode;
import org.hibernate.Session;
import org.postgresql.util.Base64;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.*;

@Controller
public class ApplicationService extends AbstractController {
    
    public static final String DOWNLOAD_SURVEY_SPECIES_URL = "/webservice/application/surveySpeciesDownload.htm";
    public static final String DOWNLOAD_SURVEY_NO_SPECIES_URL = "/webservice/application/surveyDownload.htm";
    public static final String CREATE_SURVEY_URL = "/webservice/application/createSurvey.htm";
    
    public static final String LEGACY_DOWNLOAD_SURVEY_URL = "/webservice/application/survey.htm";
    
    public static final String CLIENT_SYNC_STATUS_KEY = "status";
    
    public static final String JSON_KEY_SUCCESS = "success";
    
    public static final String JSON_KEY_MESSAGE = "message";
    
    public static final String JSON_KEY_ID = "id";
    
    public static final String JSON_KEY_SURVEY = "survey";
    public static final String JSON_KEY_ATTR_AND_OPTS = "attributesAndOptions";
    public static final String JSON_KEY_CENSUS_METHODS = "censusMethods";
    public static final String JSON_KEY_RECORD_PROP = "recordProperties";
    public static final String JSON_KEY_LOCATIONS = "locations";
    public static final String JSON_KEY_SPECIES_INFO_ITEMS = "profileItems";
    public static final String JSON_KEY_TAXON_GROUPS = "taxonGroups";

    public static final String UPLOAD_JSON_KEY_SURVEY_ID = "survey_id";

    public static final String JSON_KEY_TAXON_GROUP_ID = "taxonGroupId";
    public static final String JSON_KEY_SECONDARY_TAXON_GROUPS = "secondaryTaxonGroups";

    public static final String JSON_KEY_MANAGED_FILE = "managedFile";
    
    public static final String PARAM_IDENT = "ident";
    
    /**
     * JSON key for defining an IndicatorSpecies id.
     */
    public static final String JSON_KEY_TAXON_ID = "taxon_id";
    
    /**
     * JSON key for the number of records on the server for the user
     * submitting the http request.
     */
    public static final String JSON_KEY_SERVER_RECORDS_FOR_USER = "serverRecordCount";
    
    /**
     * JSON key for the downloaded survey template
     */
    public static final String JSON_KEY_SURVEY_TEMPLATE = "survey_template";
    
    /**
     * Query param, what index to start the get species request. starts at 0
     */
    public static final String PARAM_FIRST = "first";
    
    /**
     * Query param, json format array with ids of surveys on device.
     */
    public static final String PARAM_SURVEYS_ON_DEVICE = "surveysOnDevice";
    
    /**
     * Query param, max results per get species request
     */
    public static final String PARAM_MAX_RESULTS = "maxResults";
    
    public static final String PARAM_NAME = "name";
    
    public static final String PARAM_SURVEY_TEMPLATE = "survey_template";

    public static final String PARAM_INCLUDE_PROFILE = "includeProfile";
    
    private Logger log = Logger.getLogger(getClass());

    @Autowired
    private SurveyDAO surveyDAO;
    
    @Autowired
    private TaxaDAO taxaDAO;

    @Autowired
    private UserDAO userDAO;
    
    @Autowired
    private RecordDAO recordDAO;
    
    @Autowired
    private MetadataDAO metadataDAO;
    
    @Autowired
    private CensusMethodDAO censusMethodDAO;
    
    @Autowired
    private AttributeValueDAO attributeValueDAO;
    
    @Autowired
    private TaxaService taxaService;
    
    @Autowired
    private FileService fileService;
    
    @Autowired
    private LocationDAO locationDAO;

    @Autowired
    private ManagedFileDAO managedFileDAO;

    @Autowired
    private RecordGroupDAO recordGroupDAO;
    
    @Autowired
    private SurveyImportExportService surveyImportExportService;
    
    private SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM yyyy");

    private SpatialUtil spatialUtil = new SpatialUtilFactory().getLocationUtil();
    
    /**
     * Download a survey and all of its species.
     * @param request HttpRequest
     * @param response HttpResponse
     * @param ident Ident of user
     * @param surveyRequested Id of survey
     * @throws IOException Error writing to output stream.
     */
    @RequestMapping(value = LEGACY_DOWNLOAD_SURVEY_URL, method = RequestMethod.GET)
    public void getSurvey( HttpServletRequest request, HttpServletResponse response,
                           @RequestParam(value = "ident", defaultValue = "") String ident,
                           @RequestParam(value = "sid", defaultValue = "-1") int surveyRequested) throws IOException {

        long now = System.currentTimeMillis();
        // Checks if a user exists with the provided ident. If not a response error is returned.
        if (userDAO.getUserByRegistrationKey(ident) == null) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }
        
        if(surveyRequested < 1) {
            // The survey that you want cannot exist.
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        log.debug("Authenticated in  :" + (System.currentTimeMillis() - now));now = System.currentTimeMillis();
        
        //retrieve requested survey
        Survey survey = surveyDAO.getSurvey(surveyRequested);
        log.debug("Retrieved Survey in  :" + (System.currentTimeMillis() - now));now = System.currentTimeMillis();
        
        // Retrieve taxon groups.
        List<TaxonGroup> taxonGroups = taxaDAO.getTaxonGroup(survey);
        log.debug("Got groups  :" + (System.currentTimeMillis() - now));now = System.currentTimeMillis();
        
        //Retrieve species from survey if any, otherwise get all from model
        Collection<IndicatorSpecies> species;

        // Remove data from the requested survey that already exists on the device.
        JSONArray surveysOnDeviceArray = null;
        if(request.getParameter("surveysOnDevice") != null) {
            surveysOnDeviceArray = JSONArray.fromString(request.getParameter("surveysOnDevice"));
        }
        if ((surveysOnDeviceArray != null) && (surveysOnDeviceArray.size() > 0)) {
            List<Survey> surveysOnDevice = new ArrayList<Survey>();
            for (int i=0; i<surveysOnDeviceArray.size(); i++) {
                int sid = surveysOnDeviceArray.getInt(i);
                Survey s = surveyDAO.getSurvey(sid);
                surveysOnDevice.add(s);
                //remove species
                //species.removeAll(surveyDAO.getSurveyData((Integer) sid).getSpecies());
                //remove taxonGroups
                taxonGroups.removeAll(taxaDAO.getTaxonGroup(s));
            }
            log.debug("Removed extra groups in  :" + (System.currentTimeMillis() - now));now = System.currentTimeMillis();
            
            log.debug("Querying for species in survey : " + survey.getId() + " , not in : " + surveysOnDevice.toString());
            species = surveyDAO.getSpeciesForSurvey(survey, surveysOnDevice);
            
            log.debug("Got limited set of Species in  :" + (System.currentTimeMillis() - now));now = System.currentTimeMillis();
        } else {
            if (survey.getSpecies().size() == 0) {
                species = new ArrayList<IndicatorSpecies>();
                species.addAll(taxaDAO.getIndicatorSpecies());
                log.debug("Got all species in  :" + (System.currentTimeMillis() - now));now = System.currentTimeMillis();
                
            } else {
                species = new HashSet<IndicatorSpecies>();
                species.addAll(survey.getSpecies());
                log.debug("Got survey species in  :" + (System.currentTimeMillis() - now));now = System.currentTimeMillis();
                
            }
        }

        
        // Restructure survey data
        JSONArray attArray = new JSONArray();
        JSONArray locArray = new JSONArray();
        JSONArray speciesArray = new JSONArray();
        JSONArray taxonGroupArray = new JSONArray();
        JSONArray censusMethodArray = new JSONArray();
        JSONArray recordPropertiesArray = new JSONArray();
        for (Attribute a : survey.getAttributes()) {
            // Not sending any location scoped attributes because they do not get populated by the recording form.
            if(!AttributeScope.LOCATION.equals(a.getScope())) {
                attArray.add(a.flatten(1, true, true));
            }
        }
        log.debug("Flattened attributes in  :" + (System.currentTimeMillis() - now));now = System.currentTimeMillis();

        Map<String, Object> speciesMap = null;
        for (IndicatorSpecies s : species) {
            
                // TODO AJ modified so that we don't send down taxon group 
                // attributes with EVERY taxa, need to figure out how to 
                // efficiently cram in indicator_species_attributes.
                // previously this was flattening to depth 2.
                speciesMap = s.flatten(1, true, true);
                speciesMap.put("taxonGroup", s.getTaxonGroup().getId()); // unflatten the taxongroup.
                speciesMap.remove("_class");
                speciesArray.add(speciesMap);
        }
        log.debug("Flattened Species in  :" + (System.currentTimeMillis() - now));now = System.currentTimeMillis();
        
        for (TaxonGroup t : taxonGroups) {
            taxonGroupArray.add(t.flatten(2, true, true));
        }
        log.debug("Flatted Taxon Groups in  :" + (System.currentTimeMillis() - now));now = System.currentTimeMillis();
        
        for(CensusMethod method : survey.getCensusMethods()) {
            recurseFlattenCensusMethod(censusMethodArray, method);
        }
        log.debug("Flatted Census Methods in  :" + (System.currentTimeMillis() - now));now = System.currentTimeMillis();
        
        for (RecordPropertyType recordPropertyType : RecordPropertyType.values()) {
            RecordProperty recordProperty = new RecordProperty(survey, recordPropertyType, metadataDAO);
            if (!recordProperty.isHidden()) {
                recordPropertiesArray.add(recordProperty.flatten(true, false));
            }
        }
        log.debug("Flatted RecordProperties in  :" + (System.currentTimeMillis() - now));now = System.currentTimeMillis();
        
        // Store restructured survey data in JSONObject
        JSONObject surveyData = new JSONObject();
        surveyData.put(JSON_KEY_ATTR_AND_OPTS, attArray);
        surveyData.put("indicatorSpecies_server_ids", survey.flatten());
        surveyData.put("indicatorSpecies", speciesArray);
        surveyData.put("taxonGroups", taxonGroupArray);
        surveyData.put(JSON_KEY_CENSUS_METHODS, censusMethodArray);
        surveyData.put(JSON_KEY_RECORD_PROP, recordPropertiesArray);
        surveyData.put(JSON_KEY_SURVEY_TEMPLATE, surveyImportExportService.exportObject(survey));

        // Serialize locations AFTER all lazy loading has been done.
        // Inside the flattenLocation() method we evict the locations
        // from the hibernate cache
        now = System.currentTimeMillis();
        Session sesh = getRequestContext().getHibernate();
        for (Location l : survey.getLocations()) {
            locArray.add(flattenLocation(sesh, l));
        }
        log.debug("Flatted locations in  :" + (System.currentTimeMillis() - now));now = System.currentTimeMillis();
        surveyData.put(JSON_KEY_LOCATIONS, locArray);

        // support for JSONP
        String callback = validateCallback(request.getParameter("callback"));
        if (callback != null) {
            response.setContentType("application/javascript");
            response.getWriter().write(callback
                    + "(");
        } else {
            response.setContentType("application/json");
        }

        response.getWriter().write(surveyData.toString());
        if (callback != null) {
            response.getWriter().write(");");
        }
        log.debug("Wrote out data in  :" + (System.currentTimeMillis() - now));now = System.currentTimeMillis();
    }
    
    // There is quite alot of duplicated code from getSurvey() here. Leave it this way as
    // someone may want to change the legacy web service and we would like to keep it separate
    // from this version.
    /**
     * Download a survey and none of its species.
     * @param request HttpRequest
     * @param response HttpResponse
     * @param ident Ident of user
     * @param surveyRequested Id of survey
     * @throws IOException Error writing to output stream.
     */
    @RequestMapping(value = DOWNLOAD_SURVEY_NO_SPECIES_URL, method = RequestMethod.GET)
    public void downloadSurveyNoSpecies( HttpServletRequest request, HttpServletResponse response,
                           @RequestParam(value = "ident", defaultValue = "") String ident,
                           @RequestParam(value = "sid", defaultValue = "-1") int surveyRequested) throws IOException {

        long now = System.currentTimeMillis();
        // Checks if a user exists with the provided ident. If not a response error is returned.
        if (userDAO.getUserByRegistrationKey(ident) == null) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }
        
        if(surveyRequested < 1) {
            // The survey that you want cannot exist.
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        log.debug("Authenticated in  :" + (System.currentTimeMillis() - now));now = System.currentTimeMillis();
        
        //retrieve requested survey
        Survey survey = surveyDAO.getSurvey(surveyRequested);
        
        JSONObject surveyData = getSurveyNoSpeciesJson(survey, now);

        // support for JSONP
        String callback = validateCallback(request.getParameter("callback"));
        if (callback != null) {
            response.setContentType("application/javascript");
            response.getWriter().write(callback
                    + "(");
        } else {
            response.setContentType("application/json");
        }
        
        response.getWriter().write(surveyData.toString());
        if (callback != null) {
            response.getWriter().write(");");
        }
        log.debug("Wrote out data in  :" + (System.currentTimeMillis() - now));
    }
    
    /**
     * Gets a JSON Object representing a survey
     * 
     * @param survey Survey to jsonify.
     * @param now The system time in milliseconds from the epoch.
     * @return JSONified survey.
     */
    private JSONObject getSurveyNoSpeciesJson(Survey survey, long now) {
        log.debug("Retrieved Survey in  :" + (System.currentTimeMillis() - now));now = System.currentTimeMillis();
        
        // Restructure survey data
        JSONArray attArray = new JSONArray();
        JSONArray locArray = new JSONArray();
        JSONArray censusMethodArray = new JSONArray();
        JSONArray recordPropertiesArray = new JSONArray();
        for (Attribute a : survey.getAttributes()) {
            // Not sending any location scoped attributes because they do not get populated by the recording form.
            if(!AttributeScope.LOCATION.equals(a.getScope())) {
                attArray.add(a.flatten(1, true, true));

                if(a.getCensusMethod() != null) {
                    recurseFlattenCensusMethod(censusMethodArray, a.getCensusMethod());
                }
            }
        }
        log.debug("Flattened attributes in  :" + (System.currentTimeMillis() - now));now = System.currentTimeMillis();

        for(CensusMethod method : survey.getCensusMethods()) {
            recurseFlattenCensusMethod(censusMethodArray, method);
        }
        log.debug("Flatted Census Methods in  :" + (System.currentTimeMillis() - now));now = System.currentTimeMillis();
        
        for (RecordPropertyType recordPropertyType : RecordPropertyType.values()) {
            RecordProperty recordProperty = new RecordProperty(survey, recordPropertyType, metadataDAO);
            if (!recordProperty.isHidden()) {
                recordPropertiesArray.add(recordProperty.flatten(true, false));
            }
        }
        log.debug("Flatted RecordProperties in  :" + (System.currentTimeMillis() - now));
        
        // Store restructured survey data in JSONObject
        JSONObject surveyData = new JSONObject();
        surveyData.put(JSON_KEY_ATTR_AND_OPTS, attArray);
        surveyData.put(JSON_KEY_SURVEY, survey.flatten());
        surveyData.put(JSON_KEY_CENSUS_METHODS, censusMethodArray);
        surveyData.put(JSON_KEY_RECORD_PROP, recordPropertiesArray);
        surveyData.put(JSON_KEY_SURVEY_TEMPLATE, surveyImportExportService.exportObject(survey));

        // Serialize locations AFTER all lazy loading has been done.
        // Inside the flattenLocation() method we evict the locations
        // from the hibernate cache
        now = System.currentTimeMillis();
        Session sesh = getRequestContext().getHibernate();
        for (Location l : survey.getLocations()) {
            locArray.add(flattenLocation(sesh, l));
        }
        log.debug("Flatted locations in  :" + (System.currentTimeMillis() - now));
        surveyData.put(JSON_KEY_LOCATIONS, locArray);

        return surveyData;
    }
    
    /**
     * Basic check for XSS security. We only allow a js function name.
     * @param callback
     * @return Callback string if valid, else null.
     */
    private String validateCallback(String callback) {
        if (callback == null) {
            return null;
        }
        if (callback.contains(";")) {
            log.error("Possible cross site scripting attack. callback param : " + callback);
            return null;
        }
        return callback;
    }
    
    /**
     * Download species for a survey
     * 
     * @param request HttpRequest
     * @param response HttpResponse
     * @param surveyId Survey that owns species
     * @param first The index of the first result to be returned, starts at 0.
     * @param maxResults The max number of results to return.
     * @throws IOException Error writing to output stream
     */
    @RequestMapping(value = DOWNLOAD_SURVEY_SPECIES_URL, method = RequestMethod.GET)
    public void surveySpeciesDownload(HttpServletRequest request, HttpServletResponse response,
            @RequestParam(value=BdrsWebConstants.PARAM_SURVEY_ID, required=true) Integer surveyId,
            @RequestParam(value=PARAM_FIRST, required=true) Integer first,
            @RequestParam(value=PARAM_MAX_RESULTS, required=true) Integer maxResults,
            @RequestParam(value=PARAM_INCLUDE_PROFILE, defaultValue="true") Boolean includeProfile) throws IOException {

        Survey s = surveyDAO.get(surveyId);
        JSONObject result = new JSONObject();
        if (s == null) {
            result.put("errorMessage", "Survey could not be found for id = " + surveyId);
        } else {
            // Remove data from the requested survey that already exists on the device.
            JSONArray surveysOnDeviceArray = null;
            List<Survey> surveysOnDevice = new ArrayList<Survey>();
            if(request.getParameter(PARAM_SURVEYS_ON_DEVICE) != null) {
                surveysOnDeviceArray = JSONArray.fromString(request.getParameter(PARAM_SURVEYS_ON_DEVICE));
            }
            if ((surveysOnDeviceArray != null) && (!surveysOnDeviceArray.isEmpty())) {
                for (int i=0; i<surveysOnDeviceArray.size(); i++) {
                    int sid = surveysOnDeviceArray.getInt(i);
                    Survey survey = surveyDAO.getSurvey(sid);
                    if (survey != null) {
                        surveysOnDevice.add(survey);    
                    }
                }
            }

            Set<TaxonGroup> groupsInSpecies = new HashSet<TaxonGroup>();
            
            int speciesDownloadCount = taxaDAO.countActualSpeciesForSurvey(s, surveysOnDevice);
            List<IndicatorSpecies> list = taxaDAO.getIndicatorSpeciesBySurvey(null, s, first, maxResults, surveysOnDevice);
            result.put("count", speciesDownloadCount);
            JSONArray jsonSpeciesArray = new JSONArray();
            Map<String, Object> speciesMap = null;
            for (IndicatorSpecies sp : list) {

                groupsInSpecies.add(sp.getTaxonGroup());
                groupsInSpecies.addAll(sp.getSecondaryGroups());

                // handcraft map for json
                // if we need taxon group, other items that require table joins we need to look at
                // going back to adding left fetch join to the hibernate query for performance.
                // As things are now, accessing any of the child relations will cause a lazy load
                // for every access, i.e. it's slow.
                speciesMap = new HashMap<String, Object>();
                speciesMap.put("server_id", sp.getId());
                speciesMap.put("scientificNameAndAuthor", sp.getScientificNameAndAuthor());
                speciesMap.put("scientificName", sp.getScientificName());
                speciesMap.put("commonName", sp.getCommonName());
                speciesMap.put("author", sp.getAuthor());
                speciesMap.put("year", sp.getYear());
                speciesMap.put(ApplicationService.JSON_KEY_TAXON_GROUP_ID, sp.getTaxonGroup().getId());
                List<Integer> secondaryGroupIds = new ArrayList<Integer>(sp.getSecondaryGroups().size());
                for (TaxonGroup secondaryGroup : sp.getSecondaryGroups()) {
                    secondaryGroupIds.add(secondaryGroup.getId());
                }
                speciesMap.put(ApplicationService.JSON_KEY_SECONDARY_TAXON_GROUPS, secondaryGroupIds);

                // add the species profile list
                if (includeProfile) {
                    JSONArray speciesProfileList = new JSONArray();
                    for (SpeciesProfile profile : sp.getInfoItems()) {
                        // don't use flatten. be minimal in what we send back.
                        JSONObject profileJson = new JSONObject();
                        profileJson.put("header", profile.getHeader());
                        profileJson.put("description", profile.getDescription());
                        profileJson.put("type", profile.getType());
                        profileJson.put("content", profile.getContent());
                        profileJson.put("id", profile.getId());
                        profileJson.put("weight", profile.getWeight());

                        if (profile.isImgType()) {
                            // uuid is stored in content property.
                            ManagedFile theFile = managedFileDAO.getManagedFile(profile.getContent());
                            if (theFile != null) {
                                profileJson.put(JSON_KEY_MANAGED_FILE, JSONObject.fromMapToJSONObject(theFile.flatten()));
                            }
                        }

                        speciesProfileList.add(profileJson);
                    }
                    speciesMap.put(JSON_KEY_SPECIES_INFO_ITEMS, speciesProfileList);
                }
                
                jsonSpeciesArray.add(speciesMap);
            }
            result.put("list", jsonSpeciesArray);

            JSONArray taxonGroupJsonArray = new JSONArray();
            for (TaxonGroup tg : groupsInSpecies) {
                 JSONObject taxonGroupJson = new JSONObject();
                taxonGroupJson.put("id", tg.getId());
                taxonGroupJson.put("name", tg.getName());
                taxonGroupJson.put("image", tg.getImage());
                taxonGroupJson.put("thumbNail", tg.getThumbNail());
                taxonGroupJsonArray.add(taxonGroupJson);
            }

            result.put(JSON_KEY_TAXON_GROUPS, taxonGroupJsonArray);
        }
        this.writeJson(response, result.toString());
    }
    
    @RequestMapping(value = "/webservice/application/clientSyncLocations.htm", method = RequestMethod.POST)
    public ModelAndView clientSyncLocations(HttpServletRequest request, HttpServletResponse response,
            @RequestParam(value="inFrame", defaultValue="true") boolean inFrame) throws IOException {
        /*
         * { 
         *      status : 200,
         *      200 : { ... } 
         * }
         * 
         * or
         * 
         * {
         *      status : 500,
         *      500 : { ... }
         * }
         */
        JSONObject jsonObj = new JSONObject();
        SpatialUtilFactory spatialUtilFactory = new SpatialUtilFactory();
        try {
            String ident = request.getParameter("ident");
            if(ident == null) {
                throw new NullPointerException("Missing POST parameter 'ident'.");
            }
            
            String jsonData = request.getParameter("syncData");

            if(jsonData == null) {
                throw new NullPointerException("Missing POST parameter 'syncData'.");
            }
            
            User user = userDAO.getUserByRegistrationKey(ident);
            if (user != null) {
                JSONObject status = new JSONObject();
                // The list of json objects that shall be passed back to the 
                // client.
                
                // This should be a list of objects that map the client id 
                // to the new server id.
                SyncResponse syncResponse = new SyncResponse();
                JSONArray clientData = JSONArray.fromString(jsonData);
                SoftValueHashMap attrCache = new SoftValueHashMap();
                for(Object jsonLocationBean : clientData){
                    syncLocation(syncResponse, jsonLocationBean, user, attrCache, spatialUtilFactory);
                }
                
                status.put("sync_result", syncResponse.getResponse());
                jsonObj.put(CLIENT_SYNC_STATUS_KEY, HttpServletResponse.SC_OK);
                jsonObj.put(HttpServletResponse.SC_OK, status);
            } else {
                JSONObject auth = new JSONObject();
                auth.put("message", "Unauthorized");
                
                jsonObj.put(CLIENT_SYNC_STATUS_KEY, HttpServletResponse.SC_UNAUTHORIZED);
                jsonObj.put(HttpServletResponse.SC_UNAUTHORIZED, auth);
            }
        } catch(Throwable e) {
            // Catching throwable is bad but we do not want to cause an 
            // unhandled anything. Ever.
            // The reason is that the cross window communication on the client
            // side won't get triggered and the client will not have any idea
            // what happened.
            
            log.error(e.getMessage(), e);

            requestRollback(request);
            
            JSONObject error = new JSONObject();
            error.put("type", jsonStringEscape(e.getClass().getSimpleName().toString()));
            error.put("message", jsonStringEscape(e.getMessage()));
            
            jsonObj.put(CLIENT_SYNC_STATUS_KEY, HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            jsonObj.put(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, error);
        }

        if (inFrame) {
            ModelAndView mv = new ModelAndView("postMessage");
            mv.addObject("message", jsonObj.toString());
            return mv;
        } else {
            this.writeJson(request, response, jsonObj.toString());
            return null;
        }
    }
    
    private void syncLocation(SyncResponse syncResponse,
                              Object jsonLocationBean, User user, SoftValueHashMap attrCache, 
                              SpatialUtilFactory spatialUtilFactory)
            throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, IOException {
        
        String clientID = getJSONString(jsonLocationBean, "id", null);
        if(clientID == null) {
            throw new NullPointerException();
        }
        
        Integer locationPk = getJSONInteger(jsonLocationBean, "server_id", 0);
        Location loc;
        if(locationPk < 1) {
            loc = locationDAO.getLocationByClientID(clientID);
            if(loc == null) {
                loc = new Location();
            }
        } else {
            loc = locationDAO.getLocation(locationPk);
        }
        
        // Name & Description
        loc.setName(getJSONString(jsonLocationBean, "name", ""));
        loc.setDescription(getJSONString(jsonLocationBean, "description", ""));
        
        // Location Owner (if there is one)
        Integer userPk = getJSONInteger(jsonLocationBean, "user_id", 0);
        User owner = userDAO.getUser(userPk);
        if(owner != null) {
            loc.setUser(owner);
        }
        
        // Geometry
        String locationWKT = getJSONString(jsonLocationBean, "location", null);
        if(locationWKT != null && !locationWKT.isEmpty()) {
            String sridString = getJSONString(jsonLocationBean, "srid", "");
            Integer srid = sridString.trim().isEmpty() ? null : Integer.valueOf(sridString.trim());
            // default to lat/lon if no srid is specified.
        	SpatialUtil spatialUtil = srid != null ? spatialUtilFactory.getLocationUtil(srid) : spatialUtilFactory.getLocationUtil();
            loc.setLocation(spatialUtil.createGeometryFromWKT(locationWKT));
        }
        
        loc = locationDAO.save(loc);
        
        // Attribute Values
        List<Object> locAttrBeanList = (List<Object>) PropertyUtils.getProperty(jsonLocationBean, "attributes");
        for(Object jsonLocAttrValBean : locAttrBeanList) { 
            AttributeValue locAttrVal = syncAttributeValue(syncResponse, jsonLocAttrValBean, attrCache);
            if (locAttrVal != null) {
                loc.getAttributes().add(locAttrVal);
            }
        }
        
        // Save the client ID.
        Metadata md = loc.getMetadataForKey(Metadata.LOCATION_CLIENT_ID_KEY);
        if(md == null) {
            md = new Metadata();
            md.setKey(Metadata.LOCATION_CLIENT_ID_KEY);
        }
        md.setValue(clientID);
        md = metadataDAO.save(md);
        loc.getMetadata().add(md);
        
        // Add the location to the survey if needed
        Integer surveyPk = getJSONInteger(jsonLocationBean, "survey_id", 0);
        Survey survey = surveyDAO.getSurvey(surveyPk);
        if(survey != null) {
            if(!survey.getLocations().contains(loc)) {
                survey.getLocations().add(loc);
                surveyDAO.save(survey);
            }
        }
        
        loc = locationDAO.save(loc);

        syncResponse.add(Location.class, clientID, loc);
    }
    
    @RequestMapping(value = "/webservice/application/clientSync.htm", method = RequestMethod.POST)
    public ModelAndView clientSync(HttpServletRequest request, HttpServletResponse response,
            @RequestParam(value="inFrame", defaultValue="true") boolean inFrame) throws IOException {
        /*
         * { 
         *      status : 200,
         *      200 : { ... } 
         * }
         * 
         * or
         * 
         * {
         *      status : 500,
         *      500 : { ... }
         * }
         */

        JSONObject jsonObj = new JSONObject();
        getRequestContext().getHibernate().setFlushMode(FlushMode.MANUAL);
        SpatialUtilFactory spatialUtilFactory = new SpatialUtilFactory();
        try {
            String ident = request.getParameter("ident");
            if(ident == null) {
                throw new NullPointerException("Missing POST parameter 'ident'.");
            }

            String jsonData = request.getParameter("syncData");

            if(jsonData == null) {
                throw new NullPointerException("Missing POST parameter 'syncData'.");
            }

            User user = authenticate(ident);
            if (user != null) {

                JSONArray recordGroupJsonArray;
                String recordGroupData = request.getParameter("recordGroupData");

                if (recordGroupData != null) {
                    recordGroupJsonArray = JSONArray.fromString(recordGroupData);
                } else {
                    // This is to ensure backwards compatibility with
                    // mobile tools prior to the addition of record groups
                    // 2013-10-15
                    recordGroupJsonArray = new JSONArray();
                }

                JSONObject status = new JSONObject();

                // The list of json objects that shall be passed back to the 
                // client.
                // This should be a list of objects that map the client id 
                // to the new server id.
                SyncResponse syncResponse = new SyncResponse();

                syncRecordGroups(syncResponse, recordGroupJsonArray, user);

                JSONArray clientData = JSONArray.fromString(jsonData);

                SoftValueHashMap attrCache = new SoftValueHashMap();
                SoftValueHashMap recordGroupCache = new SoftValueHashMap();
                for(Object jsonRecordBean : clientData) {
                    syncRecord(syncResponse,
                            jsonRecordBean, user, attrCache, recordGroupCache, spatialUtilFactory);
                }

                status.put("sync_result", syncResponse.getResponse());
                int recordsForUser = recordDAO.countRecords(user);
                status.put(JSON_KEY_SERVER_RECORDS_FOR_USER, recordsForUser);
                jsonObj.put(CLIENT_SYNC_STATUS_KEY, HttpServletResponse.SC_OK);
                jsonObj.put(HttpServletResponse.SC_OK, status);
            } else {
                JSONObject auth = new JSONObject();
                auth.put("message", "Unauthorized");
                
                jsonObj.put(CLIENT_SYNC_STATUS_KEY, HttpServletResponse.SC_UNAUTHORIZED);
                jsonObj.put(HttpServletResponse.SC_UNAUTHORIZED, auth);
            }
            getRequestContext().getHibernate().flush();
        } catch(Throwable e) {
            // Catching throwable is bad but we do not want to cause an 
            // unhandled anything. Ever.
            // The reason is that the cross window communication on the client
            // side won't get triggered and the client will not have any idea
            // what happened.
            
            log.error(e.getMessage(), e);

            requestRollback(request);
            
            JSONObject error = new JSONObject();
            error.put("type", jsonStringEscape(e.getClass().getSimpleName().toString()));
            error.put("message", jsonStringEscape(e.getMessage()));
            
            jsonObj.put(CLIENT_SYNC_STATUS_KEY, HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            jsonObj.put(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, error);
        }

        if (inFrame) {
            ModelAndView mv = new ModelAndView("postMessage");
            mv.addObject("message", jsonObj.toString());
            return mv;
        } else {
            log.debug(jsonObj.toString());
            this.writeJson(request, response, jsonObj.toString());
            return null;
        }
    }

    private void syncRecordGroups(SyncResponse syncResponse, JSONArray recordGroupArray, User user) throws IllegalAccessException, NoSuchMethodException, InvocationTargetException {

        for (int i=0; i<recordGroupArray.size(); ++i) {
            JSONObject recordGroupJson = recordGroupArray.getJSONObject(i);
            int recordGroupId = recordGroupJson.optInt("server_id", 0);
            String recordGroupClientId = recordGroupJson.getString("id");
            RecordGroup group;
            if (recordGroupId != 0) {
                group = recordGroupDAO.getRecordGroup(recordGroupId);
            } else {
                group = recordGroupDAO.getRecordGroupByClientID(recordGroupClientId);
            }

            if (group == null) {
                group = new RecordGroup();
                group = recordGroupDAO.save(group);
            }

            group.setUser(user);
            int surveyId = recordGroupJson.optInt("survey_id", 0);
            group.setSurvey(surveyDAO.getSurvey(surveyId));
            group.setType(recordGroupJson.optString("type", ""));

            // Save the client ID.
            Metadata md = recordGroupDAO.getRecordGroupMetadataForKey(group,
                    Metadata.RECORD_GROUP_CLIENT_ID_KEY);
            md.setValue(recordGroupClientId);
            md = metadataDAO.save(md);
            group.getMetadata().add(md);

            group.setStartDate(getJSONDate(recordGroupJson, "startDate", null));
            group.setEndDate(getJSONDate(recordGroupJson, "endDate", null));

            syncResponse.add(RecordGroup.class, recordGroupClientId, group);
        }

        this.getRequestContext().getHibernate().flush();
    }

    /**
     * Returns the User identified by the supplied ident parameter.
     * The UserDetails is populated in the RequestContext as a side effect.
     * @param ident identifies the User.
     * @return the User identified by ident, or null if no such User exists.
     */
    private User authenticate(String ident) {
        User user = userDAO.getUserByRegistrationKey(ident);
        if (user != null) {
            RequestContext requestContext = RequestContextHolder.getContext();
            requestContext.setUserDetails(new UserDetails(user));
        }
        return user;
    }
    
    @RequestMapping(value = CREATE_SURVEY_URL, method = RequestMethod.POST)
    public void createSurvey(HttpServletRequest request, HttpServletResponse response,
            @RequestParam(value=PARAM_NAME, required=false) String name,
            @RequestParam(value=PARAM_SURVEY_TEMPLATE, required=true) String jsonTemplate) throws IOException {
        
        JSONObject result = new JSONObject();
        
        String ident = request.getParameter("ident");
        if(StringUtils.isEmpty(ident)) {
            result.put(JSON_KEY_SUCCESS, false);
            result.put(JSON_KEY_MESSAGE, "Missing post parameter 'ident'");
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            this.writeJson(response, result.toString());
            return;
        }

        User u = userDAO.getUserByRegistrationKey(ident);
        if (u == null) {
            result.put(JSON_KEY_SUCCESS, false);
            result.put(JSON_KEY_MESSAGE, "Cannot find matching user for ident");
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            this.writeJson(response, result.toString());
            return;
        }
        
        try {
            JSONObject importJson = JSONObject.fromStringToJSONObject(jsonTemplate);
            if (importJson == null) {
                result.put(JSON_KEY_SUCCESS, false);
                result.put(JSON_KEY_MESSAGE, "Parameter survey_template is not a json object");
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                this.writeJson(response, result.toString());
                return;
            }
            
            // replace the name value in the json representation
            if (name != null) {
                JSONObject idToJsonPersistentLookup = importJson.getJSONObject(Survey.class.getSimpleName());
                Object id = idToJsonPersistentLookup.keySet().iterator().next();
                JSONObject surveyPersistent = idToJsonPersistentLookup.getJSONObject(id.toString());
                surveyPersistent.put("name", name);
            }
            
            Survey createdSurvey = null;
            try {
                createdSurvey = surveyImportExportService.importObject(getRequestContext().getHibernate(), importJson);
                // all the following exceptions are thrown when there are problems introspecting the object
            } catch (InvocationTargetException e) {
                result.put(JSON_KEY_SUCCESS, false);
                result.put(JSON_KEY_MESSAGE, "Parameter survey_template does not match expected format");
                // 400
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            } catch (NoSuchMethodException e) {
                result.put(JSON_KEY_SUCCESS, false);
                result.put(JSON_KEY_MESSAGE, "Parameter survey_template does not match expected format");
                // 400
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            } catch (IllegalAccessException e) {
                result.put(JSON_KEY_SUCCESS, false);
                result.put(JSON_KEY_MESSAGE, "Parameter survey_template does not match expected format");
                // 400
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            } catch (InstantiationException e) {
                result.put(JSON_KEY_SUCCESS, false);
                result.put(JSON_KEY_MESSAGE, "Parameter survey_template does not match expected format");
                // 400
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            }
            
            if (createdSurvey != null && createdSurvey.getId() != null) {
                result.put(JSON_KEY_SUCCESS, true);
                result.put(JSON_KEY_ID, createdSurvey.getId());
                JSONObject surveyJson = this.getSurveyNoSpeciesJson(createdSurvey, System.currentTimeMillis());
                result.put(JSON_KEY_SURVEY, surveyJson);
                // 200
                
                // do some special survey settings since we don't port everything over...
                if (!createdSurvey.isPublic()) {
                    Set<User> userSet = new HashSet<User>();
                    userSet.add(u);
                    createdSurvey.setUsers(userSet);
                }
            } else {
                result.put(JSON_KEY_SUCCESS, false);
                result.put(JSON_KEY_MESSAGE, "Error importing survey");
                // 500
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            }
        } catch (JSONException je) {
            result.put(JSON_KEY_SUCCESS, false);
            result.put(JSON_KEY_MESSAGE, "Invalid json when parsing template");
            // 400
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        }
        this.writeJson(response, result.toString());
    }
    
    @RequestMapping(value = "/webservice/application/ping.htm", method = RequestMethod.GET)
    public void ping(HttpServletRequest request, HttpServletResponse response) throws IOException {
        // support for JSONP
        String callback = validateCallback(request.getParameter("callback"));
        if (callback != null) {
            response.setContentType("application/javascript");
            response.getWriter().write(callback
                    + "();");
        }
    }
    
    private void syncRecord(SyncResponse syncResponse,
            Object jsonRecordBean, User user, SoftValueHashMap attrCache,
            SoftValueHashMap recordGroupCache,
    		SpatialUtilFactory spatialUtilFactory)
        throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, IOException {

        String clientID = getJSONString(jsonRecordBean, "id", null);
        if(clientID == null) {
            throw new IllegalArgumentException("Record must have a client id");
        }
        
        Integer recordPk = getJSONInteger(jsonRecordBean, "server_id", 0);
        Record rec;
        if(recordPk < 1) {
            rec = recordDAO.getRecordByClientID(clientID);
        } else {
            rec = recordDAO.getRecord(recordPk);
            if (rec == null) {
                log.error("Sync record: Retrieved null record for record pk = " + recordPk);
                throw new IllegalStateException("Record cannot be null here");
            }
        }

        if(rec == null) {
            rec = new Record();
        }

        int weight = getJSONInteger(jsonRecordBean, "weight", PersistentImpl.DEFAULT_WEIGHT);
        rec.setWeight(weight);

        String latitudeString = getJSONString(jsonRecordBean, "latitude", "");
        Double latitude = latitudeString.trim().isEmpty() ? null : Double.parseDouble(latitudeString);
        String longitudeString = getJSONString(jsonRecordBean, "longitude", "");
        Double longitude = longitudeString.trim().isEmpty() ? null : Double.parseDouble(longitudeString);
        String sridString = getJSONString(jsonRecordBean, "srid", "");
        Integer srid = sridString.trim().isEmpty() ? null : Integer.valueOf(sridString.trim());
        
        if (latitude != null && longitude != null) {
        	// default to lat/lon if no srid specified.
        	SpatialUtil spatialUtil = srid != null ? spatialUtilFactory.getLocationUtil(srid) : spatialUtilFactory.getLocationUtil();
            rec.setPoint(spatialUtil.createPoint(latitude, longitude));
        }

        String accuracyStr = getJSONString(jsonRecordBean, "accuracy", "");
        Double accuracy = accuracyStr.trim().isEmpty() ? null : Double.parseDouble(accuracyStr);
        rec.setAccuracyInMeters(accuracy);

        String gpsAltitudeStr = getJSONString(jsonRecordBean, "gpsAltitude", "");
        Double gpsAltitude = gpsAltitudeStr.trim().isEmpty() ? null : Double.parseDouble(gpsAltitudeStr);
        rec.setGpsAltitude(gpsAltitude);
        
        //set location for record if exists
        Integer locationId = getJSONInteger(jsonRecordBean, "location", null);
        if (locationId != null) {
            Location l = locationDAO.getLocation(locationId.intValue());
            rec.setLocation(l);
        }

        //set other dwc values
        Date when = getJSONDate(jsonRecordBean, "when", null);
        rec.setWhen(when);
        rec.setTime(when == null ? null : when.getTime());
        
        Date lastDate = getJSONDate(jsonRecordBean, "lastDate", null);
        rec.setLastDate(lastDate == null ? when : lastDate);
        
        Long lastTime = null;
        if (lastDate == null) {
            if (when != null) {
                lastTime = when.getTime();
            }
        } else {
            lastTime = lastDate.getTime();
        }
        rec.setLastTime(lastTime);

        String notes = getJSONString(jsonRecordBean, "notes", null);
        rec.setNotes(notes);
        
        Integer number = getJSONInteger(jsonRecordBean, "number", null);
        rec.setNumber(number);
        
        Integer censusMethodPk = getJSONInteger(jsonRecordBean, "censusMethod_id", null);
        if(censusMethodPk != null) {
            rec.setCensusMethod(censusMethodDAO.get(censusMethodPk));
        }

        // This section operates if the id is a server side primary key.
        Integer parentRecordPk = getJSONInteger(jsonRecordBean, "parentRecord_id", null);
        if(parentRecordPk != null) {
            rec.setParentRecord(recordDAO.getRecord(parentRecordPk));
        }

        // This operates on a client side id.
        String clientParentId = getJSONString(jsonRecordBean, "parentId", null);
        if(clientParentId != null) {
            Record p = (Record)syncResponse.getPersistentForClientId(Record.class, clientParentId);
            rec.setParentRecord(p);
        }

        {
            String clientParentAttributeValueId = getJSONString(jsonRecordBean, "parentAttributeValueId", null);
            if(clientParentAttributeValueId != null) {
                AttributeValue av = (AttributeValue)syncResponse.getPersistentForClientId(AttributeValue.class, clientParentAttributeValueId);
                rec.setAttributeValue(av);
            }
        }

        {
            String clientParentAttributeValueSid = getJSONString(jsonRecordBean, "parentAttributeValueSid", null);
            if(clientParentAttributeValueSid != null) {
                AttributeValue av = attributeValueDAO.get(Integer.parseInt(clientParentAttributeValueSid));
                rec.setAttributeValue(av);
            }
        }

        Integer surveyPk = getJSONInteger(jsonRecordBean, "survey_id", null);
        
        String scientificName = getJSONString(jsonRecordBean, "scientificName", null);
        Integer taxonPk = getJSONInteger(jsonRecordBean, JSON_KEY_TAXON_ID, null);
        if(taxonPk != null) {
            IndicatorSpecies taxon = taxaDAO.getIndicatorSpecies(taxonPk);
            if(taxon == null) {
                // Must be a field species
                // Don't create a new field name attribute if one already exists...
                AttributeValue fieldName = AttributeValueUtil.getAttributeValue(taxaService.getFieldNameAttribute(), rec);
                if (fieldName == null) {
                    fieldName = new AttributeValue();
                    fieldName.setAttribute(taxaService.getFieldNameAttribute());
                }
                taxon = taxaService.getFieldSpecies();
                fieldName.setStringValue(scientificName);
                fieldName = attributeValueDAO.save(fieldName);
                rec.getAttributes().add(fieldName);
            }
            rec.setSpecies(taxon);
        }

        rec.setUser(user);
        if(surveyPk != null) {
        	Survey s = surveyDAO.getSurvey(surveyPk);
            if (s != null) {
                rec.setSurvey(s);
                rec.setRecordVisibility(s.getDefaultRecordVisibility());
                rec = recordDAO.saveRecord(rec);
            }
        }

        
        syncResponse.add(Record.class, clientID, rec);

        List<Object> recAttrBeanList = (List<Object>) PropertyUtils.getProperty(jsonRecordBean, "attributeValues");
        for(Object jsonRecAttrBean : recAttrBeanList) {
            AttributeValue recAttr = syncAttributeValue(syncResponse, jsonRecAttrBean, attrCache);
            if (recAttr != null) {
                rec.getAttributes().add(recAttr);
            }
        }

        JSONObject jsonObj = (JSONObject)jsonRecordBean;
        if (jsonObj.has("recordGroup")) {
            JSONObject recordGroupJson = jsonObj.getJSONObject("recordGroup");
            String recordGroupClientId = recordGroupJson.getString("id");
            int recordGroupId = recordGroupJson.optInt("server_id", 0);
            RecordGroup group;
            if (recordGroupId != 0) {
                group = (RecordGroup)recordGroupCache.get(recordGroupId);
                if (group == null) {
                    group = recordGroupDAO.getRecordGroup(recordGroupId);
                }
            } else {
                group = (RecordGroup)recordGroupCache.get(recordGroupClientId);
                if (group == null) {
                    group = recordGroupDAO.getRecordGroupByClientID(recordGroupClientId);
                }
            }
            if (group != null && group.getId() != null) {
                // add to cache
                recordGroupCache.put(group.getId(), group);
                recordGroupCache.put(recordGroupClientId, group);
            }
            rec.setRecordGroup(group);
        }

        // Save the client ID.
        Metadata md = recordDAO.getRecordMetadataForKey(rec, Metadata.RECORD_CLIENT_ID_KEY);
        md.setValue(clientID);
        md = metadataDAO.save(md);
        rec.getMetadata().add(md);

        recordDAO.saveRecord(rec);
    }
    
    private AttributeValue syncAttributeValue(SyncResponse syncResponse, Object jsonRecAttrBean, SoftValueHashMap attrCache)
        throws IllegalAccessException, InvocationTargetException, NoSuchMethodException, IOException {
        String id = getJSONString(jsonRecAttrBean, "id", null);
        if(id == null) {
            throw new NullPointerException();
        }

        Integer attrValPk = getJSONInteger(jsonRecAttrBean, "server_id", 0);
        Integer attrPk = getJSONInteger(jsonRecAttrBean, "attribute_id", null);
        String value = getJSONString(jsonRecAttrBean, "value", "");

        Attribute attr = (Attribute)attrCache.get(attrPk);
        if(attr == null) {
            attr = taxaDAO.getAttribute(attrPk);
            attrCache.put(attrPk, attr);
        }

        // This attribute is still null. We have a situation where
        // the attribute exists on the device but not on the server.
        // We have gotten out of sync somewhere - do not store this
        // incoming data.
        if (attr == null) {
            log.error("Device requested attribute PK " + attrPk + " but it does not exist");
            return null;
        }

        AttributeValue attrVal = attrValPk < 1 ? new AttributeValue() : attributeValueDAO.get(attrValPk);
        if (attrVal == null) {
            attrVal = new AttributeValue();
        }

        attrVal.setAttribute(attr);
        String filename = null;
        String base64 = null;
        switch(attr.getType()) {
            case INTEGER:
            case INTEGER_WITH_RANGE:
            case DECIMAL:
                try {
                    attrVal.setStringValue(value);
                    if(value != null && !value.isEmpty()) {
                        attrVal.setNumericValue(new BigDecimal(value));
                    }
                }
                catch(NumberFormatException nfe){
                    throw new IllegalArgumentException("An invalid decimal value was found: "+value, nfe);
                }
                break;
            
            case DATE:
                Date date = getJSONDate(jsonRecAttrBean, "value", null);
                if(date != null) {
                    attrVal.setDateValue(date);
                    attrVal.setStringValue(dateFormat.format(date));
                } else {
                    attrVal.setDateValue(null);
                    attrVal.setStringValue("");
                }
                break;
            case HTML:
            case HTML_RAW:
            case HTML_NO_VALIDATION:
            case HTML_COMMENT:
            case HTML_HORIZONTAL_RULE:
            case STRING:
            case STRING_AUTOCOMPLETE:
            case TEXT:
            case TIME:
            case STRING_WITH_VALID_VALUES:
            case MULTI_CHECKBOX:
            case MULTI_SELECT:
            case BARCODE:
            case REGEX:
                attrVal.setStringValue(value);
                break;
            case SINGLE_CHECKBOX:
                attrVal.setBooleanValue(value);
                break;
            case IMAGE:
                if(value != null && !value.isEmpty()) {
                    base64 = value;
                    // The mobile only uploads jpeg images.
                    filename = String.format("%s.jpeg",UUID.randomUUID().toString());
                    attrVal.setStringValue(filename);
                } else {
                    filename = null;
                    base64 = null;
                    attrVal.setStringValue("");
                }
                break;
            case AUDIO:
                if(value != null && !value.isEmpty()) {
                    base64 = value;
                    // The mobile only uploads 3gp audio.
                    filename = String.format("%s.3gp", UUID.randomUUID().toString());
                    attrVal.setStringValue(filename);
                } else {
                    filename = null;
                    base64 = null;
                    attrVal.setStringValue("");
                }
                break;
            case VIDEO:
                if(value != null && !value.isEmpty()) {
                    base64 = value;
                    filename = String.format(UUID.randomUUID().toString());
                    attrVal.setStringValue(filename);
                } else {
                    filename = null;
                    base64 = null;
                    attrVal.setStringValue("");
                }
                break;
            case FILE:
            	if(value != null && !value.isEmpty()) {
                    base64 = value;
                    filename = String.format(UUID.randomUUID().toString());
                    attrVal.setStringValue(filename);
                } else {
                    filename = null;
                    base64 = null;
                    attrVal.setStringValue("");
                }
            	break;
            case SPECIES:
            {
                Integer taxonId = getJSONInteger(jsonRecAttrBean, JSON_KEY_TAXON_ID, null);
                if (taxonId != null) {
                        IndicatorSpecies species = taxaDAO.getIndicatorSpecies(taxonId);
                        attrVal.setSpecies(species);
                        if (species != null) {
                                attrVal.setStringValue(value);
                        } else {
                                log.error("could not find species with id : " + taxonId);
                        }
                } else {
                        attrVal.setSpecies(null);
                        attrVal.setStringValue("");
                }
            }
                break;
            case CENSUS_METHOD_ROW:
            case CENSUS_METHOD_COL:
                // census method types should add a record to the attribute value
                break;
            default:
                throw new UnsupportedOperationException("Unsupported Attribute Type: "+attr.getType().toString());
        }
        attrVal = attributeValueDAO.save(attrVal);


        if(filename != null && base64 != null) {
            fileService.createFile(attrVal.getClass(), attrVal.getId(), filename, Base64.decode(base64));
        }
        
        syncResponse.add(AttributeValue.class, id, attrVal);
        return attrVal;
    }
    
    private Integer getJSONInteger(Object bean, String propertyName, Integer defaultValue) throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        Integer ret = defaultValue;
        Object obj = PropertyUtils.getProperty(bean, propertyName);
        
        if(obj != null) {
            try {
                ret = Integer.parseInt(obj.toString(), 10);
            } catch(NumberFormatException nfe) {
                ret = defaultValue;
            }
        }
        
        return ret;
    }
    
    private String getJSONString(Object bean, String propertyName, String defaultValue) throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        Object obj = PropertyUtils.getProperty(bean, propertyName);
        return obj == null ? defaultValue : obj.toString();
    }
    
    private Date getJSONDate(Object bean, String propertyName, Date defaultValue) throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        Date ret = defaultValue;
        Object obj = PropertyUtils.getProperty(bean, propertyName);
        
        if(obj != null) {
            try {
                ret = new Date(Long.parseLong(obj.toString(), 10));
            } catch(NumberFormatException nfe) {
                ret = defaultValue;
            }
        }
        
        return ret;
    }
    
    private String jsonStringEscape(String str) {
        return str == null ? str : str.replaceAll("\"", "\\\\\"");
    }
    
    private void recurseFlattenCensusMethod(JSONArray censusMethodArray, CensusMethod method) {
        // Not using the depth because I do not want all the sub census methods.
        Map<String, Object> flatCensusMethod = method.flatten(0, true, true);
        
        List<Map<String, Object>> attributeList = new ArrayList<Map<String, Object>>();
        for(Attribute attr : method.getAttributes()) {
            attributeList.add(attr.flatten(1, true, true));
            if(attr.getCensusMethod() != null) {
                recurseFlattenCensusMethod(censusMethodArray, attr.getCensusMethod());
            }
        }
        flatCensusMethod.put("attributes", attributeList);

        JSONArray subCensusMethodList = new JSONArray();
        for(CensusMethod subMethod : method.getCensusMethods()) {
            recurseFlattenCensusMethod(subCensusMethodList, subMethod);
        }
        flatCensusMethod.put("censusMethods", subCensusMethodList);
        
        censusMethodArray.add(flatCensusMethod);
    }

    /**
     * Transforms the contained geometry into lat / lon and evicts the location
     * from the session so the stored value in the DB is unchanged.
     * This method should be called after all required lazy loading
     * in calling method is complete. Otherwise you will get a
     * hibernate exception when lazy loading is attempted.
     *
     * @param sesh database session
     * @param l location to flatten
     * @return Map of flattened location
     */
    private Map<String, Object> flattenLocation(Session sesh, Location l) {

        // Reproject if necessary
        if (l.getLocation() != null) {
            l.setLocation(spatialUtil.transform(l.getLocation()));
        }
        // flatten as usual
        Map<String, Object> result = l.flatten(1, true, true);

        // Evict object from session so we can transform the contained geometry
        // evict after potential lazy load
        sesh.evict(l);

        return result;
    }
}
