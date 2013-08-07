angular.module("SharedServices", [])
.config(function ($httpProvider) {

    $httpProvider.responseInterceptors.push('myHttpInterceptor');
    var spinnerFunction = function (data, headersGetter) {
            // todo start the spinner here
            //$('#loading').show();
            //TODO - Add FB access token parameter to the FB requests
            return data;
        };
        $httpProvider.defaults.transformRequest.push(spinnerFunction);
    })
// register the interceptor as a service, intercepts ALL angular ajax http calls
.factory('myHttpInterceptor', function ($q, $window) {
    return function (promise) {
        return promise.then(function (response) {
                // do something on success
                // todo hide the spinner
                //$('#loading').hide();
                
                
                return response;

            }, function (response) {
                // do something on error
                // todo hide the spinner
                //$('#loading').hide();
                //console.log('response  error intercepted - '+response.status);
                if(response.status==400)
                {
                    //TODO - reauth with facebook
                }   
                return $q.reject(response);
            });
    };
});

angular
.module('globalErrors', [])
.config(function($provide, $httpProvider, $compileProvider) {
    var elementsList = $();

    var showMessage = function(content, cl, time) {
        $('<alert/>')
        .addClass('message')
        .addClass(cl)
        .hide()
        .fadeIn('fast')
        .delay(time)
        .fadeOut('fast', function() { $(this).remove(); })
        .appendTo(elementsList)
        .text(content);
    };

    $httpProvider.responseInterceptors.push(function($rootScope,$timeout, $q) {
        return function(promise) {
            return promise.then(function(successResponse) {
                if (successResponse.config.method.toUpperCase() != 'GET')
                    $rootScope.showAlert("Saved successfully!","success");
                return successResponse;

            }, function(errorResponse) {
                switch (errorResponse.status) {
                    case 400: $rootScope.showAlert(errorResponse.data.error.message,"error");
                    	    break;
                    case 401:
                    showMessage('Wrong usename or password', 'errorMessage', 20000);
                    break;
                    case 403:
                    showMessage('You don\'t have the right to do this', 'errorMessage', 20000);
                    break;
                    case 404:
                    showMessage('Server internal error: ' + errorResponse.data, 'errorMessage', 20000);
                    break;
                    case 500:
                    showMessage('Server internal error: ' + errorResponse.data, 'errorMessage', 20000);
                    break;
                    default:
                    showMessage('Error ' + errorResponse.status + ': ' + errorResponse.data, 'errorMessage', 20000);
                }
                return $q.reject(errorResponse);
            });
};
});

$compileProvider.directive('appMessages', function() {
    var directiveDefinitionObject = {
        link: function(scope, element, attrs) { elementsList.push($(element)); }
    };
    return directiveDefinitionObject;
});
});

angular.module('FacebookProvider', [])
.factory('Facebook', function ($rootScope) {
    return {
        getLoginStatus:function () {
            FB.getLoginStatus(function (response) {
                $rootScope.$broadcast("fb_statusChange", {'status':response.status});
                

                
            }, true);
        },
        login:function () {
            FB.getLoginStatus(function (response) {
                switch (response.status) {
                    case 'connected':
                        $rootScope.$broadcast('fb_connected', {facebook_id:response.authResponse.userID});
                        break;
                    case 'not_authorized' || 'unknown':
                        // 'not_authorized' || 'unknown': doesn't seem to work
                        FB.login(function (response) {
                            if (response.authResponse) {
                                $rootScope.$broadcast('fb_connected', {
                                    facebook_id:response.authResponse.userID,
                                    userNotAuthorized:true
                                });
                            } else {
                                $rootScope.$broadcast('fb_login_failed');
                            }
                        }, {scope: 'email,user_location'});
                        break;
                    default:
                        FB.login(function (response) {
                            if (response.authResponse) {
                                $rootScope.$broadcast('fb_connected', {facebook_id:response.authResponse.userID});
                                $rootScope.$broadcast('fb_get_login_status');
                            } else {
                                $rootScope.$broadcast('fb_login_failed');
                            }
                        }, {scope: 'email,user_location'});
                        break;
                    }
                }, true);
},
logout:function () {
    FB.logout(function (response) {
        if (response) {
            $rootScope.$broadcast('fb_logout_succeded');
        } else {
            $rootScope.$broadcast('fb_logout_failed');
        }

    });
},
unsubscribe:function () {
    FB.api("/me/permissions", "DELETE", function (response) {
        $rootScope.$broadcast('fb_get_login_status');
    });
},
getFBComments:function(mydiv){
    if(mydiv)
    {
         mydiv.innerHTML =
                  '<div class="fb-comments" href="' +window.location.href + '" data-num-posts="20" data-width="940">'; 
        FB.XFBML.parse(mydiv);   
    }    
    
}
};
});

