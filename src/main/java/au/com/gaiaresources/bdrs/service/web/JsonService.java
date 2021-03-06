package au.com.gaiaresources.bdrs.service.web;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.springframework.util.StringUtils;

import au.com.gaiaresources.bdrs.db.impl.PersistentImpl;
import au.com.gaiaresources.bdrs.json.JSONArray;
import au.com.gaiaresources.bdrs.json.JSONObject;
import au.com.gaiaresources.bdrs.model.location.Location;
import au.com.gaiaresources.bdrs.model.map.GeoMapFeature;
import au.com.gaiaresources.bdrs.model.preference.Preference;
import au.com.gaiaresources.bdrs.model.preference.PreferenceDAO;
import au.com.gaiaresources.bdrs.model.record.AccessControlledRecordAdapter;
import au.com.gaiaresources.bdrs.model.record.Record;
import au.com.gaiaresources.bdrs.model.survey.BdrsCoordReferenceSystem;
import au.com.gaiaresources.bdrs.model.taxa.Attribute;
import au.com.gaiaresources.bdrs.model.taxa.AttributeValue;
import au.com.gaiaresources.bdrs.model.taxa.IndicatorSpecies;
import au.com.gaiaresources.bdrs.model.user.User;
import au.com.gaiaresources.bdrs.servlet.BdrsWebConstants;
import au.com.gaiaresources.bdrs.util.SpatialUtil;
import au.com.gaiaresources.bdrs.util.SpatialUtilFactory;

public class JsonService {

    public static final String JSON_KEY_ITEMS = "items";
    public static final String JSON_KEY_TYPE = "type";
    public static final String JSON_KEY_ATTRIBUTES = "attributes";
    public static final String JSON_KEY_ID = "id";
    public static final String JSON_KEY_ATTR_TYPE = "type";
    public static final String JSON_KEY_ATTR_NAME = "name";
    public static final String JSON_KEY_ATTR_VALUE = "value";
    public static final String JSON_KEY_IS_SCINAME = "sciName";

    public static final String JSON_ITEM_TYPE_RECORD = "record";
    public static final String JSON_ITEM_TYPE_MAP_FEATURE = "geoMapFeature";

    public static final String JSON_KEY_SRID = "srid";
    public static final String JSON_KEY_X_NAME = "xname";
    public static final String JSON_KEY_Y_NAME = "yname";
    public static final String JSON_KEY_CRS_DISPLAY_NAME = "name";

    public static final String RECORD_KEY_CENSUS_METHOD = "census_method";
    public static final String RECORD_KEY_NUMBER = "number";
    public static final String RECORD_KEY_NOTES = "notes";
    public static final String RECORD_KEY_SPECIES = "species";
    public static final String RECORD_KEY_COMMON_NAME = "common_name";
    public static final String RECORD_KEY_HABITAT = "habitat";
    public static final String RECORD_KEY_WHEN = "when";
    public static final String RECORD_KEY_BEHAVIOUR = "behaviour";
    public static final String RECORD_KEY_RECORD_ID = BdrsWebConstants.PARAM_RECORD_ID;
    public static final String RECORD_KEY_SURVEY_ID = BdrsWebConstants.PARAM_SURVEY_ID;
    public static final String RECORD_KEY_VISIBILITY = "recordVisibility";
    public static final String RECORD_KEY_X_COORD = "x";
    public static final String RECORD_KEY_Y_COORD = "y";
    public static final String RECORD_KEY_CRS = "coord_ref_system";

    // first + last name of the recording user
    public static final String RECORD_KEY_USER = "owner";
    public static final String RECORD_KEY_USER_ID = "ownerId";
    public static final String RECORD_KEY_USER_FIRST_NAME = "ownerFirstName";
    public static final String RECORD_KEY_USER_LAST_NAME = "ownerLastName";

