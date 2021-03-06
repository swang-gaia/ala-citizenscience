package au.com.gaiaresources.bdrs.controller.map;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.LinkedList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import au.com.gaiaresources.bdrs.model.preference.PreferenceDAO;
import junit.framework.Assert;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletResponse;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import au.com.gaiaresources.bdrs.json.JSONArray;
import au.com.gaiaresources.bdrs.json.JSONObject;
import au.com.gaiaresources.bdrs.json.JSONSerializer;
import au.com.gaiaresources.bdrs.model.record.Record;
import au.com.gaiaresources.bdrs.model.record.impl.ScrollableRecordsList;
import au.com.gaiaresources.bdrs.model.user.User;
import au.com.gaiaresources.bdrs.service.web.JsonService;
import au.com.gaiaresources.bdrs.spatial.AbstractShapefileTest;
import au.com.gaiaresources.bdrs.spatial.ShapeFileReader;

public class RecordDownloadWriterTest extends AbstractShapefileTest {
	
    private static final String KML_DESCRIPTION_TAG = "description";
    private static final String SERVER_URL = "http://test.gaiaresources.com.au/test/portal/2";
    
    private Logger log = Logger.getLogger(getClass());
    
    private RecordDownloadWriter downloadWriter;

    @Autowired
    private PreferenceDAO preferenceDAO;
    
    @Before
    public void recordDownloadWriterTestSetup() {
    	downloadWriter = new RecordDownloadWriter(preferenceDAO,
                SERVER_URL, true);
    }
    
    // the export 'no' record case is the same as exporting a template
    @SuppressWarnings("deprecation")
    @Test 
    public void testExportSingleRecord() throws Exception {
        User accessor = owner;
        List<Record> recList = new LinkedList<Record>();
        recList.add(r1);
        
        downloadWriter.write(getRequestContext().getHibernate(), 
                                   request, response, 
                                   new ScrollableRecordsList(recList), 
                                   RecordDownloadFormat.SHAPEFILE, accessor);
        File exportedFile = prepareFile(response.getContentAsByteArray());
        
        ShapeFileReader reader = new ShapeFileReader(exportedFile);
        
        Assert.assertEquals(1, reader.getSurveyIdList().size());
        Assert.assertEquals(survey.getId(), reader.getSurveyIdList().get(0));
        Assert.assertEquals(1, reader.getCensusMethodIdList().size());
        Assert.assertEquals(0, reader.getCensusMethodIdList().get(0).intValue());
        
        assertFeatureCount(reader.getFeatureIterator(), recList.size());

        assertRecord(r1, reader, accessor);
    }
    
    private File prepareFile(byte[] content) throws IOException {
        File file = File.createTempFile("mytestzipfile", "zip");
        InputStream iStream = null; 
        OutputStream oStream = null; 
        try {
            iStream = new ByteArrayInputStream(content);
            oStream = new FileOutputStream(file);
            IOUtils.copy(iStream, oStream);
        } finally {
            if (iStream != null) {
                iStream.close();
            }
            if (oStream != null) {
                oStream.close();
            }
        }
        return file;
    }
    
    @SuppressWarnings("deprecation")
    @Test
    public void testExportMultiSurveyNoCensusMethod() throws Exception {
        User accessor = owner;

        List<Record> recList = new LinkedList<Record>();
        recList.add(r1);
        recList.add(r3);
        
        downloadWriter.write(getRequestContext().getHibernate(),
                                   request, response, 
                                   new ScrollableRecordsList(recList),
                                   RecordDownloadFormat.SHAPEFILE, accessor);
        File exportedFile = prepareFile(response.getContentAsByteArray());
        
        ShapeFileReader reader = new ShapeFileReader(exportedFile, true);
        
        Assert.assertEquals(2, reader.getSurveyIdList().size());
        Assert.assertTrue("should contain survey id : " + survey.getId(), reader.getSurveyIdList().contains(survey.getId()));
        Assert.assertTrue("should contain survey id : " + secondSurvey.getId(), reader.getSurveyIdList().contains(secondSurvey.getId()));
        // no census methods...
        Assert.assertEquals(1, reader.getCensusMethodIdList().size());
        Assert.assertEquals(0, reader.getCensusMethodIdList().get(0).intValue());
        
        assertFeatureCount(reader.getFeatureIterator(), recList.size());
        
        assertRecord(r1, reader, accessor);
        assertRecord(r3, reader, accessor);
    }
    
    @SuppressWarnings("deprecation")
    @Test
    public void testExportMultiSingleSurveyWithNonTaxaCensusMethod() throws Exception {
        User accessor = owner;
        List<Record> recList = new LinkedList<Record>();
        recList.add(r1);
        recList.add(r3);
        recList.add(r4);
        
        downloadWriter.write(getRequestContext().getHibernate(),
                                   request, response, 
                                   new ScrollableRecordsList(recList), 
                                   RecordDownloadFormat.SHAPEFILE, accessor);
        File exportedFile = prepareFile(response.getContentAsByteArray());
        
        ShapeFileReader reader = new ShapeFileReader(exportedFile, true);
        
        Assert.assertEquals(2, reader.getSurveyIdList().size());
        Assert.assertTrue("should contain survey id : " + survey.getId(), reader.getSurveyIdList().contains(survey.getId()));
        Assert.assertTrue("should contain survey id : " + secondSurvey.getId(), reader.getSurveyIdList().contains(secondSurvey.getId()));
        
        // no census methods...
        Assert.assertEquals(1, reader.getCensusMethodIdList().size());
        Assert.assertTrue("should contain censusmethod id : " + cm.getId(), reader.getCensusMethodIdList().contains(cm.getId()));
        
        assertFeatureCount(reader.getFeatureIterator(), recList.size());
        
        assertRecord(r1, reader, accessor);
        assertRecord(r3, reader, accessor);
        assertRecord(r4, reader, accessor);
    }
    
