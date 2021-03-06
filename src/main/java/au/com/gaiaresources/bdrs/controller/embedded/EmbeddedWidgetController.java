package au.com.gaiaresources.bdrs.controller.embedded;

import au.com.gaiaresources.bdrs.controller.AbstractController;
import au.com.gaiaresources.bdrs.db.impl.PagedQueryResult;
import au.com.gaiaresources.bdrs.model.file.ManagedFile;
import au.com.gaiaresources.bdrs.model.file.ManagedFileDAO;
import au.com.gaiaresources.bdrs.model.record.Record;
import au.com.gaiaresources.bdrs.model.record.RecordDAO;
import au.com.gaiaresources.bdrs.model.showcase.Gallery;
import au.com.gaiaresources.bdrs.model.showcase.GalleryDAO;
import au.com.gaiaresources.bdrs.model.survey.SurveyDAO;
import au.com.gaiaresources.bdrs.model.user.UserDAO;
import au.com.gaiaresources.bdrs.security.Role;
import au.com.gaiaresources.bdrs.service.template.TemplateService;
import au.com.gaiaresources.bdrs.servlet.view.PortalRedirectView;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import javax.annotation.security.RolesAllowed;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

@Controller
public class EmbeddedWidgetController extends AbstractController {

    public static final int DEFAULT_WIDTH = 250;
    public static final int DEFAULT_HEIGHT = 300;

    @SuppressWarnings("unused")
    private Logger log = Logger.getLogger(getClass());

    @Autowired
    private RecordDAO recordDAO;
    @Autowired
    private UserDAO userDAO;
    @Autowired
    private SurveyDAO surveyDAO;
    @Autowired
    private GalleryDAO galleryDAO;
    @Autowired
    private ManagedFileDAO mfDAO;
    @Autowired
    private TemplateService templateService;

    /**
     * Shows a static page that describes what Embedded Widgets are and how to use them
     * with links to the embedded widgets and image gallery pages.
     */
    @RolesAllowed({ Role.ROOT, Role.ADMIN, Role.SUPERVISOR, Role.POWERUSER })
    @RequestMapping(value="/bdrs/admin/embeddedWidgets.htm", method=RequestMethod.GET) 
    public ModelAndView renderEmbeddedWidgetsPage() {
        return new ModelAndView("embeddedWidgets");
    }
    
    @RolesAllowed({Role.ROOT, Role.ADMIN, Role.SUPERVISOR, Role.POWERUSER})
    @RequestMapping(value = "/bdrs/public/embedded/widgetBuilder.htm", method = RequestMethod.GET)
    public ModelAndView widgetBuilder(HttpServletRequest request,
            HttpServletResponse response) {

        ModelAndView mv = new ModelAndView("widgetBuilder");
        
        PagedQueryResult<Gallery> gallerySearchResult = galleryDAO.search(null, null, null); // return all galleries
        mv.addObject("galleryList", gallerySearchResult.getList());
        mv.addObject("domain", request.getServerName());
        mv.addObject("port", request.getServerPort());
        return mv;
    }

    @RequestMapping(value = "/bdrs/public/embedded/bdrs-embed.js.htm", method = RequestMethod.GET)
    public ModelAndView generateEmbeddedJS(
            HttpServletRequest request,
            HttpServletResponse response,
            @RequestParam(value = "domain", required = false) String domain,
            @RequestParam(value = "port", required = false) String port,
            @RequestParam(value = "contextPath", required = false) String contextPath,
            @RequestParam(value = "targetId", required = false) String targetId,
            @RequestParam(value = "width", required = false) String widthStr,
            @RequestParam(value = "height", required = false) String heightStr,
            @RequestParam(value = "feature", required = false) String featureStr) {

        domain = domain == null ? request.getServerName() : domain;
        port = port == null ? String.valueOf(request.getServerPort()) : port;

        int width;
        try {
            width = Integer.parseInt(widthStr);
        } catch (NumberFormatException nfe) {
            width = DEFAULT_WIDTH;
        }

        int height;
        try {
            height = Integer.parseInt(heightStr);
        } catch (NumberFormatException nfe) {
            height = DEFAULT_HEIGHT;
        }
        
        Map<String, Object> params = new HashMap<String, Object>();
        params.putAll(toSimpleParameterMap(request.getParameterMap()));
        params.put("domain", domain);
        params.put("port", port);
        params.put("contextPath", contextPath);
        params.put("targetId", targetId);
        params.put("height", height);
        params.put("width", width);
        params.put("feature", featureStr);
        params.put("showFooter", request.getParameterMap().containsKey("showFooter"));

        ModelAndView mv = new ModelAndView("bdrs_embed_js");
        mv.addAllObjects(params);
        mv.addObject("paramMap", params);
        
        response.setContentType("text/javascript");

        return mv;
    }

