angular
    .module('loadingOnAJAX', [])
    .config(function($httpProvider) {
        var numLoadings = 0;
        var loadingScreen = $('<div style="position:fixed;top:0;left:0;right:0;bottom:0;z-index:10000;background-color:gray;background-color:rgba(70,70,70,0.2);"><img style="position:absolute;top:50%;left:50%;" alt="" src="/img/fbLoading.gif" /></div>')
            .appendTo($('body')).hide();
        $httpProvider.responseInterceptors.push(function($q) {
            return function(promise) {
                numLoadings++;
                loadingScreen.show();
                function hideLoadingScreen() {
                    if (!(--numLoadings)) loadingScreen.hide();
                }
                return promise.then(function(response) {
                    hideLoadingScreen();
                    return response;
                }, function(response) {
                    hideLoadingScreen();
                    return $q.reject(response);
                });
            };
        });
    });

angular.module('globalErrors', []).config(function($provide, $httpProvider, $compileProvider) {
    $httpProvider.defaults.headers.post["Content-Type"] = "application/json;charset=UTF-8";
    $httpProvider.defaults.transformRequest.push(function(data, headersGetter) {
        // console.log(angular.toJson(headersGetter()));
        // Check if it is a post request. Mutations require authentication.
        if ((headersGetter()["Content-Type"] != null) && !isLoggedIn()) {
            // Temporarily disabling until this functionality is completed to allow for login.
            // alert("login required");
        }
        return data;
    });
    $httpProvider.responseInterceptors.push(function($rootScope, $timeout, $q) {
        return function(promise) {
            return promise.then(function(successResponse) {
                if (successResponse.config.method.toUpperCase() != 'GET' && !isExternal(successResponse.config.url)) {
                    $rootScope.showAlert("Saved successfully!", "success");
                }
                return successResponse;
            }, function(errorResponse) {
                if (!isExternal(errorResponse.config.url)) {
                    switch (errorResponse.status) {
                    case 400:
                        $rootScope.showAlert(errorResponse.data.error.message, "danger");
                        break;
                    case 401:
                        $rootScope.showAlert('Wrong usename or password', "danger");
                        break;
                    case 403:
                        $rootScope.showAlert('Permission denied', "danger");
                        break;
                    case 404:
                        $rootScope.showAlert('Failed to find resource: ' + errorResponse.data, "danger");
                        break;
                    case 500:
                        $rootScope.showAlert('Server internal error: ' + errorResponse.data, "danger");
                        break;
                    default:
                        $rootScope.showAlert('Error ' + errorResponse.status + ': ' + errorResponse.data, "danger");
                    }
                }
                return $q.reject(errorResponse);
            });
        };
    });
});

kexApp = angular.module( "kexApp", 
    ["ngResource", "ngCookies", "google-maps", "ui.bootstrap", "ui.bootstrap.ex", "loadingOnAJAX", "ngFacebook",
     "globalErrors" ,"ui.calendar", "ngSocial"] )
.config( function( $routeProvider, $httpProvider, $facebookProvider ) { 
    $routeProvider
        // .when( '/', { controller : homeCtrl, templateUrl : 'partials/home.html' } )
        // .when( '/home', { controller : homeCtrl, templateUrl : 'partials/home.html' } )
        .when( '/me', { controller : meCtrl, templateUrl : 'partials/me.html' } )
        .when( '/about', { templateUrl : 'partials/about.html' } )
        .when( '/contact', { templateUrl : 'partials/contact.html' } )
        .when( '/user/:userId', { controller : meCtrl, templateUrl : 'partials/me.html' } )
        .when( '/mysettings', { controller : meCtrl, templateUrl : 'partials/mysettings.html' } )
        .when( '/event', { controller : eventsCtrl, templateUrl : 'partials/events.html' } )
        .when( '/event/add', { controller : addEditEventsCtrl, templateUrl : 'partials/addEditevent.html' } )
        .when( '/event/:eventId/edit', { controller : addEditEventsCtrl, templateUrl : 'partials/addEditevent.html' } )
        .when( '/event/:eventId', { controller : addEditEventsCtrl, templateUrl : 'partials/viewEvent.html' } )
        .when( '/org', { controller : orgCtrl, templateUrl : 'partials/organization.html' } )
        .when( '/org/:orgId', { controller : orgDetailCtrl, templateUrl : 'partials/organizationDetail.html' } )
        .otherwise( { redirectTo : '/event' } );
    delete $httpProvider.defaults.headers.common [ 'X-Requested-With' ]; 
    //$httpProvider.defaults.headers.common['X-'] = 'X';


    var fbAppId;
    if (document.location.hostname === "localhost" ) { 
        fbAppId = '276423019167993'; 
    }   
    else if (document.location.hostname === "karmademo.dyndns.dk" ) { 
        fbAppId = '1381630838720301'; 
    } 
    else if (document.location.hostname === "kex-latest.appspot.com" ) { 
        fbAppId = '166052360247234'; 
    } 
    else { 
        fbAppId = '571265879564450'; 
    } 
    $facebookProvider.setAppId(fbAppId);
    $facebookProvider.setCustomInit({ 
        status : true, 
        cookie : true, 
        xfbml : true });
        
} ).filter( 'newlines', function( ) { 
    return function( text ) { 
        if( text ) 
        { 
            return text.replace( /\n/g, '<br/>' );  
        }
    } 
} ).filter( 'noHTML', function( ) { 
    return function( text ) { 
        if( text ) 
        { 
            return text.replace( /&/g, '&amp;' ).replace( />/g, '&gt;' ).replace( /</g, '&lt;' ); 
        }
    } 
} ).filter( 'limit10', function( ) { 
    return function( text ) { 
        if( text > 10 ) 
        { 
            return 'Need more than 10'; 
        } 
        else if( text == "0" ) 
        { 
            return 'Fully registered!'; 
        } 
        else
        { 
            return "Need " + text + " more"; 
        } 
        return text; 
    }
}
).filter( 'badge', function( ) { 
    return function( text ) { 
        if( text > 0 ) 
        { 
            return "("+text+")"; 
        } 

        else
        { 
            return ""; 
        } 
        return text; 
    }
}
).filter('truncate', function () {
    return function (text, length, end) {
        if (text == null || text.length == 0)
            return null;
    
        if (isNaN(length))
            length = 10;

        if (end === undefined)
            end = "...";

        if (text.length <= length || text.length - end.length <= length) {
            return text;
        }
        else {
            return String(text).substring(0, length-end.length) + end;
        }

    };
}
).run( function( $rootScope, Me, $location, FbUtil) { 
    $rootScope.fbUtil = FbUtil;
    $rootScope.$on( "$routeChangeStart", function( event, next, current ) { 
            $rootScope.alerts = [ ]; 
            $rootScope.locationURL = window.location.href;
    } ); 
    $rootScope.addAlert = function( message ) { 
        if( ! $rootScope.alerts ) 
        {    
            $rootScope.alerts = [ ]; 
        } 
        $rootScope.alerts.push( { msg : message } ); 
    }; 
    $rootScope.showAlert = function( message, alertType ) { 
        $rootScope.alerts = [ ]; 
        $rootScope.alerts.push( { type : alertType, msg : message } ); 
    };
    $rootScope.closeAlert = function( index ) { 
        $rootScope.alerts.splice( index, 1 ); 
    }; 
    $rootScope.isMessageOpen = false; 
    $rootScope.showMessage = function( ) { 
        $rootScope.isMessageOpen = true; 
    }; 
    $rootScope.cancelMessage = function( ) { 
        $rootScope.isMessageOpen = false; 
    }; 
    $rootScope.sendMessage = function( ) { 
        $rootScope.isMessageOpen = false; 
    };
    $rootScope.getGeoLocation = function(){
        return { latitude: 0, longitude: 0};
        // TODO(avlaiani): commented out because this is not working.
        // return google.loader.ClientLocation;
    };
    $rootScope.getGeoCenter = function( ) { 
        var options = {
          enableHighAccuracy: true,
          timeout: 5000,
          maximumAge: 0
        };
            navigator.geolocation.getCurrentPosition( function( position ) {
                return { 
                    latitude : position.coords.latitude, 
                    longitude : position.coords.longitude
                };
                
        }, function( ) {
            return { 
                    latitude : $rootScope.getGeoLocation.latitude, 
                    longitude : $rootScope.getGeoLocation.longitude
                };
        }, options ); 
               
    };

    ( function( d ) { 
            var js, id = 'facebook-jssdk', ref = d.getElementsByTagName( 'script' )[ 0 ]; 
            if( d.getElementById( id ) ) { 
                return; 
            } 
            js = d.createElement( 'script' ); 
            js.id = id; 
            js.async = true; 
            js.src = "//connect.facebook.net/en_US/all.js"; 
            ref.parentNode.insertBefore( js, ref ); 
    }( document ) );
} );

