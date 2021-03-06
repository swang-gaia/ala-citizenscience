package au.com.gaiaresources.bdrs.controller.attribute;

import au.com.gaiaresources.bdrs.controller.AbstractController;
import au.com.gaiaresources.bdrs.controller.webservice.JqGridDataBuilder;
import au.com.gaiaresources.bdrs.controller.webservice.JqGridDataHelper;
import au.com.gaiaresources.bdrs.controller.webservice.JqGridDataRow;
import au.com.gaiaresources.bdrs.db.impl.PagedQueryResult;
import au.com.gaiaresources.bdrs.db.impl.PaginationFilter;
import au.com.gaiaresources.bdrs.json.JSONArray;
import au.com.gaiaresources.bdrs.model.metadata.MetadataDAO;
import au.com.gaiaresources.bdrs.model.method.CensusMethod;
import au.com.gaiaresources.bdrs.model.method.CensusMethodDAO;
import au.com.gaiaresources.bdrs.model.method.Taxonomic;
import au.com.gaiaresources.bdrs.model.survey.Survey;
import au.com.gaiaresources.bdrs.model.survey.SurveyDAO;
import au.com.gaiaresources.bdrs.model.taxa.Attribute;
import au.com.gaiaresources.bdrs.model.taxa.AttributeDAO;
import au.com.gaiaresources.bdrs.security.Role;
import au.com.gaiaresources.bdrs.servlet.BdrsWebConstants;
import au.com.gaiaresources.bdrs.servlet.view.PortalRedirectView;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import javax.annotation.security.RolesAllowed;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@RolesAllowed( {Role.POWERUSER,Role.SUPERVISOR,Role.ADMIN} )
@Controller
public class CensusMethodController extends AbstractController {
    
    /**
     * url : get census method for survey
     */
    public static final String GET_CENSUS_METHOD_FOR_SURVEY_URL = "/bdrs/user/censusMethod/getSurveyCensusMethods.htm";
    /**
     * url : edit census method
     */
    public static final String EDIT_URL = "/bdrs/admin/censusMethod/edit.htm";
    /**
     * url : search census methods
     */
    public static final String CENSUS_METHOD_SEARCH_URL="/bdrs/admin/censusMethod/search.htm";
    /**
     * url : get census method table row via ajax
     */
    public static final String CENSUS_METHOD_ADD_AJAX_ROW_URL="/bdrs/admin/censusMethod/ajaxAddSubCensusMethod.htm";
    /**
     * Request parameter - survey ID
     */
    public static final String PARAM_SURVEY_ID = BdrsWebConstants.PARAM_SURVEY_ID;
    /**
     * Request parameter - draw point control enabled for census method
     */
    public static final String PARAM_DRAW_POINT_ENABLED = "drawPoint";
    /**
     * Request parameter - draw line control enabled for census method
     */
    public static final String PARAM_DRAW_LINE_ENABLED = "drawLine";
    /**
     * Request parameter - draw polygon control enabled for census method
     */
    public static final String PARAM_DRAW_POLYGON_ENABLED = "drawPolygon";
    
    @Autowired
    SurveyDAO surveyDAO;
    @Autowired
    CensusMethodDAO cmDAO;
    @Autowired
    AttributeDAO attributeDAO;
    @Autowired
    MetadataDAO metadataDAO;
    
    private AttributeFormFieldFactory formFieldFactory = new AttributeFormFieldFactory();
    
    @RolesAllowed({Role.ADMIN, Role.ROOT, Role.POWERUSER, Role.SUPERVISOR})
    @RequestMapping(value = "/bdrs/admin/censusMethod/listing.htm", method = RequestMethod.GET)
    public ModelAndView listing(HttpServletRequest request, HttpServletResponse response) {
        
        ModelAndView mv = new ModelAndView("censusMethodList");
        return mv;
    }
    
