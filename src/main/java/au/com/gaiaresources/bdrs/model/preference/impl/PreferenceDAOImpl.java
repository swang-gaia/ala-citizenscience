package au.com.gaiaresources.bdrs.model.preference.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import javax.annotation.PostConstruct;

import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import au.com.gaiaresources.bdrs.db.impl.AbstractDAOImpl;
import au.com.gaiaresources.bdrs.model.portal.Portal;
import au.com.gaiaresources.bdrs.model.portal.PortalDAO;
import au.com.gaiaresources.bdrs.model.preference.Preference;
import au.com.gaiaresources.bdrs.model.preference.PreferenceCategory;
import au.com.gaiaresources.bdrs.model.preference.PreferenceDAO;
import au.com.gaiaresources.bdrs.servlet.RequestContextHolder;

@Repository
public class PreferenceDAOImpl extends AbstractDAOImpl implements PreferenceDAO {

    public static final String DEFAULT_PREFERENCES = "preferences.json";

    private Logger log = Logger.getLogger(getClass());
    
    @Autowired
    private PortalDAO portalDAO;  

    // { portal : { prefKey : pref } }
    private Map<Portal, Map<String, Preference>> prefCache;

    @PostConstruct
    public void postConstruct() throws Exception {
        Session sesh = getSessionFactory().openSession();
        init(sesh);
        sesh.close();
    }
    
    public void init(Session sesh) {
        // Insert all existing preferences into the cache
        prefCache = new HashMap<Portal, Map<String, Preference>>();
        
        // Fill with the list of portals
        for (Portal portal : portalDAO.getPortals(sesh)) {
            prefCache.put(portal, new HashMap<String, Preference>());
        }
        prefCache.put(null, new HashMap<String, Preference>());
        for (Preference pref : getAllPreferences(sesh)) {
            cachePreference(pref);
        }
    }

    private void cachePreference(Preference pref) {
        
        if(pref.getId() == null) {
            log.warn("Attempt to cache unsaved preference. Not adding preference to cache.");
            return;
        }

        Portal portal = pref.getPortal();
        Map<String, Preference> prefMap;
        if (prefCache.containsKey(portal)) {
            prefMap = prefCache.get(portal);
        } else {
            prefMap = new HashMap<String, Preference>();
            prefCache.put(portal, prefMap);
        }
        prefMap.put(pref.getKey(), pref);
    }

