package au.com.gaiaresources.bdrs.controller.map;

import au.com.gaiaresources.bdrs.controller.AbstractController;
import au.com.gaiaresources.bdrs.kml.BDRSKMLWriter;
import au.com.gaiaresources.bdrs.model.preference.PreferenceDAO;
import au.com.gaiaresources.bdrs.model.record.RecordDAO;
import au.com.gaiaresources.bdrs.model.record.ScrollableRecords;
import au.com.gaiaresources.bdrs.model.user.User;
import au.com.gaiaresources.bdrs.security.Role;
import org.apache.log4j.Logger;
import org.hibernate.FlushMode;
import org.hibernate.Session;
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
import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;


@Controller
public class RecordMapController extends AbstractController {

    private Logger log = Logger.getLogger(getClass());

    @Autowired
    private RecordDAO recordDAO;
    @Autowired
    private PreferenceDAO preferenceDAO;

    public RecordMapController() {
        super();
    }

    @RolesAllowed({Role.ADMIN, Role.ROOT, Role.POWERUSER, Role.SUPERVISOR, Role.USER})
    @RequestMapping(value = "/map/recordTracker.htm", method = RequestMethod.GET)
    public ModelAndView showrecordTracker(HttpServletRequest request,
            HttpServletResponse response) throws UnsupportedEncodingException {

        boolean showDate = false;
        String showDateStr = request.getParameter("show_date");
        if(showDateStr != null){
            showDate = Boolean.parseBoolean(showDateStr);
        }

        Set<Date> recordDates = new TreeSet<Date>();
        if(showDate) {
            List<Date> dateList = recordDAO.getRecordDatesByScientificNameSearch(request.getParameter("species"));
            Calendar cal = new GregorianCalendar();
            for(Date d : dateList) {

                cal.clear();
                cal.setTime(d);
                cal.set(Calendar.HOUR_OF_DAY, 0);
                cal.clear(Calendar.MINUTE);
                cal.clear(Calendar.SECOND);
                cal.clear(Calendar.MILLISECOND);
                recordDates.add(cal.getTime());
            }
        }

        ModelAndView mv = new ModelAndView("recordTracker");
        mv.addObject("recordDateList", new ArrayList<Date>(recordDates));
        return mv;
    }

    @RolesAllowed({Role.ADMIN, Role.ROOT, Role.POWERUSER, Role.SUPERVISOR, Role.USER})
    @RequestMapping(value = "/map/addRecordBaseMapLayer.htm", method = RequestMethod.GET)
    public void addRecordBaseMapLayer(HttpServletRequest request,
                                      HttpServletResponse response,
                                      @RequestParam(value="ident", defaultValue="") String ident,
                                      @RequestParam(value="species", defaultValue="") String speciesScientificNameSearch,
                                      @RequestParam(value="user", defaultValue="0") int userPk,
                                      @RequestParam(value="group", defaultValue="0") int groupPk,
                                      @RequestParam(value="survey", defaultValue="0") int surveyPk,
                                      @RequestParam(value="taxon_group", defaultValue="0") int taxonGroupPk,
                                      @RequestParam(value="date_start", defaultValue="01 Jan 1970") Date startDate,
                                      @RequestParam(value="date_end", defaultValue="01 Jan 9999") Date endDate,
                                      @RequestParam(value="limit", defaultValue="300") int limit,
                                      @RequestParam(value="downloadFormat", defaultValue="KML") String downloadFormat)
        throws Exception {
        
        // Protect from unauthorized access
        User currentUser = getRequestContext().getUser();
        if (currentUser == null || currentUser.getId() == null) {
            // user not logged in. cannot access 'my sightings'
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            return;
        } else {
            // if not an admin but the user is requesting records of someone who is not them...
            if (!currentUser.isAdmin() && userPk != currentUser.getId()) {
                log.warn("User has attempted unauthorized access to all records. user id : "
                        + currentUser.getId());
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                return;
            }
        }
        Session sesh = getRequestContext().getHibernate(); 
        sesh.setFlushMode(FlushMode.MANUAL);
        RecordDownloadFormat format = RecordDownloadFormat.valueOf(downloadFormat);
        ScrollableRecords sc = recordDAO.getScrollableRecords(currentUser, groupPk, 
                                                              surveyPk, 
                                                              taxonGroupPk, 
                                                              startDate, 
                                                              endDate, 
                                                              speciesScientificNameSearch,
                                                              1, limit);

        new RecordDownloadWriter(preferenceDAO, getRequestContext().getServerURL(), false)
                .write(sesh, request, response, sc, format, currentUser);
    }

    @RequestMapping(value = BDRSKMLWriter.GET_RECORD_PLACEMARK_PNG_URL, method = RequestMethod.GET)
    public void renderRecordPlacemark(HttpServletRequest request, HttpServletResponse response)
        throws IOException {
        BufferedImage img = new BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = (Graphics2D)img.getGraphics();
        Color borderColor = new Color(238,153,0);

        if(request.getParameter("color") != null){
            int color = Integer.parseInt(request.getParameter("color"), 16);
            borderColor = new Color(color);
        }
        Color fillColor  = new Color(borderColor.getRed(), borderColor.getGreen(), borderColor.getBlue(), 160);

        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(fillColor);
        g2.fillOval(12,12,8,8);

        g2.setColor(borderColor);
        g2.setStroke(new BasicStroke(2.0f));
        g2.drawOval(12,12,8,8);

        response.setContentType("image/png");
        ImageIO.write(img, "png", response.getOutputStream());
    }

    @InitBinder
    public void initBinder(HttpServletRequest request, ServletRequestDataBinder binder) {
        DateFormat dateFormat = new SimpleDateFormat("dd MMM yyyy");
        dateFormat.setLenient(false);
        binder.registerCustomEditor(Date.class, new CustomDateEditor(
                dateFormat, true));
    }
}