kexApp = angular.module("kexApp", ["ngResource","ngCookies","google-maps","ui.bootstrap","SharedServices","FacebookProvider","globalErrors"]).
config(function($routeProvider,$httpProvider) {
    $routeProvider.
    when('/', { controller: homeCtrl, templateUrl: 'partials/home.html' }).
    when('/home', { controller: homeCtrl, templateUrl: 'partials/home.html' }).
    when('/me', { controller: meCtrl, templateUrl: 'partials/me.html' }).
    when('/user/:userId', { controller: meCtrl, templateUrl: 'partials/me.html' }).
    when('/mysettings', { controller: meCtrl, templateUrl: 'partials/mysettings.html' }).
    when('/event', { controller: eventsCtrl, templateUrl: 'partials/events.html' }).
    when('/events2', { controller: eventsCtrl, templateUrl: 'partials/eventsAccord.html' }).
    when('/event/add', { controller: addEditEventsCtrl, templateUrl: 'partials/addEditevent.html' }).
    when('/event/:eventId/edit', { controller: addEditEventsCtrl, templateUrl: 'partials/addEditevent.html' }).
    when('/event/:eventId', { controller: addEditEventsCtrl, templateUrl: 'partials/viewEvent.html' }).
    when('/org', { controller: orgCtrl, templateUrl: 'partials/organization.html' }).
    when('/org/:orgId', { controller: orgDetailCtrl, templateUrl: 'partials/organizationDetail.html' }).
    otherwise({ redirectTo: '/' });

    delete $httpProvider.defaults.headers.common['X-Requested-With'];
    //$httpProvider.defaults.headers.common['X-'] = 'X';

})
.filter('newlines', function () {
    return function(text) {
        if(text)
        {
          return text.replace(/\n/g, '<br/>');  
      }    

  }
})
.filter('noHTML', function () {
    return function(text) {
        if(text)
        {
           return text
           .replace(/&/g, '&amp;')
           .replace(/>/g, '&gt;')
           .replace(/</g, '&lt;'); 
       }    

   }
})
.filter('limit10', function () {
    return function(text) {
        if(text)
        {
           if(text>10)
           {
            return 'More than 10';
        } 
        return text;
    }    

}
})
.run(function($rootScope,Me,$location,Facebook){
    if (document.location.hostname === "localhost")
    {
        fbAppId = '276423019167993';
    }   
    else if (document.location.hostname === "karmademo.dyndns.dk")
    {
        fbAppId = '1381630838720301';
    }
    else
    {
        fbAppId = '571265879564450';
    }
    $rootScope.fbScope = "email,user_location";
    $rootScope.location = $location;
    $rootScope.$on("$routeChangeStart", function (event, next, current) {
	$rootScope.alerts = [];
	
    });
    $rootScope.addAlert = function(message) {
        if(!$rootScope.alerts)
        {    
            $rootScope.alerts = [];
        }
        $rootScope.alerts.push({msg: message});
    };
    $rootScope.showAlert = function(message,alertType) {
        $rootScope.alerts = [];
        $rootScope.alerts.push({type:alertType, msg: message});
    };

    $rootScope.closeAlert = function(index) {
        $rootScope.alerts.splice(index, 1);
    };
    window.fbAsyncInit = function () {
        FB.init({
            appId:fbAppId,
            status:true,
            cookie:true,
            xfbml:true
        });
        FB.Event.subscribe('auth.statusChange', function(response) {
            $rootScope.$broadcast("fb_statusChange", {'status': response.status});
        });
        FB.Event.subscribe('auth.authResponseChange', function(response) {
            $rootScope.$broadcast("fb_authResponseChange", {'response': response});
            
        });
    };


    (function (d) {
        var js, id = 'facebook-jssdk', ref = d.getElementsByTagName('script')[0];
        if (d.getElementById(id)) {
            return;
        }
        js = d.createElement('script');
        js.id = id;
        js.async = true;
        js.src = "//connect.facebook.net/en_US/all.js";
        ref.parentNode.insertBefore(js, ref);
    }(document));

    
    



});


/*
All webservice factories go here
*/


kexApp.factory('Events', function($resource) {
    return $resource('/api/event/:id/:registerCtlr/:regType', { id: '@id',registerCtlr:'@registerCtlr',regType:'@regType'}

        );
});    

kexApp.factory('User', function($resource) {
    return $resource('/api/user/:id/:resource/:filter',{ id: '@id',resource:'@resource',filter:'@filter'});
}); 

