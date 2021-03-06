
/*global jQuery: false OpenLayers: false */

/**
 * @fileoverview BDRS Scripts. Depends on jQuery 1.4.2+.
 * @author Gaia Resources
 */
if (window.bdrs === undefined) {
    window.bdrs = {};
}

/**
 * Create an empty debugging console if none exists (stops crash on Firefox
 */
if (window.console === undefined) {
    window.console = {};
}
if (window.console.log === undefined) {
    window.console.log = function(){
        //TODO if we need logging for non debug browsers (ie firefox without firebug)
    };
}

bdrs.underDev = function() {
    alert('This is still under development');
};

// OpenLayers customizations
// *********************************************************************
// if open layers is loaded in the javascript vm...
if (window.OpenLayers !== undefined) {
    // We had a problem where for a dataset with > 10000 points, the map was centering
    // on a single point after the KML was loaded.
    // The problem was caused by clustering. The data set had about 10000 points in a small area.
    // The default map zoom combined caused all of these 10000 points to be placed into a single clustered point.
    // Thus when zooming to the layer extents, we zoomed in on a single point.
    // Change the Vector layer prototype so we calculate the data
    // extent by taking into account the content of clustered features.
    OpenLayers.Layer.Vector.prototype.getDataExtent = function () {

        var maxExtent = null;
        var features = this.features;
        if(features && (features.length > 0)) {
            maxExtent = new OpenLayers.Bounds();
            var geometry = null;
            for(var i=0, len=features.length; i<len; i++) {
                geometry = features[i].geometry;
                if (geometry) {
                    maxExtent.extend(geometry.getBounds());
                }
                // begin cluster loop:
                // calculate the extent of the layer in non clustered features...
                // the rest of this js function is the same as the original
                if (features[i].cluster && (features[i].cluster.length > 0)) {
                    for (var clusterIdx=0; clusterIdx<features[i].cluster.length; ++clusterIdx) {
                       geometry = features[i].cluster[clusterIdx].geometry;
                       if (geometry) {
                          maxExtent.extend(geometry.getBounds());
                       }
                    }
                }
                // end of cluster loop - the rest of this js function is the same as
                // the original
            }
        }
        return maxExtent;
    };
}
// OpenLayers customizations end
// *********************************************************************

/**
 * Gets query parameter from the url
 * @param parameter name
 * @return decoded parameter
 */
bdrs.getParameterByName = function(name){
    name = name.replace(/[\[]/, "\\\[").replace(/[\]]/, "\\\]");
    var regexS = "[\\?&]" + name + "=([^&#]*)";
    var regex = new RegExp(regexS);
    var results = regex.exec(window.location.href);
    if (results === null)
        return null;
    else
        return decodeURIComponent(results[1].replace(/\+/g, " "));
};

bdrs.contextPath = "";
bdrs.portalContextPath = "";
bdrs.ident = "";
bdrs.dateFormat = 'dd M yy';

bdrs.monthNames = ["Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"];

if (bdrs.map === undefined) {
    bdrs.map = {};
}
bdrs.map.SHADOW_Z_INDEX = 10;
bdrs.map.MARKER_Z_INDEX = 11;

// Selected features are those that get highlighted when the map is rendered
// For example, the newly added record.
bdrs.map.SELECTED_FEATURE_STROKE_COLOR = '#FF0000';
bdrs.map.SELECTED_FEATURE_FILL_COLOR = '#FF0000';

// This is the style of every non-special record.
bdrs.map.NORMAL_FEATURE_STROKE_COLOR = '#EE9900';
bdrs.map.NORMAL_FEATURE_FILL_COLOR = '#EE9900';

// Picked features are those that the user has clicked on with the mouse.
bdrs.map.PICKED_FEATURE_STROKE_COLOR = '#66ccff';
bdrs.map.PICKED_FEATURE_FILL_COLOR = '#3399ff';

// Used by the enlarge map and shrink map functionality.
bdrs.map.ENLARGE_MAP_LABEL = 'Enlarge Map';
bdrs.map.SHRINK_MAP_LABEL = 'Shrink Map (esc)';
bdrs.map.ENLARGE_MAP_CLASS = 'review_map_fullscreen';
bdrs.map.SHRINK_MAP_CLASS = 'review_map';

// Scroll Zooming
bdrs.map.SCROLL_ZOOM_COOKIE = 'cookie.map.zoomscroll';
bdrs.map.SCROLL_ZOOM_DEFAULT = 'true';
bdrs.map.SCROLL_ZOOM_ENABLED_VALUE = 'true';

// Used by the hide/show map functionality
bdrs.map.HIDE_MAP_LABEL = 'Hide Map';
bdrs.map.SHOW_MAP_LABEL = 'Show Map';

bdrs.isIE7 = function() {
    return ($.browser.msie  && parseInt($.browser.version) == 7);
};

// Change jqGrid defaults
jQuery.extend(jQuery.jgrid.defaults,{emptyrecords: "Nothing to display"});

// Add jquery function for div overflow detection.
jQuery.fn.HasScrollBar = function() {
    //note: clientHeight= height of holder
    //scrollHeight= we have content till this height
    var _elm = $(this)[0];
    var _hasScrollBar = false;
    if ((_elm.clientHeight < _elm.scrollHeight) || (_elm.clientWidth < _elm.scrollWidth)) {
        _hasScrollBar = true;
    }
    return _hasScrollBar;
};

// This can be set in the theme custom javascript in which
// case we don't want to override it.
if (bdrs.MODAL_DIALOG_Z_INDEX === undefined) {
    bdrs.MODAL_DIALOG_Z_INDEX = 4001;
}

if (bdrs.map.DEFAULT_OPACITY === undefined) {
    bdrs.map.DEFAULT_OPACITY = 0.5;
}

if (bdrs.map.DEFAULT_HIGHLIGHT_COLOR === undefined) {
    bdrs.map.DEFAULT_HIGHLIGHT_COLOR = "#FF0000";
}

// Dynamic post helper
bdrs.postWith = function(to, p){
    var myForm = document.createElement("form");
    myForm.method = "post";
    myForm.action = to;
    for (var k in p) {
        if (jQuery.isArray(p[k])) {
            var myArray = p[k];
            for (var index=0; index<myArray.length; ++index) {
                var myInput = document.createElement("input");
                myInput.setAttribute("name", k);
                myInput.setAttribute("value", myArray[index]);
                myForm.appendChild(myInput);
            }
        } else {
            var myInput = document.createElement("input");
            myInput.setAttribute("name", k);
            myInput.setAttribute("value", p[k]);
            myForm.appendChild(myInput);
        }
    }
    document.body.appendChild(myForm);
    myForm.submit();
    document.body.removeChild(myForm);
};

// ajax post helper
bdrs.ajaxPostWith = function(to, p, callback) {
    jQuery.ajax({
        url: to,
        type: "POST",
        data: p,
        success: callback,
        traditional: true
    });
};

// form post parameter builder
bdrs.serializeObject = function(formSelector, encodeParameters) {
   var o = {};
   var a = jQuery(formSelector).serializeArray();
   jQuery.each(a, function() {
       var value = this.value;
       if (encodeParameters) {
    	   value = encodeURIComponent(value);
       }
	   if (o[this.name]) {
           if (!o[this.name].push) {
               o[this.name] = [o[this.name]];
           }

           o[this.name].push(value || '');
       } else {
           o[this.name] = value || '';
       }
   });
   return o;
};

// JQGrid helper
bdrs.JqGrid = function(gridSelector, baseUrl, baseQueryString){
    this.baseUrl = baseUrl;
    this.baseQueryString = baseQueryString;

    this.getSelected = function(){
        var grid = jQuery(gridSelector);
        if (grid.getGridParam("multiselect")) {
            return jQuery(gridSelector).getGridParam('selarrrow');
        }
        else {
            return jQuery(gridSelector).getGridParam('selrow');
        }
    };

    this.getRowData = function(rowId){
        var grid = jQuery(gridSelector);
        var selected = this.getSelected();
        return grid.jqGrid('getRowData', rowId);
    };

    this.setQueryString = function(f){
        this.queryString = f;
        jQuery(gridSelector).jqGrid("setGridParam", {
            url: this.createUrl()
        });
    };

    this.setBaseQueryString = function(f){
        this.baseQueryString = f;
        jQuery(gridSelector).jqGrid("setGridParam", {
            url: this.createUrl()
        });
    };

    this.createQueryString = function(){
        if (this.baseQueryString && !this.queryString) {
            return this.baseQueryString;
        }
        if (this.baseQueryString && this.queryString) {
            return this.baseQueryString + '&' + this.queryString;
        }
        if (!this.baseQueryString && this.queryString) {
            return this.queryString;
        }
        // no query string to return...
        return null;
    };

    this.createUrl = function(){
        var q = this.createQueryString();
        if (q) {
            return baseUrl + "?" + this.createQueryString();
        }
        else {
            return baseUrl;
        }
    };

    this.reload = function(){
        jQuery(gridSelector).trigger("reloadGrid");
    };
};

// some constants for mapping
bdrs.map.control = {
    DRAW_POINT: "drawPoint",
    DRAW_LINE: "drawLine",
    DRAW_POLYGON: "drawPolygon",
    DRAG_FEATURE: "dragFeature",
    MODIFY_FEATURE: "modifyFeature"
};

/**
 * Falling back to a default when we use open layers to auto zoom
 * to a single point.
 * Using 16 at the moment. Although google maps reports that a zoom
 * level of 19 is supported, for some areas not in the city, tiles
 * for that zoom level does not exist. This is a problem for us since
 * records are often taken outside of city limits.
 *
 * Unfortunately, 16 is too low to be able to see detail in city areas
 * but this is a comprimise we are making for now.
 */
bdrs.map.DEFAULT_POINT_ZOOM_LEVEL = 16;

// This stops the user from reaching zoom level '0' which wraps around
// the world several times.
bdrs.map.MIN_GOOGLE_ZOOM_LEVEL = 1;

// --------------------------------------
// Some notes on map projections
//
// Most spherical mercator maps use an extent of the world
// from -180 to 180 longitude, and from -85.0511 to 85.0511
// latitude. Because the mercator projection stretches to
// infinity as you approach the poles, a cutoff in the north-south
// direction is required, and this particular cutoff results in a
// perfect square of projected meters. As you can see from the
// maxExtent parameter sent in the constructor of the map, the
// coordinates stretch from -20037508.34 to 20037508.34 in each direction.
//
// The maxResolution of the map fits this extent into 256 pixels,
// resulting in a maxResolution of 156543.0339.
//
// --------------------------------------

// initBaseMap:
// initialises bdrs.map.baseMap. Will use the createCustomMap function if it
// exists, else will use createDefaultMap.
// createCustomMap can be overridden in any subproject or theme giving you
// complete control on how the open layers map is configured.
bdrs.map.baseMap = null;
bdrs.map.selectedRecord = null;
bdrs.map.initBaseMap = function(mapId, options){

    if (jQuery('#'+mapId+':visible').length > 0) {
            var mapOptions = {
            isPublic: true,
            hideShowMapLink: false,
            enlargeMapLink: true,
            zoomLock: true,
            ajaxFeatureLookup: false
        };
        jQuery.extend(mapOptions, options);

        bdrs.map.initOpenLayers();

        var map;
        if (bdrs.map.createCustomMap) {
            map = bdrs.map.createCustomMap(mapId, mapOptions);
        }
        else {
            map = bdrs.map.createDefaultMap(mapId, mapOptions);
        }
        if (mapOptions.ajaxFeatureLookup) {
            var highlightLayer = bdrs.map.addWMSHighlightLayer(map);
            bdrs.map.initAjaxFeatureLookupClickHandler(map, {
                wmsHighlightLayer: highlightLayer
            });
        }
        else {
            bdrs.map.initPointSelectClickHandler(map);
        }

        // set the global cos we have to....
        bdrs.map.baseMap = map;

        return map;
    }
    return null;
}; // End initBaseMap


bdrs.map.createDefaultMap = function(mapId, mapOptions){
    var options = {
        projection: bdrs.map.GOOGLE_PROJECTION,
        displayProjection: bdrs.map.WGS84_PROJECTION,
        units: 'm',
        maxResolution: 156543.0339,
        maxExtent: new OpenLayers.Bounds(-20037508.34, -20037508.34, 20037508.34, 20037508.34),
        // by having the mouse wheel interval we can avoid the zooming performance problems
        controls: [new OpenLayers.Control.Navigation({
            mouseWheelOptions: {
                interval: 100
            }
        }), new OpenLayers.Control.PanZoom(), new OpenLayers.Control.ArgParser(), new OpenLayers.Control.Attribution()]
    };
    var map = new OpenLayers.Map(mapId, options);

    // Cumulative mode. If this mode is deactivated,
    // only one zoom event will be performed after the delay.
    //var nav = map.getControlsByClass("OpenLayers.Control.Navigation")[0];
    //nav.handlers.wheel.cumulative = true;

    // duck punch in persistentLayers - will not be removed when other layers
    // are cleared from the map
    map.persistentLayers = new Array();

    if (mapOptions.isPublic === true) {
        var layers;
        if (bdrs.map.customMapLayers) {
            layers = bdrs.map.customMapLayers();
        } else {
            layers = bdrs.map.defaultMapLayers();
        }

        if(layers.length === 0) {
            var nobase = new OpenLayers.Layer("No Basemap",{isBaseLayer: true, 'displayInLayerSwitcher': true});
            layers.push(nobase);
        }

        map.addLayers(layers);
        if(bdrs.map.baseLayer) {
            map.setBaseLayer(bdrs.map.baseLayer);
        } else {
            if(layers.length > 0) {
                map.setBaseLayer(layers[0]);
            }
        }
    }
    else {
        // add some OSM layers...This works well.
        var mapnik = new OpenLayers.Layer.OSM();
        map.addLayer(mapnik);
    }

    map.addControl(new bdrs.map.BdrsLayerSwitcher());
    map.addControl(new OpenLayers.Control.MousePosition());

    if (mapOptions.geocode !== undefined && mapOptions.geocode !== null) {
        bdrs.map.addGeocodeControl(mapOptions.geocode);
    }

    // Create the enlarge/collapse map and zoom lock elements if required.
    bdrs.map.addControlPanel(map, mapOptions.hideShowMapLink, mapOptions.enlargeMapLink, mapOptions.zoomLock);

    if (jQuery('#OpenLayers_Control_MaximizeDiv_innerImage')) {
        jQuery('#OpenLayers_Control_MaximizeDiv_innerImage').attr("title", "Change the baselayer and turn map layers on or off");
    }

	var layerSwitcherContainerSelector = '[id^="OpenLayers.Control.LayerSwitcher_"].layersDiv';
	if (jQuery('#OpenLayers_Control_MinimizeDiv')) {
		var layerSwitcherDiv = jQuery(layerSwitcherContainerSelector);
		layerSwitcherDiv.css("width", "auto");
        layerSwitcherDiv.css("overflow-y", "auto");
		layerSwitcherDiv.css("overflow-x", "hidden");
		layerSwitcherDiv.css("max-height", "300px");
	}

    var graticule = new OpenLayers.Control.Graticule({
        displayInLayerSwitcher: true
    });
    map.addControl(graticule);
    map.persistentLayers.push(graticule.gratLayer);

    return map;
};


/*
    Convenient method to check if Google Map is loaded to avoid
    OpenLayers errors when creating Google layers out of context.
*/
bdrs.map.isGoogleMapLoaded = function() {
    return typeof window.google === 'object' && typeof window.google.maps === 'object';
};

 /*
    Creates one of the known map layers (Google and OSM) identified by its id.
    id: string from the enum in BaseMapLayerSource.java (google v2 legacy name)
    The OSM layer will be returned by default (if id unknown).
    return: warning could return null if google API not loaded.
 */
