
var kexApp = angular.module("kexApp", ["ngResource","ngCookies","google-maps","ui.bootstrap"]).
    config(function($routeProvider,$httpProvider) {
        $routeProvider.
            when('/', { controller: homeCtrl, templateUrl: 'partials/home.html' }).
            when('/home', { controller: homeCtrl, templateUrl: 'partials/home.html' }).
            when('/me', { controller: meCtrl, templateUrl: 'partials/me.html' }).
            when('/events', { controller: eventsCtrl, templateUrl: 'partials/events.html' }).
            when('/addevent', { controller: addEditEventsCtrl, templateUrl: 'partials/addEditevent.html' }).
            when('/editevent/:eventId', { controller: addEditEventsCtrl, templateUrl: 'partials/addEditevent.html' }).
            when('/viewevent/:eventId', { controller: addEditEventsCtrl, templateUrl: 'partials/addEditevent.html' }).
            otherwise({ redirectTo: '/' });

        $httpProvider.defaults.headers.common['X-'] = 'X';

    })
    .run(function($rootScope,Me){
        $rootScope.me = Me.get();
        
    });




/*
All webservice factories go here
*/

kexApp.factory('Events', function($resource) {
    return $resource('/api/event/:id/:registerCtlr/:regType', { id: '@id',registerCtlr:'@registerCtlr',regType:'@regType'}
         
    );
});    



kexApp.factory('Me', function($resource) {
    return $resource('/api/me');
}); 

/*
All app directives  go here
*/

kexApp.directive('googleplace', function() {
    return {
        require: 'ngModel',
        link: function($scope, element, attrs, model) {
            var options = {
                types: [],
                componentRestrictions: {}
            };
            $scope.gPlace = new google.maps.places.Autocomplete(element[0], options);

            google.maps.event.addListener($scope.gPlace, 'place_changed', function() {
                $scope.$apply(function() {
                    model.$setViewValue(element.val());
                                   
                });
            });
        }
    };
});

var geocoder = new google.maps.Geocoder();

/*
All app controllers  go here
*/   

     

var homeCtrl = function($scope, $location) {
    if(checkLogin($location))
    {
        if($location.$$url=="/")
        {
            $location.path("/events");   
        }    
        
    }    

};

var meCtrl = function($scope, $location, Me,$rootScope) {
    if(!checkLogin($location))
    {
        return;
    } 

    $scope.load = function(){
        $scope.me = Me.get();
        $rootScope.me = $scope.me;

    };
    $scope.save = function(){
        Me.save($scope.me);
    };

    $scope.load();


      

};

var eventsCtrl = function ($scope, $location, Events) {
	if(!checkLogin($location))
    {
        return;
    } 
    $scope.modelOpen = false;
    
	$scope.reset = function() {


    $scope.events = Events.get({q: $scope.query});
    $scope.register = function(){
        var eventId = this.modelEvent.key;
        Events.save({ id: eventId , registerCtlr :'participants',regType:'REGISTERED'}, function () {
                //alert and close
                $scope.addAlert("Registration successful!");
                $scope.closeEvent();

            });
      
    };

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
    $scope.openEvent = function(){

        $scope.modelEvent = Events.get({ id: this.event.key });
        $scope.modelOpen = true;

    };

    $scope.closeEvent = function () {
    
    $scope.modelOpen = false;
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
            $location.path('/events');
        });
        }
        else
        {    
    		Events.save({id: $scope.event.key}, $scope.event, function () {
    	        $location.path('/events');
    	    });
        }
	};

    if($location.$$url=="/addevent")
    {
        $scope.findMe();
        $scope.event = {"location":{"title":null,"description":null,"address":{"street":null,"city":null,"state":null,"country":null,"zip":null,"geoPt":null}}};

    }
    else
    {    
        $scope.event = Events.get({ id: $routeParams.eventId } ,function() {
                $("#location-title").val(''+$scope.event.location.title);
                $scope.refreshMap();

            }, function(response) {
            //404 or bad
            
            if(response.status === 404) {
        }});
    }
    //TDEBT - (hbalijepalli)
    $scope.autocomplete = new google.maps.places.Autocomplete(document.getElementById("location-title"));
    google.maps.event.addListener($scope.autocomplete, 'place_changed', function(event) {
        
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


            //Do something
        }    

            
       
        $scope.center = {
                    latitude: place.geometry.location.kb,
                    longitude: place.geometry.location.lb
                };
        $scope.setMarker(place.geometry.location.kb,place.geometry.location.lb)
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