kexApp.factory('Me', function($resource) {
    return $resource('/api/me/:resource/:filter',{ resource: '@resource',filter:'@filter'});
}); 

kexApp.factory('Org', function($resource) {
    return $resource('/api/org/:id/:resource/:filter',{ id: '@id',resource:'@resource',filter:'@filter'});
}); 



/*
All app directives  go here
*/


kexApp.directive('uiDraggable', function () {
            return {
                restrict:'A',
                link:function (scope, element, attrs) {
                    element.draggable({
                        revert:true
                    });
                }
            };
        });

kexApp.directive('uiDropListener', function () {
    return {
        restrict:'A',
        link:function (scope, eDroppable, attrs) {
            eDroppable.droppable({
                drop:function (event, ui) {
                    var fnDropListener = scope.$eval(attrs.uiDropListener);
                    if (fnDropListener && angular.isFunction(fnDropListener)) {
                        var eDraggable = angular.element(ui.draggable);
                        fnDropListener(eDraggable, eDroppable, event, ui);
                    }
                }
            });
        }
    };
});

kexApp.directive('googleplace', function() {
    return {
        require: 'ngModel',
        
        link: function(scope, element, attrs, model) {
            var options = {
                types: [],
                componentRestrictions: {}
            };
            scope.gPlace = new google.maps.places.Autocomplete(element[0], options);

            google.maps.event.addListener(scope.gPlace, 'place_changed', function() {
                var placeListener = scope.$eval(attrs.placeListener);
                if (placeListener && angular.isFunction(placeListener)) {
                        
                        placeListener(scope.gPlace.getPlace());
                    }
                scope.$apply(function() {
                    
                    model.$setViewValue(element.val());                
                });
            });
        }
    };
});



kexApp.directive('fbgallery', function($compile) {
return {
 
    scope: {
        userid: '@'
    },
  
    restrict: 'A',
    // linking method
    link: function(scope, element, attrs) {
        //$(element).plusGallery(scope.$eval(attrs.fbgallery));
        scope.$watch('userid', function(val){
        	if(val)
        	{
        		$(element).plusGallery(scope.$eval(attrs.fbgallery));	
        	}
        });
    }
  }
});

kexApp.directive("gallery", function() {
  return function(scope, element, attrs) {
    var doStuff = function(element,attrs) {
    	    console.log(attrs.userid);
      $(element).plusGallery(scope.$eval(attrs.fbgallery));
    }
    scope.$watch(attrs.userid, doStuff(element,attrs));
   // scope.$watch(attrs.testTwo, doStuff(element,attrs));

  }
})

kexApp.directive ('unfocus', function() { return {

  restrict: 'A',
  link: function (scope, element, attribs) {
      
    element[0].focus();
      
    element.bind ("blur", function() {
        scope.$apply(attribs["unfocus"]);
        
      });
                                   
  } 
    
} });

/*
All app controllers  go here
*/   

