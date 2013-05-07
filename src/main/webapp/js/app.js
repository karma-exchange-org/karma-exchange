
var kexApp = angular.module("kexApp", ["ngResource","ngCookies"]).
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

var addEventsCtrl = function($scope, $location, Events) {

	checkLogin($location);
    $scope.save = function () {
    	Events.save($scope.item, function() {
            $location.path('/events');
        });
    };
};

var editEventsCtrl =  function ($scope, $routeParams, $location, Events) {
	$scope.item = Events.get({ id: $routeParams.itemId });

	$scope.save = function () {
		Events.update({id: $scope.item.id}, $scope.item, function () {
	        $location.path('/events');
	    });
	};
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


