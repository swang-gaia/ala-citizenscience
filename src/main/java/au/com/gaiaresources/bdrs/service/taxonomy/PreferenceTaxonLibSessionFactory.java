package au.com.gaiaresources.bdrs.service.taxonomy;

import java.sql.SQLException;

import org.hibernate.HibernateException;
import org.springframework.util.StringUtils;

import au.com.gaiaresources.bdrs.model.preference.Preference;
import au.com.gaiaresources.bdrs.model.preference.PreferenceDAO;
import au.com.gaiaresources.taxonlib.ITaxonLibSession;
import au.com.gaiaresources.taxonlib.TaxonLibException;

/**
 * Factory that creates TaxonLib sessions from settings stored in the BDRS
 * preference table.
 */
public class PreferenceTaxonLibSessionFactory extends
        AbstractTaxonLibSessionFactory {

    /**
     * Preference key
     */
    private static final String TAXON_LIB_DB_URL_KEY = "taxonlib.database.url";
    /**
     * Preference key
     */
    private static final String TAXON_LIB_DB_USER_KEY = "taxonlib.database.username";
    /**
     * Preference key
     */
    private static final String TAXON_LIB_DB_PASS_KEY = "taxonlib.database.password";

    private PreferenceDAO prefDAO = null;
    
    public PreferenceTaxonLibSessionFactory(PreferenceDAO prefDAO) {
        super();
        this.prefDAO = prefDAO;
    }
    
    /**
     * Gets a taxon lib session using database properties found in the
     * preference table. Use this when providing a TaxonLibSession to client
     * classes in the BDRS.
     * 
     * @param prefDAO
     *            PreferenceDAO for retrieving database details.
     * @return The created TaxonLibSession.
     * @throws TaxonLibException
     * @throws SQLException 
     * @throws Exception
     */
    @Override
    public ITaxonLibSession getSession() throws IllegalArgumentException,
            BdrsTaxonLibException, TaxonLibException, SQLException {
        if (prefDAO == null) {
            throw new IllegalArgumentException("PreferenceDAO cannot be null");
        }

        try {
            Preference urlPref = prefDAO.getPreferenceByKey(TAXON_LIB_DB_URL_KEY);
            Preference userPref = prefDAO.getPreferenceByKey(TAXON_LIB_DB_USER_KEY);
            Preference passPref = prefDAO.getPreferenceByKey(TAXON_LIB_DB_PASS_KEY);

            if (urlPref == null || userPref == null || passPref == null) {
                throw new BdrsTaxonLibException(
                        "TaxonLib preferences have not been configured correctly.");
            }
            if (!StringUtils.hasLength(urlPref.getValue().trim())
                    || !StringUtils.hasLength(userPref.getValue().trim())
                    || !StringUtils.hasLength(passPref.getValue().trim())) {
                throw new BdrsTaxonLibException(
                        "TaxonLib preferences have not been configured correctly.");
            }
            // looks ok, lets go!

            String url = urlPref.getValue().trim();
            String username = userPref.getValue().trim();
            String password = passPref.getValue().trim();

            return getSession(url, username, password);
        } catch (HibernateException he) {
            throw new BdrsTaxonLibException("Hibernate transaction not available. Cannot query for taxonlib database details.");
        }
    }
}
