package au.com.gaiaresources.bdrs.controller.portal;

import au.com.gaiaresources.bdrs.controller.AbstractController;
import au.com.gaiaresources.bdrs.controller.insecure.HTTPErrorController;
import au.com.gaiaresources.bdrs.db.impl.PortalPersistentImpl;
import au.com.gaiaresources.bdrs.json.JSONArray;
import au.com.gaiaresources.bdrs.json.JSONObject;
import au.com.gaiaresources.bdrs.model.portal.Portal;
import au.com.gaiaresources.bdrs.model.portal.PortalDAO;
import au.com.gaiaresources.bdrs.model.portal.PortalEntryPoint;
import au.com.gaiaresources.bdrs.security.Role;
import au.com.gaiaresources.bdrs.servlet.RequestContext;
import au.com.gaiaresources.bdrs.servlet.filter.PortalMatches;
import au.com.gaiaresources.bdrs.servlet.filter.PortalSelectionFilterMatcher;
import au.com.gaiaresources.bdrs.servlet.view.PortalRedirectView;
import org.apache.log4j.Logger;
import org.codehaus.plexus.util.StringUtils;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import javax.annotation.PostConstruct;
import javax.annotation.security.RolesAllowed;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The <code>PortalController</code> handles all view requests for portal wide
 * configuration.
 */
@Controller
public class PortalController extends AbstractController {
    @SuppressWarnings("unused")
    private Logger log = Logger.getLogger(getClass());
    
    public static final String PORTAL_ENTRY_POINT_EDIT_PATTERN_TMPL = "entryPoint_pattern_%d";
    public static final String PORTAL_ENTRY_POINT_EDIT_REDIRECT_TMPL = "entryPoint_redirect_%d";
    
    public static final String PORTAL_ENTRY_POINT_ADD_PATTERN_TMPL = "add_entryPoint_pattern_%d";
    public static final String PORTAL_ENTRY_POINT_ADD_REDIRECT_TMPL = "add_entryPoint_redirect_%d";
    
    /**
     * Error message code used when an attempt to change the portal active state is denied.
     */
    public static final String PORTAL_ACTIVE_STATE_CHANGE_DENIED = "bdrs.portal.active.denied";
    
    private PortalSelectionFilterMatcher portalFilterMatcher;
    
    @Autowired
    private PortalDAO portalDAO;
    @Autowired
    private SessionFactory sessionFactory;
    @Autowired
    private PortalPrefixValidator prefixValidator;
    
    /**
     * Initialises this controller after dependency injection has been completed.
     * @throws Exception
     */
    @PostConstruct
    public void init() throws Exception {
        portalFilterMatcher = new PortalSelectionFilterMatcher(portalDAO, prefixValidator);
    }

    /**
     * Displays a page listing all active and inactive portals in the system.
     * @param request the browser request
     * @param response the server response.
     */
    @RolesAllowed( { Role.ROOT })
    @RequestMapping(value = "/bdrs/root/portal/listing.htm", method = RequestMethod.GET)
    public ModelAndView listing(HttpServletRequest request,
            HttpServletResponse response) {
        
        ModelAndView mv = new ModelAndView("portalSetup");
        mv.addObject("portalList", portalDAO.getPortals());
        return mv;
    }
    
    /**
     * Displays a page that permits editing of the portal with the specified primary key.
     * @param request the browser request
     * @param response the server response
     * @param pk the primary key of the portal to be displayed
     */
    @RolesAllowed( { Role.ROOT })
    @RequestMapping(value = "/bdrs/root/portal/edit.htm", method = RequestMethod.GET)
    public ModelAndView edit(   HttpServletRequest request, 
                                HttpServletResponse response,
                                @RequestParam(value="id", required=false, defaultValue="0") int pk) {
        
        Portal portal = portalDAO.getPortal(pk);
        List<PortalEntryPoint> portalEntryPointList;
        if(portal == null) {
            portal = new Portal();
            portalEntryPointList = new ArrayList<PortalEntryPoint>();
        } else {
            portalEntryPointList = portalDAO.getPortalEntryPoints(portal);
        }
        
        ModelAndView mv = new ModelAndView("portalEdit");
        mv.addObject("portal", portal);
        mv.addObject("portalEntryPointList", portalEntryPointList);
        return mv;
    }