function fbCntrl(Facebook, $scope, $rootScope, $http, $location, Me) {
    $scope.info = {};
    $rootScope.location = $location;
    $rootScope.fbGraphAPIURL = "https://graph.facebook.com";
    $rootScope.$on("fb_statusChange", function (event, args) {
        $rootScope.fb_status = args.status;

        if($rootScope.fb_status==='connected')
        {
           Facebook.getLoginStatus();  
           $rootScope.me = Me.get();
           $rootScope.orgs = Me.get({resource: 'org'});
        } 
        else
        {
           $.removeCookie("facebook-uid");
           $.removeCookie("facebook-token");
           $.removeCookie("login");
        }   

        
});
    $rootScope.$on("fb_authResponseChange", function (event, args) {
        
        
        if(args.response.status==='connected')
        {
            console.log('auth change called');
            $.cookie("facebook-uid",args.response.authResponse.userID);
            $.cookie("facebook-token",args.response.authResponse.accessToken);
            $.cookie("login","facebook"); 
            $rootScope.fbAccessToken = args.response.authResponse.accessToken;
            
        } 
        else
        {
           $.removeCookie("facebook-uid");
           $.removeCookie("facebook-token");
           $.removeCookie("login");
        }   

        $rootScope.$apply();
});
    $rootScope.$on("fb_get_login_status", function () {
        Facebook.getLoginStatus();

    });
    $rootScope.$on("fb_login_failed", function () {

    });
    $rootScope.$on("fb_logout_succeded", function () {

        $rootScope.id = "";
    });
    $rootScope.$on("fb_logout_failed", function () {
        c
    });

    $rootScope.$on("fb_connected", function (event, args) {
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

         function authenticateViaFacebook(parameters) {
            //posts some user data to a page that will check them against some db
            
        }

        if (args.userNotAuthorized === true) {
            //if the user has not authorized the app, we must write his credentials in our database
            //console.log("user is connected to facebook but has not authorized our app");
            FB.api(
            {
                method:'fql.multiquery',
                queries:{
                    'q1':'SELECT uid, first_name, last_name FROM user WHERE uid = ' + args.facebook_id,
                    'q2':'SELECT url FROM profile_pic WHERE width=800 AND height=800 AND id = ' + args.facebook_id
                }
            },
            function (data) {
                    //let's built the data to send to php in order to create our new user
                    params = {
                        facebook_id:data[0]['fql_result_set'][0].uid,
                        first_name:data[0]['fql_result_set'][0].first_name,
                        last_name:data[0]['fql_result_set'][0].last_name,
                        picture:data[1]['fql_result_set'][0].url
                    }
                    authenticateViaFacebook(params);
                });
        }
        else {
            //console.log("user is connected to facebook and has authorized our app");
            //the parameter needed in that case is just the users facebook id
            params = {'facebook_id':args.facebook_id};
            authenticateViaFacebook(params);
            $rootScope.facebook_id = args.facebook_id;

        }

    });


$rootScope.updateSession = function () {
        //reads the session variables if exist from php
        
    };

    $rootScope.updateSession();


    // button functions
    $scope.getLoginStatus = function () {
        Facebook.getLoginStatus();
    };

    $scope.login = function () {
        Facebook.login();
    };

    $rootScope.logout = function () {
        Facebook.logout();
        $rootScope.session = {};
        $rootScope.me={};
        $rootScope.location.path("/");
        
        $.removeCookie("facebook-uid");
        $.removeCookie("facebook-token");
        $.removeCookie("login");

        //make a call to a php page that will erase the session data
        
    };

    $scope.unsubscribe = function () {
        Facebook.unsubscribe();
    }

    $scope.getInfo = function () {
        FB.api('/' + $rootScope.facebook_id, function (response) {
            //console.log('Good to see you, ' + response.name + '.');


        });
        $rootScope.info = $rootScope.session;

    };
}

var homeCtrl = function($scope, $location) {
    if(checkLogin($location))
    {
        if($location.$$url=="/")
        {
            $location.path("/event");   
        }    
        
    }    

};

var meCtrl = function($scope, $location, User,Me,$rootScope, $routeParams) {
    if(!checkLogin($location))
    {
        return;
    } 
    $scope.newMail = {email:null,primary:null};
    $scope.load = function($location,$routeParams){

        if($location.$$url=="/me"||$location.$$url=="/mysettings")
        {
            $scope.who = 'My';
            $scope.me = Me.get(function(){
                if(!$scope.me.about)
                {
                    $scope.me.about='Click to add about yourself!';
                } 
                $scope.getOtherData($scope.me.key); 
                $rootScope.me = $scope.me;

            });
            
            
        }
        else
        {
            $scope.me = User.get({id:$routeParams.userId},function(){
                $scope.who = $scope.me.firstName+"'s";
                if(!$scope.me.about)
                {
                    $scope.me.about='No description added yet!';
                } 

            });
            $scope.getOtherData($routeParams.userId);
            
        }
        

    };
    
    $scope.getOtherData = function(key){
        $scope.events = User.get({id :key, resource: 'event'});
        $scope.pastEvents = User.get({type: 'PAST'},{id:key,resource: 'event'});
        $scope.orgs = User.get({id :key, resource: 'org'});
        $rootScope.orgs = $scope.orgs;

    };
    $scope.save = function(){
        Me.save($scope.me);
    };
    $scope.enableEdit = function() {
         if($scope.me.permission==="ALL")
         {
            $scope.edit = true;   
         }   
         
    };
    $scope.disableEdit = function() { 
        $scope.edit = false;
        User.save({id:$scope.me.key},$scope.me);
    };
    
    $scope.addEmail = function(){
    	//TO-DO check if the new one is marked primary and unmark the current primary one
    	$scope.me.registeredEmails.push($scope.newMail);
    	$scope.save();
    };
    
    $scope.removeEmail = function(){
    	console.log(" remove "+this.$index);   
    	$scope.me.registeredEmails.splice(this.$index,1);
    	$scope.save();
    };
    $scope.mailPrimaryIndex = { index: 0 };
    $scope.load($location,$routeParams);



    




};

