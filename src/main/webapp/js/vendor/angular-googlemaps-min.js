(function () {
    "use strict";

    function e(e, t) {
        return Math.abs(e - t) < 1e-6
    }
    var t = function () {
        function n(n) {
            var r = null,
                i = [],
                s = [],
                o = [],
                u = angular.extend({}, t, n),
                a = this,
                f = null;
            this.center = n.center;
            this.zoom = u.zoom;
            this.draggable = u.draggable;
            this.dragging = false;
            this.selector = u.container;
            this.markers = [];
            this.options = u.options;
            this.draw = function () {
                if (a.center == null) {
                    return
                }
                if (r == null) {
                    r = new google.maps.Map(a.selector, angular.extend(a.options, {
                        center: a.center,
                        zoom: a.zoom,
                        draggable: a.draggable,
                        mapTypeId: google.maps.MapTypeId.ROADMAP
                    }));
                    google.maps.event.addListener(r, "dragstart", function () {
                        a.dragging = true
                    });
                    google.maps.event.addListener(r, "idle", function () {
                        a.dragging = false
                    });
                    google.maps.event.addListener(r, "drag", function () {
                        a.dragging = true
                    });
                    google.maps.event.addListener(r, "zoom_changed", function () {
                        a.zoom = r.getZoom();
                        a.center = r.getCenter()
                    });
                    google.maps.event.addListener(r, "center_changed", function () {
                        a.center = r.getCenter()
                    });
                    if (s.length) {
                        angular.forEach(s, function (e, t) {
                            google.maps.event.addListener(r, e.on, e.handler)
                        })
                    }
                } else {
                    google.maps.event.trigger(r, "resize");
                    var t = r.getCenter();
                    if (!e(t.lat(), a.center.lat()) || !e(t.lng(), a.center.lng())) {
                        r.setCenter(a.center)
                    }
                    if (r.getZoom() != a.zoom) {
                        r.setZoom(a.zoom)
                    }
                }
            };
            this.fit = function () {
                if (r && i.length) {
                    var e = new google.maps.LatLngBounds;
                    angular.forEach(i, function (t, n) {
                        e.extend(t.getPosition())
                    });
                    r.fitBounds(e)
                }
            };
            this.on = function (e, t) {
                s.push({
                    on: e,
                    handler: t
                })
            };
            this.addMarker = function (e, t, n, s, o, u, l) {
                if (a.findMarker(e, t) != null) {
                    return
                }
                var c = new google.maps.Marker({
                    position: new google.maps.LatLng(e, t),
                    map: r,
                    icon: n
                });
                if (o) {}
                if (u) {}
                if (s != null) {
                    var h = new google.maps.InfoWindow({
                        content: s
                    });
                    google.maps.event.addListener(c, "click", function () {
                        if (f != null) {
                            f.close()
                        }
                        h.open(r, c);
                        f = h
                    })
                }
                i.unshift(c);
                a.markers.unshift({
                    lat: e,
                    lng: t,
                    draggable: false,
                    icon: n,
                    infoWindowContent: s,
                    label: o,
                    url: u,
                    thumbnail: l
                });
                return c
            };
            this.findMarker = function (t, n) {
                for (var r = 0; r < i.length; r++) {
                    var s = i[r].getPosition();
                    if (e(s.lat(), t) && e(s.lng(), n)) {
                        return i[r]
                    }
                }
                return null
            };
            this.findMarkerIndex = function (t, n) {
                for (var r = 0; r < i.length; r++) {
                    var s = i[r].getPosition();
                    if (e(s.lat(), t) && e(s.lng(), n)) {
                        return r
                    }
                }
                return -1
            };
            this.addInfoWindow = function (e, t, n) {
                var r = new google.maps.InfoWindow({
                    content: n,
                    position: new google.maps.LatLng(e, t)
                });
                o.push(r);
                return r
            };
            this.hasMarker = function (e, t) {
                return a.findMarker(e, t) !== null
            };
            this.getMarkerInstances = function () {
                return i
            };
            this.removeMarkers = function (e) {
                var t = this;
                angular.forEach(e, function (e, n) {
                    var r = e.getPosition(),
                        s = r.lat(),
                        o = r.lng(),
                        u = t.findMarkerIndex(s, o);
                    i.splice(u, 1);
                    t.markers.splice(u, 1);
                    e.setMap(null)
                })
            }
        }
        var t = {
            zoom: 8,
            draggable: false,
            container: null
        };
        return n
    }();
    var n = angular.module("google-maps", []);
    n.directive("googleMap", ["$log", "$timeout", "$filter",'$window','$kexAsyncLoader',
        function (n, r, i, $window, $kexAsyncLoader) {
            var s = function (e, t) {
                var n = e.map;
                self.addInfoWindow = function (e, t, r) {
                    n.addInfoWindow(e, t, r)
                }
            };
            s.$inject = ["$scope", "$element"];
            return {
                restrict: "EC",
                priority: 100,
                transclude: true,
                template: "<div class='angular-google-map' ng-transclude></div>",
                replace: false,
                scope: {
                    center: "=center",
                    markers: "=markers",
                    latitude: "=latitude",
                    longitude: "=longitude",
                    zoom: "=zoom",
                    refresh: "&refresh",
                    windows: "=windows"
                },
                controller: s,
         
                link: function (i, s, o, u) { // function content is optional
                    // in this example, it shows how and when the promises are resolved
                    //console.log('directive called');
                    if ($window.google && $window.google.maps) {
                        //console.log('gmaps already loaded');
                    } else {
                        $kexAsyncLoader.loadGoogleMaps().then(function () {
                            //console.log('promise resolved');
                            if ($window.google && $window.google.maps) {
                                if (!angular.isDefined(i.center) || !angular.isDefined(i.center.latitude) || !angular.isDefined(i.center.longitude)) {
                                    n.error("angular-google-maps: ould not find a valid center property");
                                    return
                                }
                                if (!angular.isDefined(i.zoom)) {
                                    n.error("angular-google-maps: map zoom property not set");
                                    return
                                }
                                angular.element(s).addClass("angular-google-map");
                                var a = {
                                    options: {}
                                };
                                if (o.options) {
                                    a.options = angular.fromJson(o.options)
                                }
                                var f = new t(angular.extend(a, {
                                    container: s[0],
                                    center: new google.maps.LatLng(i.center.latitude, i.center.longitude),
                                    draggable: o.draggable == "true",
                                    zoom: i.zoom
                                }));
                                f.on("drag", function () {
                                    var e = f.center;
                                    r(function () {
                                        i.$apply(function (t) {
                                            i.center.latitude = e.lat();
                                            i.center.longitude = e.lng()
                                        })
                                    })
                                });
                                f.on("zoom_changed", function () {
                                    if (i.zoom != f.zoom) {
                                        r(function () {
                                            i.$apply(function (e) {
                                                i.zoom = f.zoom
                                            })
                                        })
                                    }
                                });
                                f.on("center_changed", function () {
                                    var e = f.center;
                                    r(function () {
                                        i.$apply(function (t) {
                                            if (!f.dragging) {
                                                i.center.latitude = e.lat();
                                                i.center.longitude = e.lng()
                                            }
                                        })
                                    })
                                });
                                if (o.markClick == "true") {
                                    (function () {
                                        var e = null;
                                        f.on("click", function (t) {
                                            if (e == null) {
                                                e = {
                                                    latitude: t.latLng.lat(),
                                                    longitude: t.latLng.lng()
                                                };
                                                i.markers.push(e)
                                            } else {
                                                e.latitude = t.latLng.lat();
                                                e.longitude = t.latLng.lng()
                                            }
                                            r(function () {
                                                i.latitude = e.latitude;
                                                i.longitude = e.longitude;
                                                i.$apply()
                                            })
                                        })
                                    })()
                                }
                                i.map = f;
                                if (angular.isUndefined(i.refresh())) {
                                    f.draw()
                                } else {
                                    i.$watch("refresh()", function (e, t) {
                                        if (e && !t) {
                                            f.draw()
                                        }
                                    })
                                }
                                i.$watch("markers", function (t, n) {
                                    r(function () {
                                        angular.forEach(t, function (e, t) {
                                            if (!f.hasMarker(e.latitude, e.longitude)) {
                                                f.addMarker(e.latitude, e.longitude, e.icon, e.infoWindow)
                                            }
                                        });
                                        var n = [];
                                        angular.forEach(f.getMarkerInstances(), function (t, r) {
                                            var s = t.getPosition(),
                                                o = s.lat(),
                                                u = s.lng(),
                                                a = false;
                                            for (var f = 0; f < i.markers.length; f++) {
                                                var l = i.markers[f];
                                                if (e(l.latitude, o) && e(l.longitude, u)) {
                                                    a = true
                                                }
                                            }
                                            if (!a) {
                                                n.push(t)
                                            }
                                        });
                                        n.length && f.removeMarkers(n);
                                        if (o.fit == "true" && t.length > 1) {
                                            f.fit()
                                        }
                                    })
                                }, true);
                                i.$watch("center", function (e, t) {
                                    if (e === t) {
                                        return
                                    }
                                    if (!f.dragging) {
                                        f.center = new google.maps.LatLng(e.latitude, e.longitude);
                                        f.draw()
                                    }
                                }, true);
                                i.$watch("zoom", function (e, t) {
                                    if (e === t) {
                                        return
                                    }
                                    f.zoom = e;
                                    f.draw()
                                })
                            } 
                            else {
                                console.log('gmaps not loaded');
                            }
                        }, function () {
                            console.log('promise rejected');
                        });
                    }
                }
                
            }
        }
    ])
})()