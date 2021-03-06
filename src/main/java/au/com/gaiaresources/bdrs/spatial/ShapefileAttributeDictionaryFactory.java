package au.com.gaiaresources.bdrs.spatial;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.springframework.util.StringUtils;

import au.com.gaiaresources.bdrs.attribute.AbstractAttributeDictionaryFactory;
import au.com.gaiaresources.bdrs.model.location.Location;
import au.com.gaiaresources.bdrs.model.method.CensusMethod;
import au.com.gaiaresources.bdrs.model.record.Record;
import au.com.gaiaresources.bdrs.model.survey.Survey;
import au.com.gaiaresources.bdrs.model.taxa.Attribute;
import au.com.gaiaresources.bdrs.model.taxa.AttributeScope;
import au.com.gaiaresources.bdrs.model.taxa.TaxonGroup;

public class ShapefileAttributeDictionaryFactory extends AbstractAttributeDictionaryFactory {

    public ShapefileAttributeDictionaryFactory() {
        super();
    }

    private Logger log = Logger.getLogger(getClass()); 
    
    /**
     * upper limit on attribute length in a shapefile
     */
    private static final int MAX_KEY_LENGTH = 10;
    
    private ShapefileRecordKeyLookup klu = new ShapefileRecordKeyLookup();

    
    /**
     * Shape files cannot upload files
     * 
     */
    @Override
    public Map<Attribute, Object> createFileKeyDictionary(Record record, Survey survey, Location location, 
            TaxonGroup taxonGroup, CensusMethod censusMethod, Set<AttributeScope> scope, Map<String, String[]> dataMap) {
        return Collections.emptyMap();
    }

    @Override
    public Map<Attribute, Object> createNameKeyDictionary(Record record, Survey survey, Location location, 
            TaxonGroup taxonGroup, CensusMethod censusMethod, Set<AttributeScope> scope, Map<String, String[]> dataMap) {
        
        // surveys are mandatory, census methods are not.
        if (survey == null) {
            throw new IllegalArgumentException("Survey, survey, cannot be null");
        }
        
        List<Survey> surveyList = new LinkedList<Survey>();
        surveyList.add(survey);
        List<CensusMethod> cmList = new LinkedList<CensusMethod>();
        
        // protected from adding null object to list
        if (censusMethod != null) {
            cmList.add(censusMethod);
        }
        return createNameKeyDictionary(surveyList, taxonGroup, cmList);
    }

    protected void addKey(Map<Attribute, Object> map, Set<String> existingKeys, String baseKey, Attribute attribute, String attributeSource) {
        if (!StringUtils.hasLength(baseKey)) {
            // Ignore the attribute if it does not have a meaningful baseKey
            return;
        }
        // prefix with underscore if key starts with a number
        if (Character.isDigit(baseKey.charAt(0))) {
            baseKey = "_" + baseKey;
        }
        // Trim key to max length...
        baseKey = baseKey.length() > MAX_KEY_LENGTH ? baseKey.substring(0, MAX_KEY_LENGTH) : baseKey;
        baseKey = baseKey.trim();
        // keep on renaming the key until we can add it to our set...
        
        String modifiedKey = baseKey;
        int addKeyAttempt = 0;
        
        while (!existingKeys.add(modifiedKey)) {
            
            ++addKeyAttempt;
            String suffix = String.format("%d", addKeyAttempt);
            if (baseKey.length() + suffix.length() <= MAX_KEY_LENGTH) {
                modifiedKey = baseKey.concat(suffix);
            } else {
                modifiedKey = baseKey.substring(0, MAX_KEY_LENGTH - suffix.length());
                modifiedKey = modifiedKey.concat(suffix);
            }
        }
        
        map.put(attribute, modifiedKey);
    }

    @Override
    public Map<Attribute, Object> createFileKeyDictionary(List<Survey> surveyList,
            TaxonGroup taxonGroup, List<CensusMethod> censusMethodList) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Map<Attribute, Object> createNameKeyDictionary(List<Survey> surveyList,
            TaxonGroup taxonGroup, List<CensusMethod> censusMethodList) {
        if (surveyList == null) {
            throw new IllegalArgumentException("survey cannot be null");
        }
        if (censusMethodList == null) {
            censusMethodList = Collections.emptyList();
        }
        
        // Use linked hash map to preserve order
        Map<Attribute, Object> result = new LinkedHashMap<Attribute, Object>();
        Set<String> check = new HashSet<String>();
        check.add(klu.getSpeciesIdKey());
        check.add(klu.getSpeciesNameKey());
        check.add(klu.getIndividualCountKey());
        check.add(klu.getDateKey());
        check.add(klu.getTimeKey());
        check.add(klu.getNotesKey());
        
        for (Survey survey : surveyList) {
            if (survey.getAttributes() != null) {
                for (Attribute attribute : survey.getAttributes()) {
                    if(getDictionaryAttributeScope().contains(attribute.getScope())) {
                        String paramKey = attribute.getName();
                        addKey(result, check, paramKey, attribute, "Survey");
                    }
                }
            }
        }
        
        // note that we don't deal with taxon group attributes in shape files!
        
        for (CensusMethod censusMethod : censusMethodList) {
            if (censusMethod != null && censusMethod.getAttributes() != null) {
                for (Attribute attribute : censusMethod.getAttributes()) {
                    String paramKey = attribute.getName();
                    addKey(result, check, paramKey, attribute, "Census method");
                }
            }
        }
        
        return result;
    }

    public Map<Attribute, Object> createNameKeyDictionary(Survey survey,
            TaxonGroup taxon, CensusMethod cm) {
        Set<AttributeScope> scope = new HashSet<AttributeScope>();
        scope.add(AttributeScope.SURVEY);
        scope.add(AttributeScope.RECORD);
        return createNameKeyDictionary(null, survey, null, taxon, cm, scope, Collections.EMPTY_MAP);
    }

    @Override
    public Set<AttributeScope> getDictionaryAttributeScope() {
        return SCOPE_RECORD_SURVEY;
    }
}