var orgDetailCtrl = function($scope,$location,$routeParams,$rootScope,$http,Org,Events)
{
    if(!checkLogin($location))
    {
        return;
    };
    console.log("check");
    $scope.parser = document.createElement('a');
    $scope.org = Org.get({id:$routeParams.orgId},function(){
        $scope.parser.href = $scope.org.page.url;
        $http({method: 'GET', url: $rootScope.fbGraphAPIURL+""+$scope.parser.pathname}).success(function(data) {
                    
                    $scope.fbPage = data;
                    
                    
        });
        $scope.pastEvents = Events.get({type:"PAST",keywords:"org:"+$scope.org.searchTokenSuffix});
        $scope.upcomingEvents = Events.get({type:"UPCOMING",keywords:"org:"+$scope.org.searchTokenSuffix});
        

    });
    $scope.orgOwners = Org.get({role:"ADMIN"},{id:$routeParams.orgId, resource : "member"});
    $scope.orgOrgnaizers = Org.get({role:"ADMIN"},{id:$routeParams.orgId, resource : "member"});
    
    $scope.isMessageOpen = false;
    $scope.showMessage = function(){
    	    $scope.isMessageOpen = true;
    };
    $scope.cancelMessage = function(){
    	    $scope.isMessageOpen = false;
    };
    $scope.sendMessage = function(){
    	    $scope.isMessageOpen = false;
    };


     

}

var orgCtrl = function($scope,$location,$routeParams,Org){
    $scope.query = "";
    $scope.newOrg = {page:{url:null,urlProvider:"FACEBOOK"}};


    if(!checkLogin($location))
    {
        return;
    };

    $scope.refresh = function(){
        
            
        
            $scope.orgs = Org.get({name_prefix : $scope.query});
         
         
     };

     $scope.save = function(){
        if($scope.newOrg.page.url)
        {
            Org.save($scope.newOrg,function(){
                $scope.refresh();
                $scope.newOrg = {page:{url:null,urlProvider:"FACEBOOK"}};
            });
        }
        $scope.close(); 
     };

     $scope.join = function(){
        Org.save($scope.newOrg,function(){
                $scope.refresh();
                $scope.newOrg = {page:{url:null,urlProvider:"FACEBOOK"}};
            });
     };

     $scope.close = function(){
        $scope.newOrg = {page:{url:null,urlProvider:"FACEBOOK"}};
        $scope.shouldBeOpen = false;
    };

     $scope.open = function () {
        $scope.shouldBeOpen = true;
      };

       $scope.opts = {
        backdropFade: true,
        dialogFade:true
      };


   
    $scope.$watch('query',function(){
        $scope.refresh();
    });
    
    $scope.refresh();
       
    


};

var eventsCtrl = function ($scope, $location, Events,$rootScope) {
	if(!checkLogin($location))
    {
        return;
    } 
    $scope.modelOpen = false;
    
    $scope.reset = function() {


        $scope.events = Events.get({keywords: $scope.query});
        $scope.currentDate = new Date(1001, 01, 01, 01, 01, 01, 0);
        

    };
    $scope.query = "";
    $scope.$watch('query',function(){
        $scope.reset();
    });
    $scope.register = function(type){
        var eventId = this.modelEvent.key;
        var thisEvent = this.event;
        Events.save({ id: eventId , registerCtlr :'participants',regType:type}, function (type) {
                //alert and close
                $rootScope.showAlert("Registration successful!","success");
                $scope.modelEvent.registrationInfo = type;                
                $scope.modelEvent.numAttending++;
                /*
                thisEvent.cachedParticipantImages.push({"participant": {
                    "key": $rootScope.me.key
                    },
                    "imageUrl": $rootScope.me.profileImage.url,
                    "imageUrlProvider": "FACEBOOK"}
                    );
                */
                //TODO - push to cached event participants
                //$scope.$apply();

            });

    };
    
    
    $scope.createHeader = function(dateParam, eventobj) {

        dateVal = new Date(dateParam);
        currentDate = new Date($scope.currentDate);
        showHeader = (''+dateVal.getDate()+dateVal.getMonth()+dateVal.getFullYear()!=''+currentDate.getDate()+currentDate.getMonth()+currentDate.getFullYear()); 
       
        $scope.currentDate = new Date(dateVal);
        
        return showHeader;
    }

    $scope.reset();

    $scope.delete = function () {
        var eventId = this.event.key;

        Events.delete({ id: eventId }, function () {
            $("#event_" + eventId).fadeOut();
        });

    };
    $scope.modelEvent = {};

    $scope.toggleEvent = function(){



        if($('#'+this.event.key+'_detail').is(":visible"))
        {
            $('.event-detail').hide();
        }
        else
        {
         $('.event-detail').hide();
         $scope.modelEvent = Events.get({ id: this.event.key , registerCtlr:'expanded_search_view'});
         $('#'+this.event.key+'_detail').show();
     }    

 };



};