bdrs.map.createMapLayer = function(id) {
    var result = null;
    switch(id){
        case "G_NORMAL_MAP":
            if (bdrs.map.isGoogleMapLoaded()){
                result =  new OpenLayers.Layer.Google('Google Streets', {
                                   type: google.maps.MapTypeId.ROADMAP,
                                   numZoomLevels: 20,
                                   sphericalMercator: true,
                                   MIN_ZOOM_LEVEL: bdrs.map.MIN_GOOGLE_ZOOM_LEVEL
                             });
            }
            break;
        case "G_PHYSICAL_MAP":
            if (bdrs.map.isGoogleMapLoaded()){
                result = new OpenLayers.Layer.Google('Google Physical', {
                                   type: google.maps.MapTypeId.TERRAIN,
                                   sphericalMercator: true,
                                   MIN_ZOOM_LEVEL: bdrs.map.MIN_GOOGLE_ZOOM_LEVEL
                             });
            }
            break;
        case "G_HYBRID_MAP":
            if (bdrs.map.isGoogleMapLoaded()){
                result = new OpenLayers.Layer.Google('Google Hybrid', {
                                   type: google.maps.MapTypeId.HYBRID,
                                   numZoomLevels: 20,
                                   sphericalMercator: true,
                                   MIN_ZOOM_LEVEL: bdrs.map.MIN_GOOGLE_ZOOM_LEVEL
                             });
            }
            break;
        case "G_SATELLITE_MAP":
            if (bdrs.map.isGoogleMapLoaded()){
                result = new OpenLayers.Layer.Google('Google Satellite', {
                                   type: google.maps.MapTypeId.SATELLITE,
                                   numZoomLevels: 22,
                                   sphericalMercator: true,
                                   MIN_ZOOM_LEVEL: bdrs.map.MIN_GOOGLE_ZOOM_LEVEL
                             });
            }
            break;
        case "OPEN_LAYERS_WMS":
            result = new OpenLayers.Layer.WMS( "OpenLayers WMS", "http://vmap0.tiles.osgeo.org/wms/vmap0?", {layers: 'basic'});
            break;
        case "NO_MAP":
            result = new OpenLayers.Layer("No Basemap",{isBaseLayer: true, 'displayInLayerSwitcher': true});
            break;
        case "OSM":  //same as default
        default:
            result = new OpenLayers.Layer.OSM();
    }
    return result;
};

bdrs.map.defaultMapLayers = function() {
    var layerIds = ["G_PHYSICAL_MAP",
                    "G_NORMAL_MAP",
                    "G_HYBRID_MAP",
                    "G_SATELLITE_MAP"];
    var layers = [];
    for (var i = 0; i < layerIds.length; ++i) {
        var id = layerIds[i];
        var layer = bdrs.map.createMapLayer(id);
        if (layer){
            layers.push(layer);
            // Google hybrid as the baseLayer
            if (id === "G_HYBRID_MAP") {
                bdrs.map.baseLayer = layer;
            }
        }
    }
    if (layers.length === 0){
        layers.push(bdrs.map.createMapLayer("NO_MAP"));
    }
    return layers;
};

/**
 * Creates a 'control panel' above the map a places the appropriate controls in the
 * element if the map supports either enlarge map capabilities or zoom lock capabilities.
 *
 * @param map [object] the open layers map instance
 * @param hideShowAvailable [boolean] true if the user can hide or show the map, false otherwise.
 * @param enlargeMapAvailable [boolean] true if the user can enlarge/collapse the map, false otherwise.
 * @param zoomLockAvailable [boolean]  true if the zoom lock control is available, false otherwise.
 */
bdrs.map.addControlPanel = function(map, hideShowAvailable, enlargeMapAvailable, zoomLockAvailable) {
    // Create a control panel that will sit above the map
    var fullControlPanel = jQuery("<div id=\"mapControls\"></div>");
    jQuery(map.div).parent().before(fullControlPanel);
    // insert a clear to allow the floating to work
    fullControlPanel.after(jQuery("<div class=\"clear\"></div>"));

    // create a right and left div for adding controls to
    // Create the control panel that will house these options
    var controlPanel = jQuery("<div id=\"rightMapControls\"></div>");
    controlPanel.css({
        "text-align" : "right",
        "padding-top" : "0.5em",
        "float" : "right",
        "width" : "50%"
    });
    // Create the control panel that will house these options
    var leftControlPanel = jQuery("<div id=\"leftMapControls\"></div>");
    leftControlPanel.css({
        "text-align" : "left",
        "padding-top" : "0.5em",
        "float" : "left",
        "width" : "50%"
    });

    fullControlPanel.append(controlPanel);
    fullControlPanel.append(leftControlPanel);

    if(!hideShowAvailable && !enlargeMapAvailable && !zoomLockAvailable) {
        return;
    }

    // Create each control
    var controlArray = [];

    // add a control for displaying messages about the results on the page
    // currently only used by Location Review page to show a message that
    // the list results are limited by a map selection and a link to clear
    // the map selection
    controlArray.push(jQuery("<div id=\"resultsMessages\"></div>"));

    // Hide / Show Map
    controlArray.push(bdrs.map.getHideShowControl(controlPanel, hideShowAvailable, map));

    // Enlarge Map
    controlArray.push(bdrs.map.getEnlargeMapControl(controlPanel, enlargeMapAvailable, map));

    // Zoom Lock
    controlArray.push(bdrs.map.getZoomLockControl(controlPanel, zoomLockAvailable));

    var control;
    var sep;
    var controlCount = 0;
    for(var i=0; i<controlArray.length; ++i) {
        control = controlArray[i];
        if(control !== undefined && control !== null) {

            // if the first, then we don't need to start with a separator
            if(controlCount > 0) {
                sep = bdrs.map.getControlSeparator();
                controlPanel.append(sep);
            }
            controlPanel.append(control);
            controlCount++;
        }
    }
};

/**
 * Keep track of the original center and zoom of the map so it can be reset later.
 */
bdrs.map.originalCenter = null;
bdrs.map.originalZoom = null;
bdrs.map.recordOriginalCenterZoom = function(map) {
    // some global variables for resetting the zoom level later
    bdrs.map.originalCenter = bdrs.map.baseMap.getCenter();
    bdrs.map.originalZoom = bdrs.map.baseMap.getZoom();
};

/**
 * Creates a 'control panel' above the map with control for zoom back to original extent.
 * Can only be called after bdrs.map.addControlPanel to ensure the main controlPanel is already created!
 * Can only be called after bdrs.map.recordOriginalCenterZoom has been called too.
 *
 * @param map [object] the open layers map instance
 */
bdrs.map.addZoomControlPanel = function(map) {
    // Create the control panel that will house these options
    var fullControlPanel = jQuery("#mapControls");
    var controlPanel = jQuery('#leftMapControls');
    controlPanel.append(bdrs.map.getOriginalZoomControl(controlPanel, map));
};

/**
 * Retrieve the element used to separate the widgets on the control panel.
 */
bdrs.map.getControlSeparator = function() {
    return jQuery("<span></span>").html("&nbsp;|&nbsp;");
};

/**
 * Creates the widget to enlarge/collapse the map.
 *
 * @param controlPanel [jQuery element] the control panel where the widget will be inserted.
 * @param available [boolean] true if this widget is required, false otherwise.
 * @param map [object] the openlayers map instance.
 */
bdrs.map.getEnlargeMapControl = function(controlPanel, available, map) {
    if(!available) {
        return;
    }

    var control = jQuery("<a></a>");

    control.attr({"id": "maximiseMapLink", "href": "javascript:void(0);"});
    control.text(bdrs.map.ENLARGE_MAP_LABEL);
    control.click(function(event) {
        var trigger = jQuery(event.currentTarget);
        bdrs.map.maximiseMap(map, controlPanel, trigger,
                            bdrs.map.ENLARGE_MAP_LABEL, bdrs.map.SHRINK_MAP_LABEL,
                            bdrs.map.ENLARGE_MAP_CLASS, bdrs.map.SHRINK_MAP_CLASS);
    });
    return control;
};

/**
 * Creates the widget to zoom the map to its original extent.  For this to work,
 * you must have set bdrs.map.originalCenter and bdrs.map.originalZoom:
 * (this is done in bdrs.map.initBaseMap so if you have used that function, this will work)
 *
 * bdrs.map.originalCenter = bdrs.map.baseMap.getCenter();
 * bdrs.map.originalZoom = bdrs.map.baseMap.getZoom();
 *
 * @param controlPanel [jQuery element] the control panel where the widget will be inserted.
 * @param available [boolean] true if this widget is required, false otherwise.
 * @param map [object] the openlayers map instance.
 */
bdrs.map.getOriginalZoomControl = function(controlPanel, map) {
    var control = jQuery("<a></a>");

    control.attr({"id": "origZoomLink", "href": "javascript:void(0);"});
    control.text("Reset Map Center/Zoom");
    control.click(function(event) {
        var trigger = jQuery(event.currentTarget);
        if (bdrs.map.originalCenter && bdrs.map.originalZoom) {
            bdrs.map.centerMap(bdrs.map.baseMap, bdrs.map.originalCenter, bdrs.map.originalZoom);
        }
    });
    return control;
};

/**
 * Creates the widget to allow/disallow scroll zooming on the map.
 *
 * @param controlPanel [jQuery element] the control panel where the widget will be inserted.
 * @param available [boolean] true if this widget is required, false otherwise.
 */
bdrs.map.getZoomLockControl = function(controlPanel, available) {
    if(!available) {
        return;
    }

    var control = jQuery("<span></span>");

    var checkbox = jQuery("<input></input>");
    checkbox.attr({
        "type": "checkbox",
        "id":"scroll_zoom_cb",
        "checked": bdrs.map.isScrollZoomEnabled()
    });
    checkbox.css({"vertical-align": "text-bottom"});

    checkbox.change(function(event) {
        var scrollZoomEnabled = jQuery(event.currentTarget).prop("checked");
        bdrs.map.scrollZoom(scrollZoomEnabled, bdrs.map.baseMap, false);
    });

    var label = jQuery("<label></label>");
    label.attr({
        "for": checkbox.attr("id"),
        "title": "Uncheck this box to prevent the mouse wheel from zooming the map"
    });
    label.text("Zoom Scroll");


    control.append(checkbox).append(label);
    return control;
};

/**
 * Creates the widget that allows the map to be displayed or hidden.
 *
 * @param controlPanel [jQuery element] the control panel where the widget will be inserted.
 * @param available [boolean] true if this widget is required, false otherwise.
 */
bdrs.map.getHideShowControl = function(controlPanel, available, map) {
    if (!available) {
        return;
    }

    var control = jQuery("<a></a>");

    control.attr({"id": "hideShowMapLink", "href": "javascript:void(0);"});
    control.text(bdrs.map.HIDE_MAP_LABEL);
    control.click(function() {bdrs.map.collapseMap(jQuery(map.div).parent() , control);});

    return control;
};

/**
 * Maximise or minimise the specified map.
 * @param map [object] the Open Layers map instance
 * @param controlPanel [jQuery element] The element of the control panel that contains the trigger element (see next param).
 * @param trigger [jQuery element] The element containing the text to indicate if clicking it will enlarge or shrink the map.
 * @param enlargeMapLabel [string] The lable used to indicate that the map will be enlarged if the element is clicked.
 * @param shrinkMapLabel [string] The label used to indicate that the map will be shrunk if the element is clicked.
 * @param enlargeMapClass [string] The class that is attached to the element containing the map if it is enlarged.
 * @param shrinkMapClass [string] The class that is attached to the element containing the map if it is shrunk.
 */
bdrs.map.maximiseMap = function(map, controlPanel, trigger, enlargeMapLabel, shrinkMapLabel, enlargeMapClass, shrinkMapClass) {
    var mapDiv = jQuery(map.div);
    var mapContainer = mapDiv.parent();
    var isShrinking = mapDiv.hasClass(enlargeMapClass);

    if(isShrinking) {
        // Minimise the currently maximised map
        mapContainer.unwrap().css({'width':'inherit', 'height':'inherit'});
        // The following only needed for IE7, but does not harm other browsers
        mapDiv.css({
            position: "relative",
            top: null,
            bottom: null,
            left: null,
            right: null
        });
        mapDiv.removeAttr("style");
        mapDiv.removeClass(enlargeMapClass).addClass(shrinkMapClass);

        // Update the enlarge/shrink label
        trigger.removeClass("shrinkMapLink");
        trigger.text(enlargeMapLabel);

        // Restore scroll zooming based on cookie.
        bdrs.map.restoreScrollZoomState(map);

		// return control panel to original position.
		if (map.controlPanelPlaceHolder !== undefined) {
			controlPanel.detach();
			controlPanel.insertBefore(map.controlPanelPlaceHolder);
			map.controlPanelPlaceHolder.remove();
			delete map.controlPanelPlaceHolder;
		}
		// return map to original position.
		if (map.mapPlaceHolder !== undefined) {
			mapContainer.detach();
			mapContainer.insertBefore(map.mapPlaceHolder);
			map.mapPlaceHolder.remove();
			delete map.mapPlaceHolder;
		}

        // Restore the scrollbar to the original position
        if(map.scrollMemory !== undefined) {
            jQuery(window).scrollTop(map.scrollMemory.scrollTop).scrollLeft(map.scrollMemory.scrollLeft);
            delete map.scrollMemory;
        }
        // This is a workaround for a chrome bug (see Issue 347) - OpenLayers 2.12. does the same thing however
        // introduced problems with polygon drawing.
        mapDiv.find('svg').css('display', 'block');

    } else {
        // Fix the scrollbar issue where the scrolling is not at the extreme top and left
        var win = jQuery(window);
        map.scrollMemory = {
            scrollLeft: win.scrollLeft(),
            scrollTop: win.scrollTop()
        };
        win.scrollTop(0).scrollLeft(0);

		// save original position of map container
		var mapPlaceHolder = jQuery("<div/>");
		mapPlaceHolder.insertBefore(mapContainer);
		map.mapPlaceHolder = mapPlaceHolder;
		// save original position of control panel
		var controlPanelPlaceHolder = jQuery("<div/>");
		controlPanelPlaceHolder.insertBefore(controlPanel);
		map.controlPanelPlaceHolder = controlPanelPlaceHolder;

		// prepare to move items.
		mapContainer.detach();
		controlPanel.detach();

        // Maximise the currently minimised map
        var wrapper = jQuery("<div></div>");
        wrapper.css({
            position: 'fixed',
            top: 0,
            bottom: 0,
            left: 0,
            right: 0,
            backgroundColor: 'white',
            zIndex: 3000
        });
        mapContainer.wrap(wrapper).css({'width':'100%', 'height':'100%'});
        mapDiv.css({
            'width':'100%',
            'height':'100%',
            // The following only needed for IE7, but does not harm other browsers
            position: "fixed",
            top: 0,
            bottom: 0,
            left: 0,
            right: 0
        });
        mapDiv.removeClass(shrinkMapClass).addClass(enlargeMapClass);

        // Update the enlarge/shrink label
        trigger.addClass("shrinkMapLink");
        trigger.text(shrinkMapLabel);

		// add to items to body element.
		var targetElem = jQuery("body").first();
		targetElem.prepend(controlPanel);
		targetElem.prepend(mapContainer.parent());

        // Attach the esc key listener
        var keyHandler = function(event) {
            if(event.keyCode === 27) {
                jQuery(event.currentTarget).unbind(event);
                //simulate a click on the Shrink Map link
                jQuery("a#maximiseMapLink").click();
            }
        };
        jQuery(document).keyup(keyHandler);

        // Special case where we enable scroll zooming no matter what.
        bdrs.map.scrollZoom(true, map, true);
    }
    bdrs.map.updateMapLogo();
    map.updateSize();
};

