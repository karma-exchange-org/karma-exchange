
var kexApp = angular.module("kexApp", ["ngResource","ngCookies","google-maps","ui.bootstrap"]).
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
            when('/thanks', { controller: meCtrl, templateUrl: 'partials/thanks.html' }).
            otherwise({ redirectTo: '/' });

        $httpProvider.defaults.headers.common['X-'] = 'X';

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
    .run(function($rootScope,Me,$location){
        $rootScope.me = Me.get();
        

        
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



/*
All app directives  go here
*/





/*
All app controllers  go here
*/   

     

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

    $scope.load = function($location,$routeParams){
        if($location.$$url=="/me"||$location.$$url=="/mysettings")
        {
            $scope.who = 'My';
            $scope.me = Me.get();
            $rootScope.me = $scope.me;
            $scope.events = Me.get({resource: 'event'});
            $scope.pastEvents = Me.get({type: 'PAST'},{resource: 'event'});
        }
        else
        {
            $scope.me = User.get({id:$routeParams.userId},function(){
                $scope.who = $scope.me.firstName+"'s";
            });
            $scope.events = User.get({id :$routeParams.userId, resource: 'event'});
            $scope.pastEvents = User.get({type: 'PAST'},{id:$routeParams.userId,resource: 'event'});
        }

    };
    $scope.save = function(){
        Me.save($scope.me);
    };

    $scope.load($location,$routeParams);
   

      
    


      

};

var eventsCtrl = function ($scope, $location, Events) {
	if(!checkLogin($location))
    {
        return;
    } 
    $scope.modelOpen = false;
    
	$scope.reset = function() {


        $scope.events = Events.get({q: $scope.query});
        
        

    };
    $scope.register = function(){
        var eventId = this.modelEvent.key;
        Events.save({ id: eventId , registerCtlr :'participants',regType:'REGISTERED'}, function () {
                //alert and close
                $scope.addAlert("Registration successful!");
                $scope.modelEvent.registrationInfo = 'REGISTERED';                
                $scope.modelEvent.numAttending++;
                //TODO - push to cached event participants
                $scope.$apply();

            });
      
    };
    $scope.addAlert = function(message) {
        if(!$scope.alerts)
        {    
            $scope.alerts = [];
        }
        $scope.alerts.push({msg: message});
      };

  $scope.closeAlert = function(index) {
        $scope.alerts.splice(index, 1);
      };
    $scope.currentDate = new Date(2001, 01, 01, 01, 01, 01, 0);
    $scope.createHeader = function(dateParam) {
        
        dateVal = new Date(dateParam);
        currentDate = new Date($scope.currentDate);
       
        showHeader = (dateVal.toDateString()!=currentDate.toDateString()); 

        $scope.currentDate = new Date(dateVal);
        
        return !showHeader;
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





var addEditEventsCtrl =  function ($scope, $routeParams, $location,Events) {
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
        
    });

    $scope.register = function(){
        var eventId = $scope.event.key;
        Events.save({ id: eventId , registerCtlr :'participants',regType:'REGISTERED'}, function () {
                //alert and close
                $scope.refreshEvent();
                

            });
      
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

    

	

	$scope.save = function () {
        if($location.$$url=="/addevent")
        {
            Events.save($scope.event, function() {
            $location.path('/event');
        });
        }
        else
        {    
    		Events.save({id: $scope.event.key}, $scope.event, function () {
    	        
    	    });
        }
	};

    $scope.refreshEvent = function(){

        $scope.event = Events.get({ id: $routeParams.eventId} ,function() {
                //$("#location-title").val(''+$scope.event.location.title);
                //$scope.refreshMap();
                  
                $scope.eventOrganizers = Events.get({ id: $routeParams.eventId, registerCtlr :'participants',regType:'ORGANIZER'});
                $scope.eventRegistered = Events.get({ id: $routeParams.eventId, registerCtlr :'participants',regType:'REGISTERED'});
                $scope.eventWaitListed = Events.get({ id: $routeParams.eventId, registerCtlr :'participants',regType:'WAIT_LISTED'});
                
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

    if($location.$$url=="/event/add")
    {
        $scope.findMe();
        $scope.event = {"location":{"title":null,"description":null,"address":{"street":null,"city":null,"state":null,"country":null,"zip":null,"geoPt":null}}};
        $scope.autocomplete = new google.maps.places.Autocomplete(document.getElementById("locationTitle"));
        google.maps.event.addDomListener(document.getElementById("locationTitle"), 'keydown', function(e) { 
            if (e.keyCode == 13) 
            { 
                    if (e.preventDefault) 
                    { 
                            e.preventDefault(); 
                    } 
                    else 
                    { 

                            e.cancelBubble = true; 
                            e.returnValue = false; 
                    } 
            } 
        }); 
        google.maps.event.addListener($scope.autocomplete, 'place_changed', function(e) {
            
            
            var marker;

            var place = $scope.autocomplete.getPlace();
           
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
            $scope.setMarker($scope.center.latitude,$scope.center.longitude)
            $scope.zoom = 15;
                    
            $scope.$apply();
            
          });
        

        $('#startTimePicker')
            .datetimepicker()
            .on('changeDate', function(ev){
                

                
                $scope.event.startTime = jQuery(ev.target).data('datetimepicker').getDate();
                $scope.$apply();
                $('#endTimePicker').datetimepicker('setStartDate', $scope.event.startTime);
                
            });
        $('#endTimePicker')
            .datetimepicker()
            .on('changeDate', function(ev){
                
                $scope.event.endTime = jQuery(ev.target).data('datetimepicker').getDate();
                $scope.$apply();
                
            });
    }
    else
    {    
        $scope.refreshEvent();
    }
    //TDEBT - (hbalijepalli)

    

    
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

		setAuthCookies();
        return false;
	}
};

var logOut = function($location){
    $.removeCookie("facebook-uid");
    $.removeCookie("facebook-token");
    $.removeCookie("login");
    FB.logout();
    $location.path("/");
    
};

function getImage(id,size) {
            if(size=='small')
            {
                return "http://graph.facebook.com/" + id + "/picture?access_token=" +$.cookie("facebook-token")+"&width=25&height=25";
            }  
            return "http://graph.facebook.com/" + id + "/picture?access_token=" +$.cookie("facebook-token")+"type=square";  
           
     };


