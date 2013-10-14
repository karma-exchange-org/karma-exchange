angular
    .module('loadingOnAJAX', [])
    .config(function($httpProvider) {
        var numLoadings = 0;
        var loadingScreen = $('<div style="position:fixed;top:0;left:0;right:0;bottom:0;z-index:10000;background-color:gray;background-color:rgba(70,70,70,0.2);"><img style="position:absolute;top:50%;left:50%;" alt="" src="/img/fbLoading.gif" /></div>')
            .appendTo($('body')).hide();
        $httpProvider.responseInterceptors.push(function() {
            return function(promise) {
                numLoadings++;
                loadingScreen.show();
                var hide = function(r) { if (!(--numLoadings)) loadingScreen.hide(); return r; };
                return promise.then(hide, hide);
            };
        });
    });
angular.module( 'globalErrors', [ ] ).config( function( $provide, $httpProvider, $compileProvider ) { 
        var elementsList = $( );
        var showMessage = function( content, cl, time ) { 
            $( '<alert/>' ).addClass( 'message' ).addClass( cl ).hide( ).fadeIn( 'fast' ).delay( time ).fadeOut( 'fast', function( ) { $( this ).remove( ); } ).appendTo( elementsList ).text( content ); 
        };
        $httpProvider.defaults.headers.post["Content-Type"] = "application/json;charset=UTF-8";
		$httpProvider.defaults.transformRequest.push( function( data, headersGetter ) { 
				//console.log(angular.toJson(headersGetter()));
				//check if it is a post request or requires authentication and 
				if(headersGetter()["Content-Type"]!=null&&checkLogin())
				{
					alert("login required");
				}
			return data; 
		} );		
			$httpProvider.responseInterceptors.push( function( $rootScope, $timeout, $q ) { 
                return function( promise ) { 
                    return promise.then( function( successResponse ) { 
                            if( successResponse.config.method.toUpperCase( ) != 'GET' &&!isExternal(successResponse.config.url) ) 
                            {
                                
                                $rootScope.showAlert( "Saved successfully!", "success" ); 
                            }
                            return successResponse;
                    }, function( errorResponse ) { 
                        switch( errorResponse.status ) { 
                        case 400 : $rootScope.showAlert( errorResponse.data.error.message, "error" ); 
                            break; 
                        case 401 : 
                            showMessage( 'Wrong usename or password', 'errorMessage', 20000 ); 
                            break; 
                        case 403 : 
                            showMessage( 'You don\'t have the right to do this', 'errorMessage', 20000 ); 
                            break; 
                        case 404 : 
                            showMessage( 'Server internal error: ' + errorResponse.data, 'errorMessage', 20000 ); 
                            break; 
                        case 500 : 
                            showMessage( 'Server internal error: ' + errorResponse.data, 'errorMessage', 20000 ); 
                            break; 
                            default : 
                            showMessage( 'Error ' + errorResponse.status + ': ' + errorResponse.data, 'errorMessage', 20000 ); 
                        } 
                        return $q.reject( errorResponse ); 
                    } ); 
                }; 
        } );
        $compileProvider.directive( 'appMessages', function( ) { 
                var directiveDefinitionObject = { 
                    link : function( scope, element, attrs ) { elementsList.push( $( element ) ); } 
                }; 
                return directiveDefinitionObject; 
        } ); 
} );
angular.module( 'FacebookProvider', [ ] ).factory( 'Facebook', function( $rootScope ) { 
        return { 
            getLoginStatus : function( ) { 
                FB.getLoginStatus( function( response ) { 
                        $rootScope.$broadcast( "fb_statusChange", { 'status' : response.status } );
                }, true ); 
            }, 
            login : function( ) { 
                FB.getLoginStatus( function( response ) { 
                        switch( response.status ) { 
                        case 'connected' : 
                            $rootScope.$broadcast( 'fb_connected', { facebook_id : response.authResponse.userID } ); 
                            break; 
                        case 'not_authorized' || 'unknown' : 
                            // 'not_authorized' || 'unknown': doesn't seem to work
                            FB.login( function( response ) { 
                                    if( response.authResponse ) { 
                                        $rootScope.$broadcast( 'fb_connected', { 
                                                facebook_id : response.authResponse.userID, 
                                        userNotAuthorized : true                                } ); 
                                    } else { 
                                        $rootScope.$broadcast( 'fb_login_failed' ); 
                                    } 
                        }, { scope : 'email,user_location' } ); 
                                        break; 
                                        default : 
                                        FB.login( function( response ) { 
                                                if( response.authResponse ) { 
                                                    $rootScope.$broadcast( 'fb_connected', { facebook_id : response.authResponse.userID } ); 
                                                    $rootScope.$broadcast( 'fb_get_login_status' ); 
                                                } else { 
                                                    $rootScope.$broadcast( 'fb_login_failed' ); 
                                                } 
                        }, { scope : 'email,user_location' } ); 
                                        break; 
                                    } 
                            }, true ); 
                        }, 
                        logout : function( ) { 
                            FB.logout( function( response ) { 
                                    if( response ) { 
                                        $rootScope.$broadcast( 'fb_logout_succeded' ); 
                                    } else { 
                                        $rootScope.$broadcast( 'fb_logout_failed' ); 
                                    }
                            } ); 
                        }, 
                        unsubscribe : function( ) { 
                            FB.api( "/me/permissions", "DELETE", function( response ) { 
                                    $rootScope.$broadcast( 'fb_get_login_status' ); 
                            } ); 
                        }, 
                        getFBComments : function( mydiv ) { 
                            if( mydiv ) 
                            { 
                                mydiv.innerHTML = 
                                '<div class="fb-comments" href="' + window.location.href + '" data-num-posts="20" data-width="940">'; 
                                if(FB)
                                {
                                    FB.XFBML.parse( mydiv );  
                                }
                                 
                            }
                        },
                        sendFBMessage : function(){
                            FB.ui({
                  method: 'send',
                  link: $rootScope.getLocation()
                });
                        }
                }; 
} );
kexApp = angular.module( "kexApp", 
    ["ngResource", "ngCookies", "google-maps", "ui.bootstrap", "loadingOnAJAX", "FacebookProvider", 
     "globalErrors" ,"ui.calendar","ngSocial"] )