bdrs.map.updateMapLogo = function() {
    var mapLogoMax = jQuery(".mapLogoMax");

    if(mapLogoMax.length === 0) {
        jQuery(".mapLogoMin").removeClass("mapLogoMin").addClass("mapLogoMax");
    } else {
        mapLogoMax = mapLogoMax.removeClass("mapLogoMax").addClass("mapLogoMin");
    }
};

/**
 * Enables or disables the ability of the scroll wheel to zoom in and out of the map.
 * @param scrollZoomEnabled [boolean] true if the user can scroll to zoom, false otherwise.
 * @param map [object] the Open Layers map instance
 * @param noCookie [boolean] true if a cookie should NOT be created storing the state, false otherwise.
 */
bdrs.map.scrollZoom = function(scrollZoomEnabled, map, noCookie) {
    var controls = map.getControlsByClass('OpenLayers.Control.Navigation');

    if(controls.length > 0) {
        if (scrollZoomEnabled) {
            controls[0].enableZoomWheel();
        } else {
            controls[0].disableZoomWheel();
        }

        if (!noCookie) {
            // The false value doesn't matter. Its only enabled when the value
            // is equal to the enabled value.
            var cookieVal = scrollZoomEnabled ? bdrs.map.SCROLL_ZOOM_ENABLED_VALUE : 'false';
            bdrs.util.cookie.create(bdrs.map.SCROLL_ZOOM_COOKIE, cookieVal, 1000);
        }
    }
};

/**
 * Updates the state of the scroll zooming based on the current value of the cookie
 * @param map [object] the Open Layers map instance
 */
bdrs.map.restoreScrollZoomState = function(map) {
    var controls = map.getControlsByClass('OpenLayers.Control.Navigation');

    if(controls.length > 0) {
        if (bdrs.map.isScrollZoomEnabled()) {
            controls[0].enableZoomWheel();
        } else {
            controls[0].disableZoomWheel();
        }
    }
};

/**
 * Returns true if scroll zooming is enabled according to the cookie, false otherwise.
 */
bdrs.map.isScrollZoomEnabled = function() {
    var cookieVal = bdrs.util.cookie.read(bdrs.map.SCROLL_ZOOM_COOKIE);
    if(cookieVal === null || cookieVal === undefined) {
        cookieVal = bdrs.map.SCROLL_ZOOM_DEFAULT;
    }

    return cookieVal === bdrs.map.SCROLL_ZOOM_ENABLED_VALUE;
};

/**
 * Toggles map zoom scroll
 *
 * @param    toggleSelector
 *                 The selector of the element that triggers the toggle.
 * @param    spanCheckSelector
 *                 The selector that for visualizing the state of the toggle.
 * @param    map
 *                 The map that needs to be zoom scroll toggled.
 */
bdrs.map.scrollToggle = function(toggleSelector, spanCheckSelector, map) {
    jQuery(toggleSelector).toggle(function(){
        jQuery(spanCheckSelector).text(' ');
        bdrs.map.scrollZoom(false, map);
    },
    function(){
        jQuery(spanCheckSelector).text('x');
        bdrs.map.scrollZoom(true, map);
    });
};

/**
 * Handler function that adds a KML layer to the map via an AJAX request.
 *
 * @param formIdSelector
 *            selector for the form containing the query parameters.
 * @param mapLayersSelectSelector
 *            selector for the select element that displays added layers.
 */
bdrs.map.addRecordLayerHandler = function(formIdSelector, mapLayersSelectSelector, formOverrides){
    var form = jQuery(formIdSelector);
    var map = bdrs.map.baseMap;
    var params = bdrs.util.formToMap(formIdSelector);
    if (typeof(formOverrides) !== 'undefined') {
        for (var property in formOverrides) {
            if (formOverrides.hasOwnProperty(property)) {
                params[property] = formOverrides[property];
            }
        }
    }

    var layerName = params.layer_name;
    if (layerName.length === 0) {
        return false;
    }

    var kmlURL = form.attr("action") + "?" + jQuery.param(params);
    var recordLayer = new OpenLayers.Layer.Vector(layerName, {
        projection: map.displayProjection,
        strategies: [new OpenLayers.Strategy.Fixed(), new bdrs.map.BdrsCluster()],
        protocol: new OpenLayers.Protocol.HTTP({
            url: kmlURL,
            //data: formData,
            format: new OpenLayers.Format.KML({
                extractStyles: true,
                extractAttributes: true
            })
        })
    });

    //set styleMap from themeOverride if one exists
    if (params.styleMaps) {
        recordLayer.styleMap = new OpenLayers.StyleMap(params.styleMaps[recordLayer.name]);
    }

    recordLayer.events.register('loadend', recordLayer, function(event){
        var layerOptElem = jQuery('option[id="' + recordLayer.id + '"]');
        if (layerOptElem.length > 0) {
            layerOptElem.text(layerOptElem.text() + ' (' + recordLayer.features.length + ' Records)');
        }
        var extent = event.object.getDataExtent();
        if (extent !== null) {
            bdrs.map.baseMap.zoomToExtent(event.object.getDataExtent(), false);
        }
    });
    map.addLayers([recordLayer]);

    if (mapLayersSelectSelector !== null) {
        var layerOptionElem = jQuery('<option></option>');
        layerOptionElem.attr('id', recordLayer.id);
        layerOptionElem.text(layerName);
        layerOptionElem.val(recordLayer.id);
        // Needed later if we want to download the kml
        layerOptionElem.data('kmlRequest', kmlURL);
        jQuery(mapLayersSelectSelector).append(layerOptionElem);
    }
};

/**
 * Creates a style map that supports a different style for selected features,
 * picked features and normal features.
 *
 * @param selectedId [string] is the id of the feature. This is typically
 * the same as the id of the record. The feature sporting this id will be
 * styled using the 'SELECTED_FEATURE' styling.
 */
bdrs.map.createOpenlayersStyleMap = function(selectedId) {

    var getColor = function(feature) {
        if (feature.fid === selectedId) {
            return bdrs.map.SELECTED_FEATURE_STROKE_COLOR;
        } else {
            return bdrs.map.NORMAL_FEATURE_STROKE_COLOR;
        }
    };

    // Since the stroke and fill color is exactly the same, delegate to the
    // same function.
    var context = {
        getStrokeColor: getColor,
        getFillColor: getColor
    };

    var defaultOLStyle = new OpenLayers.Style({
        'strokeWidth': 2,
        'strokeOpacity': 1,
        'strokeColor': '${getStrokeColor}',
        'fillColor': '${getFillColor}',
        'fillOpacity': 0.8,
        'pointRadius': 5
    }, {context:context});

    var pickedOLStyle = new OpenLayers.Style({
           strokeColor: bdrs.map.PICKED_FEATURE_STROKE_COLOR,
           fillColor: bdrs.map.PICKED_FEATURE_FILL_COLOR
    });

    var styles = {
       "default": defaultOLStyle,
       "select": pickedOLStyle
    };

    return new OpenLayers.StyleMap(styles);
};

/**
 * Create a kml layer and add it to the map
 *
 * @param {Object} map OpenLayers.Map object
 * @param {Object} layerName name of the layer
 * @param {Object} kmlURL the url used to collect the KML
 * @param {Object} options layer options
 * @param {Object} ignoreId - aka the feature id of the selected feature.
 * Is called 'ignoreId' as the feature id is ignored during feature clustering.
 */
bdrs.map.addKmlLayer = function(map, layerName, kmlURL, options, ignoreId){
    return bdrs.map.addKmlLayerWithProtocol(map, layerName, options, ignoreId,
            new OpenLayers.Protocol.HTTP({
                url: kmlURL,
                format: new OpenLayers.Format.KML({
                    extractStyles: true,
                    extractAttributes: true
                })
            })
    );
};

/**
 * Create a kml layer that reads from a POST request and add it to the map
 *
 * @param {Object} map OpenLayers.Map object
 * @param {Object} layerName name of the layer
 * @param {Object} options layer options
 * @param {Object} ignoreId - aka the feature id of the selected feature.
 * Is called 'ignoreId' as the feature id is ignored during feature clustering.
 * @param {Object} urlParams parameters to past to the post request
 */
bdrs.map.addKmlLayerWithPost = function(map, layerName, kmlURL, options, ignoreId, urlParams){
    return bdrs.map.addKmlLayerWithProtocol(map, layerName, options, ignoreId,
            new OpenLayers.Protocol.HTTP({
                url: kmlURL,
                format: new OpenLayers.Format.KML({
                    extractStyles: true,
                    extractAttributes: true
                }),
                params: urlParams,
                readWithPOST: true
        })
    );
};

/**
 * Create a kml layer and add it to the map
 *
 * @param {Object} map OpenLayers.Map object
 * @param {Object} layerName name of the layer
 * @param {Object} options layer options
 * @param {Object} ignoreId - aka the feature id of the selected feature.
 * Is called 'ignoreId' as the feature id is ignored during feature clustering.
 * @param {Object} protocol the protocol with which to read the kml layer
 */
bdrs.map.addKmlLayerWithProtocol = function(map, layerName, options, ignoreId, protocol){
    if (options === undefined) {
        options = {};
    }
    var defaultOptions = {
        visible: true,
        bdrsLayerId: undefined,
        includeClusterStrategy: false
    };
    options = jQuery.extend(defaultOptions, options);

    var strategies = [new OpenLayers.Strategy.Fixed()];
    if (options.includeClusterStrategy === true) {
        strategies.push(new bdrs.map.BdrsCluster({'ignoreId': ignoreId}));
    }

    var layer = new OpenLayers.Layer.Vector(layerName, {
        projection: map.displayProjection,
        strategies: strategies,
        protocol: protocol,
        visibility: options.visible,
        minZoomLevel: options.lowerZoomLimit,
        maxZoomLevel: options.upperZoomLimit,
        styleMap: options.styleMap
    });

    // See notes for bdrs.map.calculateInRangeByZoomLevel
    layer.calculateInRange = bdrs.map.calculateInRangeByZoomLevel;

    // duck punch!
    layer.bdrsLayerId = options.bdrsLayerId;

    map.addLayers([layer]);
    return layer;
};

/**
 * Add a WMS layer for a given url. You must append query parameters
 * onto the URL if you require them. E.g. the LAYERS parameter
 * is commonly used to specify which layers you want to see rendered.
 * See the WMS standard for details.
 *
 * @param {Object} map OpenLayers map object.
 * @param {String} layerName Name of layer.
 * @param {String} url URL of WMS server.
 * @return {Object} the created layer object.
 */
bdrs.map.addWmsLayer = function(map, layerName, url, options) {
	if (options === undefined) {
        options = {};
    }
    var defaultOptions = {
        visible: true,
		opacity: bdrs.map.DEFAULT_OPACITY
    };
    options = jQuery.extend(defaultOptions, options);

	var layer = new OpenLayers.Layer.WMS( layerName,
            url,
            {
			    transparent: "true"
			}, {
				visibility: options.visible,
				opacity: options.opacity
			});
    map.addLayer(layer);
	return layer;
};

bdrs.map.getBdrsMapServerUrl = function(){
    return "http://" + document.location.hostname + "/cgi-bin/mapserv?map=bdrs.map&";
};

bdrs.map.addMapServerLayer = function(map, layerName, url, options){
    return bdrs.map.addBdrsWMSLayer(map, layerName, url, options);
};

bdrs.map.addWMSHighlightLayer = function(map, layername, url, options){
    if (options === undefined) {
        options = {};
    }
    var defaultOptions = {
        visible: false,
        wmsLayer: 'bdrs_highlight',
        opacity: 0.85,
        displayInLayerSwitcher: false,
        tileSize: new OpenLayers.Size(512, 512),
        buffer: 0
    };
    options = jQuery.extend(defaultOptions, options);

    //var layer = new OpenLayers.Layer.WMS.Post(layername ? layername : "highlightLayer", url ? url : bdrs.map.getBdrsMapServerUrl(), {
    var layer = new OpenLayers.Layer.WMS(layername ? layername : "highlightLayer", url ? url : bdrs.map.getBdrsMapServerUrl(), {
        'layers': options.wmsLayer,
        'transparent': 'true'
    }, {
        isBaseLayer: false,
        opacity: options.opacity,
        visibility: options.visible,
        displayInLayerSwitcher: options.displayInLayerSwitcher,
        tileSize: options.tileSize,
        buffer: options.buffer
    });
    map.addLayer(layer);
    return layer;
};

bdrs.map.addBdrsWMSLayer = function(map, layerName, url, options){
    if (options === undefined) {
        options = {};
    }
    var defaultOptions = {
        visible: true,
        bdrsLayerId: undefined,
        wmsLayer: 'bdrs',
        opacity: 1,
        tileSize: new OpenLayers.Size(512, 512),
        buffer: 0,
        userId: bdrs.authenticatedUserId ? bdrs.authenticatedUserId : 0,
        isAdmin: bdrs.isAdmin ? 'true' : 'false'
    };
    options = jQuery.extend(defaultOptions, options);
    var sld = bdrs.map.generateLayerSLD(options);

    //var wmsLayer = new OpenLayers.Layer.WMS.Post(layerName, url, {
    var wmsLayer = new OpenLayers.Layer.WMS(layerName, url, {
        'layers': options.wmsLayer,
        'transparent': 'true',
        'geo_map_layer_id': options.bdrsLayerId,
        'user_id': options.userId,
        'is_admin': options.isAdmin,
        'sld_body':sld
    }, {
        isBaseLayer: false,
        visibility: options.visible,
        opacity: options.opacity,
        tileSize: options.tileSize,
        buffer: options.buffer,
        minZoomLevel: options.lowerZoomLimit,
        maxZoomLevel: options.upperZoomLimit
    });

    // See notes for bdrs.map.calculateInRangeByZoomLevel
    wmsLayer.calculateInRange = bdrs.map.calculateInRangeByZoomLevel;

    // duck punch!
    wmsLayer.bdrsLayerId = options.bdrsLayerId;
    map.addLayer(wmsLayer);
    return wmsLayer;
};

// used for overriding the calculateInRange function on the WMSLayer object
// after layer creation. Should probably make a new javascript object to
// capture this behaviour but I wanted to wait until we sort out our javascript
// includes a little more.
bdrs.map.calculateInRangeByZoomLevel = function(){
    var inRange = false;

    if (this.alwaysInRange) {
        inRange = true;
    }
    else {
        if (this.map) {
            var zoom = this.map.getZoom();
            inRange = ((zoom >= this.minZoomLevel) &&
            (zoom <= this.maxZoomLevel));
        }
    }
    return inRange;
};

// Note the user/pass combination. Not the most secure way to do this I'm sure...
bdrs.map.addSlipWMSLayer = function(displayName, layerName, map){
    var slipLayer = new OpenLayers.Layer.WMS(displayName, 'https://aarongaia:I2LJ5P7ozN@www2.landgate.wa.gov.au/ows/wmspublic', {
        'layers': layerName,
        'transparent': 'true'
    }, {
        isBaseLayer: false,
        visibility: true
    });
    map.addLayer(slipLayer);
    return slipLayer;
};

