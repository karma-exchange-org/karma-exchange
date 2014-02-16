angular.module('kexAsyncLoader', []).factory('$kexAsyncLoader', function($q, $rootScope, $window) { 
    return {
        loadScript : function (type) {
            return function() {
                var s = document.createElement('script'); // use global document since Angular's $document is weak
                switch(type) {
                    case "google-maps" : 
                        s.src = '//maps.googleapis.com/maps/api/js?sensor=false&callback=initialize';
                        break;
                    default : 
                        delete s;
                        return;
                }
                document.body.appendChild(s);
            }
        },
        asynchLoad : function (type){
            var deferred = $q.defer();
            $window.initialize = function () {
                deferred.resolve();
            };
            if ($window.attachEvent) {  
                $window.attachEvent('onload', this.loadScript(type)); 
            } else {
                $window.addEventListener('load', this.loadScript(type), false);
            }
            return deferred.promise;
        },
        loadGoogleMaps : function(){
            return this.asynchLoad("google-maps");
        }
    }
});