    @SuppressWarnings("deprecation")
    @Test
    public void testExportSingleSurveyMultiCensusMethod() throws Exception {
        User accessor = owner;
        List<Record> recList = new LinkedList<Record>();
        recList.add(r1);
        recList.add(r2);
        recList.add(r4);
        downloadWriter.write(getRequestContext().getHibernate(),
                                   request, response, 
                                   new ScrollableRecordsList(recList), 
                                   RecordDownloadFormat.SHAPEFILE, accessor);
        File exportedFile = prepareFile(response.getContentAsByteArray());
        
        ShapeFileReader reader = new ShapeFileReader(exportedFile, true);
        
        Assert.assertEquals(1, reader.getSurveyIdList().size());
        Assert.assertTrue("should contain survey id : " + survey.getId(), reader.getSurveyIdList().contains(survey.getId()));
        
        // no census methods...
        Assert.assertEquals(2, reader.getCensusMethodIdList().size());
        Assert.assertTrue("should contain censusmethod id : " + taxaCm.getId(), reader.getCensusMethodIdList().contains(taxaCm.getId()));
        Assert.assertTrue("should contain censusmethod id : " + cm.getId(), reader.getCensusMethodIdList().contains(cm.getId()));
        
        assertFeatureCount(reader.getFeatureIterator(), recList.size());
        
        assertRecord(r1, reader, accessor);
        assertRecord(r2, reader, accessor);
        assertRecord(r4, reader, accessor);
    }
    
    @SuppressWarnings("deprecation")
    private void testExportMultiSurveyMultiCensus(User accessor) throws Exception {      
        List<Record> recList = new LinkedList<Record>();
        recList.add(r1);
        recList.add(r2);
        recList.add(r3);
        recList.add(r4);
        downloadWriter.write(getRequestContext().getHibernate(),
                                   request, response, 
                                   new ScrollableRecordsList(recList), 
                                   RecordDownloadFormat.SHAPEFILE, accessor);
        File exportedFile = prepareFile(response.getContentAsByteArray());
        
        ShapeFileReader reader = new ShapeFileReader(exportedFile, true);
        
        Assert.assertEquals(2, reader.getSurveyIdList().size());
        Assert.assertTrue("should contain survey id : " + survey.getId(), reader.getSurveyIdList().contains(survey.getId()));
        Assert.assertTrue("should contain survey id : " + secondSurvey.getId(), reader.getSurveyIdList().contains(secondSurvey.getId()));
        
        // no census methods...
        Assert.assertEquals(2, reader.getCensusMethodIdList().size());
        Assert.assertTrue("should contain censusmethod id : " + taxaCm.getId(), reader.getCensusMethodIdList().contains(taxaCm.getId()));
        Assert.assertTrue("should contain censusmethod id : " + cm.getId(), reader.getCensusMethodIdList().contains(cm.getId()));
        
        assertFeatureCount(reader.getFeatureIterator(), recList.size());
        
        assertRecord(r1, reader, accessor);
        assertRecord(r2, reader, accessor);
        assertRecord(r3, reader, accessor);
        assertRecord(r4, reader, accessor);
    }
    
    @Test
    public void testExportMultiSurveyMultiCensus_ByOwner() throws Exception {
        testExportMultiSurveyMultiCensus(owner);
    }
    
    @Test
    public void testExportMultiSurveyMultiCensus_ByAdmin() throws Exception {
        testExportMultiSurveyMultiCensus(admin);
    }
    
    @Test
    public void testMultiSurveyMultiCensusExport_ByNonOwner() throws Exception {
        testExportMultiSurveyMultiCensus(nonOwner);
    }
    
    // just make sure no exceptions are thrown...
    @SuppressWarnings("deprecation")
    @Test
    public void testNoRecordsToWrite() throws Exception {
        User accessor = owner;
        List<Record> recList = new LinkedList<Record>();
        downloadWriter.write(getRequestContext().getHibernate(),
                                   request, response,
                                   new ScrollableRecordsList(recList),  
                                   RecordDownloadFormat.SHAPEFILE, accessor);
    }
    
    // testing the kml code path. Not a very thorough test I know but the KML stuff is tested indirectly
    @SuppressWarnings("deprecation")
    // through other tests: GeoMapLayerController_GetLayer_Test
    @Test 
    public void testExportSingleRecord_kml() throws Exception {
        User accessor = owner;
        List<Record> recList = new LinkedList<Record>();
        recList.add(r1);
        
        downloadWriter.write(getRequestContext().getHibernate(),
                                   request, response, 
                                   new ScrollableRecordsList(recList),  
                                   RecordDownloadFormat.KML, accessor);
        
        JSONObject obj = extractJsonDescription(response);
        Assert.assertNotNull(obj);
    }
    
    private JSONObject extractJsonDescription(MockHttpServletResponse response) throws ParserConfigurationException, SAXException, IOException {
        JSONObject result = new JSONObject();
        JSONArray items = new JSONArray();
        
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();
        
        Document dom = db.parse(new InputSource(new ByteArrayInputStream(response.getContentAsByteArray())));
        NodeList descList = dom.getElementsByTagName(KML_DESCRIPTION_TAG);
        
        for (int i=0; i<descList.getLength(); ++i) {
            Node n = descList.item(i);
            JSONObject jsonRecord = (JSONObject) JSONSerializer.toJSON(n.getTextContent()); 
            items.add(jsonRecord);
        }
        result.put(JsonService.JSON_KEY_ITEMS, items);
        return result;
    }

    @Override
    protected String getServerURL() {
        return SERVER_URL;
    }
}
