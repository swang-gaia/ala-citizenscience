<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib prefix="sec" uri="http://www.springframework.org/security/tags" %>
<%@ taglib uri="/WEB-INF/cw.tld" prefix="cw" %>

<jsp:useBean id="context" scope="request" type="au.com.gaiaresources.bdrs.servlet.RequestContext"></jsp:useBean>

<h1>${geoMap.name}</h1>
<p><cw:validateHtml html="${geoMap.description}"/></p>

<!-- for now, only allow download of records when the map has no anonymous access -->
<!-- consider adding map metadata to have the option of allowing record downloads -->
<c:if test="${!geoMap.anonymousAccess}">
    <div class="left">
	    <div>
	        <a href="javascript: bdrs.map.downloadRecordsForActiveLayers(bdrs.map.baseMap, 'KML');">
	            Download KML for records
	        </a>
	        <span>&nbsp;|&nbsp;<span/>
	        <a href="javascript: bdrs.map.downloadRecordsForActiveLayers(bdrs.map.baseMap, 'SHAPEFILE');">
	            Download SHP for records
	        </a>
	    </div>
	</div>
</c:if>

<div class="clear"></div>

<div class="map_wrapper" id="map_wrapper">
    <div id="view_base_map" class="defaultmap review_map"></div>
    <div id="geocode" class="geocode"></div>
    <div class="recordCount textright"></div>  
</div>

<div class="clear"></div>

<script type="text/javascript">

    jQuery(window).load(function() {
		
        <c:choose>
	        <c:when test="${geoMap.anonymousAccess}">
                bdrs.map.initBaseMap('view_base_map', {isPublic:true, ajaxFeatureLookup: true, geocode:{selector:'#geocode'}});
	        </c:when>
	        <c:otherwise>
                bdrs.map.initBaseMap('view_base_map', {isPublic:false, ajaxFeatureLookup: true, geocode: { selector: '#geocode' }});
	        </c:otherwise>
	    </c:choose>
        
        var layerArray = new Array();
        <c:forEach items="${assignedLayers}" var="assignedLayer">
        {
            var layer;
			<c:choose>
            <c:when test="${assignedLayer.layer.layerSource == \"SHAPEFILE\" || assignedLayer.layer.layerSource == \"SURVEY_MAPSERVER\"}">
			    var layerOptions = {
					bdrsLayerId: ${assignedLayer.layer.id},
					visible: ${assignedLayer.visible},
					opacity: bdrs.map.DEFAULT_OPACITY,
					fillColor: "${assignedLayer.layer.fillColor}",
                    strokeColor: "${assignedLayer.layer.strokeColor}",
                    strokeWidth: ${assignedLayer.layer.strokeWidth},
                    size: ${assignedLayer.layer.symbolSize},
					upperZoomLimit: ${assignedLayer.upperZoomLimit != null ? assignedLayer.upperZoomLimit : 'null'},
					lowerZoomLimit: ${assignedLayer.lowerZoomLimit != null ? assignedLayer.lowerZoomLimit: 'null'}
				};
				// intentionally don't add this one as mapserver layers use transparent tiles not kml features
				bdrs.map.addMapServerLayer(bdrs.map.baseMap, "${assignedLayer.layer.name}", bdrs.map.getBdrsMapServerUrl(), layerOptions);
            </c:when>
			<c:when test="${assignedLayer.layer.layerSource == \"SURVEY_KML\"}">
			    var layerOptions = {
                    visible: ${assignedLayer.visible},
                    // cluster strategy doesn't work properly for polygons
                    includeClusterStrategy: true,
                    upperZoomLimit: ${assignedLayer.upperZoomLimit != null ? assignedLayer.upperZoomLimit : 'null'},
                    lowerZoomLimit: ${assignedLayer.lowerZoomLimit != null ? assignedLayer.lowerZoomLimit: 'null'}
                };
                layer = bdrs.map.addKmlLayer(bdrs.map.baseMap, "${assignedLayer.layer.name}", "${portalContextPath}/bdrs/map/getLayer.htm?layerPk=${assignedLayer.layer.id}", layerOptions);
			</c:when>
			<c:when test="${assignedLayer.layer.layerSource == \"KML\"}">
                var layerOptions = {
                    visible: ${assignedLayer.visible},
					// cluster strategy doesn't work properly for polygons
                    includeClusterStrategy: false,
                    upperZoomLimit: ${assignedLayer.upperZoomLimit != null ? assignedLayer.upperZoomLimit : 'null'},
                    lowerZoomLimit: ${assignedLayer.lowerZoomLimit != null ? assignedLayer.lowerZoomLimit: 'null'}
                };
				layer = bdrs.map.addKmlLayer(bdrs.map.baseMap, "${assignedLayer.layer.name}", "${portalContextPath}/bdrs/map/getLayer.htm?layerPk=${assignedLayer.layer.id}", layerOptions);
            </c:when>
            </c:choose>
            if (layer) {
                layerArray.push(layer);
                layer.events.register('loadend', layer, function(event) {
        	        bdrs.map.centerMapToLayerExtent(bdrs.map.baseMap, layerArray);
        	        bdrs.map.recordOriginalCenterZoom(bdrs.map.baseMap);
        	    });
            }
        }
        </c:forEach>

        // must perform a default centering for the map tiles to appear
        if (layerArray.length > 0) {
        	bdrs.map.centerMapToLayerExtent(bdrs.map.baseMap, layerArray);
        } else {
            bdrs.map.centerMap(bdrs.map.baseMap, null, 3);
        }
        // Add select for KML stuff
        bdrs.map.addSelectHandler(bdrs.map.baseMap, layerArray);
        
		// In order to force correct map centering in IE7
	    jQuery("#view_base_map").removeClass("defaultmap");
		jQuery("#view_base_map").addClass("defaultmap");
		
        // keep track of the original map center and zoom in case we want to 
        // reset the page later
        bdrs.map.recordOriginalCenterZoom(bdrs.map.baseMap);
        
        var layerSwitcherContainerSelector = '[id^="OpenLayers.Control.LayerSwitcher_"].layersDiv';
        if (jQuery('#OpenLayers_Control_MinimizeDiv')) {
            var layerSwitcherDiv = jQuery(layerSwitcherContainerSelector); 
            
            jQuery(layerSwitcherDiv).find('.dataLayersDiv').find('br').css("clear", "both");
            console.log(jQuery(layerSwitcherDiv).find('.dataLayersDiv').find('br'));
            
        }
    });

</script>