bdrs.map.addSlipWFSLayer = function(displayLayerName, layerName, map, styleMap){
    // self explanatory....
    throw "Do not use. Have not set up proxy";

    var slipLayer = new OpenLayers.Layer.Vector(displayLayerName, {
        projection: map.displayProjection,
        strategies: [new OpenLayers.Strategy.BBOX()], // at least bbox seems to work out of the bbox, yay
        protocol: new OpenLayers.Protocol.WFS({
            version: "1.0.0",
            srsName: "EPSG:4326", // this is the default
            url: bdrs.contextPath + "/slip/wfspublic_4326/wfs",
            featureNS: "http://www.openplans.org/slip",
            featureType: layerName // overview roads
        }),
        styleMap: styleMap,
        visibility: false
    });
    map.addLayer(slipLayer);
    slipLayer.refresh(); // trigger bbox to download data
    return slipLayer;
};



// add a select handler for multiple layers.
// 'layers' is an array of layer objects
bdrs.map.addSelectHandler = function(map, layers){
    if (!layers || layers.length === 0) {
        return;
    }
    var select = new OpenLayers.Control.SelectFeature(layers);

    for (var i = 0; i < layers.length; ++i) {
        var layer = layers[i];

        layer.events.on({
            "featureselected": function(event){

                bdrs.map.clearPopups(map);

                var feature = event.feature;
                try {
                    var itemArray = new Array();
                    for (var i = 0; i < feature.cluster.length; ++i) {
                        itemArray.push(jQuery.parseJSON(feature.cluster[i].attributes.description));
                    }
                    bdrs.map.createFeaturePopup(map, feature.geometry.getBounds().getCenterLonLat(), itemArray);

                }
                catch (jsonParsingError) {
                    // exception thrown, we will assume it is a json parsing error...

                    // Since KML is user-generated, do naive protection against
                    // Javascript.
                    var content = "<h1>" + (feature.attributes.name ? feature.attributes.name : "") + "</h1>" +
                    (feature.attributes.description ? feature.attributes.description : "");
                    if (content.search("<script") != -1) {
                        content = "Content contained Javascript! Escaped content below.<br />" + content.replace(/</g, "&lt;");
                    }
                    popup = new OpenLayers.Popup.FramedCloud("chicken", feature.geometry.getBounds().getCenterLonLat(), new OpenLayers.Size(100, 100), content, null, true, function(evt){
                        select.unselectAll();
                    });
                    feature.popup = popup;
                    map.addPopup(popup);
                }
            },
            "featureunselected": function(event){
                var feature = event.feature;
                if (feature.popup) {
                    map.removePopup(feature.popup);
                    feature.popup.destroy();
                    delete feature.popup;
                }
            }
        });
    }
    map.addControl(select);

    // enable map drag panning while mouse is over feature
    select.handlers.feature.stopDown = false;

    select.activate();
};

bdrs.map.addMapLogo = function(map, imgUrl, containerNodeSelector){
    var img = jQuery('<img class="mapLogoMin" src="' + imgUrl + '" />');
    jQuery(containerNodeSelector).append(img);
};

/**
 * Clears all layers in the specified select element from the map.
 *
 * @param mapLayersSelectSelector
 *            selector for the select element that displays added layers.
 */
bdrs.map.clearAllLayers = function(mapLayersSelectSelector){
    jQuery(mapLayersSelectSelector).children().each(function(index, element){
        var jelem = jQuery(element);
        var layerId = jelem.val();
        var layer = bdrs.map.baseMap.getLayer(layerId);
        bdrs.map.baseMap.removeLayer(layer);
        jelem.remove();
    });
};

bdrs.map.clearAllVectorLayers = function(map){
    var layers = bdrs.map.baseMap.getLayersByClass('OpenLayers.Layer.Vector');
    for (var i = 0; i < layers.length; i++) {
        // persistentLayers must exist
        // persistentLayers must be an array
        // anything contained in persistent layers cannot be removed
        // if the duck punched persistentLayers property is missing then just remove the layer.
        if (map.persistentLayers && jQuery.isArray(map.persistentLayers)) {
            if (jQuery.inArray(layers[i], map.persistentLayers) == -1) {
                map.removeLayer(layers[i]);
            }
        }
        else {
            map.removeLayer(layers[i]);
        }
    }
};

bdrs.map.collapseMap = function(mapWrapper, mapToggle) {
    mapWrapper.slideToggle(function() {

        var canSee = mapWrapper.css('display') === 'none';
        mapToggle.text(canSee ? bdrs.map.SHOW_MAP_LABEL : bdrs.map.HIDE_MAP_LABEL);
        if(canSee) {
            mapWrapper.hide();
            mapToggle.siblings().hide();
        } else {
            mapWrapper.show();
            mapToggle.siblings().show();
        }
    });
};

/**
 * Removes a layer from the map.
 *
 * @param layerId
 *            the id of the layer to remove.
 */
bdrs.map.removeLayerById = function(layerId){
    // Remove the layer
    var layer = bdrs.map.baseMap.getLayer(layerId);
    bdrs.map.baseMap.removeLayer(layer);
};

/**
 * Removes the selected layer in the specified selector from the map.
 *
 * @param mapLayersSelectSelector
 *            selector for the select element that displays added layers.
 */
bdrs.map.removeLayer = function(mapLayersSelectSelector){
    var jelem = jQuery(mapLayersSelectSelector).find("option:selected");
    if (jelem.length === 0) {
        return;
    }
    jelem.remove();
};

/**
 * Triggers a KML download by navigating (window.document.location) to the KML
 * URL.
 *
 * @param formIdSelector
 *            selector for the form containing the query parameters.
 * @param mapLayersSelectSelector
 *            selector for the select element that displays added layers.
 */
bdrs.map.downloadKML = function(formIdSelector, mapLayersSelectSelector){
    var jelem = jQuery(mapLayersSelectSelector).find("option:selected");
    if (jelem.length === 0) {
        // There is no map layers select option available. This is where the
        // previously used url would be saved. Return the kml
        // for the current form.
        var form = jQuery(formIdSelector);
        window.document.location = form.attr("action") + '?' + form.serialize();
    }
    else {
        window.document.location = jelem.data("kmlRequest");
    }
};

// For downloading records from the mysightings page
bdrs.map.downloadMapData = function(formSelector, format, userId) {
    var formatParam = "&downloadFormat=";
    if (format === 'SHAPEFILE') {
        formatParam += "SHAPEFILE";
    } else {
        // default is KML
        formatParam += "KML";
    }
    var userParam = "";
    if (userId !== undefined && userId !== null) {
        userParam = "&user=" + userId;
    }
    var form = jQuery(formSelector);
    window.document.location = form.attr("action") + '?' + form.serialize() + userParam + formatParam;
};

// For downloading records from the 'view map' page
bdrs.map.downloadRecordsForActiveLayers = function(map, format) {

    bdrs.message.clear();
    var mapLayerIds = bdrs.map.getSelectedBdrsLayerIds(map);

    if (mapLayerIds.length === 0) {
        bdrs.message.set("There are no map layers enabled to download records for.");
    } else {
        var params = {};
        if (format === 'SHAPEFILE') {
            params.downloadFormat = "SHAPEFILE";
        } else if (format === 'KML') {
            params.downloadFormat = "KML";
        } else {
            throw 'invalid record download format';
        }
        params.mapLayerId = mapLayerIds;
        var url = bdrs.portalContextPath + "/bdrs/map/downloadRecords.htm?" + jQuery.param(params, true);
        window.document.location = url;
    }
};

if (bdrs.openlayers === undefined){
bdrs.openlayers = {};
}

if (bdrs.openlayers.positionlayer === undefined){
bdrs.openlayers.positionlayer = {};
}

bdrs.openlayers.positionlayer.stylemap = {
    'strokeWidth': 2,
    'strokeOpacity': 1,
    'strokeColor': '#EE9900',
    'fillColor': '#EE9900',
    'fillOpacity': 0.6,
    'pointRadius': 4
};


bdrs.map.addPositionLayer = function(layerName){
    var layer = new OpenLayers.Layer.Vector(layerName, {
        styleMap: new OpenLayers.StyleMap(bdrs.openlayers.positionlayer.stylemap)
    });

    var map = bdrs.map.baseMap;
    map.addLayers([layer]);
    return layer;
};

if (bdrs.openlayers.locationlayer === undefined){
    bdrs.openlayers.locationlayer = {};
    }

bdrs.openlayers.locationlayer.stylemap = {
        'strokeWidth': 2,
        'strokeOpacity': 1,
        'strokeColor': '#669900',
        'fillColor': '#669900',
        'fillOpacity': 0.2,
        'pointRadius': 4
    };

bdrs.map.addLocationLayer = function(map, layerName){
    var layer = bdrs.map.addVectorLayer(map, layerName, bdrs.openlayers.locationlayer.stylemap);
    map.addLayers([layer]);
    return layer;
};

bdrs.map.addVectorLayer = function(map, layerName, stylemap, options) {
    var layerOptions = jQuery.extend({
        styleMap: new OpenLayers.StyleMap(stylemap)
    }, options);
    var layer = new OpenLayers.Layer.Vector(layerName, layerOptions);
    return layer;
}

/**
 * Round a number to 'dec' decimal places.
 * @param {float} num number to round.
 * @param {int} dec number of decimal places to round to.
 */
bdrs.map.roundNumber = function(num, dec){
    var result = Math.round(num * Math.pow(10, dec)) / Math.pow(10, dec);
    return result;
};

/**
 * Rounds a lat/lon to 6 decimal places.
 * @param {float} latOrLong
 */
bdrs.map.roundLatOrLon = function(latOrLong){
    return bdrs.map.roundNumber(latOrLong, 6);
};

/**
 * Round a coordinate based on the currently selected
 * coordinate reference system.
 * @param {number} value Value to round.
 * @param {jQuery selector} crsSelector jQuery selector for an input containing SRID setting.
 */
bdrs.map.roundCoord = function(value, crsSelector) {
	var srid = jQuery(crsSelector).val();
	if (srid) {
		var proj = bdrs.map.getProjectionBySrid(srid);
		if (proj.proj.units === 'm') {
			return bdrs.map.roundNumber(value, 3);
		} else {
			// lat and lon falls through to here.
			return bdrs.map.roundNumber(value, 6);
		}
	} else {
		// default to 6 dp
		// lat and lon falls through to here.
	    return bdrs.map.roundNumber(value, 6);
	}
};

/**
 * Add a map click handler that makes a single point feature on a map when clicked.
 *
 * @param {OpenLayers.Map} map Map to add to.
 * @param {String} layerName name of layer.
 * @param {String} latitudeInputSelector selector.
 * @param {String} longitudeInputSelector selector.
 * @param {Object} options Options for layer.
 */
bdrs.map.addSingleClickPositionLayer = function(map, layerName, latitudeInputSelector, longitudeInputSelector, crsSelector, options){
    var layer = bdrs.map.addPositionLayer(layerName);

    var defaultOptions = {
    };
    options = jQuery.extend(defaultOptions, options);

    var updateLatLon = function(feature, pixel){
        var lonLat = map.getLonLatFromViewPortPx(pixel).clone();
        lonLat.transform(bdrs.map.GOOGLE_PROJECTION, bdrs.map.getProjection(crsSelector));
        jQuery(latitudeInputSelector).val(bdrs.map.roundCoord(lonLat.lat, crsSelector)).blur();
        jQuery(longitudeInputSelector).val(bdrs.map.roundCoord(lonLat.lon, crsSelector)).blur();
    };

    // Add a drag feature control to move features around.
    var dragFeature = new OpenLayers.Control.DragFeature(layer, {
        onDrag: updateLatLon,
        onComplete: updateLatLon
    });
    map.addControl(dragFeature);
    dragFeature.activate();

    var clickControl = new OpenLayers.Control.Click({
        handlerOptions: {
            "single": true,
            'featureLayerId': layer.id,
            'featureCount': 1,
            'latitudeInputSelector': latitudeInputSelector,
            'longitudeInputSelector': longitudeInputSelector,
			'crsSelector': crsSelector
        }
    });
    map.addControl(clickControl);
    clickControl.activate();

    return layer;
};

if (bdrs.openlayers.multiclickpositionlayer === undefined){
    bdrs.openlayers.multiclickpositionlayer = {};
    }

bdrs.openlayers.multiclickpositionlayer.stylemap = {
        'strokeWidth': 2,
        'strokeOpacity': 1,
        'strokeColor': '#EE9900',
        'fillColor': '#EE9900',
        'fillOpacity': 0.6,
        'pointRadius': 4
    };


bdrs.map.addMultiClickPositionLayer = function(layerName, featureAddedHandler, featureMovedHandler){
    var layer = new OpenLayers.Layer.Vector(layerName, {
        styleMap: new OpenLayers.StyleMap(bdrs.openlayers.multiclickpositionlayer.stylemap)
    });

    var map = bdrs.map.baseMap;
    map.addLayers([layer]);

    if (jQuery.isFunction(featureAddedHandler)) {
        layer.events.register('featureadded', null, function(event){
            featureAddedHandler(event.feature);
        });
    }

    // Add a drag feature control to move features around.
    var dragFeature = new OpenLayers.Control.DragFeature(layer, {
        onDrag: featureMovedHandler,
        onComplete: featureMovedHandler
    });
    map.addControl(dragFeature);
    dragFeature.activate();

    var clickControl = new OpenLayers.Control.Click({
        handlerOptions: {
            "single": true,

            'featureLayerId': layer.id,
            'featureCount': 0,
            'latitudeInputSelector': null,
            'longitudeInputSelector': null
        }
    });
    map.addControl(clickControl);
    clickControl.activate();

    return layer;
};

/**
 * Will only draw 1 feature at a time. Can be a point, line or polygon
 * depending on the config of the underlying editing toolbar
 *
 * @param {Object} map - the OpenLayers.Map object
 * @param {Object} layerName - name for the layer that we will draw on
 * @param {Object} options - the options for the draw layer. see DefaultOptions object
 */