    /**
     * Updates the portal with the specified primary key.
     * 
     * @param request the browser request
     * @param response the server response.
     * @param pk the primary key of the portal to be updated.
     * @param name the new name of the portal.
     * @param isDefault true if the portal is the new default portal, false otherwise.
     * @param isActive true if the portal is currently in use, false otherwise.
     * @param add_portalEntryPointIndexes an array of indexes containing new portal entry points.
     * @param portalEntryPointPks an array of existing portal entry points (those that have not been deleted via the browser)
     * @throws Exception
     */
    @RolesAllowed( { Role.ROOT })
    @RequestMapping(value = "/bdrs/root/portal/edit.htm", method = RequestMethod.POST)
    public ModelAndView editSubmit( HttpServletRequest request, 
                                    HttpServletResponse response,
        @RequestParam(value="portalId", required=false, defaultValue="0") int pk,
        @RequestParam(value="name", required=true) String name,
        @RequestParam(value="urlPrefix", required=false, defaultValue="") String urlPrefix,
        @RequestParam(value="default", required=false, defaultValue="false") boolean isDefault,
        @RequestParam(value="active", required=false, defaultValue="false") boolean isActive,
        @RequestParam(value="add_portalEntryPoint", required=false) int[] add_portalEntryPointIndexes,
        @RequestParam(value="portalEntryPoint_id", required=false) int[] portalEntryPointPks) throws Exception {
        try {
            Portal portal = parsePortalEditForm(getRequestContext().getHibernate(),
                                            request, pk, name, urlPrefix, isDefault, isActive, add_portalEntryPointIndexes, portalEntryPointPks);
            getRequestContext().addMessage("bdrs.portal.save.success", new Object[]{portal.getName()});
            return new ModelAndView(new PortalRedirectView("/bdrs/root/portal/listing.htm", true));
        } catch (IllegalArgumentException e) {
            getRequestContext().addMessage("bdrs.portal.save.fail", new Object[]{e.getMessage()});
            // reconstruct the parameters so the view is the same
            // as the one they submitted
            Portal portal;
            List<PortalEntryPoint> portalEntryPointList;
            if (pk != 0) {
                portal = portalDAO.getPortal(pk);
                portalEntryPointList = portalDAO.getPortalEntryPoints(portal);
            } else {
                portal = new Portal();
                portalEntryPointList = new ArrayList<PortalEntryPoint>();
                portal.setName(name);
                portal.setUrlPrefix(urlPrefix);
                portal.setDefault(isDefault);
            }
            if (add_portalEntryPointIndexes != null) {
                for (int entryPointId : add_portalEntryPointIndexes) {
                    PortalEntryPoint entryPoint = new PortalEntryPoint();
                    
                    String pattern = request.getParameter(String.format(PORTAL_ENTRY_POINT_ADD_PATTERN_TMPL, entryPointId));
                    String redirect = request.getParameter(String.format(PORTAL_ENTRY_POINT_ADD_REDIRECT_TMPL, entryPointId));
                    
                    entryPoint.setPortal(portal);
                    entryPoint.setPattern(pattern);
                    entryPoint.setRedirect(redirect);
                    portalEntryPointList.add(entryPoint);
                }
            }
            
            ModelAndView mv = new ModelAndView("portalEdit");
            mv.addObject("portal", portal);
            mv.addObject("portalEntryPointList", portalEntryPointList);
            return mv;
        }
    }

    /**
     * Returns a HTML snippet for a new portal entry point form. 
     * @param request the browser request.
     * @param response the server response.
     * @param index the unique adding index for the portal entry point fields.
     */
    @RolesAllowed( { Role.ROOT })
    @RequestMapping(value = "/bdrs/root/portal/ajaxAddPortalEntryPoint.htm", method = RequestMethod.GET)
    public ModelAndView ajaxAddPortalEntryPoint(HttpServletRequest request, HttpServletResponse response,
            @RequestParam(value="index", required=true) int index) {
       
        ModelAndView mv = new ModelAndView("portalEntryPointRow");
        mv.addObject("portalEntryPoint", new PortalEntryPoint());
        mv.addObject("index", index);
        
        return mv;
    }
    