.config( function( $routeProvider, $httpProvider ) { 
        $routeProvider.when( '/', { controller : homeCtrl, templateUrl : 'partials/home.html' } )
            .when( '/home', { controller : homeCtrl, templateUrl : 'partials/home.html' } )
            .when( '/me', { controller : meCtrl, templateUrl : 'partials/me.html' } )
            .when( '/user/:userId', { controller : meCtrl, templateUrl : 'partials/me.html' } )
            .when( '/mysettings', { controller : meCtrl, templateUrl : 'partials/mysettings.html' } )
            .when( '/event', { controller : eventsCtrl, templateUrl : 'partials/events.html' } )
            .when( '/event/add', { controller : addEditEventsCtrl, templateUrl : 'partials/addEditevent.html' } )
            .when( '/event/:eventId/edit', { controller : addEditEventsCtrl, templateUrl : 'partials/addEditevent.html' } )
            .when( '/event/:eventId', { controller : addEditEventsCtrl, templateUrl : 'partials/viewEvent.html' } )
            .when( '/org', { controller : orgCtrl, templateUrl : 'partials/organization.html' } )
            .when( '/org/:orgId', { controller : orgDetailCtrl, templateUrl : 'partials/organizationDetail.html' } )
            .otherwise( { redirectTo : '/' } );
        delete $httpProvider.defaults.headers.common [ 'X-Requested-With' ]; 
        //$httpProvider.defaults.headers.common['X-'] = 'X';
        
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
)
.run( function( $rootScope, Me, $location, Facebook ) { 
    if( document.location.hostname === "localhost" ) 
    { 
        fbAppId = '276423019167993'; 
    }   
    else if( document.location.hostname === "karmademo.dyndns.dk" ) 
    { 
        fbAppId = '1381630838720301'; 
    } 
    else if( document.location.hostname === "kex-latest.appspot.com" ) 
    { 
        fbAppId = '166052360247234'; 
    } 
    else
    { 
        fbAppId = '571265879564450'; 
    } 
    $rootScope.fbScope = "email,user_location"; 
    $rootScope.facebook = Facebook;
    $rootScope.location = $location; 
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
    $rootScope.getFbImageUrl = function(id, type) {
        return "//graph.facebook.com/" + id + "/picture?type=" + type;
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

    window.fbAsyncInit = function( ) { 
        FB.init( { 
                appId : fbAppId, 
                status : true, 
                cookie : true, 
        xfbml : true        } ); 
        FB.Event.subscribe( 'auth.statusChange', function( response ) { 
                $rootScope.$broadcast( "fb_statusChange", { 'status' : response.status } ); 
        } ); 
        FB.Event.subscribe( 'auth.authResponseChange', function( response ) { 
                $rootScope.$broadcast( "fb_authResponseChange", { 'response' : response } );
        } ); 
    };( function( d ) { 
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
All webservice factories go here
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
All app directives  go here
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



kexApp.directive( "timelineblock", function( ) { 
        
        return {
                restrict : "A",
     
          
        
        
        link : function( scope, element, attrs ) { 
            
            
            $(element).BlocksIt({
                numOfCol: 2,
                offsetX: 0,
                offsetY: 5,
                blockElement: 'div'
               });
             $('#timelineblock').BlocksIt({
                numOfCol: 2,
                offsetX: 0,
                offsetY: 5,
                blockElement: 'div'
               });
            var all_blocks = $(element).find('.block');
            console.log(all_blocks);
            $.each(all_blocks, function(i, obj){
                var posLeft = $(obj).css("left");
    
                if ( posLeft == "0px" ) {
                    $(obj).css("margin", "0px 0px 20px 65px").css("width", "340px").css("float", "left");
                    $(obj).children("span#edge").addClass("redge");         
                } else  {
                    $(obj).css("margin", "0px 0px 20px 18px").css("float","right").css("width", "340px").css("clear","both");
                    $(obj).children("span#edge").addClass("ledge");         
                }       
                    
            });
            $(".block").hover(function() {
            var posLeft = $(this).css("left");
            
            if ( posLeft == "0px" ) {
                        $(this).children("span#edge").addClass("redge_h");          
                    } else  {
                        $(this).children("span#edge").addClass("ledge_h");          
                    }   
            }, function () {
            var posLeft = $(this).css("left");
        
                if ( posLeft == "0px" ) {
                            $(this).children("span#edge").removeClass("redge_h");           
                        } else  {
                            $(this).children("span#edge").removeClass("ledge_h");           
                        } 
            });
            
        } 
        }
} );

kexApp.directive('shareButtons', function() {
        return {
            restrict: 'E',
            scope: {
                url: '=',  // TODO(avaliani): figure out how to avoid explicitly passing the url
                title: '=',
                description: '=',
                image: '='
            },
            replace: true,
            transclude: false,
            template:
                '<div>' +
                    '<ul data-ng-social-buttons ' +
                         // 'data-url="getLocation()" ' +
                         'data-url="url" ' +                         
                         'data-title="title" ' +
                         'data-description="description" ' +
                         'data-image="image">' +
                        '<li class="ng-social-facebook">Facebook</li>' +
                        '<li class="ng-social-google-plus">Google+</li>' +
                        '<li class="ng-social-twitter">Twitter</li>' +
                        '<li class="ng-social-facebook-message">Facebook</li>' +
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
                '<li data-ng-repeat="userImage in event.cachedParticipantImages">' +
                    '<a href="#/user/{{userImage.participant.key}}">' +
                        '<img data-ng-src="{{userImage.imageUrl}}?type=square" class="kex-thumbnail-user-mini">' +
                    '</a>' +
                '</li>' +
            '</ul>'
    }
});

/*
All app controllers  go here
*/
function fbCntrl( Facebook, $scope, $rootScope, $http, $location, Me ) { 
    $scope.info = {}; 
    $rootScope.location = $location;
    $rootScope.getLocation = function()
    {
        return window.location.href;
    }   
    $rootScope.fbGraphAPIURL = "https://graph.facebook.com"; 
    $rootScope.$on( "fb_statusChange", function( event, args ) { 
            $rootScope.fb_status = args.status;
            if( $rootScope.fb_status === 'connected' ) 
            { 
                Facebook.getLoginStatus( );  
                $rootScope.me = Me.get( ); 
                $rootScope.orgs = Me.get( { resource : 'org' } ); 
            } 
            else
            { 
                $.removeCookie( "facebook-uid" ); 
                $.removeCookie( "facebook-token" ); 
                $.removeCookie( "login" ); 
            }
    } ); 
    $rootScope.$on( "fb_authResponseChange", function( event, args ) {
            if( args.response.status === 'connected' ) 
            {
                $.cookie( "facebook-uid", args.response.authResponse.userID ); 
                $.cookie( "facebook-token", args.response.authResponse.accessToken ); 
                $.cookie( "login", "facebook" ); 
                $rootScope.fbAccessToken = args.response.authResponse.accessToken;
            } 
            else
            { 
                removeCookies(); 
            }
            $rootScope.$apply( ); 
    } ); 
    $rootScope.$on( "fb_get_login_status", function( ) { 
            Facebook.getLoginStatus( );
    } ); 
    $rootScope.$on( "fb_login_failed", function( ) {
    		removeCookies();
    } ); 
    $rootScope.$on( "fb_logout_succeded", function( ) {
            $rootScope.id = ""; 
    } ); 
    $rootScope.$on( "fb_logout_failed", function( ) { 
            
    } );
    $rootScope.$on( "fb_connected", function( event, args ) { 
            /*
            If facebook is connected we can follow two paths:
            The users has either authorized our app or not.
            
            ---------------------------------------------------------------------------------
            http://developers.facebook.com/docs/reference/javascript/FB.getLoginStatus/
            
            the user is logged into Facebook and has authenticated your application (connected)
            the user is logged into Facebook but has not authenticated your application (not_authorized)
            the user is not logged into Facebook at this time and so we don't know if they've authenticated
            your application or not (unknown)
            ---------------------------------------------------------------------------------
            
            If the user is connected to facebook, his facebook_id will be enough to authenticate him in our app,
            the only thing we will have to do is to post his facebook_id to 'php/auth.php' and get his info
            from the database.
            
            If the user has a status of unknown or not_authorized we will have to do a facebook api call to force him to
            connect and to get some extra data we might need to unthenticated him.
            */
            
            var params = {};
            function authenticateViaFacebook( parameters ) { 
                //posts some user data to a page that will check them against some db
                
            }
            if( args.userNotAuthorized === true ) { 
                //if the user has not authorized the app, we must write his credentials in our database
                //console.log("user is connected to facebook but has not authorized our app");
                FB.api( 
                    { 
                        method : 'fql.multiquery', 
                        queries : { 
                            'q1' : 'SELECT uid, first_name, last_name FROM user WHERE uid = ' + args.facebook_id, 
                            'q2' : 'SELECT url FROM profile_pic WHERE width=800 AND height=800 AND id = ' + args.facebook_id
                        } 
                    }, 
                    function( data ) { 
                        //let's built the data to send to php in order to create our new user
                        params = { 
                            facebook_id : data [ 0 ][ 'fql_result_set' ][ 0 ].uid, 
                            first_name : data [ 0 ][ 'fql_result_set' ][ 0 ].first_name, 
                            last_name : data [ 0 ][ 'fql_result_set' ][ 0 ].last_name, 
                            picture : data [ 1 ][ 'fql_result_set' ][ 0 ].url
                        } 
                        authenticateViaFacebook( params ); 
                    } ); 
            } 
            else { 
                //console.log("user is connected to facebook and has authorized our app");
                //the parameter needed in that case is just the users facebook id
                params = { 'facebook_id' : args.facebook_id }; 
                authenticateViaFacebook( params ); 
                $rootScope.facebook_id = args.facebook_id;
            }
    } );
    $rootScope.updateSession = function( ) { 
        //reads the session variables if exist from php
        
    };
    $rootScope.updateSession( );
    // button functions
    $scope.getLoginStatus = function( ) { 
        Facebook.getLoginStatus( ); 
    };
    $scope.login = function( ) { 
        Facebook.login( ); 
    };
    $rootScope.logout = function( ) { 
        Facebook.logout( ); 
        $rootScope.session = {}; 
        $rootScope.me = {}; 
        $rootScope.location.path( "/" );
        removeCookies();
        //make a call to a php page that will erase the session data
        
    };
    $scope.unsubscribe = function( ) { 
        Facebook.unsubscribe( ); 
    }
    $scope.getInfo = function( ) { 
        FB.api( '/' + $rootScope.facebook_id, function( response ) { 
                //console.log('Good to see you, ' + response.name + '.');
                
                
        } ); 
        $rootScope.info = $rootScope.session;
    }; 
}
var homeCtrl = function( $scope, $location ) { 
    if( checkLogin( $location ) ) 
    { 
        if( $location.$$url == "/" ) 
        { 
            $location.path( "/event" );   
        }
    }
};
var meCtrl = function( $scope, $location, User, Me, $rootScope, $routeParams ) { 
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
        $scope.pastEvents = User.get( { type : 'PAST' }, { id : key, resource : 'event' } ); 
        $scope.orgs = User.get( { id : key, resource : 'org' } ); 
        
    }; 
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
var orgDetailCtrl = function( $scope, $location, $routeParams, $rootScope, $http, Org, Events ) 
{ 
    $scope.events = [];
    $scope.parser = document.createElement( 'a' ); 
    $scope.org = Org.get( { id : $routeParams.orgId }, function( ) { 
            $scope.parser.href = $scope.org.page.url; 
            $http( { method : 'GET', url : $rootScope.fbGraphAPIURL + "" + $scope.parser.pathname } ).success( function( data ) {
                    $scope.fbPage = data;
            } ); 
            $scope.pastEvents = Events.get( { type : "PAST", keywords : "org:" + $scope.org.searchTokenSuffix },function(){
            
                    angular.forEach($scope.pastEvents.data, function(event) {
                            $http( { method : 'GET', url : $rootScope.fbGraphAPIURL +"/" + event.album.id +"/photos"} ).success( function( data ) {
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
    $scope.isMessageOpen = false; 
    $scope.showMessage = function( ) { 
        $scope.isMessageOpen = true; 
    }; 
    $scope.cancelMessage = function( ) { 
        $scope.isMessageOpen = false; 
    }; 
    $scope.sendMessage = function( ) { 
        $scope.isMessageOpen = false; 
    }; 
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
var orgCtrl = function( $scope, $location, $routeParams, Org ) { 
    $scope.query = ""; 
    $scope.newOrg = { page : { url : null, urlProvider : "FACEBOOK" }};
    $scope.refresh = function( ) {
        $scope.orgs = Org.get( { name_prefix : $scope.query } );
    };
    $scope.save = function( ) { 
        if( $scope.newOrg.page.url ) 
        { 
            Org.save( $scope.newOrg, function( ) { 
                    $scope.refresh( ); 
                    $scope.newOrg = { page : { url : null, urlProvider : "FACEBOOK" }}; 
            } ); 
        } 
        $scope.close( ); 
    };
    $scope.join = function( ) { 
        Org.save( $scope.newOrg, function( ) { 
                $scope.refresh( ); 
                $scope.newOrg = { page : { url : null, urlProvider : "FACEBOOK" }}; 
        } ); 
    };
    $scope.close = function( ) { 
        $scope.newOrg = { page : { url : null, urlProvider : "FACEBOOK" }}; 
        $scope.shouldBeOpen = false; 
    };
    $scope.open = function( ) { 
        $scope.shouldBeOpen = true; 
    };
    $scope.opts = { 
        backdropFade : true, 
        dialogFade : true      };
        $scope.$watch( 'query', function( ) { 
                $scope.refresh( ); 
        } );
        $scope.refresh( );
};
var eventsCtrl = function( $scope, $location, Events, $rootScope ) { 
    $scope.modelOpen = false;
    $scope.reset = function( ) {
        $scope.events = Events.get( { keywords : $scope.query } ); 
        $scope.currentDate = new Date( 1001, 01, 01, 01, 01, 01, 0 );
    }; 
    $scope.query = ""; 
    $scope.$watch( 'query', function( ) { 
            $scope.reset( ); 
    } ); 
    $scope.register = function( type ) { 
        var eventId = this.modelEvent.key;
        $scope.modelIndex = this.$index;
        Events.save( { id : eventId, registerCtlr : 'participants', regType : type }, function( index ) { 
                //alert and close
                
                $scope.modelEvent.registrationInfo = type;                
                $scope.modelEvent.numRegistered ++;
                if( type === 'REGISTERED' ) 
                { 
                    //$rootScope.showAlert("Your registration is successful!","success");   
                    $scope.events.data [ $scope.modelIndex ].cachedParticipantImages.push( 
                        { 
                            "participant" : { 
                                "key" : $rootScope.me.key
                            }, 
                            "imageUrl" : $rootScope.me.profileImage.url, 
                        "imageUrlProvider" : "FACEBOOK"                            }
                        ); 
                } 
                else if( type === 'WAIT_LISTED' ) 
                { 
                    //$rootScope.addAlert("You are added to waitlist!","success");
                    
                }
        } );
    };
    $scope.now = new Date();
    $scope.processEvent = function( dateParam, eventobj, first) {
        dateVal = new Date( dateParam ); 
        showHeader = true;
        if (!first) {
            showHeader = (dateVal.getDate() != $scope.currentDate.getDate()) ||
                (dateVal.getMonth() != $scope.currentDate.getMonth()) ||
                (dateVal.getFullYear() != $scope.currentDate.getFullYear());
        }
        $scope.currentDate = dateVal;
        $scope.dateFormat = ($scope.now.getFullYear() == dateVal.getFullYear()) ? 'EEEE, MMM d' : 'EEEE, MMM d, y';
        $scope.showHeader = showHeader;
    }
    $scope.reset( );
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
};
var addEditEventsCtrl =  function( $scope, $rootScope, $routeParams, $filter, $location, Events, $http, Facebook ) { 
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
                        c
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
                    Facebook.getFBComments( mydiv ); 
                    if( $scope.event.status == 'COMPLETED' ) 
                    {
                        if( $scope.event.album ) 
                        { 
                            $http( { method : 'GET', url : $rootScope.fbGraphAPIURL + "/" + $scope.event.album.id + "/photos" } ).success( function( data ) {
                                    $scope.fbAlbum = data.data; 
                                    $scope.myInterval = 5000;
                            } );
                        }
                        $scope.eventRating = Events.get( { id : $routeParams.eventId, registerCtlr : 'review' }, function( ) {
                                if( ! $scope.eventRating ||! $scope.eventRating.rating ) 
                                { 
                                    $scope.eventRating = { "rating" : { "value" : 0 }}; 
                                } 
                                //TODO - Make sure that the event is not called on-load
                                //if($scope.event.registrationInfo!='ORGANIZER')
                                if( true ) 
                                { 
                                    $scope.$watch( 'eventRating.rating.value', function( val, oldVal ) {
                                            if( val != 0 && val != oldVal ) 
                                            {
                                                Events.save( { id : $scope.event.key, registerCtlr : 'review' }, { "rating" : { "value" : val }}, function( ) { 
                                                        //alert and close
                                                        $scope.event = Events.get( { id : $scope.event.key } );
                                                } ); 
                                            }
                                    } ); 
                                }
                        } ); 
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
var checkLogin = function( $location ) { 
    if( $.cookie( "facebook-token" ) ) 
    { 
        return true; 
    } 
    else
    { 
        if( $location) 
			$location.path( "/" );  
        removeCookies();
        //Facebook.login();
        
        return false; 
    } 
};
var removeCookies = function()
{
	
	$.removeCookie( "facebook-uid" ); 
	$.removeCookie( "facebook-token" ); 
	$.removeCookie( "login" );
}
var colorClass = [ "primary", "success", "info", "warning", "danger", "default" ];

function isExternal(url) {
    var match = url.match(/^([^:\/?#]+:)?(?:\/\/([^\/?#]*))?([^?#]+)?(\?[^#]*)?(#.*)?/);
    if (typeof match[1] === "string" && match[1].length > 0 && match[1].toLowerCase() !== location.protocol) return true;
    if (typeof match[2] === "string" && match[2].length > 0 && match[2].replace(new RegExp(":("+{"http:":80,"https:":443}[location.protocol]+")?$"), "") !== location.host) return true;
    return false;
}