    public static final String DATE_FORMAT = "dd-MMM-yyyy";
    private SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT);

    private Logger log = Logger.getLogger(getClass());

    private PreferenceDAO prefDAO;

    private String serverURL;

    /**
     * Create a JsonService object
     *
     * @param prefDAO PreferenceDAO
     * @param serverURL The Server URL
     */
    public JsonService(PreferenceDAO prefDAO, String serverURL) {

        if (prefDAO == null) {
            throw new IllegalArgumentException("PreferenceDAO cannot be null");
        }

        if (!StringUtils.hasLength(serverURL)) {
            throw new IllegalArgumentException("String cannot be null or empty");
        }

        this.serverURL = serverURL;
        this.prefDAO = prefDAO;
    }

    /**
     * @param record      - the record to convert to json
     * @return jsonified record
     */
    public JSONObject toJson(AccessControlledRecordAdapter record, SpatialUtilFactory spatialUtilFactory) {
        return toJson(record, spatialUtilFactory, false);
    }

    /**
     * @param record   - the record to convert to json
     * @param serializelazyLoadedValues - Whether to serialize values that require a hibernate lazy load.
     *                 Lazy loading is slow so we want to avoid it when working with large data sets.
     *                 when iterating over large amounts of records.
     * @return jsonified record
     */
    public JSONObject toJson(AccessControlledRecordAdapter record, SpatialUtilFactory spatialUtilFactory, boolean serializelazyLoadedValues) {
        if (record == null) {
            throw new IllegalArgumentException("AccessControlledRecordAdapter, record, cannot be null");
        }

        Map<String, Object> attrMap = new HashMap<String, Object>(16);

        addToAttributeMap(attrMap, RECORD_KEY_USER_ID, record.getUser().getId());

        if (record.getNotes() != null) {
            addToAttributeMap(attrMap, RECORD_KEY_NOTES, record.getNotes());
        }

        addToAttributeMap(attrMap, RECORD_KEY_HABITAT, record.getHabitat());

        if (serializelazyLoadedValues) {
            if (record.getCensusMethod() != null) {
                addToAttributeMap(attrMap, RECORD_KEY_CENSUS_METHOD, record.getCensusMethod().getName());
            } else {
                addToAttributeMap(attrMap, RECORD_KEY_CENSUS_METHOD, "Standard Taxonomic");
            }

            String firstName = record.getUser().getFirstName();
            String lastName = record.getUser().getLastName();
            addToAttributeMap(attrMap, RECORD_KEY_USER, firstName + " " + lastName);
            addToAttributeMap(attrMap, RECORD_KEY_USER_FIRST_NAME, firstName);
            addToAttributeMap(attrMap, RECORD_KEY_USER_LAST_NAME, lastName);

            if (record.getSpecies() != null) {
                addToAttributeMap(attrMap, RECORD_KEY_SPECIES, record.getSpecies().getScientificName());
                addToAttributeMap(attrMap, RECORD_KEY_COMMON_NAME, record.getSpecies().getCommonName());
                addToAttributeMap(attrMap, RECORD_KEY_NUMBER, record.getNumber());
            }

            addToAttributeMap(attrMap, JSON_KEY_ATTRIBUTES, getOrderedAttributes(record.getOrderedAttributes()));
        }

        addToAttributeMap(attrMap, RECORD_KEY_BEHAVIOUR, record.getBehaviour());

        if (record.getWhen() != null) {
            addToAttributeMap(attrMap, RECORD_KEY_WHEN, record.getWhen().getTime());

            String k = String.format(PersistentImpl.FLATTENED_FORMATTED_DATE_TMPL, RECORD_KEY_WHEN);
            SimpleDateFormat formatter = new SimpleDateFormat(PersistentImpl.DATE_FORMAT_PATTERN);
            String v = formatter.format(record.getWhen());
            addToAttributeMap(attrMap, k, v);
        }

        // legacy
        addToAttributeMap(attrMap, RECORD_KEY_RECORD_ID, record.getId());
        addToAttributeMap(attrMap, RECORD_KEY_SURVEY_ID, record.getSurvey().getId());

        // This is important, always include this stuff
        addToAttributeMap(attrMap, JSON_KEY_ID, record.getId());
        addToAttributeMap(attrMap, JSON_KEY_TYPE, JSON_ITEM_TYPE_RECORD);
        addToAttributeMap(attrMap, RECORD_KEY_VISIBILITY, record.getRecordVisibility());


        if (record.getGeometry() != null) {
            SpatialUtil spatialUtil = spatialUtilFactory.getLocationUtil(record.getGeometry().getSRID());
            BdrsCoordReferenceSystem crs = BdrsCoordReferenceSystem.getBySRID(record.getGeometry().getSRID());
            addToAttributeMap(attrMap, RECORD_KEY_CRS, toJson(crs));
            addToAttributeMap(attrMap, RECORD_KEY_X_COORD, spatialUtil.truncate(record.getLongitude()));
            addToAttributeMap(attrMap, RECORD_KEY_Y_COORD, spatialUtil.truncate(record.getLatitude()));
        }

        return JSONObject.fromMapToJSONObject(attrMap);
    }

    public JSONObject toJson(BdrsCoordReferenceSystem crs) {
        JSONObject obj = new JSONObject();
        obj.accumulate(JSON_KEY_SRID, crs.getSrid());
        obj.accumulate(JSON_KEY_X_NAME, crs.getXname());
        obj.accumulate(JSON_KEY_Y_NAME, crs.getYname());
        obj.accumulate(JSON_KEY_CRS_DISPLAY_NAME, crs.getDisplayName());
        return obj;
    }

    /**
     * Serialize a geo map feature, no attributes.
     *
     * @param feature geo map feature to serialize
     * @return JSONObject
     */
    public JSONObject toJson(GeoMapFeature feature) {
        return toJson(feature, false);
    }

    /**
     * Serialize a geo map feature
     *
     * @param feature             feature to serialize
     * @param serializeAttributes true to include attributes (slow db access over large data sets)
     * @return JSONObject
     */
    public JSONObject toJson(GeoMapFeature feature, boolean serializeAttributes) {
        Map<String, Object> attrMap = new HashMap<String, Object>(3);
        attrMap.put(JSON_KEY_ID, feature.getId());
        attrMap.put(JSON_KEY_TYPE, JSON_ITEM_TYPE_MAP_FEATURE);
        // it's ok to use an empty context path here since GeoMapFeatures cannot have file attributes
        // which is the only type that requires the portalContextPath to create the download link
        if (serializeAttributes) {
            attrMap.put(JSON_KEY_ATTRIBUTES, getOrderedAttributes(feature.getOrderedAttributes()));
        }
        return JSONObject.fromMapToJSONObject(attrMap);
    }

    private void addToAttributeMap(Map<String, Object> attrMap, String key, Object value) {
        if (attrMap.containsKey(key)) {
            log.warn("overwriting attribute map key : " + key);
        }
        if (value != null) {
            attrMap.put(key, value);
        }
    }

    private JSONArray getOrderedAttributes(List<AttributeValue> attributeValues) {
        JSONArray array = new JSONArray();
        for (AttributeValue av : attributeValues) {
            array.add(toJson(av));
        }
        return array;
    }

    private JSONObject toJson(AttributeValue av) {
        Preference sciNamePref = prefDAO.getPreferenceByKey(Preference.SHOW_SCIENTIFIC_NAME_KEY);
        // default to true.
        Boolean showSciName = sciNamePref != null ? Boolean.valueOf(sciNamePref.getValue()) : true;

        Attribute attr = av.getAttribute();
        JSONObject obj = new JSONObject();
        String key = StringUtils.hasLength(attr.getDescription()) ? attr.getDescription() : attr.getName();
        obj.accumulate(JSON_KEY_ATTR_TYPE, attr.getTypeCode());
        obj.accumulate(JSON_KEY_ATTR_NAME, key);
        switch (attr.getType()) {
            case INTEGER:
            case INTEGER_WITH_RANGE:
            case DECIMAL:
                obj.accumulate(JSON_KEY_ATTR_VALUE, av.getNumericValue());
                break;
            case DATE:
                Date d = av.getDateValue();
                String format = d == null ? null : dateFormat.format(av.getDateValue());
                obj.accumulate(JSON_KEY_ATTR_VALUE, format);
                break;
            case HTML:
            case HTML_NO_VALIDATION:
            case HTML_COMMENT:
            case HTML_HORIZONTAL_RULE:
                // ignore html attributes because they do not have attribute values
                break;
            case STRING:
            case STRING_AUTOCOMPLETE:
            case TEXT:
            case STRING_WITH_VALID_VALUES:
                obj.accumulate(JSON_KEY_ATTR_VALUE, av.getStringValue());
                break;
            // allow download of files and image attribute types
            case IMAGE:
            case AUDIO:
            case VIDEO:
            case FILE:
                obj.accumulate(JSON_KEY_ATTR_VALUE, getAttributeValueFileDownloadLink(av));
                break;
            case SPECIES: {
                IndicatorSpecies species = av.getSpecies();
                obj.accumulate(JSON_KEY_IS_SCINAME, showSciName);
                if (species != null) {
                    obj.accumulate(JSON_KEY_ATTR_VALUE, showSciName.equals(Boolean.TRUE) ?
                            species.getScientificName() : species.getCommonName());
                }
            }
            break;
            case CENSUS_METHOD_ROW:
            case CENSUS_METHOD_COL:
                Set<Record> records = av.getRecords();
                if (records != null) {
                    JSONObject recObj = new JSONObject();
                    for (Record record : records) {
                        JSONObject attObj = new JSONObject();
                        for (AttributeValue recordValue : record.getAttributes()) {
                            attObj.accumulate(JSON_KEY_ATTR_VALUE, toJson(recordValue));
                        }
                        recObj.accumulate(JSON_ITEM_TYPE_RECORD, attObj);
                    }
                    obj.accumulate(JSON_KEY_ATTR_VALUE, recObj);
                }
                break;
            case TIME:
            case MULTI_CHECKBOX:
            case MULTI_SELECT:
                obj.accumulate(JSON_KEY_ATTR_VALUE, av.getStringValue());
                break;
            default:
                // ignore
        }
        return obj;
    }

    private String getAttributeValueFileDownloadLink(AttributeValue av) {
        StringBuilder sb = new StringBuilder();
        sb.append("<a href=\"");
        sb.append(serverURL);
        sb.append("/files/download.htm?className=au.com.gaiaresources.bdrs.model.taxa.AttributeValue&id=");
        sb.append(av.getId().toString());
        sb.append("&fileName=");
        sb.append(av.getStringValue());
        sb.append("\">Download file</a>");
        return sb.toString();
    }

    /**
     * Returns a JSON representation of a location.  (Used for writing kml records)
     *
     * @param location    the location to jsonify
     * @return A JSONObject representing the location.
     */
    public JSONObject toJson(Location location, SpatialUtilFactory spatialUtilFactory) {
        Map<String, Object> attrMap = new HashMap<String, Object>(16);
        addToAttributeMap(attrMap, "name", location.getName());
        addToAttributeMap(attrMap, "description", location.getDescription());

        User owner = location.getUser();
        if (owner != null) {
            addToAttributeMap(attrMap, RECORD_KEY_USER, owner.getFirstName() + " " + owner.getLastName());
            addToAttributeMap(attrMap, RECORD_KEY_USER_ID, owner.getId());
        } else {
            // use the creator if the owner is null? createdBy only returns id, must retrieve entire user
        }

        if (location.getCreatedAt() != null) {
            addToAttributeMap(attrMap, RECORD_KEY_WHEN, location.getCreatedAt().getTime());
        }

        if (location.getLocation() != null) {
            int srid = location.getLocation().getSRID();
            SpatialUtil spatialUtil = spatialUtilFactory.getLocationUtil(srid);
            BdrsCoordReferenceSystem crs = BdrsCoordReferenceSystem.getBySRID(srid);
            addToAttributeMap(attrMap, RECORD_KEY_CRS, toJson(crs));
            addToAttributeMap(attrMap, RECORD_KEY_X_COORD, spatialUtil.truncate(location.getLongitude()));
            addToAttributeMap(attrMap, RECORD_KEY_Y_COORD, spatialUtil.truncate(location.getLatitude()));
        }

        attrMap.put(JSON_KEY_ATTRIBUTES, getOrderedAttributes(location.getOrderedAttributes()));

        return JSONObject.fromMapToJSONObject(attrMap);
    }
}