    /**
     * Tests the specified test URL against all current portals and 
     * the portals and entry points currently on the editing form but not yet 
     * saved. 
     */
    @RolesAllowed( { Role.ROOT })
    @RequestMapping(value = "/bdrs/root/portal/ajaxTestPortalEntryPointPattern.htm", method = RequestMethod.GET)
    public void ajaxTestPortalEntryPointPattern(HttpServletRequest request, HttpServletResponse response,
                                                @RequestParam(value="portalId", required=false, defaultValue="0") int pk,
                                                @RequestParam(value="name", required=true) String name,
                                                @RequestParam(value="urlPrefix", required=false, defaultValue = "") String urlPrefix,
                                                @RequestParam(value="default", required=false, defaultValue="false") boolean isDefault,
                                                @RequestParam(value="active", required=false, defaultValue="true") boolean isActive,
                                                @RequestParam(value="add_portalEntryPoint", required=false) int[] add_portalEntryPointIndexes,
                                                @RequestParam(value="portalEntryPoint_id", required=false) int[] portalEntryPointPks,
                                                @RequestParam(value="testUrl", required=true) String testUrl) throws Exception {

        // A new session is started, and later rollbacked because we will 
        // first perform a 'fake' save of all form data to create the necessary
        // Portal and PortalEntryPoints. Next, this data will be fed into the
        // PortalSelectionFilterMatcher using the session below. When complete
        // the json data will be encoded and the session roll backed to remove
        // the data that was temporarily 'saved' for the purposes of 
        // portal matching.
        Session sesh = sessionFactory.openSession();
        sesh.disableFilter(PortalPersistentImpl.PORTAL_FILTER_PORTALID_PARAMETER_NAME);
        sesh.beginTransaction();
        
        try {
            parsePortalEditForm(sesh, request, pk, name, urlPrefix, isDefault, isActive, add_portalEntryPointIndexes, portalEntryPointPks);
        } catch (IllegalArgumentException e) {
            log.warn("Error while mocking portal save for testing entry point: " + e.getMessage());
        }
        
        PortalMatches matches = portalFilterMatcher.matchEntryPoints(sesh, testUrl);
        
        Map<String, Object> content = new HashMap<String, Object>();
        content.put("defaultPortal", matches.getDefaultPortal().flatten());
        content.put("matchedPortal", matches.getMatchedPortal() == null ? null : matches.getMatchedPortal().flatten());
        content.put("matchedEntryPoint", matches.getMatchedEntryPoint() == null ? null : matches.getMatchedEntryPoint().flatten());
        content.put("testURL", testUrl);
        
        JSONArray invalidEntries = new JSONArray();
        for(PortalEntryPoint invalid : matches.getInvalidPatternList()) {
            invalidEntries.add(invalid.flatten());
        }
        
        content.put("invalidPatternList", invalidEntries);
        
        
        response.setContentType("application/json");
        response.getWriter().write(JSONObject.fromMapToString(content));
        
        sesh.getTransaction().rollback();
        sesh.close();
    }

    @RolesAllowed( { Role.ROOT })
    @RequestMapping(value = "/bdrs/root/portal/ajaxValidatePortalPrefix.htm", method = RequestMethod.GET)
    public void ajaxTestPortalEntryPointPattern(HttpServletRequest request, HttpServletResponse response,
                                                @RequestParam(value="portalId", required=false, defaultValue="0") int pk,
                                                @RequestParam(value="urlPrefix", required=false, defaultValue = "") String urlPrefix) throws Exception {

        boolean valid = prefixValidator.isURLPrefixValid(pk, urlPrefix);

        JSONObject result = new JSONObject();
        result.put("valid", valid);
        if (!valid) {
            result.put("message", "Prefix already in use.");
        }
        writeJson(request, response, result.toJSONString());
    }