/*
 * Webservice factories
 */

kexApp.factory( 'Events', function( $resource ) { 
        return $resource( '/api/event/:id/:registerCtlr/:regType', { id : '@id', registerCtlr : '@registerCtlr', regType : '@regType' }
            ); 
} );
kexApp.factory( 'User', function( $resource ) { 
        return $resource( '/api/user/:id/:resource/:filter', { id : '@id', resource : '@resource', filter : '@filter' } ); 
} );
kexApp.factory( 'Me', function( $resource ) { 
        return $resource( '/api/me/:resource/:filter', { resource : '@resource', filter : '@filter' } ); 
} );
kexApp.factory( 'Org', function( $resource ) { 
        return $resource( '/api/org/:id/:resource/:filter', { id : '@id', resource : '@resource', filter : '@filter' } ); 
} );

/*
 * Utility API factories
 */

kexApp.factory('kexUtil', function($rootScope) {
    return {
        getLocation: function() {
            return window.location.href;
        }
    }
});

kexApp.factory('ApiCache', function($rootScope) {
    var cache = {};

    $rootScope.$on( "fbUtil.userChanged", function (event) {
        cache = {};
    });

    return {
        lookup: function (key) {
            if (angular.isUndefined(cache[key])) return undefined;
            return cache[key];
        },
        update: function (key, value) {
            cache[key] = value;
        },
        key: function () {
            var result = arguments[0] + "(";
            for (var i = 1; i < arguments.length; i++) {
                if (i != 1) {
                    result += ", ";
                }
                result += arguments[i].toString();
            };
            return result + ")";
        }
    };
});

kexApp.factory('FbUtil', function($rootScope, kexUtil, $facebook, Me, $location, $window, ApiCache) {
    var fbAccessToken;
    var firstAuthResponse = true;

    $rootScope.$on( "fb.auth.authResponseChange", function( event, response ) {
        if ( response.status === 'connected' ) {                    
            setCookies(response.authResponse);

            var updateUser = $rootScope.fbUserId != response.authResponse.userID;
            $rootScope.fbUserId = response.authResponse.userID;
            fbAccessToken = response.authResponse.accessToken;

            if ( updateUser ) {
                $rootScope.me = Me.get(); 
                $rootScope.orgs = Me.get( { resource : 'org' } ); 
                processUserChange();
            }
        } else {
            removeCookies();
            if ( $rootScope.fbUserId ) {
                $rootScope.fbUserId = undefined;
                fbAccessToken = undefined;
                $rootScope.me = undefined;
                $rootScope.orgs = undefined;
                processUserChange();
            }
        }
        firstAuthResponse = false;
    } ); 
    function processUserChange() {
        if (!firstAuthResponse) {
            // Fired if the user changes after the first FB.init() invocation.
            $rootScope.$broadcast("fbUtil.userChanged");
        }
        if (!$rootScope.$$phase) $rootScope.$apply();            
    }

    function setCookies(authResponse) {
        $.cookie( "facebook-uid", authResponse.userID ); 
        $.cookie( "facebook-token", authResponse.accessToken ); 
        $.cookie( "login", "facebook" );         
    }

    function removeCookies() {        
        $.removeCookie( "facebook-uid" ); 
        $.removeCookie( "facebook-token" ); 
        $.removeCookie( "login" );
    }

    return {
        GRAPH_API_URL: "https://graph.facebook.com",
        LOGIN_SCOPE: "email,user_location",

        getImageUrl: function (id, type) {
            return "//graph.facebook.com/" + id + "/picture?type=" + type;
        },

        getAccessToken: function () {
            return fbAccessToken;
        },

        login: function () {
            $facebook.login();
        },

        logout: function () {
            $facebook.logout().then( function(response) {
                // $window.location.href = "/";
                // $rootScope.$apply();
                $location.path("/");
                // window.location.reload(true);
                // Make a call to the backend to wipe out any cached user state.
            });
        },

        getComments: function (mydiv) {
            if (mydiv) {
                mydiv.innerHTML = '<div class="fb-comments" href="' + window.location.href + '" num-posts="20" width="940">';
                $facebook.promise.then( function(FB) { FB.XFBML.parse(mydiv); } );
            }
        },

        getAlbumCoverUrl: function (albumId) {
            var cacheKey = ApiCache.key("getAlbumCoverUrl", albumId);
            var promise = ApiCache.lookup(cacheKey);
            if (promise !== undefined) {
                return promise;
            }

            promise = $facebook.api("/" + albumId).then(function (response) {
                return $facebook.api("/" + response.cover_photo);
            }).then(function (response) {
                return response.source;
            });

            ApiCache.update(cacheKey, promise);
            return promise;
        }
    };
});


