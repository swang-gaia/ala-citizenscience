package au.com.gaiaresources.bdrs.model.taxa.impl;

import au.com.gaiaresources.bdrs.db.QueryOperation;
import au.com.gaiaresources.bdrs.db.ScrollableResults;
import au.com.gaiaresources.bdrs.db.impl.AbstractDAOImpl;
import au.com.gaiaresources.bdrs.db.impl.HqlQuery;
import au.com.gaiaresources.bdrs.db.impl.PagedQueryResult;
import au.com.gaiaresources.bdrs.db.impl.PaginationFilter;
import au.com.gaiaresources.bdrs.db.impl.PersistentImpl;
import au.com.gaiaresources.bdrs.db.impl.Predicate;
import au.com.gaiaresources.bdrs.db.impl.QueryPaginator;
import au.com.gaiaresources.bdrs.db.impl.ScrollableResultsImpl;
import au.com.gaiaresources.bdrs.db.impl.SortOrder;
import au.com.gaiaresources.bdrs.model.index.IndexingConstants;
import au.com.gaiaresources.bdrs.model.metadata.Metadata;
import au.com.gaiaresources.bdrs.model.region.Region;
import au.com.gaiaresources.bdrs.model.survey.Survey;
import au.com.gaiaresources.bdrs.model.taxa.*;
import au.com.gaiaresources.bdrs.search.SearchService;
import au.com.gaiaresources.bdrs.service.db.DeleteCascadeHandler;
import au.com.gaiaresources.bdrs.service.db.DeletionService;
import au.com.gaiaresources.bdrs.servlet.RequestContextHolder;
import au.com.gaiaresources.bdrs.util.Pair;
import au.com.gaiaresources.bdrs.util.StringUtils;
import org.apache.log4j.Logger;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.PerFieldAnalyzerWrapper;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.hibernate.Hibernate;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.StaleStateException;
import org.hibernate.search.Search;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 
 * @author Tim Carpenter
 * 
 */
@SuppressWarnings("unchecked")
@Repository
public class TaxaDAOImpl extends AbstractDAOImpl implements TaxaDAO {
    
    // Leaving this here incase we want to turn on left join fetch for the hibernate query.
    // Note this does slow down the query quite alot.
    //private static final String LEFT_JOIN_FETCH = " left join fetch sp.taxonGroup left join fetch sp.attributes left join fetch sp.infoItems";
    
    // Use a minimal left fetch join.
    private static final String LEFT_JOIN_FETCH = " left join fetch sp.taxonGroup";
    /**
     * Partial query string
     */
    private static final String GET_SPECIES_BASE_QUERY = " from IndicatorSpecies sp" + LEFT_JOIN_FETCH;
    /**
     * Partial query string
     */
    private static final String GET_SPECIES_BY_SURVEY_BASE_QUERY = 
            " from Survey surv join surv.species sp " + LEFT_JOIN_FETCH + " where surv = :survey";
    /**
     * Partial query string
     */
    private static final String GET_SPECIES_EXCLUDE_SURVEY_CLAUSE = 
            " sp.id not in (select survey_sp.id from Survey s2 join s2.species survey_sp where s2 in (:notIds))";
    
    /**
     * Partial query string
     */
    private static final String ORDER_SPECIES_BY_ID = " order by sp.id";
    

    private Logger log = Logger.getLogger(getClass());
    
    @Autowired
    private DeletionService delService;
    
    @Autowired
    private SearchService searchService;
    
    @Autowired
    private TaxaService taxaService;
    
    @PostConstruct
    public void init() throws Exception {
        delService.registerDeleteCascadeHandler(TaxonGroup.class, new DeleteCascadeHandler() {
            @Override
            public void deleteCascade(PersistentImpl instance) {
                delete((TaxonGroup)instance);
            }
        });
        delService.registerDeleteCascadeHandler(IndicatorSpecies.class, new DeleteCascadeHandler() {
            @Override
            public void deleteCascade(PersistentImpl instance) {
                delete((IndicatorSpecies)instance);
            }
        });
        delService.registerDeleteCascadeHandler(AttributeValue.class, new DeleteCascadeHandler() {
            @Override
            public void deleteCascade(PersistentImpl instance) {
                delete((AttributeValue)instance);
            }
        });
    }
    
    @Override
    public TaxonGroup createTaxonGroup(String name, boolean includeBehaviour,
            boolean includeFirstAppearance, boolean includeLastAppearance,
            boolean includeHabitat, boolean includeWeather,
            boolean includeNumber) {
        TaxonGroup tg = new TaxonGroup();
        tg.setName(name);
        tg.setBehaviourIncluded(includeBehaviour);
        tg.setFirstAppearanceIncluded(includeFirstAppearance);
        tg.setLastAppearanceIncluded(includeLastAppearance);
        tg.setHabitatIncluded(includeHabitat);
        tg.setWeatherIncluded(includeWeather);
        tg.setNumberIncluded(includeNumber);
        return save(tg);
    }

    @Override
    public TaxonGroup createTaxonGroup(String name, boolean includeBehaviour,
            boolean includeFirstAppearance, boolean includeLastAppearance,
            boolean includeHabitat, boolean includeWeather,
            boolean includeNumber, String image, String thumbNail) {
        TaxonGroup tg = new TaxonGroup();
        tg.setName(name);
        tg.setBehaviourIncluded(includeBehaviour);
        tg.setFirstAppearanceIncluded(includeFirstAppearance);
        tg.setLastAppearanceIncluded(includeLastAppearance);
        tg.setHabitatIncluded(includeHabitat);
        tg.setWeatherIncluded(includeWeather);
        tg.setNumberIncluded(includeNumber);
        tg.setImage(image);
        tg.setThumbNail(thumbNail);
        return save(tg);
    }

    @Override
    public TaxonGroup updateTaxonGroup(Integer id, String name,
            boolean includeBehaviour, boolean includeFirstAppearance,
            boolean includeLastAppearance, boolean includeHabitat,
            boolean includeWeather, boolean includeNumber) {
        
        TaxonGroup tg = getTaxonGroup(id);
        tg.setName(name);
        tg.setBehaviourIncluded(includeBehaviour);
        tg.setFirstAppearanceIncluded(includeFirstAppearance);
        tg.setLastAppearanceIncluded(includeLastAppearance);
        tg.setHabitatIncluded(includeHabitat);
        tg.setWeatherIncluded(includeWeather);
        tg.setNumberIncluded(includeNumber);
        return update(tg);
    }