var addEditEventsCtrl =  function ($scope, $rootScope,$routeParams, $filter,$location,Events,$http,Facebook) {
    if(!checkLogin($location))
    {
        return;
    } 
    angular.extend($scope, {

        /** the initial center of the map */
        center: {
            latitude: 37,
            longitude: -122
        },

        /** the initial zoom level of the map */
        zoom: 4,

        /** list of markers to put in the map */
        markers: [ {}],

        // These 2 properties will be set when clicking on the map
        clicked: null,  
        clicked: null,

        eventStartDate : null,
        eventStartTime : null,
        eventEndDate : null,
        eventEndTime : null,

        
    });
    $scope.suitableForList = [
        { name: 'Kids', key:'KIDS',   checked: false },
        { name: 'Seniors', key:'AGE_55_PLUS',   checked: false },
        { name: 'Groups', key:'GROUPS',     checked: false },
        { name: 'Teens', key:'TEENS',  checked: false }
      ];

    $scope.unregister = function()
    {
        Events.delete({ id: $scope.event.key , registerCtlr :'participants'}, function () {
            $scope.refreshEvent();

        });
    }
    $scope.register = function(type){
        var eventId = $scope.event.key;
        Events.save({ id: eventId , registerCtlr :'participants',regType : type}, function (req,$rootScope) {
                //alert and close
                $scope.refreshEvent();
                //$scope.addAlert("Registration successful!");
                $scope.event.registrationInfo = type;                
                $scope.event.numAttending++;
                console.log(type);
                if(type==='REGISTERED')
                {
                    $scope.eventRegistered.data.push(
                        {
                            "user":{
                                "key": $rootScope.me.key,
                                 "profileImage": {
                                    "url": $rootScope.me.profileImage.url
                                 },
                                 "firstName":$rootScope.me.firstName,
                                 "lastName": $rootScope.me.lastName
                            }
                        }
                        );
                }
                else if(type==='WAIT_LISTED')
                {
                    $scope.eventWaitListed.data.push(
                        {
                            "user":{
                                "key": $rootScope.me.key,
                                 "profileImage": {
                                    "url": $rootScope.me.profileImage.url
                                 },
                                 "firstName":$rootScope.me.firstName,
                                 "lastName": $rootScope.me.lastName
                            }
                        }

                        );
                }  
                
               
                
                

            });

    };

    $scope.dropListener = function (eDraggable, eDroppable) {

                var isDropForbidden = function (aTarget, item) {
                    if (aTarget.some(function (i) {
                        return i.key == item.key;
                    })) {
                        return {reason:'target already contains "' + item.key + '"'};
                    } else {
                        return false;
                    }
                };

                var onDropRejected = function (error) {
                    alert('Operation not permitted: ' + error.reason);
                };

                var onDropComplete = function (eSrc, item, index) {
                    //console.log('moved "' + item.key + ' from ' + eSrc.data('model') + '[' + index + ']' + ' to ' + eDroppable.data('model'));
                };

                var eSrc = eDraggable.parent();
                var sSrc = eSrc.data('model');
                var sTarget = eDroppable.data('model');

                if (sSrc != sTarget) {
                    $scope.$apply(function () {
                        var index = eDraggable.data('index');
                        var aSrc = $scope.$eval(sSrc);
                        var aTarget = $scope.$eval(sTarget);
                        var item = aSrc[index];
                        var error = isDropForbidden(aTarget, item);
                        if (error) {
                            onDropRejected(error);
                        } else {
                            aTarget.push(item);
                            aSrc.splice(index, 1);
                            onDropComplete(eSrc, item, index);
                        }
                    });
                }
                

            };
    
    $scope.refreshMap = function(){

        if($scope.event&&$scope.event.location.address.street)
        {    
            geocoder.geocode({ 'address': $scope.event.location.address.street+','+$scope.event.location.address.city+','+$scope.event.location.address.state+','+$scope.event.location.address.country }, function (results, status) {
                if (status == google.maps.GeocoderStatus.OK) {
                    $scope.center.latitude = results[0].geometry.location.lat();
                    $scope.center.longitude = results[0].geometry.location.lng();

                    $scope.setMarker($scope.center.latitude,$scope.center.longitude);
                    $scope.zoom = 15;
                    $scope.$apply();


                }

            })}
        };



        $scope.addMarker = function (markerLat,markerLng) {
            $scope.markers.push({
                latitude: parseFloat(markerLat),
                longitude: parseFloat(markerLng)
            });
            
            
        };

        $scope.setMarker = function (markerLat,markerLng) {
            $scope.markers = [{}];
            $scope.markers.push({
                latitude: parseFloat(markerLat),
                longitude: parseFloat(markerLng)
            });
            
            
        };    
        
        $scope.findMe = function () {

            if ($scope.geolocationAvailable) {

                navigator.geolocation.getCurrentPosition(function (position) {

                    $scope.center = {
                        latitude: position.coords.latitude,
                        longitude: position.coords.longitude
                    };

                    $scope.zoom = 15;

                    $scope.$apply();
                }, function () {

                });
            }   
        };            


        $scope.getMore = function(type)
        {
            if($scope.eventRegistered.paging.next==null)
            {
                return;
            }    
            if(type==='REGISTERED')
            {
               
                $http({method: 'GET', url: $scope.eventRegistered.paging.next}).success(function(data) {
                    
                    for(var i=0;i<data.data.length;i++)
                    {
                       $scope.eventRegistered.data.push(data.data[i]); 
                    }    
                                  
                    $scope.eventRegistered.paging.next = data.paging?data.paging.next:null;
                    
                    
                });
                
            }
            else if(type==='ORGANIZER')
            {
               
                $http({method: 'GET', url: $scope.eventOrganizers.paging.next}).success(function(data) {
                    
                    for(var i=0;i<data.data.length;i++)
                    {
                       $scope.eventOrganizers.data.push(data.data[i]); 
                    }    
                                  
                    $scope.eventOrganizers.paging.next = data.paging?data.paging.next:null;
                    
                    
                });
                
            }
            else if(type==='WAIT_LISTED')
            {
               
                $http({method: 'GET', url: $scope.eventWaitListed.paging.next}).success(function(data) {
                    
                    for(var i=0;i<data.data.length;i++)
                    {
                       $scope.eventWaitListed.data.push(data.data[i]); 
                    }    
                                  
                    $scope.eventWaitListed.paging.next = data.paging?data.paging.next:null;
                    
                    
                });
                
            }    
        };


        $scope.save = function () {
            $scope.event.startTime = $scope.parseDateReg($('#dtst').val()+' '+$('#tmst').val());
            $scope.event.endTime = $scope.parseDateReg($('#dtend').val()+' '+$('#tmend').val());
            $scope.event.suitableForTypes=[];
            for(var i=0;i<$scope.suitableForList.length;i++)
            {

                if($scope.suitableForList[i].checked===true)
                {
                    $scope.event.suitableForTypes.push($scope.suitableForList[i].key);
                }    
            }    
            if($location.$$url=="/event/add")
            {
                Events.save($scope.event, function(data, status, headers, config) {
                		
                    $location.path('/event');
                });
            }
            else
            {    
              Events.save({id: $scope.event.key}, $scope.event, function (data, status, headers, config) {
              		      c

              });
          }
      };

      $scope.refreshEvent = function(){

        $scope.event = Events.get({ id: $routeParams.eventId} ,function() {
                //$("#location-title").val(''+$scope.event.location.title);
                //$scope.refreshMap();
                //TDEBT - remove DOM references
                
                $('#dtst').val($filter('date')($scope.event.startTime, 'MM/dd/yyyy'));
                $('#tmst').val($filter('date')($scope.event.startTime, 'h:mma'));
                $('#dtend').val($filter('date')($scope.event.endTime, 'MM/dd/yyyy'));
                $('#tmend').val($filter('date')($scope.event.endTime, 'h:mma'));
                

                //$scope.eventStartTime : null,
                //$scope.eventEndDate : null,
                //$scope.eventEndTime : null,

                angular.forEach($scope.suitableForList, function(client){
                    angular.forEach($scope.event.suitableForTypes , function(server)
                    {
                        
                        if(server===client.key)
                        {
                            client.checked = true;
                        }    
                    });
                });

                if($scope.event.location.address.geoPt!=null)
                {
                    
                    $scope.center = {
                        latitude: $scope.event.location.address.geoPt.latitude,
                        longitude: $scope.event.location.address.geoPt.longitude
                    };
                    
                    $scope.setMarker($scope.center.latitude,$scope.center.longitude);

                    $scope.zoom = 15;

                }

                $scope.eventOrganizers = Events.get({ id: $routeParams.eventId, registerCtlr :'participants',regType:'ORGANIZER'},function(){
                    $scope.eventOrganizers.data.push = function (){
                    Events.save({user:arguments[0].key},{ id: $routeParams.eventId, registerCtlr :'participants',regType:'ORGANIZER'});
                    return Array.prototype.push.apply(this,arguments);
                }

                });
                $scope.eventRegistered = Events.get({ id: $routeParams.eventId, registerCtlr :'participants',regType:'REGISTERED'},function(){
                    $scope.eventRegistered.data.push = function (){
                    Events.save({user:arguments[0].key},{ id: $routeParams.eventId, registerCtlr :'participants',regType:'REGISTERED'});
                    return Array.prototype.push.apply(this,arguments);
                }

                });
                $scope.eventWaitListed = Events.get({ id: $routeParams.eventId, registerCtlr :'participants',regType:'WAIT_LISTED'},function(){
                    $scope.eventWaitListed.data.push = function (){
                    Events.save({user:arguments[0].key},{ id: $routeParams.eventId, registerCtlr :'participants',regType:'WAIT_LISTED'});
                    return Array.prototype.push.apply(this,arguments);
                }

                });
                
                
                var mydiv = document.getElementById('myCommentsDiv'); 
                
                Facebook.getFBComments(mydiv);
                if($scope.event.status=='COMPLETED')
                {
                    $scope.eventRating = Events.get({ id: $routeParams.eventId, registerCtlr :'review'}, function(){

                        if(!$scope.eventRating||!$scope.eventRating.rating)
                        {
                            $scope.eventRating = {"rating":{"value":0}};
                        }
                        //TODO - Make sure that the event is not called on-load
                        //if($scope.event.registrationInfo!='ORGANIZER')
                        if(true)
                        {
                            $scope.$watch('eventRating.rating.value', function(val){

                                if(val!=0)
                                {

                                    Events.save({ id: $scope.event.key , registerCtlr :'review'}, {"rating":{"value":val}},function () {
                                        //alert and close
                                        $scope.event = Events.get({ id: $scope.event.key });


                                        

                                    }); 
                                }                          

                            });
                        }    

                    });
} 


}, function(response) {
            //404 or bad
            
            if(response.status === 404) {
            }});
}

$scope.parseDateReg = function(input) {
        
        var dateReg = 
    /(\d{1,2})\/(\d{1,2})\/(\d{4})\s*(\d{1,2}):(\d{2})*(am|pm|AM|PM)/;
        var year, month, day, hour, minute, second,
            result = dateReg.exec(input);
        if (result) {
            year = +result[3];
            month = +result[1];
            day = +result[2];
            hour = +result[4];
            minute = +result[5];
            second = 0;
            if ((result[6] === 'pm' || result[6] === 'PM') && hour !== 12) {
                hour += 12;
            }       
        }
       
        return new Date(year, month-1, day, hour, minute, second);
    }
    $scope.placeChanged = function(place){
        
        $scope.event.location.title = place.name;

        $scope.event.location.address.street = '';
        for (var i = 0; i < place.address_components.length; i++) {

            if(place.address_components[i].types[0]=='locality')
            {
                $scope.event.location.address.city = place.address_components[i].long_name;
            } 
            else if(place.address_components[i].types[0]=='country')
            {
                $scope.event.location.address.country = place.address_components[i].long_name;
            } 
            else if(place.address_components[i].types[0]=='postal_code')
            {
                $scope.event.location.address.zip = place.address_components[i].long_name;
            } 
            else if(place.address_components[i].types[0]=='administrative_area_level_1')
            {
                $scope.event.location.address.state = place.address_components[i].long_name;
            } 
            else if(place.address_components[i].types[0]=='street_number')
            {
                $scope.event.location.address.street = place.address_components[i].long_name+' '+$scope.event.location.address.street;
            }
            else if(place.address_components[i].types[0]=='route')
            {
                $scope.event.location.address.street = $scope.event.location.address.street+' '+place.address_components[i].long_name;
            }


            
        }    



        $scope.center = {
            latitude: place.geometry.location.lat(),
            longitude: place.geometry.location.lng()
        };
        $scope.setMarker($scope.center.latitude,$scope.center.longitude);

        $scope.zoom = 15;

        $scope.event.location.address.geoPt = {latitude: $scope.center.latitude, longitude : $scope.center.longitude};
        $scope.$apply();

    };

  
   


if($location.$$url=="/event/add")
{
    $scope.findMe();
    $scope.event = {"location":{"title":null,"description":null,"address":{"street":null,"city":null,"state":null,"country":null,"zip":null,"geoPt":null}}};
    
    
    

}

else
{    
    $scope.refreshEvent();
}

 

    
};



var checkLogin = function($location){
	if($.cookie("facebook-token"))
	{
		return true;
	}
	else
	{
		$location.path("/");
		$.removeCookie("facebook-uid");
		$.removeCookie("facebook-token");
		$.removeCookie("login");
        //Facebook.login();

        return false;
    }
};



function getImage(id,size) {
    if(size=='small')
    {
        return "//graph.facebook.com/" + id + "/picture?access_token=" +$.cookie("facebook-token")+"&width=25&height=25";
    }  
    return "//graph.facebook.com/" + id + "/picture?access_token=" +$.cookie("facebook-token")+"type=square";  

};