kexApp.factory('EventUtil', function() {
    return {
        canWriteReview: function (event) {
            // The registration info is computed with respect to the current user's
            // key. Also, organizers can not write reviews.
            return event.registrationInfo == 'REGISTERED';
        },
    };
});

/*
 * App directives
 */

kexApp.directive( 'uiDraggable', function( ) { 
        return { 
            restrict : 'A', 
            link : function( scope, element, attrs ) { 
                element.draggable( { 
                revert : true                    } ); 
            } 
        }; 
} );
kexApp.directive( 'uiDropListener', function( ) { 
        return { 
            restrict : 'A', 
            link : function( scope, eDroppable, attrs ) { 
                eDroppable.droppable( { 
                        drop : function( event, ui ) { 
                            var fnDropListener = scope.$eval( attrs.uiDropListener ); 
                            if( fnDropListener && angular.isFunction( fnDropListener ) ) { 
                                var eDraggable = angular.element( ui.draggable ); 
                                fnDropListener( eDraggable, eDroppable, event, ui ); 
                            } 
                        } 
                } ); 
            } 
        }; 
} );
kexApp.directive( 'googleplace', function( ) { 
        return { 
            require : 'ngModel',
            link : function( scope, element, attrs, model ) { 
                var options = { 
                    types : [ ], 
                    componentRestrictions : {} 
                }; 
                scope.gPlace = new google.maps.places.Autocomplete( element [ 0 ], options );
                google.maps.event.addListener( scope.gPlace, 'place_changed', function( ) { 
                        var placeListener = scope.$eval( attrs.placeListener ); 
                        if( placeListener && angular.isFunction( placeListener ) ) {
                            placeListener( scope.gPlace.getPlace( )); 
                        } 
                        scope.$apply( function( ) {
                                model.$setViewValue( element.val( ));                
                        } ); 
                } ); 
            } 
        }; 
} );
kexApp.directive( 'fbgallery', function( $compile ) { 
        return {
            scope : { 
            userid : '@'    },
            restrict : 'A', 
            // linking method
            link : function( scope, element, attrs ) { 
                //$(element).plusGallery(scope.$eval(attrs.fbgallery));
                scope.$watch( 'userid', function( val ) { 
                        if( val ) 
                        { 
                            $( element ).plusGallery( scope.$eval( attrs.fbgallery ) );     
                        } 
                } ); 
            } 
        } 
} );
kexApp.directive( "gallery", function( ) { 
        return function( scope, element, attrs ) { 
            var doStuff = function( element, attrs ) {
                $( element ).plusGallery( scope.$eval( attrs.fbgallery ) ); 
            } 
            scope.$watch( attrs.userid, doStuff( element, attrs ) ); 
            // scope.$watch(attrs.testTwo, doStuff(element,attrs));
            
        } 
} );
kexApp.directive( 'unfocus', function( ) { return {
        restrict : 'A', 
        link : function( scope, element, attribs ) {
            element [ 0 ].focus( );
            element.bind( "blur", function( ) { 
                    scope.$apply( attribs [ "unfocus" ] );
            } );
        }
} } );

kexApp.directive( "eventrepeat", function( ) { 
        return function( scope, element, attrs ) { 
            
                $( element ).recurrenceinput(); 
            
            
        } 
} );


kexApp.directive('shareButtons', function(kexUtil) {
        return {
            restrict: 'E',
            scope: {
                title: '=',
                description: '=',
                image: '='
            },
            replace: true,
            transclude: false,
            link: function (scope, element, attrs) {
                scope.util = kexUtil;
            },
            template:
                '<div>' +
                    '<ul ng-social-buttons ' +
                            'url="util.getLocation()" ' +
                            'title="title" ' +
                            'description="description" ' +
                            'image="image">' +
                        '<li class="ng-social-facebook">Facebook</li>' +
                        '<li class="ng-social-google-plus">Google+</li>' +
                        '<li class="ng-social-twitter">Twitter</li>' +
                    '</ul>' +
                '</div>'                
        }
    });

kexApp.directive('eventParticipantImgsMini', function() {
    return {
        restrict: 'E',
        scope: {
            event: '=',
        },
        replace: true,
        transclude: false,
        template:
            '<ul class="list-inline">' +
                '<li ng-repeat="userImage in event.cachedParticipantImages">' +
                    '<a href="#/user/{{userImage.participant.key}}">' +
                        '<img ng-src="{{userImage.imageUrl}}?type=square" class="kex-thumbnail-user-mini">' +
                    '</a>' +
                '</li>' +
            '</ul>'
    }
});

