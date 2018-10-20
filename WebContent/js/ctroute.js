(function($, CTr) {
    $(function() {
        var olat, olon, dlat, dlon;
        var oriHTML, destHTML;

        //get geographical positions
        var opos = CTr.getParameterByName("opos");
        var dpos = CTr.getParameterByName("dpos");

        if(opos && dpos){
            opos = opos.split(","); olat = opos[1]; olon = opos[0];
            dpos = dpos.split(","); dlat = dpos[1]; dlon = dpos[0];
            oriHTML = "(Lon="+olon+",Lat="+olat+")";
            destHTML = "(Lon="+dlon+",Lat="+dlat+")";

            $("#title").text("From (Lon="+olon+",Lat="+olat+") to (Lon="+dlon+",Lat="+dlat+")");

            follow();
        }

        function follow(){
            $.ajax({
                url : "ctws",
                data : "ser=rou&opos="+olon+","+olat+"&dpos="+dlon+","+dlat
            }).done(function(rou) {
                $("#waitimg").hide();

                if(rou.status!="ok"){
                    console.warn("Could not retrieve maritime route.");
                    return;
                }

                var d = rou.dist;
                $("#infos").append("<p>Estimated distance: " + d.toFixed(2) + " km</p>");

                //create map
                $("#map").show();
                var map = L.map("map", { center : [ 30, 0 ], zoom : 2, attributionControl : false, minZoom : 0, maxZoom : 10 });
                L.control.scale({ imperial : false }).addTo(map);

                //locations layer
                var locations = new L.LayerGroup();
                var style = { color : 'red', fillColor : '#f03', fillOpacity : 0.5 };
                L.circle([olat,olon], 1000, style).bindPopup(oriHTML).addTo(locations);
                L.circle([dlat,dlon], 1000, style).bindPopup(destHTML).addTo(locations);

                function samePositionPos(lat1,lon1,lat2,lon2){
                    return (lat1===lat2 && lon1===lon2);
                }

                //maritime route
                var mRouteLayer = new L.LayerGroup();
                var sm = { color:"red",weight:2,opacity:1 };
                var mrl = new L.GeoJSON([], {
                    style : sm,
                    onEachFeature : function (feature, layer) { layer.bindPopup("Maritime route - "+d+" km"); }
                });
                if(rou.status==="ok") mrl.addData(rou.geom);
                if(!samePositionPos(olat, olon, dlat, dlon)) mRouteLayer.addLayer(mrl);

                //geodetic route
                var gRouteLayer = new L.LayerGroup();
                var sg = { color:"black",weight:2,opacity:1 };
                if(!samePositionPos(olat, olon, dlat, dlon)) {
                    var line = new L.Geodesic([ [ L.latLng(olat, olon), L.latLng(dlat, dlon) ] ], sg);
                    gRouteLayer.addLayer(line);
                }

                //add layers to map
                var bls = {
                    Light : L.tileLayer('http://services.arcgisonline.com/ArcGIS/rest/services/Canvas/World_Light_Gray_Base/MapServer/tile/{z}/{y}/{x}'),
                    Dark : L.tileLayer('http://services.arcgisonline.com/ArcGIS/rest/services/Canvas/World_Dark_Gray_Base/MapServer/tile/{z}/{y}/{x}'),
                    Topography : L.tileLayer('http://services.arcgisonline.com/ArcGIS/rest/services/World_Topo_Map/MapServer/tile/{z}/{y}/{x}')
                };
                map.addLayer(bls.Light);
                gRouteLayer.addTo(map);
                mRouteLayer.addTo(map);
                locations.addTo(map);

                //add layer control
                L.control.layers(bls, { "Origin and destination" : locations, "Maritime route" : mRouteLayer, "Geodetic route" : gRouteLayer},{position:"topleft",collapsed:true}).addTo(map);

                //fit map
                //map.fitBounds( mrl.getBounds() );

            }).fail(function(XMLHttpRequest, textStatus) { console.warn(textStatus); });
        }
    });
}( jQuery, window.CTr = window.CTr || {} ));