bdrs.map.addSingleFeatureDrawLayer = function(map, layerName, options){

    var defaultOptions = {
        drawPoint: true,
        drawLine: false,
        drawPolygon: false,
        latSelector: "",
        longSelector: "",
		crsSelector: "",
        wktSelector: "",
        initialDrawTool: bdrs.map.control.DRAW_POINT
    };

    options = jQuery.extend(defaultOptions, options);

    var latSelector = options.latSelector;
    var longSelector = options.longSelector;
    var areaSelector = options.areaSelector;
    var wktSelector = options.wktSelector;
	var crsSelector = options.crsSelector;

    var triggerWktValidation = function(){
        // remove any error text from lat/long inputs
        // note that this can't run if javascript is disabled so
        // in this case, any messages returned by the server will
        // remain.
        $(latSelector).siblings('.error').text("");
        $(longSelector).siblings('error').text("");

        // trigger change event on move complete
        jQuery(wktSelector).change();
    };

    // Will remove all but the most recently added feature
    var featureAddedHandler = function(feature) {
        // protect from any shenanigans
        if (!feature) {
            return;
        }
        var layer = feature.layer;
        if (!layer) {
            return;
        }
        var featuresToRemove = new Array();
        for (var i = 0; i < layer.features.length; ++i) {
            if (feature != layer.features[i]) {
                featuresToRemove.push(layer.features[i]);
            }
        }
        layer.removeFeatures(featuresToRemove);

		var centroid = feature.geometry.getCentroid().clone();
        centroid.transform(bdrs.map.GOOGLE_PROJECTION, bdrs.map.getProjection(crsSelector));
        jQuery(longSelector).val(bdrs.map.roundCoord(centroid.x, crsSelector));
        jQuery(latSelector).val(bdrs.map.roundCoord(centroid.y, crsSelector));
        if (areaSelector && jQuery(areaSelector)) {
            var shape = feature.geometry;
            if (shape instanceof OpenLayers.Geometry.MultiPolygon ||
                    shape instanceof OpenLayers.Geometry.Polygon) {
				// round to a reasonable number...
                jQuery(areaSelector).html(bdrs.map.roundNumber(shape.getArea()/10000, 2));
                jQuery(areaSelector).val(bdrs.map.roundNumber(shape.getArea()/10000, 2));
            } else {
                jQuery(areaSelector).html('');
                jQuery(areaSelector).val('');
            }
        }

		var wktWriter = bdrs.map.getWktWriter(crsSelector);
        jQuery(wktSelector).val(wktWriter.write(feature));

        triggerWktValidation();
    };

    // note no trigger wkt validation - we don't want it to occur on drag
    var featureMovedHandler = function(feature, pixel){
        var wktWriter = bdrs.map.getWktWriter(crsSelector);
        var centroid = feature.geometry.getCentroid();
        centroid.transform(bdrs.map.GOOGLE_PROJECTION, bdrs.map.getProjection(crsSelector));
        jQuery(longSelector).val(bdrs.map.roundCoord(centroid.x, crsSelector));
        jQuery(latSelector).val(bdrs.map.roundCoord(centroid.y, crsSelector));
        jQuery(wktSelector).val(wktWriter.write(feature));
    };

    var featureMoveCompleteHandler = function(feature, pixel){

        featureMovedHandler(feature, pixel);

        triggerWktValidation();
    };

    var featureModifiedHandler = function(feature, pixel) {
        if (areaSelector && jQuery(areaSelector)) {
            var shape = feature.geometry;
            if (shape instanceof OpenLayers.Geometry.MultiPolygon ||
                    shape instanceof OpenLayers.Geometry.Polygon) {
				// round to a reasonable number...
                jQuery(areaSelector).html(bdrs.map.roundNumber(shape.getArea()/10000, 2));
                jQuery(areaSelector).val(bdrs.map.roundNumber(shape.getArea()/10000, 2));
            } else {
                jQuery(areaSelector).html('');
                jQuery(areaSelector).val('');
            }
        }

        featureMoveCompleteHandler(feature, pixel);
    }

    var drawOptions = {
        drawPoint: options.drawPoint,
        drawPolygon: options.drawPolygon,
        modifyFeature: true,
        drawLine: options.drawLine,
        initialDrawTool: options.initialDrawTool,
        toolActivatedHandler: options.toolActivatedHandler,

        featureAddedHandler: featureAddedHandler,
        featureMovedHandler: featureMovedHandler,
        featureMoveCompleteHandler: featureMoveCompleteHandler,
        featureModifiedHandler: featureModifiedHandler,
        dragStartHandler: options.dragStartHandler
    };

    return bdrs.map.addPolygonDrawLayer(map, layerName, drawOptions);
};

/**
 * cache object so we don't make lots of OpenLayers projection objects.
 */
bdrs.map.projectionCache = {};

/**
 * Get an OpenLayers.Projection object by srid, e.g. 4326
 * @param {String or int} srid SRID
 */
bdrs.map.getProjectionBySrid = function(srid) {
	return bdrs.map.getProjectionByEpsgCode("EPSG:"+srid);
};

/**
 * Get an OpenLayers.Projection object by EPSG code e.g. EPSG:4326
 * @param {String} code EPSG code
 */
bdrs.map.getProjectionByEpsgCode = function(code) {
	if (!bdrs.map.projectionCache[code]) {
		bdrs.map.projectionCache[code] = new OpenLayers.Projection(code);
	}
	var result = bdrs.map.projectionCache[code];
	return result;
};

/**
 * Get an open layers projection from the value contained in the crs selector.
 * we expect the value in the selector to be an SRID.
 * @param {jQuery selector string} crsSelector Selector for an input containing the desired SRID.
 */
bdrs.map.getProjection = function(crsSelector) {
	return bdrs.map.getProjectionBySrid(jQuery(crsSelector).val());
};

/**
 * Get a WKT writer from the value contained in the crs selector.
 * we expect the value in the selector to be an SRID.
 * @param {jQuery selector string} crsSelector Selector for an input containing the desired SRID.
 */
bdrs.map.getWktWriter = function(crsSelector) {
	return new OpenLayers.Format.WKT({
        'internalProjection': bdrs.map.GOOGLE_PROJECTION,
        'externalProjection': bdrs.map.getProjection(crsSelector)
    });
};

/**
 * Get a OpenLayers.Format.WKT object that converts to/from
 * the given srid.
 * @param {Object} srid SRID to convert to/from.
 */
bdrs.map.getWktFormatBySrid = function(srid) {
	return new OpenLayers.Format.WKT({
		'internalProjection': bdrs.map.GOOGLE_PROJECTION,
        'externalProjection': bdrs.map.getProjectionBySrid(srid)
	});
};

bdrs.map.addPolygonDrawLayer = function(map, layerName, options){

    var defaultOptions = {
        // called when feature added to map
        featureAddedHandler: function(){
        },
        // called when feature is dragged
        featureMovedHandler: function(){
        },
        // called on move complete vs on drag
        featureMoveCompleteHandler: function(){
        },
        // called on mouse down when dragging an item
        dragStartHandler: function() {
        }
    };
    options = jQuery.extend(defaultOptions, options);

    var layer = new OpenLayers.Layer.Vector(layerName, {        /*styleMap: new OpenLayers.StyleMap({
         'strokeWidth': 2,
         'strokeOpacity': 1,
         'strokeColor': '#EE9900',
         'fillColor': '#EE9900',
         'fillOpacity': 0.6,
         'pointRadius': 4
         })
         */
    });

    map.addLayers([layer]);

    if (jQuery.isFunction(options.featureAddedHandler)) {
        layer.events.register('featureadded', null, function(event){
            options.featureAddedHandler(event.feature);
        });
    }

    // Add a drag feature control to move features around.
    var dragFeature = new OpenLayers.Control.DragFeature(layer, {
        onDrag: options.featureMovedHandler,
        onComplete: options.featureMoveCompleteHandler,
        onStart: options.dragStartHandler
    });
    map.addControl(dragFeature);
    dragFeature.activate();
    // add the editing toolbar

    bdrs.map.addEditingToolbar(map, layer, dragFeature, options.featureMoveCompleteHandler, options);

    return layer;
};

bdrs.map.addEditingToolbar = function(map, layer, dragFeature, featureMovedHandler, options){
    var defaultOptions = {
        // default editing options
        modifyFeature: true,
        drawPoint: true,
        drawLine: true,
        drawPolygon: true,
        initialDrawTool: "drag",
        // handler that is called when an item on the toolbar
        toolActivatedHandler: null
    };

    options = jQuery.extend(defaultOptions, options);

    // OpenLayers' EditingToolbar internally creates a Navigation control, we
    // want a TouchNavigation control here so we create our own editing toolbar
    //var toolbar = new OpenLayers.Control.EditingToolbar(layer);
    var toolbar = new OpenLayers.Control.Panel({
        displayClass: 'olControlEditingToolbar'
    });
    var featureAdded = function(){
        // activate the first control to render the "navigation icon"
        // as active
        toolbar.controls[0].activate();
        // deactivate current control
        this.deactivate();
    };

    var activateDrag = function(dragOn){
        if (dragOn) {
            dragFeature.activate();
        }
        else {
            dragFeature.deactivate();
        }
    };

    var getActivateDragFunc = function(active){
        // clone the boolean flag to pass the value instead of reference
        // this helper function is here because calling the activateDrag function
        // directly failed!
        var activate = active;
        return function(){
            activateDrag(activate);
        };
    };

    // event listeners to turn the drag handling on/off and
    // trigger more events
    var getToolActivatedFunc = function(controlId){
        var id = controlId;
        var toolActivatedHandler = options.toolActivatedHandler;
        return function(){
            // activate drag if required
            activateDrag(id === bdrs.map.control.DRAG_FEATURE);
            if (toolActivatedHandler) {
                toolActivatedHandler(id);
            }
        };
    };

    // this control is just there to be able to deactivate the drawing
    // tools
    var navControl = new OpenLayers.Control({
        displayClass: 'olControlNavigation',
        autoActivate: true,
        defaultControl: true,
        title: "Select/Drag feature"
    });
    navControl.events.register("activate", navControl, getToolActivatedFunc(bdrs.map.control.DRAG_FEATURE));
    toolbar.addControls([navControl]);

    var drawPointControl = null;
    if (options.drawPoint) {
        drawPointControl = new OpenLayers.Control.DrawFeature(layer, OpenLayers.Handler.Point, {
            displayClass: 'olControlDrawFeaturePoint',
            featureAdded: featureAdded,
            title: "Draw a point"
        });
        drawPointControl.events.register("activate", drawPointControl, getToolActivatedFunc(bdrs.map.control.DRAW_POINT));
        toolbar.addControls([drawPointControl]);
    }

    if (options.drawLine) {
        var drawLineControl = new OpenLayers.Control.DrawFeature(layer, OpenLayers.Handler.Path, {
            displayClass: 'olControlDrawFeaturePath',
            featureAdded: featureAdded,
            title: "Draw a line"
        });
        drawLineControl.events.register("activate", drawLineControl, getToolActivatedFunc(bdrs.map.control.DRAW_LINE));
        toolbar.addControls([drawLineControl]);
    }

    if (options.drawPolygon) {
        var drawPolygonControl = new OpenLayers.Control.DrawFeature(layer, OpenLayers.Handler.Polygon, {
            displayClass: 'olControlDrawFeaturePolygon',
            featureAdded: featureAdded,
            title: "Draw a polygon"
        });
        drawPolygonControl.events.register("activate", drawPolygonControl, getToolActivatedFunc(bdrs.map.control.DRAW_POLYGON));
        toolbar.addControls([drawPolygonControl]);
    }

    var featureModifiedHandler = options.featureModifiedHandler ? options.featureModifiedHandler : featureMovedHandler;

    if (options.modifyFeature) {
        var modifyFeatureControl = new OpenLayers.Control.ModifyFeature(layer, {
            vertexRenderIntent: 'temporary',
            displayClass: 'olControlModifyFeature',
            featureModified: featureModifiedHandler,
            title: "Select and modify feature"
        });
        modifyFeatureControl.events.register("activate", modifyFeatureControl, getToolActivatedFunc(bdrs.map.control.MODIFY_FEATURE));
        // trigger after each vertice move completes
        layer.events.register("featuremodified", null, function(event){
            featureModifiedHandler(event.feature, event.pixel);
        });
        toolbar.addControls([modifyFeatureControl]);
    }

    map.addControl(toolbar);

    if (options.initialDrawTool === bdrs.map.control.DRAW_POINT) {
        if (drawPointControl) {
            drawPointControl.activate();
            // if we were asked to select a drawing tool but point isn't there, make it a line
        } else if (drawLineControl) {
            drawLineControl.activate();
            // if we were asked to select a drawing tool but line isn't there, make it a poly.
        } else if (drawPolygonControl) {
            drawPolygonControl.activate();
        } else {
            // for some reason we have no drawing controls. select the nav tool
            navControl.activate();
        }
    }
    else {
        // activate the first control to render the "navigation icon"
        // as active - this is the default
        navControl.activate();
    }
};

bdrs.map.addFeaturePopUpHandler = function(event){
    var control = bdrs.map.addFeatureClickPopup(event.layer);
    event.layer.selectPopUpControl = control;
};

bdrs.map.removeFeaturePopUpHandler = function(event){
    delete event.layer.selectPopupControl;
};

bdrs.map.addFeatureClickPopup = function(layer){
    var onPopupClose = function(evt){
        // 'this' is the popup.
        selectControl.unselect(this.feature);
    };

    var onFeatureSelect = function(feature){
        var selectedFeature = feature;
		if (feature.cluster) {
			var descObj = jQuery.parseJSON(feature.cluster[0].attributes.description);

	        var descObjArray = new Array();
	        for (var i = 0; i < feature.cluster.length; ++i) {
	            descObjArray.push(jQuery.parseJSON(feature.cluster[i].attributes.description));
	        }

	        var popupOptions = {
	            onPopupClose: onPopupClose,
	            featureToBind: feature
	        };

	        bdrs.map.createFeaturePopup(bdrs.map.baseMap, feature.geometry.getBounds().getCenterLonLat(), descObjArray, popupOptions);
		}
    };
    var onFeatureUnselect = function(feature){
        if (feature.popup) {
            bdrs.map.baseMap.removePopup(feature.popup);
            feature.popup.destroy();
            feature.popup = null;
        }
    };

    var selectControl = new OpenLayers.Control.SelectFeature(layer, {
        onSelect: onFeatureSelect,
        onUnselect: onFeatureUnselect
    });

    bdrs.map.baseMap.addControl(selectControl);
    selectControl.activate();

    return selectControl;
};

// note bdrs.map.getRecord shares the same interface with bdrs.map.getFeature
bdrs.map.getRecord = function(id, successCallbackFcn, errorCallbackFcn, completeCallbackFcn){
    jQuery.ajax(bdrs.contextPath + '/webservice/record/getRecordById_v2.htm', {
        data: {
            regkey: bdrs.ident,
            recordId: id
        },
        success: successCallbackFcn,
        error: errorCallbackFcn,
        complete: completeCallbackFcn
    });
};

// note bdrs.map.getRecord shares the same interface with bdrs.map.getFeature
bdrs.map.getFeature = function(id, successCallbackFcn, errorCallbackFcn, completeCallbackFcn){

    jQuery.ajax(bdrs.contextPath + '/bdrs/map/getFeatureInfoById.htm', {
        data: {
            featureId: id
        },
        success: successCallbackFcn,
        error: errorCallbackFcn,
        complete: completeCallbackFcn
    });
};

/**
 * Create content state for the popup
 *
 * @param {Array} itemArray json items (records or geo map features) to display. These should not include
 * attribute values.
 * @param {Object} popup Openlayers popup object.
 * @param {Object} mapServerQueryManager Object to handle queries to MapServer. Used for the highlight layer.
 */
bdrs.map.createContentState = function(itemArray, popup, mapServerQueryManager){

    var popupContent = jQuery("<div></div>").addClass("popupContent");

    popupContent.appendTo(popup.contentDiv);

    var result = {
        currentPage: 0,
        itemArray: itemArray,
        popup: popup,
        mapServerQueryManager: mapServerQueryManager,
        loadingCounter: 0
    };

    return result;
};

/**
 * Appends html to the popup content.
 *
 * @param {Object} contentState ContentState dict
 * @param {String} htmlContent content to append
 */
bdrs.map.appendContentPage = function(contentState, htmlContent) {
    var popupContent = jQuery(contentState.popup.contentDiv).find(".popupContent");
    popupContent.append(htmlContent);
};

/**
 * Get the jQuery wrapped dom element for the table body
 * for a given item page in the popup content.
 *
 * @param {Object} contentState ContentState dict
 * @param {int} itemIndex Index of item
 * @return {Object} jQuery node
 */
bdrs.map.getPopupContentTableBody = function(contentState, itemIndex) {
    // each page has the class "popupPage" + itemIndex
    return jQuery(contentState.popup.contentDiv).find(".popupPage"+itemIndex).find("tbody");
};

/**
 * Create the content html
 *
 * @param {Object} item Record or GeoMapFeature
 * @param {int} itemIndex Index of item in popup
 * @return {String} html content
 */