    @Override
    public TaxonGroup updateTaxonGroup(Integer id, String name,
            boolean includeBehaviour, boolean includeFirstAppearance,
            boolean includeLastAppearance, boolean includeHabitat,
            boolean includeWeather, boolean includeNumber, String image,
            String thumbNail) {
        TaxonGroup tg = getTaxonGroup(id);
        tg.setName(name);
        tg.setBehaviourIncluded(includeBehaviour);
        tg.setFirstAppearanceIncluded(includeFirstAppearance);
        tg.setLastAppearanceIncluded(includeLastAppearance);
        tg.setHabitatIncluded(includeHabitat);
        tg.setWeatherIncluded(includeWeather);
        tg.setNumberIncluded(includeNumber);
        tg.setImage(image);
        tg.setThumbNail(thumbNail);
        return update(tg);
    }

    public List<TaxonGroup> getTaxonGroup(Survey survey) {
        if (survey.getSpecies().size() == 0) {
            return getTaxonGroups();
        } else {

            return find("select distinct g from IndicatorSpecies i join i.taxonGroup g where i in (select elements(b.species) from Survey b where b = ?)", survey);
        }
    }

    /**
     * Gets all primary AND secondary groups for a survey
     * @param survey survey to search in
     * @return list of unique taxon groups
     */
    public List<TaxonGroup> getAllTaxonGroups(Survey survey) {
        if (survey.getSpecies().size() == 0) {
            return getTaxonGroups();
        } else {
            // Do a union in code....
            List<TaxonGroup> primaryGroups = find("select distinct g from IndicatorSpecies i " +
                    "join i.taxonGroup g where i in (select elements(b.species) " +
                    "from Survey b where b = ?)", survey);

            List<TaxonGroup> secondaryGroups = find("select distinct g from IndicatorSpecies i " +
                    "join i.secondaryGroups g where i in (select elements(b.species) " +
                    "from Survey b where b = ?)", survey);

            Set<TaxonGroup> tgSet = new HashSet<TaxonGroup>(primaryGroups.size() + secondaryGroups.size());

            for (TaxonGroup tg : primaryGroups) {
                tgSet.add(tg);
            }
            for (TaxonGroup tg : secondaryGroups) {
                tgSet.add(tg);
            }

            return new ArrayList<TaxonGroup>(tgSet);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public TaxonGroup getTaxonGroup(String name) {
        return newQueryCriteria(TaxonGroup.class).add("name",
                QueryOperation.EQUAL, name).runAndGetFirst();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public TaxonGroup getTaxonGroup(Session sesh, String name) {
        List<TaxonGroup> groups = this.find(sesh,
                "from TaxonGroup g where name = ?", name);
        if (groups.isEmpty()) {
            return null;
        } else {
            if (groups.size() > 1) {
                log.warn("Multiple TaxonGroups matched. Returning the first.");
            }
            return (TaxonGroup) groups.get(0);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<TaxonGroup> getTaxonGroupSearch(String nameFragment) {
        return this.find("from TaxonGroup g where UPPER(name) like UPPER('%"
                + nameFragment + "%')");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public TaxonGroup getTaxonGroup(Integer id) {
        return getByID(TaxonGroup.class, id);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<TaxonGroup> getTaxonGroups() {
        TaxonGroup fieldNameGroup = taxaService.getFieldNameGroup();
        return find("from TaxonGroup g where g.id != ? order by g.id", fieldNameGroup.getId());
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public List<TaxonGroup> getTaxonGroupsSortedByName() {
        TaxonGroup fieldNameGroup = taxaService.getFieldNameGroup();
        return find("from TaxonGroup g where g.id != ? order by g.name", fieldNameGroup.getId()); 
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Attribute createAttribute(TaxonGroup group, String name,
            AttributeType type, boolean required) {
        Attribute attribute = new Attribute();

        attribute.setName(name);
        attribute.setTypeCode(type.getCode());
        attribute.setRequired(required);
        Attribute att = save(attribute);
        group.getAttributes().add(att);
        save(group);
        return att;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Attribute createAttribute(TaxonGroup group, String name,
            AttributeType type, boolean required, boolean isTag) {
        return createAttribute(group, name, null, type, required, isTag);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Attribute createAttribute(TaxonGroup group, String name,
            String description, AttributeType type, boolean required,
            boolean isTag) {
        Attribute attribute = new Attribute();

        attribute.setName(name);
        attribute.setDescription(description);
        attribute.setTypeCode(type.getCode());
        attribute.setRequired(required);
        attribute.setTag(isTag);
        Attribute att = save(attribute);
        group.getAttributes().add(att);
        save(group);
        return att;
    }

    @Override
    public Attribute save(Attribute attribute) {
        return super.save(attribute);
    }

    @Override
    public TaxonGroup save(TaxonGroup taxongroup) {
        return super.save(taxongroup);
    }

    @Override
    public TaxonGroup save(Session sesh, TaxonGroup taxongroup) {
        return super.save(sesh, taxongroup);
    }

    @Override
    public IndicatorSpecies save(IndicatorSpecies taxon) {
        return super.save(taxon);
    }

    @Override
    public IndicatorSpecies save(Session sesh, IndicatorSpecies taxon) {
        return super.save(sesh, taxon);
    }

    @Override
    public AttributeOption save(AttributeOption opt) {
        return super.save(opt);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Attribute updateAttribute(Integer id, String name,
            AttributeType type, boolean required) {
        Attribute att = getByID(Attribute.class, id);
        att.setName(name);
        att.setTypeCode(type.getCode());
        att.setRequired(required);
        return update(att);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Attribute updateAttribute(Integer id, String name,
            String description, AttributeType type, boolean required) {
        Attribute att = getByID(Attribute.class, id);
        att.setName(name);
        att.setDescription(description);
        att.setTypeCode(type.getCode());
        att.setRequired(required);
        return update(att);
    }

    /**
     * {@inheritDoc}
     */
    public AttributeOption createAttributeOption(Attribute attribute,
            String option) {
        AttributeOption optionImpl = new AttributeOption();
        optionImpl.setValue(option);
        AttributeOption opt = save(optionImpl);
        attribute.getOptions().add(opt);
        save(attribute);
        return opt;
    }

    public TypedAttributeValue createAttributeValue(
            IndicatorSpecies species, Attribute attr, String value) {
        return createAttributeValue(species, attr, value, null);
    }

    public TypedAttributeValue createAttributeValue(
            IndicatorSpecies species, Attribute attr, String value, String desc) {
        AttributeValue impl = new AttributeValue();
        impl.setDescription(desc);
        impl.setAttribute(attr);
        impl.setStringValue(value);
        if (attr == null) {
            log.debug("Indicator species: " + species.getScientificName()
                    + "attribute " + value + " null.");
            return impl;
        }
        return save(impl);
    }

    /**
     * {@inheritDoc}
     */
    public void deleteTaxonGroupAttributeOption(Integer id) {
        delete(getOption(id));
    }

    public AttributeOption getOption(Integer id) {
        return getByID(AttributeOption.class, id);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Attribute getAttribute(Integer id) {
        return getByID(Attribute.class, id);
    }

    @Override
    public Attribute getAttribute(TaxonGroup taxonGroup, String name,
            boolean isTag) {
        Object[] args = { taxonGroup, name, (Boolean) isTag };
        String hql = "select attribute from TaxonGroup tg join tg.attributes attribute where tg = ? and attribute.name = ? and attribute.tag = ?";
        List attributes = find(hql, args);
        if (attributes.size() > 0) {
            return (Attribute) attributes.get(0);
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public IndicatorSpecies createIndicatorSpecies(String scientificName,
            String commonName, TaxonGroup taxonGroup,
            Collection<Region> regions, List<SpeciesProfile> infoItems) {
        IndicatorSpecies species = new IndicatorSpecies();
        species.setCommonName(commonName);
        species.setScientificName(scientificName);
        species.setTaxonGroup(taxonGroup);
        Set<Region> regionSet = new HashSet<Region>();
        for (Region r : regions) {
            regionSet.add(r);
        }
        species.setRegions(regionSet);
        if (infoItems != null)
            species.setInfoItems((List<SpeciesProfile>) infoItems);
        return save(species);
    }

    /**
     * {@inheritDoc}
     */
    public IndicatorSpecies updateIndicatorSpecies(Integer id,
            String scientificName, String commonName, TaxonGroup taxonGroup,
            Collection<Region> regions, List<SpeciesProfile> infoItems) {
        IndicatorSpecies species = getIndicatorSpecies(id);
        species.setCommonName(commonName);
        species.setScientificName(scientificName);
        species.setTaxonGroup(taxonGroup);
        Set<Region> regionSet = new HashSet<Region>();
        for (Region r : regions) {
            regionSet.add(r);
        }
        species.setRegions(regionSet);

        if (infoItems != null) {
            for (SpeciesProfile oldProfile : species.getInfoItems()) {
                delete(oldProfile);
            }
            species.setInfoItems((List<SpeciesProfile>) infoItems);
        }

        return update(species);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IndicatorSpecies updateIndicatorSpecies(Integer id,
            String scientificName, String commonName, TaxonGroup taxonGroup,
            Collection<Region> regions, List<SpeciesProfile> infoItems,
            Set<AttributeValue> attributes) {
        IndicatorSpecies species = getIndicatorSpecies(id);
        species.setCommonName(commonName);
        species.setScientificName(scientificName);
        species.setTaxonGroup(taxonGroup);
        Set<Region> regionSet = new HashSet<Region>();
        for (Region r : regions) {
            regionSet.add(r);
        }
        species.setRegions(regionSet);

        if (infoItems != null) {
            for (SpeciesProfile oldProfile : species.getInfoItems()) {
                delete(oldProfile);
            }
            species.setInfoItems((List<SpeciesProfile>) infoItems);
        }
        species.setAttributes(attributes);

        return update(species);
    }

    @Override
    public IndicatorSpecies updateIndicatorSpecies(IndicatorSpecies species) {
        return save(species);
    }

    @Override
    public List<IndicatorSpecies> getIndicatorSpecies() {
        return find("from IndicatorSpecies sp where sp.scientificName != ?", IndicatorSpecies.FIELD_SPECIES_NAME);
    }

    @Override
    public List<IndicatorSpecies> getIndicatorSpecies(Session sess) {
    	IndicatorSpecies fieldSpecies = taxaService.getFieldSpecies(sess);
        return find(sess, "from IndicatorSpecies sp where sp != ?", fieldSpecies);
    }
    
    @Override
    public List<IndicatorSpecies> getIndicatorSpeciesById(Integer[] pks) {
        Query q = getSession().createQuery(
                "from IndicatorSpecies s where s.id in (:pks)");
        q.setParameterList("pks", pks);
        return q.list();
    }

     @Override
     public IndicatorSpecies getIndicatorSpeciesBySourceDataID(Session sesh,
                     String sourceDataId) {
         
         String query = "select s from IndicatorSpecies s where s.sourceId = :sourceId";
         
         Query q;
         if (sesh == null) {
             q = getSession().createQuery(query);
         } else {
                 q = sesh.createQuery(query);
         }
         q.setParameter("sourceId", sourceDataId);
         List<IndicatorSpecies> taxonList = q.list();
         if (taxonList.isEmpty()) {
             return null;
         } else {
             if (taxonList.size() > 1) {
                 log.warn("More than one IndicatorSpecies returned for the provided Source Data ID: "
                         + sourceDataId + " Returning the first");
             }
             return taxonList.get(0);
         }
     }
	
	@Override
    public List<IndicatorSpecies> getIndicatorSpeciesBySourceDataID(Session sesh, String source, List<String> sourceDataIdList) {
        if(source == null) {
            throw new IllegalArgumentException("Source ID cannot be null");
        }

        if(sourceDataIdList == null || sourceDataIdList.isEmpty()) {
            return Collections.emptyList();
        }

        String query = "select s from IndicatorSpecies s where s.source = :source and s.sourceId in (:sourceIdList)";

        Query q;
        if (sesh == null) {
            q = getSession().createQuery(query);
        } else {
            q = sesh.createQuery(query);
        }

        q.setParameter("source", source);
        q.setParameterList("sourceIdList", sourceDataIdList);
        return q.list();
    }

	@Override
	public IndicatorSpecies getIndicatorSpeciesBySourceDataID(Session sesh, String source, String sourceDataId) {
	    String query = "select s from IndicatorSpecies s where s.source = :source and s.sourceId = :sourceId";
            Query q;
            if (sesh == null) {
                q = getSession().createQuery(query);
            } else {
                q = sesh.createQuery(query);
            }
            
            q.setParameter("sourceId", sourceDataId);
            q.setParameter("source", source);
            return (IndicatorSpecies)q.uniqueResult();
	}

    @Override
    public List<IndicatorSpecies> getIndicatorSpeciesBySpeciesProfileItem(
            String type, String content) {
        String[] args = { type, content };
        String hql = "select s from IndicatorSpecies s join s.infoItems p where p.type = ? and p.content = ?";
        List<IndicatorSpecies> result = this.find(hql, args);
        return result;
    }
    
    @Override
    public List<IndicatorSpecies> getIndicatorSpeciesBySurvey(Session sesh, Survey survey, int start, int maxResults) {
        return getIndicatorSpeciesBySurvey(sesh, survey, start, maxResults, null);
    }
    
    @Override
    public List<IndicatorSpecies> getIndicatorSpeciesBySurvey(Session sesh, Survey survey, int start, int maxResults, List<Survey> excludeSpeciesInSurvey) {
        
        if (sesh == null) {
            sesh = getSession();
        }
        boolean excludeSurvey = excludeSpeciesInSurvey != null && !excludeSpeciesInSurvey.isEmpty();
        if (excludeSurvey) {
            for (Survey s : excludeSpeciesInSurvey) {
                if (countSpeciesForSurvey(s) == 0) {
                        // Device already has all species, so let's get outta here.
                        return new ArrayList<IndicatorSpecies>();
                }
            }
        }
        
        IndicatorSpecies fieldSpecies = taxaService.getFieldSpecies();
        // Check whether survey has all species.
        boolean hasAllSpecies = countSpeciesForSurvey(survey) == 0;
        String query;
        Query q;
        if(hasAllSpecies) {
            query = "select sp" + GET_SPECIES_BASE_QUERY;
            if (excludeSurvey) {
                query += " where ";
                query += GET_SPECIES_EXCLUDE_SURVEY_CLAUSE;
                query += " and sp != :fieldSpecies ";
            } else {
                query += " where sp != :fieldSpecies ";
            }
            query += ORDER_SPECIES_BY_ID;
            q = sesh.createQuery(query);
        } else {
            query = "select sp" + GET_SPECIES_BY_SURVEY_BASE_QUERY;
            query += " and ";
            query += "sp != :fieldSpecies ";
            if (excludeSurvey) {
                query += " and ";
                query += GET_SPECIES_EXCLUDE_SURVEY_CLAUSE;
            }
            query += ORDER_SPECIES_BY_ID;
            q = sesh.createQuery(query);
            q.setParameter("survey", survey);
        }
        if (excludeSurvey) {
            q.setParameterList("notIds", excludeSpeciesInSurvey);
        }
        q.setParameter("fieldSpecies", fieldSpecies);
        q.setFirstResult(start);
        q.setMaxResults(maxResults);
        return q.list();
    }

    @Override
    public List<IndicatorSpecies> getIndicatorSpecies(Region region) {
        return this.find(
                "from IndicatorSpecies i where ? in elements(i.regions)",
                region.getId());
    }

    @Override
    public List<IndicatorSpecies> getIndicatorSpecies(TaxonGroup group) {
        return this.find("from IndicatorSpecies i where i.taxonGroup.id = ?",
                group.getId());
    }

    @Override
    public List<IndicatorSpecies> getIndicatorSpeciesByNameSearch(String name, boolean includeFieldSpecies) {
        String searchString = toSQLSearchString(name);
        if (includeFieldSpecies) {
            return find("from IndicatorSpecies i where UPPER(commonName) like UPPER(?) or UPPER(scientificName) like UPPER (?)", 
                        new Object[] {searchString, searchString}, AUTOCOMPLETE_RESULTS_COUNT);
        } else {
            // exclude the field species!
            IndicatorSpecies fieldSpecies = taxaService.getFieldSpecies();
            return find("from IndicatorSpecies i where i != ? and (UPPER(commonName) like UPPER(?) or UPPER(scientificName) like UPPER (?))", 
                        new Object[] {fieldSpecies, searchString, searchString}, AUTOCOMPLETE_RESULTS_COUNT);
        }
    }
    
    @Override
    public List<IndicatorSpecies> getIndicatorSpeciesByNameSearchExact(String name) {
    	return find("from IndicatorSpecies i where UPPER(commonName) like UPPER(?) or UPPER(scientificName) like UPPER (?)", 
                new Object[] {name, name}, AUTOCOMPLETE_RESULTS_COUNT);
    }
    
    @Override
    public SpeciesProfile getSpeciesProfileById(Integer id) {
        return getByID(SpeciesProfile.class, id);
    }
    
    @Override
    public IndicatorSpecies getIndicatorSpecies(Integer id) {
        return getByID(IndicatorSpecies.class, id);
    }

    @Override
    public IndicatorSpecies getIndicatorSpeciesByGuid(String guid) {
        Object[] args = new Object[2];
        args[0] = Metadata.SCIENTIFIC_NAME_SOURCE_DATA_ID;
        args[1] = guid;
        List<IndicatorSpecies> list = find("select i from IndicatorSpecies i join i.metadata m where m.key = ? and m.value = ?", args);
        if (list.size() == 0) {
            log.warn("No species found for guid : " + guid);
            return null;
        } else if (list.size() > 1) {
            log.warn("Multiple species found for guid, return first : " + guid);
        }
        return list.get(0);
    }

    @Override
    public List<IndicatorSpecies> getIndicatorSpeciesByCommonName(
            String commonName) {
        return this.find("from IndicatorSpecies i where UPPER(commonName) like UPPER(?)", commonName);
    }

    @Override
    public List<IndicatorSpecies> getIndicatorSpeciesByCommonName(Session sesh, Collection<Survey> surveys, String commonName) {
        String allSpeciesQuery = "from IndicatorSpecies i where UPPER(commonName) = UPPER(:commonName)";
        String surveySpeciesQuery = "select i from Survey s left join s.species i where UPPER(i.commonName) = UPPER(:commonName) and s.id = :surveyId";

        ArrayList<IndicatorSpecies> speciesList = new ArrayList<IndicatorSpecies>();

        if(sesh == null) {
            sesh = getSession();
        }
        for(Survey survey : surveys) {
            Query q = null;
            if(survey.getSpecies().isEmpty()) {
                // All species
                q = sesh.createQuery(allSpeciesQuery);
            } else {
                // Species Subset
                q = sesh.createQuery(surveySpeciesQuery);
                q.setParameter("surveyId", survey.getId());
            }

            q.setParameter("commonName", commonName);
            speciesList.addAll(q.list());
        }
        return speciesList;
    }

    @Override
    public List<IndicatorSpecies> getIndicatorSpeciesByScientificName(Session sesh, Collection<Survey> surveys, String scientificName) {
        String allSpeciesQuery = "from IndicatorSpecies i where UPPER(scientificName) = UPPER(:scientificName)";
        String surveySpeciesQuery = "select i from Survey s left join s.species i where UPPER(i.scientificName) = UPPER(:scientificName) and s.id = :surveyId";

        ArrayList<IndicatorSpecies> speciesList = new ArrayList<IndicatorSpecies>();

        if(sesh == null) {
            sesh = getSession();
        }
        for(Survey survey : surveys) {
            Query q = null;
            if(survey.getSpecies().isEmpty()) {
                // All species
                q = sesh.createQuery(allSpeciesQuery);
            } else {
                // Species Subset
                q = sesh.createQuery(surveySpeciesQuery);
                q.setParameter("surveyId", survey.getId());
            }

            q.setParameter("scientificName", scientificName);
            speciesList.addAll(q.list());
        }
        return speciesList;
    }

    @Override
    public IndicatorSpecies getIndicatorSpeciesByCommonName(Session sesh,
            String commonName) {
        List<IndicatorSpecies> species = find(sesh,
                "select i from IndicatorSpecies i where i.commonName = ?",
                commonName);
        if (species.isEmpty()) {
            return null;
        } else {
            if (species.size() > 1) {
                log
                        .warn("Multiple IndicatorSpecies with the same common name found. Returning the first");
            }
            return species.get(0);
        }
    }
    
    @Override
    public IndicatorSpecies getIndicatorSpeciesByScientificNameAndRank(String scientificName, TaxonRank rank) {
        return this.getIndicatorSpeciesByScientificNameAndRank(null, scientificName, rank);
    }
    
    @Override
    public IndicatorSpecies getIndicatorSpeciesByScientificNameAndRank(Session sesh,
                String scientificName, TaxonRank rank) { 
        Query q;
        String query = "select t from IndicatorSpecies t where t.scientificName = :name and t.taxonRank = :rank";
            if(sesh == null) {
                q = getSession().createQuery(query);
            } else {
                q = sesh.createQuery(query);
            }
            q.setParameter("name", scientificName);
            q.setParameter("rank", rank);
            
            List<IndicatorSpecies> list = q.list();
            if (list.size() == 0) {
                    log.warn(String.format("No taxon found for scientific name %s with rank %s: ", scientificName, rank));
                    return null;
            } else if (list.size() > 1) {
                    log.warn(String.format("Multiple taxa found for scientific name %s with rank %s. Returning the first ", scientificName, rank));
            }
            return list.get(0);
    }

    @Override
    public IndicatorSpecies getIndicatorSpeciesByScientificName(Session sesh,
            String scientificName) {
        if(sesh == null) {
            sesh = getSession();
        }
        List<IndicatorSpecies> species = find(sesh,
                "select i from IndicatorSpecies i where upper(i.scientificName) = ?",
                scientificName.toUpperCase());
        if (species.isEmpty()) {
            return null;
        } else {
            if (species.size() > 1) {
                log.warn("Multiple IndicatorSpecies with the same scientific name found. Returning the first");
            }
            return species.get(0);
        }
    }
    
    @Override
        public IndicatorSpecies getIndicatorSpeciesByScientificNameAndParent(Session sesh, String source, 
                String scientificName, TaxonRank rank, Integer parentId) {
            if(sesh == null) {
                sesh = getSession();
            }
            HqlQuery hQuery = new HqlQuery("select i from IndicatorSpecies i");
            hQuery.and(Predicate.eq("upper(i.scientificName)", scientificName.toUpperCase()));
            if (parentId != null) {
                hQuery.and(Predicate.eq("i.parent.id", parentId));    
            }
            if (source != null) {
                hQuery.and(Predicate.eq("i.source", source));    
            }
            if (rank != null) {
                hQuery.and(Predicate.eq("i.taxonRank", rank));    
            }
            
            Query q = sesh.createQuery(hQuery.getQueryString());
            Object[] args = hQuery.getParametersValue();
            for (int i = 0; i<args.length; ++i) {
                q.setParameter(i, args[i]);
            }
            return (IndicatorSpecies)q.uniqueResult();
        }
	
	@Override
        public List<IndicatorSpecies> getIndicatorSpeciesListByScientificName(String scientificName) {
            return find("select i from IndicatorSpecies i where upper(i.scientificName) = ?", scientificName.toUpperCase());
        }

    @Override
    public IndicatorSpecies getIndicatorSpeciesByScientificName(
            String scientificName) {
        return getIndicatorSpeciesByScientificName(null, scientificName);
    }

    @Override
    public List<IndicatorSpecies> getIndicatorSpecies(Integer[] taxonGroupIds) {
        if (taxonGroupIds.length == 0) {
            return Collections.emptyList();
        }

        StringBuilder builder = new StringBuilder();
        builder.append("select i");
        builder.append(" from IndicatorSpecies i where i.taxonGroup in (select g from TaxonGroup g where g.id in (:ids))");

        Query q = getSession().createQuery(builder.toString());
        q.setParameterList("ids", taxonGroupIds, Hibernate.INTEGER);
        return q.list();
    }

    @Override
    public Integer countIndicatorSpecies(Integer[] taxonGroupIds) {
        if (taxonGroupIds.length == 0) {
            return 0;
        }

        StringBuilder builder = new StringBuilder();
        builder.append("select count(*)");
        builder.append(" from IndicatorSpecies i where i.taxonGroup in (select g from TaxonGroup g where g.id in (:ids))");
        
        Query q = getSession().createQuery(builder.toString());
        q.setParameterList("ids", taxonGroupIds, Hibernate.INTEGER);
        return Integer.parseInt(q.list().get(0).toString(), 10);
    }
    
    @Override
    public IndicatorSpecies refresh(IndicatorSpecies s) {
        return s;
    }

    /*
     * (non-Javadoc)
     * @see au.com.gaiaresources.bdrs.model.taxa.TaxaDAO#countAllSpecies()
     */
    @Override
    public Integer countAllSpecies() {
        Query q = getSession().createQuery(
                "select count(*) from IndicatorSpecies");
        Integer count = Integer.parseInt(q.list().get(0).toString(), 10);
        return count;
    }

    /*
     * (non-Javadoc)
     * @see au.com.gaiaresources.bdrs.model.taxa.TaxaDAO#countSpeciesForSurvey(au.com.gaiaresources.bdrs.model.survey.Survey)
     */
    @Override
    public int countSpeciesForSurvey(Survey survey) {
        Query q = getSession().createQuery("select count(t) from Survey s left join s.species t where s = :survey");
        q.setParameter("survey", survey);
        Integer count = Integer.parseInt(q.list().get(0).toString(), 10);
        return count;
    }
    
    /*
     * (non-Javadoc)
     * @see au.com.gaiaresources.bdrs.model.taxa.TaxaDAO#countActualSpeciesForSurvey(au.com.gaiaresources.bdrs.model.survey.Survey, java.util.List)
     */
    @Override
    public int countActualSpeciesForSurvey(Survey survey,
            List<Survey> notTheseSurveys) {

        IndicatorSpecies fieldSpecies = taxaService.getFieldSpecies();
        
        boolean exclude = notTheseSurveys != null && !notTheseSurveys.isEmpty();
        boolean hasAllSpecies = countSpeciesForSurvey(survey) == 0;
        if (exclude) {
            for (Survey s : notTheseSurveys) {
                if (countSpeciesForSurvey(s) == 0) {
                    // one of the excluded surveys has all of the species.
                    // Thus we can never have anything but 0 for the count.
                    return 0;
                }
            }
        }
        String queryString;
        if (hasAllSpecies) {
            queryString = "select count(sp) from IndicatorSpecies sp where ";
        } else {
            queryString = "select count(sp) from Survey s join s.species sp where s = :survey and ";
        }
        if (exclude) {
            queryString += GET_SPECIES_EXCLUDE_SURVEY_CLAUSE;
            queryString += " and ";
            queryString += " sp != :fieldSpecies ";
        } else {
            queryString += " sp != :fieldSpecies ";    
        }
        
        Query q = getSession().createQuery(queryString);
        if (exclude) {
            q.setParameterList("notIds", notTheseSurveys);
        }
        if (!hasAllSpecies) {
            q.setParameter("survey", survey);   
        }
        q.setParameter("fieldSpecies", fieldSpecies);
        Integer count = Integer.parseInt(q.list().get(0).toString(), 10);
        return count;
    }
    
    /*
     * (non-Javadoc)
     * @see au.com.gaiaresources.bdrs.model.taxa.TaxaDAO#getTopSpecies(int, int)
     */
    public List<Pair<IndicatorSpecies, Integer>> getTopSpecies(int userPk, int limit) {
        List<Pair<IndicatorSpecies, Integer>> list = new ArrayList<Pair<IndicatorSpecies, Integer>>();

        StringBuilder queryString = new StringBuilder(
                "select i.id, count (r) from Record as r join r.species as i ");
        if (userPk != 0) {
            queryString.append("where r.user.id = " + userPk);
        }
        queryString.append(" group by i.id order by count(r) desc");
        Query q = getSession().createQuery(queryString.toString());
        q.setMaxResults(limit);
        List<Object[]> results = q.list();
        for (int i = 0; i < results.size(); i++) {
            Integer id = Integer.parseInt(results.get(i)[0].toString());
            Integer count = Integer.parseInt(results.get(i)[1].toString());
            list.add(new Pair<IndicatorSpecies, Integer>(getIndicatorSpecies(id), count));
        }
        return list;
    }
    
    @Override 
    public List<IndicatorSpecies> getIndicatorSpecies(TaxonGroup taxonGroup, String search) {
    	String searchString = toSQLSearchString(search);
        return find("from IndicatorSpecies i where (UPPER(commonName) like UPPER(?) or UPPER(scientificName) like UPPER (?)) and i.taxonGroup = ? order by i.scientificName", 
                new Object[] {searchString, searchString, taxonGroup}, AUTOCOMPLETE_RESULTS_COUNT);
    }
    		
    public PagedQueryResult<IndicatorSpecies> getIndicatorSpeciesByGroup(Integer taxonGroupId, PaginationFilter filter) {
        HqlQuery q = new HqlQuery("select distinct s from IndicatorSpecies s");
        if (taxonGroupId != null) {
            q.leftJoin("s.secondaryGroups", "secondaryGroup");
            q.and(Predicate.eq("s.taxonGroup.id", taxonGroupId));
            q.or(Predicate.eq("secondaryGroup.id", taxonGroupId));
        }
        
        return new QueryPaginator<IndicatorSpecies>().page(this.getSession(), q.getQueryString(), q.getParametersValue(), filter, "s");
    }
    
    @Override
    public List<IndicatorSpecies> getChildTaxa(IndicatorSpecies taxon) {
        return find("from IndicatorSpecies s where s.parent = ?", taxon);
    }
    
    @Override
    public void delete(IndicatorSpecies taxon) throws StaleStateException {
        
        DeleteCascadeHandler taxonCascadeHandler = 
            delService.getDeleteCascadeHandlerFor(IndicatorSpecies.class);
        for(IndicatorSpecies child : this.getChildTaxa(taxon)) {
            taxonCascadeHandler.deleteCascade(child);
        }
        
        delService.deleteRecords(taxon);
        delService.unlinkFromSurvey(taxon);
        
        Set<AttributeValue> taxonAttributes = new HashSet(taxon.getAttributes());
        taxon.getAttributes().clear();
        
        Set<SpeciesProfile> taxonProfiles = new HashSet(taxon.getInfoItems());
        taxon.getInfoItems().clear();
        
        taxon = save(taxon);
        
        DeleteCascadeHandler taxonAttributeCascadeHandler = 
            delService.getDeleteCascadeHandlerFor(AttributeValue.class);
        for(AttributeValue attr : taxonAttributes) {
            attr = save(attr);
            taxonAttributeCascadeHandler.deleteCascade(attr);
        }
        
        DeleteCascadeHandler taxonProfileCascadeHandler = 
            delService.getDeleteCascadeHandlerFor(SpeciesProfile.class);
        for(SpeciesProfile profile : taxonProfiles) {
            profile = save(profile);
            taxonProfileCascadeHandler.deleteCascade(profile);
        }
        
        deleteByQuery(taxon);
    }
    
    @Override
    public void delete(TaxonGroup taxonGroup) {
        
        DeleteCascadeHandler taxonCascadeHandler = 
            delService.getDeleteCascadeHandlerFor(IndicatorSpecies.class);
        for(IndicatorSpecies taxon : this.getIndicatorSpecies(taxonGroup)) {
            try {
                taxonCascadeHandler.deleteCascade(taxon);
            } catch(StaleStateException sse) {
                // State State Exception can becaused by deleting a taxon
                // that has already been deleted. This can be caused by
                // deleting a parent taxon where the parent and child taxon
                // are in the same group. Deletion of the parent taxon
                // automatically deletes the child taxon. The child is still
                // in the group list and gets deleted twice.
                log.error("The exception that you see occurs because we are deleting a taxon twice. This is expected behaviour.");
            }
        }
        
        List<Attribute> attributeList = new ArrayList(taxonGroup.getAttributes());
        taxonGroup.getAttributes().clear();
        taxonGroup = save(taxonGroup);
        
        DeleteCascadeHandler attributeCascadeHandler = 
            delService.getDeleteCascadeHandlerFor(Attribute.class); 
        for(Attribute attr : attributeList) {
            attr = save(attr);
            attributeCascadeHandler.deleteCascade(attr);
        }
        
        deleteByQuery(taxonGroup);
    }

    /* (non-Javadoc)
     * @see au.com.gaiaresources.bdrs.model.taxa.TaxaDAO#getDistinctRecordedTaxaForSurvey(int)
     */
    @Override
    public List<IndicatorSpecies> getDistinctRecordedTaxaForSurvey(int surveyId) {
    	Session sesh = this.getSession();
    	// 2 queries, no outer joins required.
    	List<IndicatorSpecies> result1 = sesh.createQuery("select r.species from Record r join r.survey s where s.id = ?")
    			.setParameter(0, surveyId).list();
    	List<IndicatorSpecies> result2 = sesh.createQuery("select distinct av.species from Record r join r.attributes av join r.survey s where s.id = ?")
    			.setParameter(0,  surveyId).list();
    	Set<IndicatorSpecies> speciesSet = new HashSet<IndicatorSpecies>();
    	for (IndicatorSpecies s : result1) {
    		speciesSet.add(s);
    	}
    	for (IndicatorSpecies s : result2) {
    		speciesSet.add(s);
    	}
    	List<IndicatorSpecies> finalResult = new ArrayList<IndicatorSpecies>(speciesSet.size());
    	finalResult.addAll(speciesSet);
    	return finalResult;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public List<IndicatorSpecies> searchIndicatorSpeciesByGroupName(String groupName, String taxonName) {
        IndicatorSpecies fieldSpecies = taxaService.getFieldSpecies();
        Query q = getSession().createQuery("from IndicatorSpecies i where i != :fieldSpecies and (UPPER(i.commonName) like UPPER(:taxonName) or UPPER(i.scientificName) like UPPER (:taxonName)) and UPPER(i.taxonGroup.name) like UPPER(:groupName)");
        q.setParameter("groupName", toSQLSearchString(groupName));
        q.setParameter("taxonName", toSQLSearchString(taxonName));
        q.setParameter("fieldSpecies", fieldSpecies);
        
        return q.list();
    }

    @Override
    public List<Integer> searchIndicatorSpeciesPk(String groupName, String taxonName, boolean includeSecondaryGroups) {

        HqlQuery hqlQuery = new HqlQuery("select distinct i.id from IndicatorSpecies i");
        if (StringUtils.notEmpty(taxonName)) {
            Predicate speciesNamePredicate = Predicate.expr("UPPER(i.scientificName) like UPPER(:taxonName)", taxonName, "taxonName");
            speciesNamePredicate.or(Predicate.expr("UPPER(i.commonName) like UPPER(:taxonName)", taxonName, "taxonName"));
            hqlQuery.and(speciesNamePredicate);
        }
        if (StringUtils.notEmpty(groupName)) {
            Predicate groupPredicate = Predicate.expr("UPPER(i.taxonGroup.name) like UPPER(:groupName)", groupName, "groupName");
            if (includeSecondaryGroups) {
                hqlQuery.leftJoin("i.secondaryGroups", "sg");
                groupPredicate.or(Predicate.expr("UPPER(sg.name) like UPPER(:groupName)", groupName, "groupName"));
            }
            hqlQuery.and(groupPredicate);
        }

        Query q = getSession().createQuery(hqlQuery.getQueryString());
        hqlQuery.applyNamedArgsToQuery(q);

        return q.list();
    }

    /**
     * Helper method that turns a search term into an SQL search term such that:
     * <ul>
     *     <li>% is prepended and appended to the supplied string</li>
     *     <li>spaces are replaced by % in the supplied string</li>
     * </ul></li>
     * @param name the search term to modify.
     * @return a new string suitable for SQL parameter substitution.
     */
    private String toSQLSearchString(String name) {
        StringBuilder query = new StringBuilder("");
        String[] bits = name.split(" ");
        for (int i = 0; i < bits.length; i++) {
            query.append("%");
            query.append(bits[i]);
        }
        query.append("%");

        return query.toString();
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public PagedQueryResult<IndicatorSpecies> getIndicatorSpeciesByQueryString(
            Integer groupId, String searchInGroups, String searchInResult, PaginationFilter filter) {
        
        HashMap<String, Object> queryMap = new HashMap<String, Object>();
        
        StringBuilder builder = new StringBuilder();
        builder.append("select distinct sp from IndicatorSpecies sp ");
        builder.append(" left outer join sp.infoItems profile");
        
        if(groupId != null) {
            queryMap.put("groupId",     groupId);
            builder.append(" where sp.taxonGroup in (select g from TaxonGroup g where g.id in (:groupId))");
        } else {
            queryMap.put("searchInGroups", "%" + searchInGroups + "%");
            queryMap.put("searchInGroupsExact", searchInGroups);
            builder.append(" where ((UPPER(sp.commonName) like UPPER(:searchInGroups)");
            builder.append(" or UPPER(sp.scientificName) like UPPER(:searchInGroups)");
            //TODO: maybe in the future make querying the species profile dynamic by using Facets or google like "key:value".
            builder.append(" or UPPER(profile.content) like UPPER(:searchInGroups)");
            builder.append(" or sp.taxonGroup in (select g from TaxonGroup g where UPPER(g.name) in (UPPER(:searchInGroupsExact)))))");
        }
        
        if(searchInResult != null) {
            queryMap.put("searchInResult", "%" + searchInResult + "%");
            builder.append(" and ((UPPER(sp.commonName) like UPPER(:searchInResult)");
            builder.append(" or UPPER(sp.scientificName) like UPPER(:searchInResult)");
            builder.append(" or UPPER(profile.content) like UPPER(:searchInResult)))");
        }
        
        return new QueryPaginator<IndicatorSpecies>().page(this.getSession(), builder.toString(), queryMap, filter, "sp");
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public PagedQueryResult<IndicatorSpecies> searchTaxa(
            Integer groupId, TaxonGroupSearchType searchType,
            String searchInGroups, String searchInResult, PaginationFilter filter) throws org.apache.lucene.queryParser.ParseException {
        if (groupId != null && StringUtils.nullOrEmpty(searchInGroups) && StringUtils.nullOrEmpty(searchInResult)) {
            // do a normal database search
            // add sorting since none is passed to the query
            // if no sorting, add this one
            filter.addSortingCriteria("scientificName", SortOrder.ASCENDING);
            return getIndicatorSpeciesByGroup(groupId, filter);
        } else {
            // do an indexed search
            // the fields to search on
            String[] fields = new String[]{"commonName", "scientificName", "infoItems.content", "taxonGroup.name", "taxonGroup.id"};
            
            PerFieldAnalyzerWrapper aWrapper = new PerFieldAnalyzerWrapper(new StandardAnalyzer());
            Analyzer customAnalyzer = Search.getFullTextSession(getSession()).getSearchFactory().getAnalyzer(IndexingConstants.FULL_TEXT_ANALYZER);
            aWrapper.addAnalyzer("commonName", customAnalyzer);
            aWrapper.addAnalyzer("scientificName", customAnalyzer);
            aWrapper.addAnalyzer("infoItems.content", customAnalyzer);
            aWrapper.addAnalyzer("taxonGroup.name", customAnalyzer);
            
            // the '+' indicates that the term must be matched, only necessary if one of 
            // searchInResults or groupId is specified, but is done implicitly anyway when there is only one term
            String searchTerm = !StringUtils.nullOrEmpty(searchInGroups) ? "+" + searchInGroups + "" : "";
            if (!StringUtils.nullOrEmpty(searchInResult)) {
                searchTerm += " +("+searchInResult + ")" ;
            }

            if (groupId != null) {
                // add the group to the query
                // the '+' indicates that the term must be matched, so it must be in the group and contain the term
                switch (searchType) {
                    case PRIMARY:
                        searchTerm += " +(taxonGroup.id:"+groupId+ ")";
                        break;
                    case SECONDARY:
                        searchTerm += " +(secondaryGroups.id:"+groupId+ ")";
                        break;
                    case PRIMARY_OR_SECONDARY:
                        searchTerm += " +(taxonGroup.id:"+groupId+" OR secondaryGroups.id:"+groupId+ ")";
                        break;
                }
            }
            
            // exclude the field species
            IndicatorSpecies fieldSpecies = taxaService.getFieldSpecies();
            searchTerm += " -(id:"+fieldSpecies.getId().toString()+")";
            
            return searchService.searchPaged(getSession(), fields, aWrapper, searchTerm, filter, IndicatorSpecies.class, SpeciesProfile.class);
        }
    }

    /**
     * Implementation Note: a DML update may be a more appropriate technique, however this will prevent the
     * lucene index from being updated which in turn will prevent these changes from being reflected in the field guide.
     * {@inheritDoc}
     */
    @Override
    public void bulkUpdatePrimaryGroup(List<Integer> taxonIds, TaxonGroup group) {

        // First ensure none of the taxa have the new primary group as a secondary group.
        bulkRemoveSecondaryGroup(taxonIds, group);

        List<IndicatorSpecies> taxa = getIndicatorSpeciesById(taxonIds.toArray(new Integer[taxonIds.size()]));
        for (IndicatorSpecies taxon : taxa) {
            taxon.setTaxonGroup(group);
        }

    }

    /**
     * Implementation Note: a DML update may be a more appropriate technique, however this will prevent the
     * lucene index from being updated which in turn will prevent these changes from being reflected in the field guide.
     * {@inheritDoc}
     */
    @Override
    public void bulkRemoveSecondaryGroup(List<Integer> taxonIds, TaxonGroup group) {

        Session session = RequestContextHolder.getContext().getHibernate();

        // Now remove any secondary groups.
        String hql = "from IndicatorSpecies where :taxonGroup in elements(secondaryGroups) and id in (:taxonIds)";

        Query query = session.createQuery(hql);
        query.setParameter("taxonGroup", group);
        query.setParameterList("taxonIds", taxonIds);
        List<IndicatorSpecies> taxa = query.list();
        for (IndicatorSpecies taxon : taxa) {
            taxon.getSecondaryGroups().remove(group);
        }
    }


    /**
     * Implementation Note: a DML update may be a more appropriate technique, however this will prevent the
     * lucene index from being updated which in turn will prevent these changes from being reflected in the field guide.
     * {@inheritDoc}
     */
    @Override
    public void bulkAssignSecondaryGroup(List<Integer> taxonIds, TaxonGroup group) {

        Session session = RequestContextHolder.getContext().getHibernate();

        // In theory we should be batching this, but the current use case is limited by the page size of
        // the jqGrid used to present the taxa so shouldn't be more than 100 at a time.
        String hql = "from IndicatorSpecies where not :taxonGroup in elements(secondaryGroups) and :taxonGroup != taxonGroup and id in (:taxonIds)";
        Query query = session.createQuery(hql);
        query.setParameter("taxonGroup", group);
        query.setParameterList("taxonIds", taxonIds);
        List<IndicatorSpecies> taxa = query.list();

        for (IndicatorSpecies taxon : taxa) {
            taxon.addSecondaryGroup(group);
        }
    }
    
    /*
     * (non-Javadoc)
     * @see au.com.gaiaresources.bdrs.model.taxa.TaxaDAO#getSpeciesForRecord(org.hibernate.Session, java.lang.Integer)
     */
    @Override
    public List<IndicatorSpecies> getSpeciesForRecord(Session sesh, Integer recId) {
    	
    	if (sesh == null) {
    		sesh = this.getSession();
    	}
    	// 2 queries, no outer joins required.
    	List<IndicatorSpecies> result1 = sesh.createQuery("select r.species from Record r where r.id = ?")
    			.setParameter(0, recId).list();
    	List<IndicatorSpecies> result2 = sesh.createQuery("select distinct av.species from Record r join r.attributes av where r.id = ?")
    			.setParameter(0,  recId).list();
    	Set<IndicatorSpecies> speciesSet = new HashSet<IndicatorSpecies>();
    	for (IndicatorSpecies s : result1) {
    		speciesSet.add(s);
    	}
    	for (IndicatorSpecies s : result2) {
    		speciesSet.add(s);
    	}
    	List<IndicatorSpecies> finalResult = new ArrayList<IndicatorSpecies>(speciesSet.size());
    	finalResult.addAll(speciesSet);
    	return finalResult;
    }
    
    /*
     * (non-Javadoc)
     * @see au.com.gaiaresources.bdrs.model.taxa.TaxaDAO#getAll(org.hibernate.Session)
     */
    @Override
    public ScrollableResults<IndicatorSpecies> getAll(Session sesh) {
        if (sesh == null) {
            sesh = getSession();
        }
        Query q = sesh.createQuery("from IndicatorSpecies");
        return new ScrollableResultsImpl<IndicatorSpecies>(q);
    }
    
    /*
     * (non-Javadoc)
     * @see au.com.gaiaresources.bdrs.model.taxa.TaxaDAO#getSpeciesForSurvey(org.hibernate.Session, au.com.gaiaresources.bdrs.model.survey.Survey, java.lang.String)
     */
    @Override
    public IndicatorSpecies getSpeciesForSurvey(Session sesh, Survey s, String sciName) {
        if (s == null) {
            throw new IllegalArgumentException("Survey cannot be null");
        }
        if (StringUtils.nullOrEmpty(sciName)) {
            throw new IllegalArgumentException("Sci name cannot be null or empty");
        }
        if (sesh == null) {
            sesh = getSession();
        }
        int speciesCount = countSpeciesForSurvey(s);
        if (speciesCount == 0) {
            // i.e. survey has ALL species.
            return this.getIndicatorSpeciesByScientificName(sesh, sciName);
        } else {
            List<IndicatorSpecies> species = find(sesh, "select i from Survey s join s.species i where upper(i.scientificName) = ? and s = ?", 
                        new Object[] { sciName.toUpperCase(), s });
            if (species.isEmpty()) {
                return null;
            } else {
                if (species.size() > 1) {
                    log.warn("Multiple IndicatorSpecies with the same scientific name found. Returning the first");
                }
                return species.get(0);
            }
        }
    }
}