    @RolesAllowed({Role.ADMIN, Role.ROOT, Role.POWERUSER, Role.SUPERVISOR})
    @RequestMapping(value = EDIT_URL, method = RequestMethod.GET)
    public ModelAndView openEdit(HttpServletRequest request, HttpServletResponse response,
            @RequestParam(value=BdrsWebConstants.PARAM_CENSUS_METHOD_ID, defaultValue="0", required=false) Integer pk) {
        CensusMethod cm;
        if(pk == 0) {
            cm = new CensusMethod();
        } else {
            cm = cmDAO.get(pk);
        }
        
        List<AttributeFormField> attributeFormFieldList = new ArrayList<AttributeFormField>();
        for(Attribute attr : cm.getAttributes()) {
            AttributeFormField formField = formFieldFactory.createAttributeFormField(attributeDAO, attr);
            attributeFormFieldList.add(formField);
        }
            
        Collections.sort(attributeFormFieldList);
        
        ModelAndView mv = new ModelAndView("censusMethodEdit");
        mv.addObject("censusMethod", cm);
        mv.addObject("attributeFormFieldList", attributeFormFieldList);
        return mv;
    }
    
    @SuppressWarnings("unchecked")
    @RolesAllowed({Role.ADMIN, Role.ROOT, Role.POWERUSER, Role.SUPERVISOR})
    @RequestMapping(value = EDIT_URL, method = RequestMethod.POST)
    public ModelAndView save(HttpServletRequest request, HttpServletResponse response,
            @RequestParam(value="censusMethodName", required=true) String name,
            @RequestParam(value="taxonomic", required=false, defaultValue="NONTAXONOMIC") String taxonomic,
            @RequestParam(value="type", required=true, defaultValue="") String type,
            @RequestParam(value="description", required=true, defaultValue="") String description,
            @RequestParam(value=BdrsWebConstants.PARAM_CENSUS_METHOD_ID, defaultValue="0", required=false) Integer pk,
            @RequestParam(value="attribute", required=false) int[] attributePkArray,
            @RequestParam(value="add_attribute", required=false) int[] attributeIndexArray,
            @RequestParam(value="childCensusMethod", required=false) int[] childCensusMethodList,
            @RequestParam(value=PARAM_DRAW_POINT_ENABLED, defaultValue="false") boolean drawPoint,
            @RequestParam(value=PARAM_DRAW_LINE_ENABLED, defaultValue="false") boolean drawLine,
            @RequestParam(value=PARAM_DRAW_POLYGON_ENABLED, defaultValue="false") boolean drawPolygon) {
        
        CensusMethod cm;
        if(pk == 0) {
            cm = new CensusMethod();
        } else {
            cm = cmDAO.get(pk);
        }
        
        cm.setName(name);
        cm.setType(type);
        cm.setDescription(description);
        cm.setTaxonomic(Taxonomic.valueOf(taxonomic));
        cm.setDrawPointEnabled(drawPoint, metadataDAO);
        cm.setDrawLineEnabled(drawLine, metadataDAO);
        cm.setDrawPolygonEnabled(drawPolygon, metadataDAO);
        
        // -- Attributes --
        List<Attribute> attributeList = new ArrayList<Attribute>();

        // Attribute Updates
        // All attributes have a hidden input called 'attribute'
        Attribute attr;
        if(attributePkArray != null) {
            for(int attributePk : attributePkArray) {
                String attrName = request.getParameter(String.format("name_"+attributePk));
                if(attrName != null && !attrName.isEmpty()) {
                    attr = attributeDAO.get(attributePk);
                    AttributeFormField formField = formFieldFactory.createAttributeFormField(attributeDAO, cmDAO, attr, request.getParameterMap());
                    attr = (Attribute) formField.save();
                    attributeList.add(attr);
                }
            }
        }

        // Create new Attributes
        if(attributeIndexArray != null) {
            for(int index : attributeIndexArray) {
                String attrName = request.getParameter(String.format("add_name_"+index));
                if(attrName != null && !attrName.isEmpty()) {
                    AttributeFormField formField = formFieldFactory.createAttributeFormField(attributeDAO, cmDAO, index, request.getParameterMap());
                    attributeList.add((Attribute)formField.save());
                }
            }
        }
        cm.setAttributes(attributeList);
        
        // no child protection!
        List<CensusMethod> childList = new ArrayList<CensusMethod>();
        if (childCensusMethodList != null) {
            for (int cmId : childCensusMethodList) {
                CensusMethod child = cmDAO.get(cmId);
                childList.add(child);
            }
        }
        cm.setCensusMethods(childList);
        
        cmDAO.save(cm);
        
        return new ModelAndView(new PortalRedirectView("/bdrs/admin/censusMethod/listing.htm", true));
    }
    
