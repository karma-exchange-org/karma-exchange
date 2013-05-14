
var kexApp = angular.module("kexApp", ["ngResource","ngCookies","google-maps"]).
    config(function($routeProvider,$httpProvider) {
        $routeProvider.
            when('/', { controller: homeCtrl, templateUrl: 'partials/home.html' }).
            when('/events', { controller: eventsCtrl, templateUrl: 'partials/events.html' }).
            when('/addevent', { controller: addEditEventsCtrl, templateUrl: 'partials/addEditevent.html' }).
            when('/editevent/:itemId', { controller: addEditEventsCtrl, templateUrl: 'partials/addEditevent.html' }).
            otherwise({ redirectTo: '/' });

        $httpProvider.defaults.headers.common['X-'] = 'X';

    });


var homeCtrl = function($scope, $location) {
	checkLogin($location);
};




kexApp.factory('Events', function($resource) {
    return $resource('/api/event/:id/:registerCtlr', { id: '@id',registerCtlr:'@registerCtlr' }, 
        { update: { method: 'POST' },
         fetch:  {method:'GET', isArray:false}
         
          
    });



});

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
        



var eventsCtrl = function ($scope, $location, Events) {
	checkLogin($location);
	$scope.reset = function() {


        $scope.items = Events.fetch({q: $scope.query});
        $scope.register = function(){
            var itemId = this.item.key;
            if($("#event_register_" + itemId).hasClass('btn-success'))
            {    
                Events.update({ id: itemId , registerCtlr :'registered'}, function () {
                    $("#event_register_" + itemId).removeClass('btn-success').addClass('btn-danger').html('Un-register');
                });
            }
            else
            {
                Events.delete({ id: itemId , registerCtlr :'registered'}, function () {
                    $("#event_register_" + itemId).removeClass('btn-danger').addClass('btn-success').html('Register');
                });

            }    
        };

    };

    $scope.reset();

    $scope.delete = function () {
        var itemId = this.item.key;

        Events.delete({ id: itemId }, function () {
            $("#event_" + itemId).fadeOut();
        });

    };

};



var addEditEventsCtrl =  function ($scope, $routeParams, $location,Events) {

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
                
                if($scope.item&&$scope.item.location.address.street)
                {    
                    geocoder.geocode({ 'address': $scope.item.location.address.street+','+$scope.item.location.address.city+','+$scope.item.location.address.state+','+$scope.item.location.address.country }, function (results, status) {
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
            Events.save($scope.item, function() {
            $location.path('/events');
        });
        }
        else
        {    
    		Events.update({id: $scope.item.key}, $scope.item, function () {
    	        $location.path('/events');
    	    });
        }
	};

    if($location.$$url=="/addevent")
    {
        $scope.findMe();
        $scope.item = {"location":{"title":null,"description":null,"address":{"street":null,"city":null,"state":null,"country":null,"zip":null,"geoPt":null}}};

    }
    else
    {    
        $scope.item = Events.get({ id: $routeParams.itemId } ,function() {
                $("#location-title").val(''+$scope.item.location.title);
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
        $scope.item.location.title = place.name;
        
        $scope.item.location.address.street = '';
        for (var i = 0; i < place.address_components.length; i++) {
            
            if(place.address_components[i].types[0]=='locality')
            {
                $scope.item.location.address.city = place.address_components[i].long_name;
            } 
            else if(place.address_components[i].types[0]=='country')
            {
                $scope.item.location.address.country = place.address_components[i].long_name;
            } 
            else if(place.address_components[i].types[0]=='postal_code')
            {
                $scope.item.location.address.zip = place.address_components[i].long_name;
            } 
            else if(place.address_components[i].types[0]=='administrative_area_level_1')
            {
                $scope.item.location.address.state = place.address_components[i].long_name;
            } 
            else if(place.address_components[i].types[0]=='street_number')
            {
                $scope.item.location.address.street = place.address_components[i].long_name+' '+$scope.item.location.address.street;
            }
            else if(place.address_components[i].types[0]=='route')
            {
                $scope.item.location.address.street = $scope.item.location.address.street+' '+place.address_components[i].long_name;
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
            

            
            $scope.item.startTime = jQuery(ev.target).data('datetimepicker').getDate();
            $scope.$apply();
            
        });
    $('#endTimePicker')
        .datetimepicker()
        .on('changeDate', function(ev){
            
            $scope.item.endTime = jQuery(ev.target).data('datetimepicker').getDate();
            $scope.$apply();
            
        });
};
    

var checkLogin = function($location){
	if($.cookie("facebook-token"))
	{
		//do nothing
	}
	else
	{
		$location.path("/");
		$.removeCookie("facebook-uid");
		$.removeCookie("facebook-token");
		$.removeCookie("login");

		setAuthCookies();
	}
};