    @RequestMapping(value = "/bdrs/public/embedded/bdrs-embed.css.htm", method = RequestMethod.GET)
    public void generateEmbeddedJS(HttpServletRequest request,
            HttpServletResponse response) {
        response.setContentType("text/css");
        
        Map<String, Object> params = new HashMap<String, Object>();
        
        Enumeration<String> en = request.getParameterNames();
        while (en.hasMoreElements()) {
            String name = en.nextElement();
            params.put(name, request.getParameter(name));
        }
        
        try {
            response.getWriter().write(templateService.transformToString("bdrs-embed.vm", getClass(), params));    
        } catch (IOException ioe) {
            log.error("Could not write bdrs-embed.css", ioe);
        }
    }

    @SuppressWarnings("unchecked")
    @RequestMapping(value = "/bdrs/public/embedded/redirect.htm", method = RequestMethod.GET)
    public ModelAndView edit(HttpServletRequest request,
            HttpServletResponse response,
            @RequestParam(value = "feature", required = false) String featureStr) {

        EmbeddedFeature feature = featureStr == null ? EmbeddedFeature.LATEST_STATISTICS
                : EmbeddedFeature.valueOf(featureStr);

        ModelAndView mv = new ModelAndView(
                new PortalRedirectView(
                        String.format("/bdrs/public/embedded/%s.htm", feature.toString().toLowerCase()),
                        true));
        mv.addAllObjects(request.getParameterMap());
        return mv;
    }

    @RequestMapping(value = "/bdrs/public/embedded/latest_statistics.htm", method = RequestMethod.GET)
    public ModelAndView latest_statistics(HttpServletRequest request,
            HttpServletResponse response) {

        Record latestRecord = recordDAO.getLatestRecord();

        Map<String, Object> params = new HashMap<String, Object>();
        params.putAll(toSimpleParameterMap(request.getParameterMap()));
        params.put("recordCount", recordDAO.countAllRecords());
        params.put("latestRecord", latestRecord == null ? "" : latestRecord);
        params.put("uniqueSpeciesCount", recordDAO.countUniqueSpecies());
        params.put("userCount", userDAO.countUsers());
        params.put("publicSurveys", surveyDAO.getActivePublicSurveys(true));

        ModelAndView mv = new ModelAndView("latest_statistics");
        mv.addAllObjects(params);
        mv.addObject("paramMap", params);
        
        response.setContentType("text/javascript");

        return mv;
    }
    
    @RequestMapping(value = "/bdrs/public/embedded/image_slideshow.htm", method = RequestMethod.GET)
    public ModelAndView image_slideshow(HttpServletRequest request, HttpServletResponse response,
            @RequestParam(value="galleryId", required=true) int galleryId) {
        Map<String, Object> params = new HashMap<String, Object>();
        params.putAll(toSimpleParameterMap(request.getParameterMap()));
        
        Gallery gallery = galleryDAO.get(galleryId);
        
        Map<String, ManagedFile> mfMap = new HashMap<String, ManagedFile>();
        for (String uuid : gallery.getFileUUIDS()) {
            ManagedFile file = mfDAO.getManagedFile(uuid);
            mfMap.put(uuid, file);
        }

        params.put("gallery", gallery);
        params.put("mfMap", mfMap);

        ModelAndView mv = new ModelAndView("image_slideshow");
        mv.addAllObjects(params);
        mv.addObject("paramMap", params);
        
        response.setContentType("text/javascript");
        return mv;
    }

    private Map<String, String> toSimpleParameterMap(Map requestParameterMap) {
        Map<String, String> simple = new HashMap<String, String>(
                requestParameterMap.size());
        for (Map.Entry<String, String[]> entry : ((Map<String, String[]>) requestParameterMap).entrySet()) {
            if (entry.getValue().length > 0) {
                simple.put(entry.getKey(), entry.getValue()[0]);
            }
        }
        return simple;
    }
}