    private void uncachePreference(Preference pref) {
        if(pref.getId() == null) {
            // There are no unsaved preferences in the cache.
            return;
        }
        
        Map<String, Preference> prefMap = prefCache.get(pref.getPortal());
        if (prefMap == null) {
            log.warn(String.format("Attempt to delete pref with id \"%d\" from unknown portal.", pref.getId()));
        } else {
            
            // Cannot trust the pref key to be unmodified so cannot pull it from
            // the map using the pref key.
            for(Preference p : prefMap.values()) {
                if(p.getId().equals(pref.getId())) {
                    prefMap.remove(p.getKey());
                    return;
                }
            }
            log.warn(String.format("Attempt to delete uncached pref with id \"%d\"", pref.getId()));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PreferenceCategory getPreferenceCategory(Integer pk) {
        // We need to use the session.get method of querying here
        // because we need to bypass portal query filtering.
        // By definition, these preferences have no portal set
        // and therefore cannot ever be located using a 'normal'
        // query.
        return (PreferenceCategory)super.getSession().get(PreferenceCategory.class, pk);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Preference getPreference(Integer pk) {
        return super.getByID(Preference.class, pk);
    }

    /**
     * Returns the Preference identified by the supplied primary key using the supplied Session.
     * @param session the Session to use for database access.
     * @param pk the primary key of the Preference to retrieve.
     * @return the Preference object with the supplied primary key.
     */
    private Preference getPreference(Session session, Integer pk) {
        return super.getByID(session, Preference.class, pk);
    }

    private PreferenceCategory getCategoryByName(Session sesh, String name) {

        if (sesh == null) {
            sesh = getSessionFactory().getCurrentSession();
        }

        List<PreferenceCategory> cats = find(sesh, "from PreferenceCategory c where c.name = ?", name);
        if (cats.isEmpty()) {
            return null;
        }

        if (cats.size() > 1) {
            log.error(String.format("More than one PreferenceCategory with the name \"%s\" found. Returning the first.", name));
        }

        return cats.get(0);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Preference getPreferenceByKey(String key) {
        return this.getPreferenceByKey(null, key);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Preference getPreferenceByKey(Session sesh, String key) {
        if (sesh == null) {
            sesh = getSessionFactory().getCurrentSession();
        }
        Portal portal = RequestContextHolder.getContext().getPortal();
        if (prefCache.containsKey(portal)) {
            Preference pref = prefCache.get(portal).get(key);
            // if preference cannot be found, fall back to system prefs
            pref = pref == null && portal != null ? prefCache.get(null).get(key)
                    : pref;

            if (pref == null) {
                log.warn(String.format("Cannot find preference with key \"%s\". Returning null.", key));
            } else {
                pref = this.getPreference(sesh, pref.getId());
            }
            return pref;
        } else {

            log.error("Unknown portal returned by the Portal Service: "
                    + portal.getName());
            throw new IllegalArgumentException();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Preference save(Preference pref) {
        return this.save(null, pref);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Preference save(Session sesh, Preference pref) {
        if (sesh == null) {
            sesh = getSessionFactory().getCurrentSession();
        }

        if(pref.getId() != null) {
            uncachePreference(pref);
        }
        
        pref = super.save(sesh, pref);
        cachePreference(pref);
        return pref;
    }

    @Override
    public void delete(Preference pref) {
        this.delete(null, pref);
    }

    @Override
    public void delete(Session sesh, Preference pref) {
        if(pref.getPortal() == null) {
            log.warn("Unable to delete system preference");
            return;
        }
        uncachePreference(pref);
        super.delete(pref);
    }
    
    @Override 
    public void delete(Session sesh, PreferenceCategory prefCat) {
        if (sesh == null) {
            throw new IllegalArgumentException("Session, sesh, cannot be null");
        }
        if (prefCat == null) {
            throw new IllegalArgumentException("PreferenceCategory, prefCat, cannot be null");
        }
        super.delete(sesh, prefCat);
    }

    private List<Preference> getAllPreferences(Session sesh) {
        if (sesh == null) {
            sesh = getSessionFactory().getCurrentSession();
        }

        return find(sesh, "from Preference pref left join fetch pref.portal");
    }

    @Override
    public Map<String, Preference> getPreferences() {
        return this.getPreferences(null);
    }

    @Override
    public Map<String, Preference> getPreferences(Session sesh) {
        Portal portal = RequestContextHolder.getContext().getPortal();
        if (prefCache.containsKey(portal)) {

            Preference temp;
            // the preferences should be sorted by key
            Map<String, Preference> prefMap = new TreeMap<String, Preference>();
            if (portal != null) {
                for (Preference p : prefCache.get(null).values()) {
                    try {
                        throw new Exception(
                                "Null portal is not allowed");
                    } catch (Exception e) {
                        log.error("We should never hit this. There are no preferences with a null portal,  however you may hit this if you are using an old database", e);
                    }
                }
            }

            for (Preference p : prefCache.get(portal).values()) {
                temp = getPreference(p.getId());
                if (temp == null) {
                    // this could be a serious error, the cache contains something the db does not
                    log.warn("Preference "+p.getKey()+" exisits in cache, but not in DAO");
                    temp = p;
                } else {
                    prefMap.put(temp.getKey(), temp);
                }
            }

            return Collections.unmodifiableMap(prefMap);

        } else {
            log.error("Unknown portal returned by the Portal Service: "
                    + portal.getName());
            throw new IllegalArgumentException();
        }
    }

    @SuppressWarnings("unused")
    private void logPrefCache() {
        for (Map.Entry<Portal, Map<String, Preference>> portalEntry : prefCache.entrySet()) {

            Portal portal = portalEntry.getKey();
            Map<String, Preference> prefMap = portalEntry.getValue();
            
            log.info(String.format("Portal: %s", portal == null ? "null": portal.getName()));
            logPrefMap(prefMap);
            log.info("End Portal");
            log.info("");
        }
    }

    private void logPrefMap(Map<String, Preference> prefMap) {
        for (Map.Entry<String, Preference> prefEntry : prefMap.entrySet()) {
            log.info(String.format("    %d:%s=%s", prefEntry.getValue().getId(), prefEntry.getKey(), prefEntry.getValue().getValue()));
        }
    }
    
    /*
     * Note that these entries are not actually linked to hibernate in
     * a deployed scenario. If you try WRITE to this preference prepare
     * to have your changes totally ignored.
     * 
     * (non-Javadoc)
     * @see au.com.gaiaresources.bdrs.model.preference.PreferenceDAO#getPreferenceByKeyPrefix(java.lang.String)
     */
    @Override
    public List<Preference> getPreferenceByKeyPrefix(String prefix) {
        Portal portal = RequestContextHolder.getContext().getPortal();
        if (prefCache.containsKey(portal)) {
            // Note that these entries are not actually linked to hibernate in
            // a deployed scenario. If you try WRITE to this preference prepare
            // to have your changes totally ignored.
            List<Preference> result = new ArrayList<Preference>();
            for (Entry<String, Preference> entry : prefCache.get(portal).entrySet()) {
                if (entry.getKey().startsWith(Preference.GOOGLE_MAP_KEY_PREFIX)) {
                    result.add(entry.getValue());
                }
            }
            return result;
        } else {

            log.error("Unknown portal returned by the Portal Service: "
                    + portal.getName());
            throw new IllegalArgumentException();
        }
    }
    
    @Override
    public PreferenceCategory save(Session sesh, PreferenceCategory cat) {
        if (sesh == null) {
            sesh = getSessionFactory().getCurrentSession();
        }
        return super.save(sesh, cat);
    }
    
    @Override
    public List<PreferenceCategory> getPreferenceCategories() {
        Session sesh = getSessionFactory().getCurrentSession();
        return (List<PreferenceCategory>)sesh.createQuery("from PreferenceCategory").list();
    }
    
    @Override
    public PreferenceCategory getPreferenceCategoryByName(Session sesh, String name, Portal p) {
        if (sesh == null) {
            sesh = getSessionFactory().getCurrentSession();
        }
        Object[] args = new Object[] { name, p };
        List<PreferenceCategory> cats = find(sesh, "from PreferenceCategory c where c.name = ? and c.portal = ?", args);
        if (cats.isEmpty()) {
            return null;
        }
        if (cats.size() > 1) {
            log.error(String.format("More than one PreferenceCategory with the name \"%s\" found. Returning the first.", name));
        }
        return cats.get(0);
    }
    
    @Override
    public Preference getPreferenceByKey(Session sesh, String key, Portal p) {
        if (sesh == null) {
            sesh = getSessionFactory().getCurrentSession();
        }
        
        Object[] args = new Object[] { key, p };
        List<Preference> prefs = find(sesh, "from Preference p where p.key = ? and p.portal = ?", args);
        if (prefs.isEmpty()) {
            return null;
        }
        if (prefs.size() > 1) {
            log.error(String.format("More than one Preference with the key \"%s\" found. Returning the first.", key));
        }
        return prefs.get(0);
    }
}