    /**
     * Jqgrid compliant census method search
     * 
     * @param request - HttpServletRequest 
     * @param response - HttpServletResponse
     * @param name - Name of census method
     * @param taxonomic - Taxonomic setting of census method
     * @param surveyId - Gets the census methods assigned to this survey ID.
     * @throws Exception
     */
    @RolesAllowed({Role.ADMIN, Role.ROOT, Role.POWERUSER, Role.SUPERVISOR})
    @RequestMapping(value=CENSUS_METHOD_SEARCH_URL, method = RequestMethod.GET)
    public void searchService(HttpServletRequest request, HttpServletResponse response,
            @RequestParam(value="name", required=false) String name,
            @RequestParam(value="taxonomic", required=false) Boolean taxonomic,
            @RequestParam(value=BdrsWebConstants.PARAM_SURVEY_ID, required=false) Integer surveyId) throws Exception {
        JqGridDataHelper jqGridHelper = new JqGridDataHelper(request);
        PaginationFilter filter = jqGridHelper.createFilter(request);
        
        PagedQueryResult<CensusMethod> queryResult = cmDAO.search(filter, name, surveyId);
        
        JqGridDataBuilder builder = new JqGridDataBuilder(jqGridHelper.getMaxPerPage(), queryResult.getCount(), jqGridHelper.getRequestedPage());

        if (queryResult.getCount() > 0) {
            for (CensusMethod cm : queryResult.getList()) {
                JqGridDataRow row = new JqGridDataRow(cm.getId());
                row
                .addValue("name", cm.getName())
                .addValue("taxonomic", cm.getTaxonomic().getName());
                builder.addRow(row);
            }
        }
        response.setContentType("application/json");
        response.getWriter().write(builder.toJson());
    }
    
    @RolesAllowed( {Role.USER,Role.POWERUSER,Role.SUPERVISOR,Role.ADMIN} )
    @RequestMapping(value=GET_CENSUS_METHOD_FOR_SURVEY_URL, method = RequestMethod.GET)
    public void getSurveyCensusMethods(HttpServletRequest request, HttpServletResponse response,
            @RequestParam(value=PARAM_SURVEY_ID, required=false) Integer surveyId) throws Exception {
        
        if (surveyId == null) {
            // return empty json object
            this.writeJson(request, response, "[]");
            return;
        }
        Survey survey = surveyDAO.get(surveyId);
        if (survey == null) {
            // return empty json object
            this.writeJson(request, response, "[]");
            return;
        }
        List<CensusMethod> censusMethodList = survey.getCensusMethods();
        JSONArray array = new JSONArray();
        
        if (survey.isDefaultCensusMethodProvided()) {
         // cover the default case...
            CensusMethod defaultCensusMethod = new CensusMethod();
            defaultCensusMethod.setId(0);
            defaultCensusMethod.setName(CensusMethod.DEFAULT_NAME);
            array.add(defaultCensusMethod.flatten());
        }
        if (censusMethodList != null) {
            for(CensusMethod cm : censusMethodList) {
                array.add(cm.flatten());
            }
        }
        writeJson(request, response, array.toString());
    }
    
    // AL - I'm doing it this way to use the tile as a template since we have no javascript templating at the moment and 
    // it keeps it consistent with the rest of the form. Yes I know it's inefficient.
    @RolesAllowed({Role.ADMIN, Role.ROOT, Role.POWERUSER, Role.SUPERVISOR})
    @RequestMapping(value=CENSUS_METHOD_ADD_AJAX_ROW_URL, method = RequestMethod.GET)
    public ModelAndView addSubCensusMethod(HttpServletRequest request, HttpServletResponse response,
            @RequestParam(value="id", required=true) Integer id) {
        CensusMethod cm = cmDAO.get(id);
        ModelAndView mv = new ModelAndView("censusMethodEditRow");
        mv.addObject("id", cm.getId());
        mv.addObject("name", cm.getName());
        mv.addObject("taxonomic", cm.getTaxonomic());
        return mv;
    }
}