kexApp.directive('eventRegistrationInfo', function() {
    return {
        restrict: 'E',
        scope: {
            type: '@',
            registrationInfo: '=',
        },
        replace: true,
        transclude: false,
        link: function (scope, element, attrs) {
            scope.showLabel = false;
            var registrationInfoMapping = {
                ORGANIZER: { text: 'Organizer', labelClass: 'label-success', type: ['UPCOMING', 'PAST']},
                REGISTERED: { text: 'Registered', labelClass: 'label-success', type: ['UPCOMING']},
                WAIT_LISTED: { text: 'Waitlisted', labelClass: 'label-warning', type: ['UPCOMING']},
                CAN_WAIT_LIST: { text: 'Waitlist Open', labelClass: 'label-warning', type: ['UPCOMING']}
            };
            scope.$watch('type', function() {
                if (scope.type) {
                    var mapping = registrationInfoMapping[scope.registrationInfo];
                    if (mapping && ($.inArray(scope.type, mapping.type) != -1)) {
                        scope.showLabel = true;
                        scope.labelText = mapping.text;
                        scope.labelClass = 'label kex-bs-label ' + mapping.labelClass;
                    }
                } else {
                    scope.showLabel = false;
                }
            });
        },
        template:
            '<span ng-class="labelClass" ng-show="showLabel">{{labelText}}</span>'
    }
});

kexApp.directive('eventUserRating', function(Events) {
    return {
        restrict: 'E',
        scope: {
            userRating: '=',
            eventKey: '='
        },
        replace: true,
        transclude: false,
        link: function (scope, element, attrs) {
            scope.updateUserRating = function (newValue) {
                if (newValue) {
                    Events.save( 
                        { id : scope.eventKey, registerCtlr : 'review' }, 
                        { "rating" : { "value" : newValue }},
                        null,
                        function () {
                            Events.get(
                                { id : scope.eventKey, registerCtlr : 'review' },
                                function (value) {
                                    scope.userRating.value = 
                                        (value && value.rating) ? value.rating.value : undefined;
                                });
                        });                                    
                }
            };
        },
        template:
            '<div>' +
                '<rating-ex value="userRating.value" max="5" readonly="false" on-update="updateUserRating(newValue)"></rating-ex>' +
            '</div>'
    }
});

kexApp.directive('aggregateRating', function() {
    return {
        restrict: 'E',
        scope: {
            value: '=',
        },
        replace: true,
        transclude: false,
        template:
            '<div>' +
                '<rating value="value.value" max="5" readonly="true"></rating>' +
                ' ({{value.count}})' +
            '</div>'
    }
});


/*
 * App controllers
 */