    private Portal parsePortalEditForm(Session sesh, HttpServletRequest request, int pk,
            String name, String urlPrefix, boolean isDefault, boolean isActive, int[] add_portalEntryPointIndexes,
            int[] portalEntryPointPks) throws Exception {
        
        Portal portal = portalDAO.getPortal(sesh, pk);
        if(portal == null) {
            portal = new Portal();
        }
        // return an error if we try to create a portal with the same name as another
        Portal namedPortal = portalDAO.getPortalByName(sesh, name);
        if (namedPortal != null && !portal.equals(namedPortal)) {
            throw new IllegalArgumentException("Portal name must be unique.");
        }
        portal.setName(name);

        if (StringUtils.isNotBlank(urlPrefix)) {
            if (!prefixValidator.isURLPrefixValid(portal.getId(), urlPrefix)) {
                throw new IllegalArgumentException("Portal alias must be unique.");
            }
        }

        portal.setUrlPrefix(urlPrefix.trim());
        if(isDefault) {
            // Only one default portal is allowed.
            Portal defaultPortal = portalDAO.getDefaultPortal();
            if(defaultPortal != null) {
                defaultPortal.setDefault(false);
                portalDAO.save(sesh, defaultPortal);
            }
        }
        portal.setDefault(isDefault);
        
        // You cannot change the active state of the portal that you are currently using.
        RequestContext context = getRequestContext();
        if(portal != null && portal.getId() != null && 
                portal.getId().equals(context.getPortal().getId())) {
            if (isActive != portal.isActive()) {
                isActive = portal.isActive();
                context.addMessage(PORTAL_ACTIVE_STATE_CHANGE_DENIED);
            }
        }
        portal.setActive(isActive);
        
        portal = portalDAO.save(sesh, portal);
        
        Map<Integer, PortalEntryPoint> portalEntryPointMap = new HashMap<Integer, PortalEntryPoint>(); 
        for(PortalEntryPoint ep : portalDAO.getPortalEntryPoints(sesh, portal)) {
            portalEntryPointMap.put(ep.getId(), ep);
        }
        
        PortalEntryPoint entryPoint;
        String pattern;
        String redirect;
        
        // Edited Portal Entries First.
        if(portalEntryPointPks != null) {
            for(int entryPk : portalEntryPointPks) {
                entryPoint = portalEntryPointMap.remove(entryPk);
                
                pattern = request.getParameter(String.format(PORTAL_ENTRY_POINT_EDIT_PATTERN_TMPL, entryPk));
                redirect = request.getParameter(String.format(PORTAL_ENTRY_POINT_EDIT_REDIRECT_TMPL, entryPk));
                entryPoint.setPortal(portal);
                entryPoint.setPattern(pattern);
                entryPoint.setRedirect(redirect);
                portalDAO.save(sesh, entryPoint);
            }
        }
        
        if(add_portalEntryPointIndexes != null) {
            for(int index : add_portalEntryPointIndexes) {
                entryPoint = new PortalEntryPoint();
                
                pattern = request.getParameter(String.format(PORTAL_ENTRY_POINT_ADD_PATTERN_TMPL, index));
                redirect = request.getParameter(String.format(PORTAL_ENTRY_POINT_ADD_REDIRECT_TMPL, index));
                
                entryPoint.setPortal(portal);
                entryPoint.setPattern(pattern);
                entryPoint.setRedirect(redirect);
                portalDAO.save(sesh, entryPoint);
            }
        }
        
        // Delete the remaining entry points.
        for(Map.Entry<Integer, PortalEntryPoint> mapEntry : portalEntryPointMap.entrySet()) {
            portalDAO.delete(sesh, mapEntry.getValue());
        }
        return portal;
    }
    
    @RequestMapping(value = "/portal/**")
    public void restfulPortalRequestForward(HttpServletRequest request, 
                                                    HttpServletResponse response) throws ServletException, IOException {
        Pattern pattern = Pattern.compile(PortalSelectionFilterMatcher.RESTFUL_PORTAL_PATTERN_STR);
        Matcher matcher = pattern.matcher(request.getServletPath());
        
        String subServletPath = matcher.replaceFirst("");
        
        String path;
        if(request.getServletPath().equals(subServletPath)) {
            path = HTTPErrorController.NOT_FOUND_URL;
        } else {
            path = "/"+subServletPath;
        }
        
        RequestDispatcher dispatcher = request.getRequestDispatcher(path);
        dispatcher.forward(request, response);
    }
}