bdrs.map.createContentHtml = function(item, itemIndex) {

    var tbody = jQuery("<tbody></tbody>");

    var linkValue = jQuery("<div></div>");
    if (item.type == "record") {
        // record specific stuff
        var recordAttrKeys = ["owner", "census_method", "species", "common_name", "number", "notes", "habitat", "when", "behaviour", "coord_ref_system", "x", "y"];
        var recordId = item["recordId"];
        var ownerId = item["ownerId"];
        var recordVisibility = item["recordVisibility"];

        // if the record is public or if you are the owner or admin, add the view link
        if (recordVisibility == 'PUBLIC' || (bdrs.authenticated && (bdrs.authenticatedUserId === ownerId || bdrs.isAdmin))) {
            var viewRecordRow = jQuery("<tr><td></td></tr>");
            viewRecordRow.attr('colspan', '2');
            var surveyId = item["surveyId"];
            var recordUrl = bdrs.portalContextPath + "/bdrs/user/surveyRenderRedirect.htm?surveyId=" + surveyId + "&recordId=" + recordId;
            jQuery("<a>View&nbsp;Full&nbsp;Record</a>").attr('href', recordUrl).appendTo(linkValue);
            jQuery("<a>View&nbsp;Record</a>").attr('href', recordUrl).appendTo(viewRecordRow.find("td"));
            tbody.append(viewRecordRow);
        }
        if (bdrs.authenticated) {
            var requestRecordInfoRow = jQuery("<tr><td></td></tr>");
            requestRecordInfoRow.attr('colspan', '2');
            var requestRecordInfoUrl = bdrs.portalContextPath + "/bdrs/user/contactRecordOwner.htm?recordId=" + recordId;
            jQuery("<a>Contact&nbsp;Owner</a>").attr('href', requestRecordInfoUrl).appendTo(requestRecordInfoRow.find("td"));
            tbody.append(requestRecordInfoRow);
        }
        var crs = item["coord_ref_system"];
        for (var i = 0; i < recordAttrKeys.length; i++) {
            var key = recordAttrKeys[i];
            var value;
            if (key === 'when') {
                // Only show the date if non null else we get 'NaN' in the formatted
                // date string. The following actually omits the 'when' row
                var formatted_date = item._when_formatted;
                if (formatted_date !== undefined && formatted_date !== null) {
                    value = item._when_formatted;
                }
                else {
                    value = "";
                }
            } else if (key === 'species' && item[key]) {
                value = jQuery("<i></i>").append(item[key]);
            } else if ((key === 'number' || key ==='x' || key === 'y') && item[key]) {
                value = item[key];
                // It will be a number, change to string
                if (value.toString) {
                    value = value.toString();
                }
            } else if (key === 'coord_ref_system' && crs) {
                value = crs.name;
            } else {
                value = item[key];
            }

            // value set, alter key if required
            if (key === "x") {
                if (crs) {
                    key = crs.xname;
                }
            } else if (key === "y") {
                if (crs) {
                    key = crs.yname;
                }
            } else if (key === "coord_ref_system") {
                key = "Coord Ref System";
            }

            if (value && value !== null && value.length > 0 && value !== '-1') {
                var row = jQuery("<tr></tr>");
                row.append(jQuery("<th></th>").css('whiteSpace', 'nowrap').append(titleCaps(key.replace("_", " ")) + ":"));
                row.append(jQuery("<td></td>").css('whiteSpace', 'nowrap').append(value));
                tbody.append(row);
            }
        }
    }
    else if (item.type === "geoMapFeature") {
            // Map feature specific stuff
            var mapFeatureTitle = jQuery("<tr></tr>");
            mapFeatureTitle.attr('colspan', '2');
            jQuery("<span>Map&nbsp;Feature<span>").appendTo(mapFeatureTitle);
            tbody.append(mapFeatureTitle);
    }

    if (item.attributes && jQuery.isArray(item.attributes)) {
        var attrArray = item.attributes;

        var k, v, type, attrType;
        for (var j = 0; j < attrArray.length; j++) {
            var tuple = attrArray[j];

            k = tuple["name"];
            v = tuple["value"];
            type = tuple["type"];
            attrType = bdrs.model.taxa.attributeType.code[type];
            // if it is a census method attribute, the value is a link the the full record
            if (attrType.isCensusMethodType()) {
                v = linkValue.html();
            }
            // if v is a number, change it to a string..
            if (v && v.toString) {
                v = v.toString();
            }
            if (v && v.length > 0 && v !== '-1') {
                var r = jQuery("<tr></tr>");
                r.append(jQuery("<th></th>").css('whiteSpace', 'nowrap').append(k + ":"));
                var dataCell = jQuery("<td></td>");
                dataCell.css('whiteSpace', 'nowrap').append(v);
                if (bdrs.model.taxa.attributeType.SPECIES === attrType) {
                    dataCell.addClass("scientificName");
                }
                r.append(dataCell);
                tbody.append(r);
            }
        }
    }

    var table = jQuery("<table></table>").append(tbody);
    table.addClass("kmlDescriptionTable");
    var tableDiv = jQuery("<div></div>").append(table);
    tableDiv.addClass("popupPage" + itemIndex);

    // information is initially hidden
    tableDiv.hide();
    return tableDiv;
}

/**
 * Get the left arrow click handler which decrements the current popup page
 *
 * @param {Object} contentState ContentState dict
 * @return {Function} handler func
 */
bdrs.map.getPopupLeftHandler = function(contentState){
    var myContentState = contentState;
    return function(){
        if (myContentState.currentPage > 0) {
            myContentState.currentPage -= 1;
            bdrs.map.displayPopupPage(myContentState);
        }
    };
};

/**
 * Get the right arrow click handler which increments the current popup page
 *
 * @param {Object} newContentState ContentState dict
 * @return {Function} handler func
 */
bdrs.map.getPopupRightHandler = function(newContentState){
    var contentState = newContentState;
    return function(){
        if (contentState.currentPage < contentState.itemArray.length - 1) {
            contentState.currentPage += 1;
            bdrs.map.displayPopupPage(contentState);
        }
    };
};

/**
 * Displays the current page of the content state
 *
 * @param {Object} contentState ContentState dict
 */
bdrs.map.displayPopupPage = function(contentState){
    jQuery(".currentPage", contentState.popup.contentDiv).text(contentState.currentPage + 1);

    // if the html content has not been generated yet...
    if (!contentState.itemArray[contentState.currentPage].htmlContent) {
        // download and generate it.
        bdrs.map.createPopupPage(contentState, contentState.currentPage);
    } else {
        bdrs.map.setPopupPageVisible(contentState);
    }
};

/**
 * Helper to get the record ID
 *
 * @param {Object} item record
 * @return {int} record ID or null
 */
bdrs.map.getItemRecordId = function(item) {
    if (item.type === "record") {
        return item.recordId;
    } else {
        return null;
    }
};

/**
 * Helper to get the geo map feature ID
 *
 * @param {Object} item geo map feature
 * @return {int} geo map feature ID or null
 */
bdrs.map.getItemFeatureId = function(item) {
    if (item.type === "geoMapFeature") {
        return item.id;
    } else {
        return null;
    }
};

/**
 * Create the popup page
 *
 * @param {Object} contentState ContentState dict
 * @param {int} itemIndex item index
 * It must fulfill an interface, see in code.
 */
bdrs.map.createPopupPage = function(contentState, itemIndex) {
    var itemIndex = contentState.currentPage;
    var item = contentState.itemArray[itemIndex];
    var type = item.type;
    var surveyId = item["surveyId"];

    var getInfoFunc = null;
    var id = null;
    if (type === "record") {
        getInfoFunc = bdrs.map.getRecord;
        id = bdrs.map.getItemRecordId(item);
    } else if (type === "geoMapFeature") {
        getInfoFunc = bdrs.map.getFeature;
        id = bdrs.map.getItemFeatureId(item);
    } else {
        throw 'type not handled : ' + type;
    }

    if (!id) {
        throw 'invalid id to try to fetch : ' + id;
    }

    jQuery(".loadingPageSpinner").show();
    // keep track of loading counter to stop popup jitter
    contentState.loadingCounter += 1;

    getInfoFunc(id,
        // success handler
        function(rec) {
            item.htmlContent = bdrs.map.createContentHtml(rec, itemIndex);
            bdrs.map.appendContentPage(contentState, item.htmlContent);
        },
        // error handler
        function(ev) {
            // create page saying failed to get data
            console.log('error getting attributes');
        },
        // complete handler
        function() {
            contentState.loadingCounter -= 1;
            if (contentState.loadingCounter === 0) {
                jQuery(".loadingPageSpinner").hide();
                bdrs.map.setPopupPageVisible(contentState);
                contentState.popup.show();
            }
        }
    );
};

/**
 * Makes the current page visible
 *
 * @param {Object} contentState ContentState dict
 */
bdrs.map.setPopupPageVisible = function(contentState){
    jQuery(".currentPage", contentState.popup.contentDiv).text(contentState.currentPage + 1);
    // if a query manager was passed in, query the map server appropriately!
    if (contentState.mapServerQueryManager) {
        var qm = contentState.mapServerQueryManager;
        var item = contentState.itemArray[contentState.currentPage];
        qm.highlightFeature(item);
    }
    for (var i = 0; i < contentState.itemArray.length; ++i) {
        jQuery(".popupPage" + i).hide();
    }
    jQuery(".popupPage" + contentState.currentPage).show();
    if (contentState.currentPage == contentState.itemArray.length - 1) {
        // hide the right arrow
        jQuery(".shiftContentRight").css("visibility", "hidden");
    }
    else {
        // show the right arrow
        jQuery(".shiftContentRight").css("visibility", "visible");
    }

    if (contentState.currentPage === 0) {
        // hide the left arrow
        jQuery(".shiftContentLeft").css("visibility", "hidden");
    }
    else {
        // show the left arrow
        jQuery(".shiftContentLeft").css("visibility", "visible");
    }
    contentState.popup.updateSize();
};

//-----------------------------------------------
/**
 * Adds the Geocoder Control to the map
 *     Have hacked it in here...
 *       Putting it in Openlayers.js (while making more sense) would
 *     mean we would forever have to maintain our custom OL build
 */
bdrs.map.addGeocodeControl = function(geocodeOptions){
    // See bdrs.map.createDefaultMap for a description of geocode options.
    var defaultOptions = {
        useKeyHandler: false,
        zoom: -1
    };
    var options = jQuery.extend(true, {}, defaultOptions, geocodeOptions);

    if (options.selector === undefined || options.selector === null) {
        // Cannot do anything without a container for the geocode inputs
        return;
    }

    var geocodeContainer = jQuery(options.selector);
    if (geocodeContainer.length === 0) {
        // Cannot find the element so we can't add controls to it.
        return;
    }

    var inputGeocode = jQuery('<input type="text"></input>');
    inputGeocode.attr({
        'id': 'inputGeocode'
    });

    if (options.useKeyHandler !== false) {
        inputGeocode.keydown(function(event){
            bdrs.map.geocode(options, jQuery("#inputGeocode").val());
            if (event.keyCode == 13) {
                return false;
            }
                    });
    }

    var btnGeocode = jQuery('<input type="submit"></input>');
    btnGeocode.attr({
        'id': 'btnGeocode',
        'value': 'Find'
    });
    btnGeocode.addClass("form_action");
    btnGeocode.click(function(){
        bdrs.map.geocode(options, jQuery("#inputGeocode").val(), options.clickHandler);
        return false;
    });

    var frmGeocode = jQuery("<form></form>");
    frmGeocode.attr({
        'id': 'geocodeForm'
    });
    frmGeocode.append(inputGeocode);
    frmGeocode.append(btnGeocode);
    geocodeContainer.append(frmGeocode);
};

//Takes a text string, sets map view to point with no return
bdrs.map.geocode = function(options, address, doAfter){
    if (address.length > 3 && window.GClientGeocoder !== undefined) {
        address = address + ', Australia';
        var geocoder = new GClientGeocoder();
        if (geocoder) {

            geocoder.getLocations(address, function(placemark) {
                if (placemark && placemark.Placemark[0]) {
                    if (placemark.Placemark[0].ExtendedData) {
                        var extents = placemark.Placemark[0].ExtendedData.LatLonBox;
                        var bounds = new OpenLayers.Bounds(extents.west, extents.south, extents.east, extents.north);
                        bdrs.map.baseMap.zoomToExtent(bounds.transform(bdrs.map.WGS84_PROJECTION, bdrs.map.GOOGLE_PROJECTION), true);
                    }
                    else {
                        var zoom;
                        if(options.zoom < 0) {
                            zoom = bdrs.map.baseMap.baseLayer.maxZoomLevel;
                        } else {
                            zoom = options.zoom;
                        }
                        var centre = placemark.Placemark[0].Point.coordinates;
                        var lonLat = new OpenLayers.LonLat(centre[0], centre[1]);
                        bdrs.map.baseMap.setCenter(lonLat.transform(bdrs.map.WGS84_PROJECTION, bdrs.map.GOOGLE_PROJECTION), zoom);
                    }
                }
                else {

                    //Set to Australia wide view
                    bdrs.map.baseMap.setCenter(new OpenLayers.LonLat(130, -27).transform(bdrs.map.WGS84_PROJECTION, bdrs.map.GOOGLE_PROJECTION), 4);
                }

            });

        }
    }
};

/**
 * The latLonToNameTimer is used to prevent sending too many requests to the
 * google server. The timer is restarted each time the function is invoked.
 */
bdrs.map.latLonToNameTimer = null;
/**
 * Retrieves the name of the lat lon coordinate.
 *
 * @param latitude the latitude coordinate
 * @param longitude the longitude coordinate
 * @param callback the callback function. This function will receive a single
 * parameter which is the name of the location.
 */
bdrs.map.latLonToName = function(latitude, longitude, callback){

    if (bdrs.map.latLonToNameTimer !== null) {
        clearTimeout(bdrs.map.latLonToNameTimer);
    }

    bdrs.map.latLonToNameTimer = setTimeout(function(){
        new GClientGeocoder().getLocations(new GLatLng(latitude, longitude), function(data){
            var name;
            if (data !== null && data.Status.code === 200) {
                if (data.Placemark.length > 0) {
                    name = data.Placemark[0].address;
                }
                else {
                    name = data.name;
                }
            }
            else {
                name = [latitude, longitude].join(', ');
            }
            callback(name);
        });
    }, 500);
};

bdrs.map.centerMapToLayerExtent = function(map, layer){
    var extent = null;
    if (layer instanceof Array) {
        // get the extent from all of the layers
        for (var i = 0; i < layer.length; i++) {
            var thislayer = layer[i];
            if (extent !== null && thislayer && thislayer.getDataExtent() !== null) {
                extent.extend(thislayer.getDataExtent());
            } else if (thislayer) {
                extent = thislayer.getDataExtent();
            }
        }
    } else {
        extent = layer.getDataExtent();
    }

    if (extent && extent !== null) {
        bdrs.map.baseMap.zoomToExtent(extent, false);
    }
    else {
        bdrs.map.setDefaultCenter(bdrs.map.baseMap);
    }
};

bdrs.map.centerMap = function(map, center, zoom){
    if (!map) {
        map = bdrs.map.baseMap;
    }
    if (center) {
        if (!zoom) {
            zoom = map.getZoomForExtent(center);
        }

        map.setCenter(center, zoom);
    }
    else {
        bdrs.map.setDefaultCenter(map);
    }
};

bdrs.map.centerProjectMap = function(map){
    var arrayLocation = jQuery('#location').options;

    bdrs.map.zoomToLocations(arrayLocation);
};

bdrs.map.zoomToLocations = function(arrayLocation){
    if (arrayLocation.length > 1) {
        var locIds = [];
        for (var i = 0; i < arrayLocation.length; i++) {
            locIds.push(arrayLocation[i].value);
        }

        bdrs.map.zoomToLocationsById(locIds);
    }
    else {
        bdrs.map.setDefaultCenter(map);
    }
};