var meCtrl = function( $scope, $location, User, Me, $rootScope, $routeParams, FbUtil, EventUtil ) {
    $scope.EventUtil = EventUtil;
    $scope.newMail = { email : null, primary : null }; 
    $scope.load = function( $location, $routeParams ) {
        if( $location.$$url == "/me" || $location.$$url == "/mysettings" ) 
        { 
            $scope.who = 'My'; 
            $scope.me = Me.get( function( ) { 
                    $scope.origAboutMe = $scope.me.about; 
                    if( ! $scope.me.about ) 
                    { 
                        $scope.me.about = 'Click here to write a few words about yourself!'; 
                    } 
                    
                    $scope.getOtherData( $scope.me.key ); 
                    $rootScope.me = $scope.me;
            } );
            
        } 
        else
        { 
            $scope.me = User.get( { id : $routeParams.userId }, function( ) { 
                    $scope.who = $scope.me.firstName + "'s"; 
                    if( ! $scope.me.about ) 
                    { 
                        $scope.me.about = 'No description added yet!'; 
                    }
            } ); 
            $scope.getOtherData( $routeParams.userId );
        }
    };
    $scope.getOtherData = function( key ) { 
        $scope.events = User.get( { id : key, resource : 'event' } ); 
        $scope.pastEvents = User.get( { id : key, resource : 'event', type : 'PAST'},
            processImpactTimeline); 
        $scope.orgs = User.get( { id : key, resource : 'org' } ); 
        
    };
    function processImpactTimeline() {
        for (var idx = 0; idx < $scope.pastEvents.data.length; idx++) {
            var event = $scope.pastEvents.data[idx];
            if (event.album) {
                event.album.coverPhotoUrl = FbUtil.getAlbumCoverUrl(event.album.id);
            }
            if (!event.currentUserRating) {
                event.currentUserRating = { value: undefined };
            }
        }
    }
    $scope.save = function( ) { 
        Me.save( $scope.me ); 
    }; 
    $scope.enableEdit = function( ) { 
        if( $scope.me.permission === "ALL" ) 
        { 
            $scope.edit = true;   
        }
    }; 
    $scope.saveEdit = function( ) { 
        $scope.edit = false; 
        $scope.origAboutMe  = $scope.me.about;
        User.save( { id : $scope.me.key }, $scope.me ); 
    };
    $scope.disableEdit = function( ) { 
        $scope.edit = false; 
        $scope.me.about = $scope.origAboutMe;
        
    };
    $scope.addEmail = function( ) { 
        //TO-DO check if the new one is marked primary and unmark the current primary one
        $scope.me.registeredEmails.push( $scope.newMail ); 
        $scope.save( ); 
    };
    $scope.removeEmail = function( ) {
        $scope.me.registeredEmails.splice( this.$index, 1 ); 
        $scope.save( ); 
    }; 
    $scope.mailPrimaryIndex = { index : 0 }; 
    $scope.load( $location, $routeParams );
};
var orgDetailCtrl = function( $scope, $location, $routeParams, $rootScope, $http, Org, Events, FbUtil ) 
{ 
    $scope.events = [];
    $scope.parser = document.createElement( 'a' ); 
    $scope.org = Org.get( { id : $routeParams.orgId }, function( ) { 
            // TODO(avaliani): Switch to using $facebook.
            $scope.parser.href = $scope.org.page.url; 
            $http( { method : 'GET', url : FbUtil.GRAPH_API_URL + "" + $scope.parser.pathname } ).success( function( data ) {
                    $scope.fbPage = data;
            } ); 
            $scope.pastEvents = Events.get( { type : "PAST", keywords : "org:" + $scope.org.searchTokenSuffix },function(){
            
                    angular.forEach($scope.pastEvents.data, function(event) {
                            $http( { method : 'GET', url : FbUtil.GRAPH_API_URL +"/" + event.album.id +"/photos"} ).success( function( data ) {
                                    event.fbAlbum = data;
                            } );
                            $scope.events.push({title:event.title, start:(new Date(event.startTime)), end:(new Date(event.endTime)),allDay:false, url:"/#/event/"+event.key});
            });
            } ); 
            $scope.upcomingEvents = Events.get( { type : "UPCOMING", keywords : "org:" + $scope.org.searchTokenSuffix }, function(){
            
                    angular.forEach($scope.upcomingEvents.data, function(event) {
                        
                            $scope.events.push({title:event.title, start:(new Date(event.startTime)),end:(new Date(event.endTime)),allDay:false, url:"/#/event/"+event.key});
            });
            } ); 
            
            $scope.allTimeLeaders = Org.get( { type : "ALL_TIME" }, { id : $routeParams.orgId, resource : "leaderboard" } ); 
            
            $scope.lastMonthLeaders = Org.get( { type : "THIRTY_DAY" }, { id : $routeParams.orgId, resource : "leaderboard" } ); 

            if( $scope.org.permission === "ALL" ) 
            { 
                $scope.pendingMembers = Org.get( { membership_status : "PENDING" }, { id : $routeParams.orgId, resource : "member" } ); 
            }
    } ); 
    $scope.orgOwners = Org.get( { role : "ADMIN" }, { id : $routeParams.orgId, resource : "member" } ); 
    $scope.orgOrgnaizers = Org.get( { role : "ORGANIZER" }, { id : $routeParams.orgId, resource : "member" } );
    $scope.childOrgs = Org.get( { id : $routeParams.orgId, resource : "children" } )
    $scope.getColorClass = function( index ) 
    {
        return colorClass [ index ]; 
    }
    $scope.calendarRender = function(myCal){
        myCal.fullCalendar('changeView','month');
    }
    /* config object */
    $scope.uiConfig = {
      calendar:{
        height: 450,
        editable: false,
        header:{
          left: 'month agendaWeek agendaDay',
          center: 'title',
          right: 'today prev,next'
        }
      }
    };
    //$scope.myCalendar.fullCalendar('render');
    $scope.eventSources = [$scope.events];
    
    
}
var orgCtrl = function( $scope, $location, $routeParams, $modal, Org ) { 
    $scope.query = ""; 
    $scope.newOrg = { page : { url : null, urlProvider : "FACEBOOK" }};
    $scope.refresh = function( ) {
        $scope.orgs = Org.get( { name_prefix : $scope.query } );
    };
    $scope.join = function( ) { 
        Org.save( $scope.newOrg, function( ) { 
                $scope.refresh( ); 
                $scope.newOrg = { page : { url : null, urlProvider : "FACEBOOK" }}; 
        } ); 
    };

    $scope.open = function( ) { 
        var modalInstance = $modal.open({
            templateUrl: 'createOrgModal.html',
            scope: $scope,
            controller: createOrgCtrl
        });
    };
    $scope.$watch( 'query', function( ) { 
        $scope.refresh( ); 
    });
    $scope.refresh( );
};
var createOrgCtrl = function ($scope, $modalInstance) {
    $scope.save = function( ) { 
        if( $scope.newOrg.page.url ) { 
            Org.save( $scope.newOrg, function( ) { 
                    $scope.refresh( ); 
                    $scope.newOrg = { page : { url : null, urlProvider : "FACEBOOK" }}; 
            } ); 
        } 
        $scope.close( );
    };    
    $scope.close = function( ) { 
        $scope.newOrg = { page : { url : null, urlProvider : "FACEBOOK" }}; 
        $modalInstance.close();
    };
}
var eventsCtrl = function( $scope, $location, Events, $rootScope ) {
    $scope.modelOpen = false;
    angular.extend( $scope, {
            /** the initial center of the map */
            center : { 
                latitude : $rootScope.getGeoLocation().latitude, 
                longitude : $rootScope.getGeoLocation().longitude 
            },
            /** the initial zoom level of the map */
            zoom : 4,
            /** list of markers to put in the map */
            markers : [{}],

    } ); 
    
    $scope.isMap=false;
    $scope.showMap = function()
    {
        $scope.isMap=true;
        angular.forEach($scope.events.data, function(event){
                $scope.addMarker(event.location.address.geoPt.latitude,event.location.address.geoPt.longitude);
        });
        $scope.zoom = 10;
    }
    $scope.showList = function(){
        $scope.isMap=false;
    };
    var fbUserId, query;
    $scope.reset = function( force ) {
        if ((fbUserId != $scope.fbUserId) || (query != $scope.query) || force) {
            fbUserId = $scope.fbUserId;
            query = $scope.query;
            $scope.events = Events.get(
                { 
                    keywords: ($scope.query ? $scope.query : ""),
                    lat: $scope.center.latitude,
                    long:$scope.center.longitude
                },
                processEvents);
        }
    };
    function processEvents() {
        var now = new Date();
        var currentDate = new Date( 1001, 01, 01, 01, 01, 01, 0 );
        for (var idx = 0; idx < $scope.events.data.length; idx++) {
            var event = $scope.events.data[idx];
            var dateVal = new Date(event.startTime); 
            var showHeader = (dateVal.getDate() != currentDate.getDate()) ||
                (dateVal.getMonth() != currentDate.getMonth()) ||
                (dateVal.getFullYear() != currentDate.getFullYear());
            currentDate = dateVal;
            event.dateFormat = (now.getFullYear() == dateVal.getFullYear()) ? 'EEEE, MMM d' : 'EEEE, MMM d, y';
            event.showHeader = showHeader;
        }
    }

    $scope.$watch('query', function( ) { 
        $scope.reset( ); 
    } );    
    $scope.$watch('fbUserId', function( ) { 
        $scope.reset( );
    } );
    $scope.addMarker = function( markerLat, markerLng ) { 
        $scope.markers.push( { 
            latitude : parseFloat( markerLat ), 
            longitude : parseFloat( markerLng ) 
        } );
    };
    $scope.register = function(type) {
        var eventId = this.modelEvent.key;
        $scope.modelIndex = this.$index;
        Events.save(
            {
                id: eventId,
                registerCtlr: 'participants',
                regType: type
            }, 
            null,
            function() {
                //alert and close
                $scope.modelEvent.registrationInfo = type;
                $scope.events.data[$scope.modelIndex].registrationInfo = type;
                if (type === 'REGISTERED') {
                    //$rootScope.showAlert("Your registration is successful!","success");
                    $scope.modelEvent.numRegistered++;
                    $scope.events.data[$scope.modelIndex].cachedParticipantImages.push({
                        "participant": {
                            "key": $rootScope.me.key
                        },
                        "imageUrl": $rootScope.me.profileImage.url,
                        "imageUrlProvider": "FACEBOOK"
                    });
                } else if (type === 'WAIT_LISTED') {
                    //$rootScope.addAlert("You are added to waitlist!","success");
                }
            });
    };

    $scope.delete = function( ) { 
        var eventId = this.event.key;
        Events.delete( { id : eventId }, function( ) { 
                $( "#event_" + eventId ).fadeOut( ); 
        } );
    }; 
    $scope.modelEvent = {};
    $scope.toggleEvent = function( ) {
        if( $( '#' + this.event.key + '_detail' ).is( ":visible" ) ) 
        { 
            $( '.event-detail' ).hide( ); 
        } 
        else
        { 
            $( '.event-detail' ).hide( ); 
            $scope.modelEvent = Events.get( { id : this.event.key, registerCtlr : 'expanded_search_view' } );
            $( '#' + this.event.key + '_detail' ).show( ); 
        }
    };

    $scope.reset(true);
};
var addEditEventsCtrl =  function( $scope, $rootScope, $routeParams, $filter, $location, Events, $http, FbUtil, EventUtil ) {
    $scope.EventUtil = EventUtil;
    angular.extend( $scope, {
            /** the initial center of the map */
            center : { 
                latitude : 37, 
                longitude : - 122 
            },
            /** the initial zoom level of the map */
            zoom : 4,
            /** list of markers to put in the map */
            markers : [ {} ],
            // These 2 properties will be set when clicking on the map
            clicked : null,  
            clicked : null,
            eventStartDate : null, 
            eventStartTime : null, 
            eventEndDate : null, 
            eventEndTime : null,
    } ); 
    $scope.suitableForList = [ 
        { name : 'Kids', key : 'KIDS',   checked : false }, 
        { name : 'Seniors', key : 'AGE_55_PLUS',   checked : false }, 
        { name : 'Groups', key : 'GROUPS',     checked : false }, 
        { name : 'Teens', key : 'TEENS',  checked : false } 
    ];
    $scope.currentUserRating = { value: undefined };

    $scope.unregister = function( ) 
    { 
        Events.delete( { id : $scope.event.key, registerCtlr : 'participants' }, function( ) { 
                $scope.refreshEvent( );
        } ); 
    } 
    $scope.register = function( type ) { 
        var eventId = $scope.event.key; 
        Events.save( { id : eventId, registerCtlr : 'participants', regType : type }, function( req, $rootScope ) { 
                //alert and close
                $scope.refreshEvent( ); 
                //$scope.addAlert("Registration successful!");
                $scope.event.registrationInfo = type;                
                $scope.event.numAttending ++;
                if( type === 'REGISTERED' ) 
                { 
                    $scope.eventRegistered.data.push( 
                        { 
                            "user" : { 
                                "key" : $rootScope.me.key, 
                                "profileImage" : { 
                                    "url" : $rootScope.me.profileImage.url
                                }, 
                                "firstName" : $rootScope.me.firstName, 
                                "lastName" : $rootScope.me.lastName
                            } 
                        } 
                        ); 
                } 
                else if( type === 'WAIT_LISTED' ) 
                { 
                    $scope.eventWaitListed.data.push( 
                        { 
                            "user" : { 
                                "key" : $rootScope.me.key, 
                                "profileImage" : { 
                                    "url" : $rootScope.me.profileImage.url
                                }, 
                                "firstName" : $rootScope.me.firstName, 
                                "lastName" : $rootScope.me.lastName
                            } 
                        }
                        ); 
                }
        } );
    };
    $scope.fbUpload = function()
    {
        //https://graph.facebook.com/{{event.album.id}}/photos?access_token={{fbAccessToken}}
    }
    $scope.dropListener = function( eDraggable, eDroppable ) {
                var isDropForbidden = function( aTarget, item ) { 
                    if( aTarget.some( function( i ) { 
                            return i.key == item.key; 
                    } )) { 
                        return { reason : 'target already contains "' + item.key + '"' }; 
                        } else { 
                            return false; 
                        } 
                };
                var onDropRejected = function( error ) { 
                    alert( 'Operation not permitted: ' + error.reason ); 
                };
                var onDropComplete = function( eSrc, item, index ) { 
                    //console.log('moved "' + item.key + ' from ' + eSrc.data('model') + '[' + index + ']' + ' to ' + eDroppable.data('model'));
                };
                var eSrc = eDraggable.parent( ); 
                var sSrc = eSrc.data( 'model' ); 
                var sTarget = eDroppable.data( 'model' );
                if( sSrc != sTarget ) { 
                    $scope.$apply( function( ) { 
                            var index = eDraggable.data( 'index' ); 
                            var aSrc = $scope.$eval( sSrc ); 
                            var aTarget = $scope.$eval( sTarget ); 
                            var item = aSrc [ index ]; 
                            var error = isDropForbidden( aTarget, item ); 
                            if( error ) { 
                                onDropRejected( error ); 
                            } else { 
                                aTarget.push( item ); 
                                aSrc.splice( index, 1 ); 
                                onDropComplete( eSrc, item, index ); 
                            } 
                    } ); 
                }
        };
        $scope.refreshMap = function( ) {
            if( $scope.event && $scope.event.location.address.street ) 
            {    
                geocoder.geocode( { 'address' : $scope.event.location.address.street + ',' + $scope.event.location.address.city + ',' + $scope.event.location.address.state + ',' + $scope.event.location.address.country }, function( results, status ) { 
                        if( status == google.maps.GeocoderStatus.OK ) { 
                            $scope.center.latitude = results [ 0 ].geometry.location.lat( ); 
                            $scope.center.longitude = results [ 0 ].geometry.location.lng( );
                            $scope.setMarker( $scope.center.latitude, $scope.center.longitude ); 
                            $scope.zoom = 15; 
                            $scope.$apply( );
                        }
            } )} 
        };
        $scope.addMarker = function( markerLat, markerLng ) { 
            $scope.markers.push( { 
                    latitude : parseFloat( markerLat ), 
                    longitude : parseFloat( markerLng ) 
            } );
        };
        $scope.setMarker = function( markerLat, markerLng ) { 
            $scope.markers = [ {} ]; 
            $scope.markers.push( { 
                    latitude : parseFloat( markerLat ), 
                    longitude : parseFloat( markerLng ) 
            } );
        };
        $scope.findMe = function( ) {
            if( $scope.geolocationAvailable ) {
                navigator.geolocation.getCurrentPosition( function( position ) {
                        $scope.center = { 
                            latitude : position.coords.latitude, 
                            longitude : position.coords.longitude
                        };
                        $scope.zoom = 15;
                        $scope.$apply( ); 
                }, function( ) {
                } ); 
            }   
        };
        $scope.getMore = function( type ) 
        { 
            if( $scope.eventRegistered.paging.next == null ) 
            { 
                return; 
            }    
            if( type === 'REGISTERED' ) 
            {
                $http( { method : 'GET', url : $scope.eventRegistered.paging.next } ).success( function( data ) {
                        for( var i = 0; i < data.data.length; i ++ ) 
                        { 
                            $scope.eventRegistered.data.push( data.data [ i ] ); 
                        }
                        $scope.eventRegistered.paging.next = data.paging ? data.paging.next : null;
                } );
            } 
            else if( type === 'ORGANIZER' ) 
            {
                $http( { method : 'GET', url : $scope.eventOrganizers.paging.next } ).success( function( data ) {
                        for( var i = 0; i < data.data.length; i ++ ) 
                        { 
                            $scope.eventOrganizers.data.push( data.data [ i ] ); 
                        }
                        $scope.eventOrganizers.paging.next = data.paging ? data.paging.next : null;
                } );
            } 
            else if( type === 'WAIT_LISTED' ) 
            {
                $http( { method : 'GET', url : $scope.eventWaitListed.paging.next } ).success( function( data ) {
                        for( var i = 0; i < data.data.length; i ++ ) 
                        { 
                            $scope.eventWaitListed.data.push( data.data [ i ] ); 
                        }
                        $scope.eventWaitListed.paging.next = data.paging ? data.paging.next : null;
                } );
            }    
        };
        $scope.save = function( ) { 
            $scope.event.startTime = $scope.parseDateReg( $( '#dtst' ).val( )+ ' ' + $( '#tmst' ).val( )); 
            $scope.event.endTime = $scope.parseDateReg( $( '#dtend' ).val( )+ ' ' + $( '#tmend' ).val( )); 
            $scope.event.suitableForTypes =[ ]; 
            for( var i = 0; i < $scope.suitableForList.length; i ++ ) 
            {
                if( $scope.suitableForList [ i ].checked === true ) 
                { 
                    $scope.event.suitableForTypes.push( $scope.suitableForList [ i ].key ); 
                }    
            }    
            if( $location.$$url == "/event/add" ) 
            { 
                Events.save( $scope.event, function( data, status, headers, config ) {
                        $location.path( '/event' ); 
                } ); 
            } 
            else
            {    
                Events.save( { id : $scope.event.key }, $scope.event, function( data, status, headers, config ) { 
                        
                } ); 
            } 
        };
        $scope.refreshEvent = function( ) {
            $scope.event = Events.get( { id : $routeParams.eventId }, function( ) { 
                    //$("#location-title").val(''+$scope.event.location.title);
                    //$scope.refreshMap();
                    //TDEBT - remove DOM references
                    
                    $( '#dtst' ).val( $filter( 'date' )( $scope.event.startTime, 'MM/dd/yyyy' ) ); 
                    $( '#tmst' ).val( $filter( 'date' )( $scope.event.startTime, 'h:mma' ) ); 
                    $( '#dtend' ).val( $filter( 'date' )( $scope.event.endTime, 'MM/dd/yyyy' ) ); 
                    $( '#tmend' ).val( $filter( 'date' )( $scope.event.endTime, 'h:mma' ) );
                    //$scope.eventStartTime : null,
                    //$scope.eventEndDate : null,
                    //$scope.eventEndTime : null,
                    
                    angular.forEach( $scope.suitableForList, function( client ) { 
                            angular.forEach( $scope.event.suitableForTypes, function( server ) 
                                {
                                    if( server === client.key ) 
                                    { 
                                        client.checked = true; 
                                    }    
                                } ); 
                    } );
                    if( $scope.event.location.address.geoPt != null ) 
                    {
                        $scope.center = { 
                            latitude : $scope.event.location.address.geoPt.latitude, 
                            longitude : $scope.event.location.address.geoPt.longitude
                        };
                        $scope.setMarker( $scope.center.latitude, $scope.center.longitude );
                        $scope.zoom = 15;
                    }
                    $scope.eventOrganizers = Events.get( { id : $routeParams.eventId, registerCtlr : 'participants', regType : 'ORGANIZER' }, function( ) { 
                            $scope.eventOrganizers.data.push = function( ) { 
                                Events.save( { user : arguments [ 0 ].key }, { id : $routeParams.eventId, registerCtlr : 'participants', regType : 'ORGANIZER' } ); 
                                return Array.prototype.push.apply( this, arguments ); 
                            }
                    } ); 
                    $scope.eventRegistered = Events.get( { id : $routeParams.eventId, registerCtlr : 'participants', regType : 'REGISTERED' }, function( ) { 
                            $scope.eventRegistered.data.push = function( ) { 
                                Events.save( { user : arguments [ 0 ].key }, { id : $routeParams.eventId, registerCtlr : 'participants', regType : 'REGISTERED' } ); 
                                return Array.prototype.push.apply( this, arguments ); 
                            }
                    } ); 
                    $scope.eventWaitListed = Events.get( { id : $routeParams.eventId, registerCtlr : 'participants', regType : 'WAIT_LISTED' }, function( ) { 
                            $scope.eventWaitListed.data.push = function( ) { 
                                Events.save( { user : arguments [ 0 ].key }, { id : $routeParams.eventId, registerCtlr : 'participants', regType : 'WAIT_LISTED' } ); 
                                return Array.prototype.push.apply( this, arguments ); 
                            }
                    } );
                    
                    $scope.eventNoShow = Events.get( { id : $routeParams.eventId, registerCtlr : 'participants', regType : 'REGISTERED_NO_SHOW' }, function( ) { 
                            $scope.eventNoShow.data.push = function( ) { 
                                Events.save( { user : arguments [ 0 ].key }, { id : $routeParams.eventId, registerCtlr : 'participants', regType : 'REGISTERED_NO_SHOW' } ); 
                                return Array.prototype.push.apply( this, arguments ); 
                            }
                    } );                    
                    
                    $scope.noShow = function(){
                        $scope.eventNoShow.data.push($scope.eventRegistered.data[this.$index]);
                        $scope.eventRegistered.data.splice(this.$index,1);
                    };
                    
                    $scope.attended = function(){
                        $scope.eventRegistered.data.push($scope.eventRegistered.data[this.$index]);
                        $scope.eventNoShow.data.splice(this.$index,1);
                    };
                    
                    $scope.openImageUpload = function( ) { 
                        $scope.isImageModelOpen = true; 
                    };
                    $scope.closeImageUpload = function( ) { 
                        //$scope.closeMsg = 'I was closed at: ' + new Date();
                        $scope.isImageModelOpen = false; 
                    };
                    $scope.imageModalOpts = { 
                        backdropFade : true, 
                    dialogFade : true         }; 
                    var mydiv = document.getElementById( 'myCommentsDiv' );
                    FbUtil.getComments( mydiv ); 
                    if( $scope.event.status == 'COMPLETED' ) 
                    {
                        if( $scope.event.album ) 
                        { 
                            // TODO(avaliani): switch to using $facebook.
                            $http( { method : 'GET', url : FbUtil.GRAPH_API_URL + "/" + $scope.event.album.id + "/photos" } ).success( function( data ) {
                                    $scope.fbAlbum = data.data; 
                                    $scope.myInterval = 5000;
                            } );
                        }

                        if (EventUtil.canWriteReview($scope.event)) {
                            Events.get(
                                { id : $routeParams.eventId, registerCtlr : 'review' },
                                function (value) {
                                    $scope.currentUserRating.value = 
                                        (value && value.rating) ? value.rating.value : undefined;
                                });
                        }
                    }
            }, function( response ) { 
                //404 or bad
                
                if( response.status === 404 ) { 
            }} );
        }
        $scope.parseDateReg = function( input ) {
            var dateReg = 
                /(\d{1,2})\/(\d{1,2})\/(\d{2,4})\s*(\d{1,2})(:\d{2})*\s*(am|pm|AM|PM)/;
            var year, month, day, hour, minute, second, 
            result = dateReg.exec( input ); 
            if( result ) { 
                year = + result[3];
                if (year < 100) {
                    year += 2000;
                } else if (year < 2000) {
                    year = undefined;
                }
                month = + result[1]; 
                day = + result[2]; 
                hour = + result[4];
                minute = result[5] ? (+ result[5].substr(1)) : 0;
                second = 0; 
                if(( result[6] == 'pm' || result[6] == 'PM' ) && hour != 12 ) { 
                    hour += 12; 
                }       
            }
            return new Date( year, month - 1, day, hour, minute, second ); 
        } 
        $scope.placeChanged = function( place ) {
            $scope.event.location.title = place.name;
            $scope.event.location.address.street = ''; 
            for( var i = 0; i < place.address_components.length; i ++ ) {
                if( place.address_components [ i ].types [ 0 ]== 'locality' ) 
                { 
                    $scope.event.location.address.city = place.address_components [ i ].long_name; 
                } 
                else if( place.address_components [ i ].types [ 0 ]== 'country' ) 
                { 
                    $scope.event.location.address.country = place.address_components [ i ].long_name; 
                } 
                else if( place.address_components [ i ].types [ 0 ]== 'postal_code' ) 
                { 
                    $scope.event.location.address.zip = place.address_components [ i ].long_name; 
                } 
                else if( place.address_components [ i ].types [ 0 ]== 'administrative_area_level_1' ) 
                { 
                    $scope.event.location.address.state = place.address_components [ i ].long_name; 
                } 
                else if( place.address_components [ i ].types [ 0 ]== 'street_number' ) 
                { 
                    $scope.event.location.address.street = place.address_components [ i ].long_name + ' ' + $scope.event.location.address.street; 
                } 
                else if( place.address_components [ i ].types [ 0 ]== 'route' ) 
                { 
                    $scope.event.location.address.street = $scope.event.location.address.street + ' ' + place.address_components [ i ].long_name; 
                }
            }
            $scope.center = { 
                latitude : place.geometry.location.lat( ), 
                longitude : place.geometry.location.lng( ) 
            }; 
            $scope.setMarker( $scope.center.latitude, $scope.center.longitude );
            $scope.zoom = 15;
            $scope.event.location.address.geoPt = { latitude : $scope.center.latitude, longitude : $scope.center.longitude }; 
            $scope.$apply( );
        };
        if( $location.$$url == "/event/add" ) 
        { 
            $scope.findMe( ); 
            $scope.event = { "location" : { "title" : null, "description" : null, "address" : { "street" : null, "city" : null, "state" : null, "country" : null, "zip" : null, "geoPt" : null }}};
        }
        else
        {    
            $scope.refreshEvent( ); 
        }
};

// TODO(avaliani): refactor this dependency
function isLoggedIn() {
    if( $.cookie( "facebook-token" ) ) { 
        return true; 
    } else { 
        return false; 
    }
}
var colorClass = [ "primary", "success", "info", "warning", "danger", "default" ];

function isExternal(url) {
    var match = url.match(/^([^:\/?#]+:)?(?:\/\/([^\/?#]*))?([^?#]+)?(\?[^#]*)?(#.*)?/);
    if (typeof match[1] === "string" && match[1].length > 0 && match[1].toLowerCase() !== location.protocol) return true;
    if (typeof match[2] === "string" && match[2].length > 0 && match[2].replace(new RegExp(":("+{"http:":80,"https:":443}[location.protocol]+")?$"), "") !== location.host) return true;
    return false;
}

