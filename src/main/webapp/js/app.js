
var kexApp = angular.module("kexApp", ["ngResource","ngCookies","google-maps"]).
    config(function($routeProvider,$httpProvider) {
        $routeProvider.
            when('/', { controller: homeCtrl, templateUrl: 'partials/home.html' }).
            when('/events', { controller: eventsCtrl, templateUrl: 'partials/events.html' }).
            when('/addevent', { controller: addEventsCtrl, templateUrl: 'partials/addevent.html' }).
            when('/editevent/:itemId', { controller: editEventsCtrl, templateUrl: 'partials/addevent.html' }).
            otherwise({ redirectTo: '/' });

        $httpProvider.defaults.headers.common['X-'] = 'X';

    });


var homeCtrl = function($scope, $location) {
	checkLogin($location);
};




kexApp.factory('Events', function($resource) {
    return $resource('/api/event/:id', { id: '@id' }, { update: { method: 'POST' } });
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
                scope.$apply(function() {
                    model.$setViewValue(element.val());                
                });
            });
        }
    };
});
var geocoder = new google.maps.Geocoder();
        



var eventsCtrl = function ($scope, $location, $http, Events,$cookieStore) {
	checkLogin($location);
	$scope.reset = function() {


        $scope.items = Events.query({q: $scope.query});
    };

    $scope.reset();

    $scope.delete = function () {
        var itemId = this.item.id;
        Events.delete({ id: itemId }, function () {
            $("#event_" + itemId).fadeOut();
        });
    };

};

var addEventsCtrl = function($scope, $location, Events, geocoder) {

	checkLogin($location);

    angular.extend($scope, {

        /** the initial center of the map */
        center: {
            latitude: 37,
            longitude: -122
        },

        /** the initial zoom level of the map */
        zoom: 4,

        /** list of markers to put in the map */
        markers: [ {
                
            }],

        // These 2 properties will be set when clicking on the map
        clicked: null,  
        clicked: null,
    });
    $scope.save = function () {
    	Events.save($scope.item, function() {
            $location.path('/events');
        });
    };
};

var editEventsCtrl =  function ($scope, $routeParams, $location, Events) {

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
                geocoder.geocode({ 'address': $scope.item.location.address.street+','+$scope.item.location.address.city+','+$scope.item.location.address.state+','+$scope.item.location.address.country }, function (results, status) {
                if (status == google.maps.GeocoderStatus.OK) {
                    $scope.center.latitude = results[0].geometry.location.lat();
                    $scope.center.longitude = results[0].geometry.location.lng();
                    $scope.addMarker($scope.center.latitude,$scope.center.longitude);
                    $scope.zoom = 15;
                    $scope.$apply();
                    
                    
                }

    })};

    $scope.addMarker = function (markerLat,markerLng) {
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
                
                $scope.$apply();
            }, function () {
                
            });
        }   
    };            

    

	

	$scope.save = function () {
		Events.update({id: $scope.item.id}, $scope.item, function () {
	        $location.path('/events');
	    });
	};

    $scope.item = Events.get({ id: $routeParams.itemId } ,function() {
        $scope.refreshMap();

        }, function(response) {
        //404 or bad
        console.log(response);
        if(response.status === 404) {
    }});
    $scope.findMe();
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