bdrs.map.zoomToLocationsById = function(locIds){
    jQuery.getJSON(bdrs.portalContextPath + "/webservice/location/getLocationsById.htm", {
        ids: JSON.stringify(locIds)
    }, function(data){
        var wkt = new OpenLayers.Format.WKT(bdrs.map.wkt_options);
        var feature = wkt.read(data.geometry);
        bdrs.map.zoomToFeatures(feature);
    });
};

bdrs.map.zoomToFeatures = function(feature){
    var geobounds;
    if (feature.length >= 1) {
        geobounds = feature[0].geometry.getBounds();
        for (var i = 1; i < feature.length; i++) {
            geobounds.extend(feature[i].geometry.getBounds());
        }
    }
    else
        if (feature.geometry) {
            geobounds = feature.geometry.getBounds();
        }
        else {
            geobounds = new OpenLayers.LonLat(136.5, -28.5);
        }
    var zoom = map.getZoomForExtent(geobounds);
    bdrs.map.centerMap(map, geobounds.getCenterLonLat(), zoom);
};

bdrs.map.setDefaultCenter = function(map){
    var latitude = bdrs.map.defaultCenterLat ? bdrs.map.defaultCenterLat : -28.5;
    var longitude = bdrs.map.defaultCenterLong ?  bdrs.map.defaultCenterLong : 136.5;
    var zoom = bdrs.map.defaultCenterZoom ? bdrs.map.defaultCenterZoom : 3;
    map.setCenter(new OpenLayers.LonLat(longitude, latitude).transform(bdrs.map.WGS84_PROJECTION, bdrs.map.GOOGLE_PROJECTION), zoom);
};


bdrs.map.addCrsChangeHandler = function(layer, longSelector, latSelector, wktSelector, crsSelector) {
	bdrs.map.addLonLatChangeHandler(layer, longSelector, latSelector, crsSelector, {});
    jQuery(crsSelector).change(function() {
        var feature = layer.features.length > 0 ? layer.features[0] : null;
        var proj = bdrs.map.getProjection(crsSelector);
        if (feature) {
			var wktWriter = bdrs.map.getWktWriter(crsSelector);
			jQuery(wktSelector).val(wktWriter.write(feature));
            var point = feature.geometry.getCentroid().clone();
            point.transform(bdrs.map.GOOGLE_PROJECTION, proj);
            jQuery(longSelector).val(bdrs.map.roundCoord(point.x, crsSelector));
            jQuery(latSelector).val(bdrs.map.roundCoord(point.y, crsSelector));
        }
    }).change();
};

// Attaches for handling when longitude/latitude inputs are changed.
// Will put the new feature on the draw layer
//
// layer: OpenLayers Layer object
// longSelector: jquery selector for the longitude input
// latSelector: jquery selector for the latitude input
bdrs.map.addLonLatChangeHandler = function(layer, longSelector, latSelector, crsSelector, options){

	if (!options) {
		options = {};
	}

    jQuery(longSelector).unbind('change');
    jQuery(latSelector).unbind('change');

    var defaultOptions = {
        // only 1 feature on the layer at a time
        uniqueFeature: true
    };

    options = jQuery.extend(defaultOptions, options);

    var handler = function(){
        if (jQuery(latSelector).val() !== '' && jQuery(longSelector).val() !== '') {
            if (options.uniqueFeature) {
                layer.removeFeatures(layer.features);
            }
            // draw a new point
            var lonLat = new OpenLayers.LonLat(jQuery(longSelector).val(), jQuery(latSelector).val());
            lonLat = lonLat.transform(bdrs.map.getProjection(crsSelector), bdrs.map.GOOGLE_PROJECTION);
            layer.addFeatures(new OpenLayers.Feature.Vector(new OpenLayers.Geometry.Point(lonLat.lon, lonLat.lat)));
        }
    };
    jQuery(longSelector).change(handler);
    jQuery(latSelector).change(handler);
};

//-----------------------------------------------

bdrs.map.initOpenLayers = function(){
    bdrs.map.GOOGLE_PROJECTION = new OpenLayers.Projection('EPSG:900913');
    bdrs.map.WGS84_PROJECTION = new OpenLayers.Projection('EPSG:4326');
    bdrs.map.wkt_options = {
        'internalProjection': bdrs.map.GOOGLE_PROJECTION,
        'externalProjection': bdrs.map.WGS84_PROJECTION
    };
};

bdrs.map.initPointSelectClickHandler = function(map){
    if (!map) {
        throw 'must pass in a map argument';
    }

    // --------------------------------------
    // Extension point for OpenLayers
    // --------------------------------------
    // Click handler for adding features (points) to the map.
    OpenLayers.Control.Click = OpenLayers.Class(OpenLayers.Control, {
        defaultHandlerOptions: {
            'single': true,
            'double': false,
            'pixelTolerance': 0,
            'stopSingle': false,
            'stopDouble': false,

            'featureLayerId': null,
            'featureCount': 0,
            'latitudeInputSelector': null,
            'longitudeInputSelector': null
        },

        initialize: function(options){
            this.handlerOptions = OpenLayers.Util.extend({}, this.defaultHandlerOptions);
            OpenLayers.Control.prototype.initialize.apply(this, arguments);
            this.handler = new OpenLayers.Handler.Click(this, {
                'click': this.onClick
            }, this.handlerOptions);
        },

        onClick: function(evt){
            // You didn't specify what layer to put the feature.
            if (this.handlerOptions.featureLayerId === null) {
                return;
            }
            // Check for the number of features.
            var layer = map.getLayer(this.handlerOptions.featureLayerId);
            if (this.handlerOptions.featureCount > 0 &&
            layer.features.length >= this.handlerOptions.featureCount) {
                return;
            }

            var lonLat = map.getLonLatFromViewPortPx(evt.xy).clone();
            var feature = new OpenLayers.Feature.Vector(new OpenLayers.Geometry.Point(lonLat.lon, lonLat.lat));

			var crsSelector = this.handlerOptions.crsSelector;
			var dstProj = bdrs.map.getProjection(crsSelector);

            lonLat.transform(bdrs.map.GOOGLE_PROJECTION, dstProj);
            // The extra blur is for ketchup
            if (this.handlerOptions.latitudeInputSelector !== null) {
                jQuery(this.handlerOptions.latitudeInputSelector).val(bdrs.map.roundCoord(lonLat.lat, crsSelector)).trigger("change").trigger("blur");
            }
            if (this.handlerOptions.longitudeInputSelector !== null) {
                jQuery(this.handlerOptions.longitudeInputSelector).val(bdrs.map.roundCoord(lonLat.lon, crsSelector)).trigger("change").trigger("blur");
            }

            if (layer !== null) {
				if (this.handlerOptions.single === true) {
					layer.removeFeatures(layer.features);
				}
                layer.addFeatures(feature);
            }
        }
    });
};

bdrs.map.initAjaxFeatureLookupClickHandler = function(map, options){
    if (!map) {
        throw 'must pass in a map argument';
    }

    if (options === undefined) {
        options = {};
    }
    var defaultOptions = {};

    options = jQuery.extend(defaultOptions, options);

    var mapServerQueryManager = new bdrs.map.MapServerQueryManager({
        wmsHighlightLayer: options.wmsHighlightLayer
    });

    var popupOptions = {
        mapServerQueryManager: mapServerQueryManager
    };

    // --------------------------------------
    // Extension point for OpenLayers
    // --------------------------------------
    // Click handler for adding features (points) to the map.
    OpenLayers.Control.Click = OpenLayers.Class(OpenLayers.Control, {
        defaultHandlerOptions: {
            'single': true,
            'double': false,
            'pixelTolerance': 0,
            'stopSingle': false,
            'stopDouble': false,
            'delay': 100,
            'featureLayerId': null,
            'featureCount': 0,
            'latitudeInputSelector': null,
            'longitudeInputSelector': null
        },

        initialize: function(options){
            this.handlerOptions = OpenLayers.Util.extend({}, this.defaultHandlerOptions);
            OpenLayers.Control.prototype.initialize.apply(this, arguments);
            this.handler = new OpenLayers.Handler.Click(this, {
                'click': this.onClick
            }, this.handlerOptions);
        },

        onClick: function(evt){

            bdrs.map.clearPopups(map);

            mapServerQueryManager.unhighlightAll();

            var googleProjectionLonLat = map.getLonLatFromViewPortPx(evt.xy);
            var lonLat = map.getLonLatFromViewPortPx(evt.xy);
            lonLat.transform(bdrs.map.GOOGLE_PROJECTION, bdrs.map.WGS84_PROJECTION);
            var lat = bdrs.map.roundLatOrLon(lonLat.lat);
            var lon = bdrs.map.roundLatOrLon(lonLat.lon);

            var mapLayerIds = bdrs.map.getSelectedBdrsLayerIds(map);

            // buffer based on current map zoom level...
            // pixel tolerance based on the default point placemark - a circle
            // of radius 4 pixels.
            var bufferKm = bdrs.map.calcClickBufferKm(map, 4);

            var ajaxParams = {
                latitude: lat,
                longitude: lon,
                buffer: bufferKm,
                mapLayerId: mapLayerIds
            };

            jQuery.ajax({
                url: bdrs.portalContextPath + '/bdrs/map/getFeatureInfo.htm',
                data: jQuery.param(ajaxParams, true),
                success: function(data, textStatus, jqXhr){
                    if (data.items && jQuery.isArray(data.items) && data.items.length > 0) {
                        bdrs.map.createFeaturePopup(map, googleProjectionLonLat, data["items"], popupOptions);
                    }
                },
                error: function(data){
                    bdrs.message.set("Error retrieving feature info");
                }
            });
        }
    });

    var clickControl = new OpenLayers.Control.Click();
    map.addControl(clickControl);
    clickControl.activate();
};

// returns an array of selected layer Ids.
// will only return layers that are part of the BDRS
bdrs.map.getSelectedBdrsLayerIds = function(map) {
    var mapLayerIds = new Array();
    var visibleLayers = map.getLayersBy("visibility", true);
    for (var i = 0; i < visibleLayers.length; ++i) {
        // check the duck punched property which indicates
        // whether the layer is a bdrs layer.
        // the layer also needs to be in zoom limit range
        if (visibleLayers[i].bdrsLayerId && visibleLayers[i].calculateInRange()) {
            mapLayerIds.push(visibleLayers[i].bdrsLayerId);
        }
    }
    return mapLayerIds;
};

bdrs.map.clearPopups = function(map){
    // Clear all popups
    while (map.popups.length > 0) {
        map.removePopup(map.popups[0]);
    }
};

bdrs.map.createFeaturePopup = function(map, googleProjectionLonLatPos, featureArray, options){

    if (!options) {
        options = {};
    }
    defaultOptions = {
        popupName: "popcorn",
        onPopupClose: null,
        mapServerQueryManager: null,
        featureToBind: null
    };
    options = jQuery.extend(defaultOptions, options);

    // the final target....
    var content = jQuery("<span></span>");
    var cyclerDiv = jQuery("<div></div>").addClass("textcenter").appendTo(content);

    // only add the arrows if more than one total page
    var leftShiftDiv = jQuery('<div><span class="loadingPageSpinner"><img src="' + bdrs.contextPath + '/images/wdTree/tree/loading.gif" /></span></div>').addClass("shiftContentLeftContainer").appendTo(content);;
    var rightShiftDiv = jQuery('<div></div>').addClass("shiftContentRightContainer textright").appendTo(content);
    jQuery('<img src="' + bdrs.contextPath + '/images/icons/left.png" />').addClass("shiftContentLeft").prependTo(leftShiftDiv);
    jQuery("<span></span>").addClass("currentPage").appendTo(cyclerDiv);
    jQuery("<span>&nbsp;of&nbsp;</span>").appendTo(cyclerDiv);
    jQuery("<span></span>").addClass("totalPages").appendTo(cyclerDiv);
    // only add the arrows if more than one total page
    jQuery('<img src="' + bdrs.contextPath + '/images/icons/right.png" />').addClass("shiftContentRight").appendTo(rightShiftDiv);

    var popup = new OpenLayers.Popup.FramedCloud(options.popupName, googleProjectionLonLatPos, null, content.html(), null, true, options.onPopupClose);

    // stops scroll bars appearing in the popup.
    // the popup will resize to the content
    jQuery(popup.contentDiv).css("overflow", "hidden");

    if (options.featureToBind) {
        var feature = options.featureToBind;
        feature.popup = popup;
        // So we can deselect the feature later via onPopupClose
        popup.feature = feature;
    }

    // popup is hidden until data is finished downloading / rendering.
    popup.hide();
    map.addPopup(popup);

    // now the dom is recreated from our content string, add our handlers and content state
    var contentState = bdrs.map.createContentState(featureArray, popup, options.mapServerQueryManager);
    bdrs.map.displayPopupPage(contentState);
    jQuery(".shiftContentLeft", popup.contentDiv).click(bdrs.map.getPopupLeftHandler(contentState));
    jQuery(".shiftContentRight", popup.contentDiv).click(bdrs.map.getPopupRightHandler(contentState));
    jQuery(".totalPages", popup.contentDiv).text(contentState.itemArray.length);

    return popup;
};

//--------------------------------------
// Attributes
//--------------------------------------

// define here so it can be called from updateLocation below
if (bdrs.attribute === undefined) {
    bdrs.attribute = {};
}


//--------------------------------------
//Survey
//--------------------------------------

bdrs.survey = {};

/**
 * Deletes a record via ajax post.
 * @param userIdent - The users registrationKey.
 * @param recordId - The server id of the record that needs to be deleted.
 * @param recordSelector [optional] - The id of the element that needs to be removed from the DOM.
 * @param msgSelector [optional] - The id of the domElement that will receive a statusMessage in regards to the delete action.
 */
bdrs.survey.deleteAjaxRecord = function(userIdent, recordId, recordSelector, msgSelector, rowIndexElem) {
    if(recordId !== undefined && recordId !== ""){
        if(confirm('Are you sure you want to delete this record?')) {
            var statusMsg;
            jQuery.ajax({
                url: bdrs.portalContextPath + "/webservice/record/deleteRecord.htm",
                type: "POST",
                data: {
                    ident: userIdent,
                    recordId: recordId
                },
                success: function(data){
                    if(data.succeeded){
                        if(recordSelector !== undefined){
                            jQuery(recordSelector).remove();
                        }
                        // have to unbind the disable handler so the form can be submitted
                        bdrs.unbindDisableHandler();
                    } else {
                        bdrs.message.set("The record is not deleted.");
                    }
                },
                error: function(){
                    bdrs.message.set("The record is not deleted.");
                }
            });
        }
    } else {
        jQuery(recordSelector).remove();
        // have to unbind the disable handler so the form can be submitted
        bdrs.unbindDisableHandler();
    }
};

bdrs.survey.location = {};

bdrs.survey.location.LAYER_NAME = 'Position Layer';
bdrs.survey.location.LOCATION_LAYER_NAME = 'Location Layer';

bdrs.survey.location.updateLocation = function(pk, surveyId, options) {
    if(pk > 0) {
        var params = {id: pk, surveyId: surveyId, displayAttributes: true};
        jQuery.get(bdrs.portalContextPath+"/webservice/location/getLocationById.htm", params, function(data) {
			// bdrs.map.wkt_options will always expect the input to be in WGS84 and will return
			// the result in google projection. Also note that getLocationById webservice will
			// always return in WGS84.
            var wkt = new OpenLayers.Format.WKT(bdrs.map.wkt_options);
            var feature = wkt.read(data.location);
			var dstProj = bdrs.map.getProjection(options.crsSelector);
            var point = feature.geometry.getCentroid().clone().transform(
                    bdrs.map.GOOGLE_PROJECTION,
                    dstProj);
            var lat = jQuery('input[name=latitude]').val(point.y).blur();
            var lon = jQuery('input[name=longitude]').val(point.x).blur();

            // add the location point to the map
            var layer = bdrs.map.baseMap.getLayersByName(bdrs.survey.location.LAYER_NAME)[0];
            layer.removeFeatures(layer.features);
            var lonLat = new OpenLayers.LonLat(feature.geometry.getCentroid().x, feature.geometry.getCentroid().y);
            layer.addFeatures(new OpenLayers.Feature.Vector(
                new OpenLayers.Geometry.Point(lonLat.lon, lonLat.lat)));
            // add the location geometry to the map
            var loclayer = bdrs.map.baseMap.getLayersByName(bdrs.survey.location.LOCATION_LAYER_NAME)[0];
            loclayer.removeFeatures(loclayer.features);
            loclayer.addFeatures(feature);
            // zoom the map to show the currently selected location
            var geobounds = feature.geometry.getBounds();
            var zoom = bdrs.map.baseMap.getZoomForExtent(geobounds);
            zoom = zoom > bdrs.map.DEFAULT_POINT_ZOOM_LEVEL ? bdrs.map.DEFAULT_POINT_ZOOM_LEVEL : zoom;
            bdrs.map.baseMap.setCenter(geobounds.getCenterLonLat(), zoom);
            // show the location attributes in the locationAttributesContainer
            if (options) {
                bdrs.attribute.createAttributeDisplayDiv(data.orderedAttributes, options.attributeSelector);
            }
        });
    }
    else {
        jQuery('input[name=latitude]').val("").blur();
        jQuery('input[name=longitude]').val("").blur();
        // clear the location attributes in the locationAttributesContainer
        if (options) {
            bdrs.attribute.createAttributeDisplayDiv(null, options.attributeSelector);
        }
    }
};

bdrs.survey.location.updateLocationAttributes = function(pk, surveyId, options) {
    if(pk > 0) {
        var params = {id: pk, surveyId: surveyId, displayAttributes: true};
        jQuery.get(bdrs.portalContextPath+"/webservice/location/getLocationById.htm", params, function(data) {
            // show the location attributes in the locationAttributesContainer
            if (options) {
                bdrs.attribute.createAttributeDisplayDiv(data.orderedAttributes, options.attributeSelector);
            }
        });
    }
    else {
        // clear the location attributes in the locationAttributesContainer
        if (options) {
            bdrs.attribute.createAttributeDisplayDiv(null, options.attributeSelector);
        }
    }
};

bdrs.getDatePickerParams = function() {
    // This blur prevents the ketchup validator from indicating the
    // field is required when it is already filled in
    var onSelectHandler = function(dateText, datepickerInstance){
        jQuery(this).trigger('blur');
    };

    var onCloseHandler = function(dateText, datepickerInstance) {
        // when you select a date, the cursor is still on the
        // date picker input field
        jQuery(this).focus();
    };

    var params = {
        showAnim: '',
        changeMonth: true,
        changeYear: true,
        dateFormat: bdrs.dateFormat,
        onSelect: onSelectHandler,
        onClose: onCloseHandler
    };

    return params;
};

bdrs.initDatePicker = function(){

    // this function prevents the from date from being after the to date
    // and vice versa in the date picker
    var onSelectDateRangeHandler = function(selectedDate, instance){
        var option = this.id == "from" ? "minDate" : "maxDate";
        var targetSelector = this.id == "from" ? "#to" : "#from";
        var date = $.datepicker.parseDate((instance.settings.dateFormat || $.datepicker._defaults.dateFormat), selectedDate, instance.settings);
        jQuery(targetSelector).datepicker("option", option, date);
        jQuery(this).trigger('blur');
    };

    var standardDpParams = bdrs.getDatePickerParams();

    jQuery(".datepicker").not(".hasDatepicker").datepicker(standardDpParams);

    var historicalDpParams = bdrs.getDatePickerParams();
    historicalDpParams.maxDate = new Date(); // Cannot pick a date in the future,
    historicalDpParams.beforeShow = function(){
        // only way to change z-index of datepicker
        setTimeout(function() {jQuery('#ui-datepicker-div').css('z-index',800);},  50);
    };

    jQuery(".datepicker_historical").not(".hasDatepicker").datepicker(historicalDpParams);

    var rangeDpParams = bdrs.getDatePickerParams();
    rangeDpParams.onSelect = onSelectDateRangeHandler;
    jQuery(".datepicker_range").not(".hasDatepicker").datepicker(rangeDpParams);

    // this handler is used when the user is typing in their date. Date.parse
    // will handle most funky inputs. We will attempt to parse the date and
    // fill out the date input in our expected format.
    jQuery(".datepicker, .datepicker_historical, .datepicker_range").bind("keydown", function(event) {
        // i.e. if 'enter' is pressed
        if (event.keyCode === 13) {
            try {
                var dateInput = jQuery(this);
                var parsedDate = Date.parse(dateInput.val());
                if (parsedDate) {
                    dateInput.val(bdrs.util.formatDate(parsedDate));
                }
            } catch (ex) {
                // catch exception quietly...
            }
        }
    });

    // initialise timepicker inputs
    // Pass an empty object or the timepicker will be appended to a datepicker.
    jQuery('.timepicker').timepicker({});
};

bdrs.initColorPicker = function(){
    jQuery(".COLOR").each(function() {
        bdrs.util.createColorPicker(jQuery(this));
    });
};

/**
 * Remove the disabled attribute from the submit inputs on the form.
 *
 * @param {Object} formNode - the form we are currently submitting
 */
bdrs.unbindDisableHandler = function(formNode) {
    jQuery('form[method=post] input[type=submit]', formNode).unbind('click.disable');
}

//disable form submit button on click to prevent double-click dual submission
bdrs.initSubmitDisabler = function() {
    var disabledClickHandler = function() {
        return false;
    };
    jQuery("form[method=post]").submit(function() {
        jQuery('input[type=submit]', this).bind('click.disable', disabledClickHandler);
    });

    // get form containing the input
    var form = jQuery('form[method=post] input[type=submit]').parents('form');
    // remove the disabled attribute when any input on the form is changed
    // to allow a form with invalid entries to be submitted once values are changed
    var unbindDisableHandler = function() {
        jQuery('input[type=submit]', form).unbind('click.disable');
    };

    jQuery(form).delegate("input", "focus", unbindDisableHandler);
    jQuery(form).delegate("input", "change", unbindDisableHandler);
    jQuery(form).delegate("textarea", "focus", unbindDisableHandler);
    jQuery(form).delegate("textarea", "change", unbindDisableHandler);
    jQuery(form).delegate("select", "focus", unbindDisableHandler);
    jQuery(form).delegate("select", "change", unbindDisableHandler);
};

/**
 * Stops jQuery dialogs in IE7 being 100% screen width
 * @param {Object} jqDialog the jquery dialog
 */
bdrs.fixJqDialog = function(dialogSelector) {

    jQuery(dialogSelector).dialog().bind("dialogopen", function(event, ui) {
        // fix for width:auto in IE
        var jqDialog = jQuery(this);
        var parent = jqDialog.parent();
        var contentWidth = jqDialog.width();
        parent.find('.ui-dialog-titlebar').each(function() {
            jQuery(this).width(contentWidth);
        });

        // 28 pixels is a magic number that makes everything line up nicely.
        parent.width(contentWidth + 28);
        jqDialog.dialog('option', 'position', 'center');

        // fix for scrollbars in IE
        jQuery('body').css('overflow', 'hidden');
        jQuery('.ui-widget-overlay').css('width', '100%');
    }).bind("dialogclose", function(event, ui) {
        // fix for width:auto in IE
        jQuery(this).parent().css("width", "auto");

        // fix for scrollbars in IE
        jQuery('body').css('overflow', 'auto');
    });
};

/**
 *
 * @param {Object} paramName the name to search for e.g. class="sortBy(hello)" would use 'sortBy' for the paramName
 * @param {Object} paramFoundHandler the function to call when there is a node found with a non null parameter.
 * signature of the function is function(node, value). 'node' is the dom node found, value is the value of the parameter
 * found, i.e., the contents of the rounded brackets.
 */
bdrs.handleClassParamNodes = function(paramName, paramFoundHandler) {
    jQuery("[class^='" + paramName + "']").each(function(index, node) {
        var getClassParamValue = bdrs.getClassParamValue(node, paramName);
        if (getClassParamValue !== null && getClassParamValue !== undefined) {
            paramFoundHandler(node, getClassParamValue);
        }
    });
};

/**
 * Get the class parameter from a node using the regex specified
 *
 * @param {Object} node the node to search in
 * @param {Object} paramName the param name to search for.
 */
bdrs.getClassParamValue = function(node, paramName) {
    var regex = paramName + "\\(([\\w\\.]+)\\)$";
    var classAttr = jQuery(node).attr('class');
    if (classAttr != null && classAttr != undefined) {
        var classList = classAttr.split(/\s+/);
        for (var i=0; i<classList.length; ++i) {
            var match = classList[i].match(regex);
            if (match && match.length === 2) {
                return match[1];
            }
        }
        // not found return null
        return null;
    } else {
        return null;
    }
};

bdrs.initDeferredKetchup = function() {
    // Deferred_ketchup may be used if the form contains many inputs
    // such that using normal ketchup causes a large initial overhead
    // when loading the page.
    jQuery('[class*=deferred_ketchup]').blur(function(evt){

       var elem = jQuery(evt.target);
       var klass_attr = elem.attr("class");
       var klass_split = klass_attr.split(" ");

       var i;
       var klass_name;
       var complete = false;
       for (i = 0; i < klass_split.length && !complete; i++) {
           klass_name = klass_split[i];
           if (klass_name.indexOf("deferred_ketchup") === 0) {
               elem.attr("class", klass_attr.replace("deferred_ketchup", "validate"));
               elem.parents("form").ketchup();
               elem.trigger("blur");
               complete = true;
           }
       }
    });
};

bdrs.initPlaceholderIEFix = function() {
    // create the options
    var options = {
            // custom wrapper to remove display:block; style to keep searchContainer span from wrapping
            inputWrapper: '<span style="position:relative;"></span>',

            // more or less just emulating what webkit does here
            // changed default top and color
            placeholderCSS: {
              'color':'#aaa',
              'position': 'absolute',
              'left':'4px',
              'top':'-2px',
              'overflow-x': 'hidden',
              'display': 'block'
            },
            // necessary to keep color working as options.color replaces options.placeholderCSS.color
            color: '#aaa'
          };

    if (bdrs.isIE7()) {
        options.placeholderCSS['top'] = '6px';
    }

    $('input[placeholder]').placeholder(options);
};

bdrs.init = function(){
    bdrs.initDatePicker();
    bdrs.initColorPicker();
    bdrs.initSubmitDisabler();
    bdrs.initDeferredKetchup();
    bdrs.initPlaceholderIEFix();

    // Changing blockUI defaults
    // this puts the block UI above all known items...
    jQuery.blockUI.defaults.baseZ = 1070;
    // CSS can be found in base.css in the theme.
    jQuery.blockUI.defaults.css = {};
};

/*
 * Title Caps
 *
 * Ported to JavaScript By John Resig - http://ejohn.org/ - 21 May 2008 Original
 * by John Gruber - http://daringfireball.net/ - 10 May 2008 License:
 * http://www.opensource.org/licenses/mit-license.php
 */
(function(){
    var small = "(a|an|and|as|at|but|by|en|for|if|in|of|on|or|the|to|v[.]?|via|vs[.]?)";
    var punct = "([!\"#$%&'()*+,./:;<=>?@[\\\\\\]^_`{|}~-]*)";

    this.titleCaps = function(title){
        var parts = [], split = /[:.;?!] |(?: |^)["Ò]/g, index = 0;

        while (true) {
            var m = split.exec(title);
            parts.push(title.substring(index, m ? m.index : title.length).replace(/\b([A-Za-z][a-z.'Õ]*)\b/g, function(all){
                return (/[A-Za-z]\.[A-Za-z]/).test(all) ? all : upper(all);
            }).replace(RegExp("\\b" + small + "\\b", "ig"), lower).replace(RegExp("^" + punct + small + "\\b", "ig"), function(all, punct, word){
                return punct + upper(word);
            }).replace(RegExp("\\b" + small + punct + "$", "ig"), upper));

            index = split.lastIndex;

            if (m) {
                parts.push(m[0]);
            }
            else {
                break;
            }
        }

        return parts.join("").replace(/ V(s?)\. /ig, " v$1. ").replace(/(['Õ])S\b/ig, "$1s").replace(/\b(AT&T|Q&A)\b/ig, function(all){
            return all.toUpperCase();
        });
    };

    function lower(word){
        return word.toLowerCase();
    }

    function upper(word){
        return word.substr(0, 1).toUpperCase() + word.substr(1);
    }
})();

/* jQuery.require plugin */
/**
 * require is used for on demand loading of JavaScript
 *
 * require r1 // 2008.02.05 // jQuery 1.2.2
 *
 * // basic usage (just like .accordion)
 * $.require("comp1.js");
 *
 * @param  jsFiles string array or string holding the js file names to load
 * @param  params object holding parameter like browserType, callback, cache
 * @return The jQuery object
 * @author Manish Shanker
 */
(function($){
    $.require = function(jsFiles, params){

        var params = params || {};
        var bType = params.browserType === false ? false : true;

        if (!bType) {
            return $;
        }

        var cBack = params.callBack ||
        function(){
        };
        var eCache = params.cache === false ? false : true;

        if (!$.require.loadedLib)
            $.require.loadedLib = {};

        if (!$.scriptPath) {
            var path = $('script').attr('src');
            $.scriptPath = path.replace(/\w+\.js$/, '');
        }
        if (typeof jsFiles === "string") {
            jsFiles = new Array(jsFiles);
        }
        for (var n = 0; n < jsFiles.length; n++) {
            if (!$.require.loadedLib[jsFiles[n]]) {
                $.ajax({
                    type: "GET",
                    url: $.scriptPath + jsFiles[n],
                    success: cBack,
                    dataType: "script",
                    cache: eCache,
                    async: false
                });
                $.require.loadedLib[jsFiles[n]] = true;
            }
        }
        return $;
    };
})(jQuery);

if (bdrs.require === undefined) {
    // we can overwrite this function in the test framework to change our path appropriately...
    bdrs.require = function(file){
        jQuery.require("/../" + file);
    };
}

/* Required files section */
bdrs.require("jsCompatibility.js");
bdrs.require("bdrs/map/MapServerQueryManager.js");
bdrs.require("bdrs/form.js");
bdrs.require("bdrs/censusMethod.js");
bdrs.require("bdrs/menu.js");
bdrs.require("bdrs/message.js");
bdrs.require("bdrs/user.js");
bdrs.require("bdrs/util.js");
bdrs.require("bdrs/preferences.js");
bdrs.require("bdrs/contribute.js");
bdrs.require("bdrs/location.js");
bdrs.require("bdrs/dnd.js");
bdrs.require("bdrs/map.js");
bdrs.require("bdrs/model/attribute_scope.js");
bdrs.require("bdrs/model/attribute_type.js");
bdrs.require("bdrs/attribute.js");
bdrs.require("bdrs/url/urls.js");
bdrs.require("bdrs/bulkdata/bulkdata.js");
