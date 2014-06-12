angular.module('globalErrors', []).config(function($provide, $httpProvider, $compileProvider) {
    $httpProvider.defaults.headers.post["Content-Type"] = "application/json;charset=UTF-8";
    $httpProvider.defaults.transformRequest.push(function(data, headersGetter) {
        return data;
    });
    $httpProvider.responseInterceptors.push(function($rootScope, $q) {
        return function(promise) {
            return promise.then(function(successResponse) {
                // if (successResponse.config.method.toUpperCase() != 'GET' && !isExternal(successResponse.config.url)) {
                //     // $rootScope.showAlert("Saved successfully!", "success");
                // }
                return successResponse;
            }, function(errorResponse) {
                if (  !isExternal(errorResponse.config.url) &&
                      !angular.isDefined(errorResponse.config.noAlertOnError) ) {

                    $rootScope.displayHttpRequestFailure(errorResponse);

                }
                return $q.reject(errorResponse);
            });
        };
    });
});

angular.module('HashBangURLs', []).config(['$locationProvider', function($location) {
  $location.hashPrefix('!');
}]);

kexApp = angular.module( "kexApp",
    ["ngResource", "ngCookies", "ui.bootstrap", "ui.bootstrap.ex", "ngFacebook",
     "globalErrors" ,"ui.calendar", "ngSocial","HashBangURLs", "geocoder"] )

.constant('PAGE_TITLE_SUFFIX', ' - Karma Exchange')

.filter( 'textToHtml', function() {
    var options = {
        callback: function( text, href ) {
            if ( href ) {
                var trimmedText = trimLinkText(text);
                var hrefEscaped = _.escape(href);
                var hrefText = '<a href="' + hrefEscaped +
                    '" title="' + hrefEscaped + '">' + _.escape(trimmedText) + '</a>';
                return hrefText;
            } else {
                var noHtmlText = _.escape(text);
                // preserve newlines
                return noHtmlText.replace( /\n/g, '<br/>' );
            }

        },
        punct_regexp: /(?:[!?.,:;'"]|(?:&|&amp;)(?:lt|gt|quot|apos|raquo|laquo|rsaquo|lsaquo);)$/
    };

    function trimLinkText(str) {
        var MAX_LINK_TEXT_LEN = 30;
        if (str.length > MAX_LINK_TEXT_LEN) {
            return str.slice(0, MAX_LINK_TEXT_LEN) + "...";
        } else {
            return str;
        }
    }

    return function ( text ) {
        if (text) {
            return linkify(text, options);
        }
    }

})

.filter( 'limit10', function( ) {
    return function( text ) {
        if( text > 10 )
        {
            return 'More than 10';
        }
        else if ( text == "0" )
        {
            return 'Event is full';
        }
        else
        {
            return text;
        }
    };
}
)

.filter( 'limit10Verbose', function( ) {
    return function( text ) {
        if( text > 10 )
        {
            return 'Volunteers needed: more than 10';
        }
        else if ( text == "0" )
        {
            return "No additional volunteers are needed at this time";
        }
        else
        {
            return "Volunteers needed: " + text;
        }
    };
}
)

.filter( 'badge', function( ) {
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
    };
}
)

.filter('truncate', function (KexUtil) {
    return function (text, length) {
        if (!angular.isDefined(text) || (text.length === 0)) {
            return null;
        }

        if (isNaN(length)) {
            length = 10;
        }

        if (text.length <= length) {
            return text;
        } else {
            return KexUtil.truncateToWordBoundary(text, length).str;
        }

    };
})

.filter('eventStartEndDate', function () {
    return function (event) {
        if (!angular.isDefined(event)) {
            return;
        }

        var dateStr;
        var startTime = moment(event.startTime);
        var endTime = moment(event.endTime);
        if ( (startTime.year() != endTime.year()) ||
             (startTime.dayOfYear() != endTime.dayOfYear()) ) {
            dateStr =
                '<div>' +
                    '<h4 class="date-start">' +
                        startTime.format('dddd, MMMM Do YYYY') + '<br/>' +
                        startTime.format('h:mm A') +
                    '</h4>' +
                '</div>' +
                '<div>' +
                    '<h4 class="date-to"> to </h4>' +
                '</div>' +
                '<div>' +
                    '<h4 class="date-end">' +
                        endTime.format('dddd, MMMM Do YYYY') + '<br/>' +
                        endTime.format('h:mm A') +
                    '</h4>' +
                '</div>';
        } else {
            dateStr =
                '<h4 class="date-combined">' +
                    startTime.format('dddd, MMMM Do YYYY') + '<br/>' +
                    startTime.format('h:mm A') + ' to ' + endTime.format('h:mm A') +
                '</h4>';
        }
        return dateStr;
    };
});

/*
 * Session management
 */
kexApp.factory( 'AuthResource', function( $resource ) {
    return $resource('/api/auth/:action',
        { action: '@action' },
        {
            login: { method:'POST', params: { action: 'login' } },
            logout: { method:'POST', params: { action: 'logout' } },
        });
} );

kexApp.factory('SessionManager', function($rootScope, $q, $http, FbUtil, $resource,
        $window, $modal, AuthResource, PersonaUtil, MeUtil, $log) {

    $rootScope.isLoggedIn = false;

    var isInitializedDef = $q.defer();
    var isInitialized = false;
    isInitializedDef.promise.then(function (user) {
        if (user) {
            $rootScope.isLoggedIn = true;
        }
        $rootScope.$broadcast("SessionManager.initialized", user);
        isInitialized = true;
    });

    var fbFirstAuthRespDef = $q.defer();
    var isFirstAuthResp = true;
    $rootScope.$on("fbUtil.userChanged", function (event, response) {
        if ( isFirstAuthResp ) {
            isFirstAuthResp = false;
            fbFirstAuthRespDef.resolve(response);
        }
    });

    var personaInitializedDef = $q.defer();
    $rootScope.$on("PersonaUtil.initialized", function (event, response) {
        $log.log("Persona initialized: loginRequest=%o", PersonaUtil.getLoginRequest());
        personaInitializedDef.resolve(response);
    });

    initSession();

    function initSession() {

        $log.log("initSession invoked...");

        if ($.cookie('session')) {

            $log.log("session cookie found: " + $.cookie('session'));

            // Have to use $http to pass a custom config.
            $http( {
                method: 'POST',
                url: '/api/auth/renew',
                noAlertOnError: true
            }).then(
                function (response) {
                    $log.log("SUCCESS /api/auth/renew: %o", response.data);
                    $log.log("session cookie updated: " + $.cookie('session'));

                    MeUtil.initMandatoryFields(response.data).then( function(user) {
                        isInitializedDef.resolve(user);
                    });
                },
                function (response) {
                    if (  isSessionExpiredRequestFailure(response) ) {
                        $log.log("FAILURE /api/auth/renew: SESSION_EXPIRED");
                        $log.log("Deleting cookie and re-initializing...");

                        $.removeCookie('session');
                        initSession();

                    } else {
                        $log.log("FAILURE /api/auth/renew: %o", response);

                        $rootScope.displayHttpRequestFailure(response);

                        // Keep state as logged out.
                        isInitializedDef.resolve();
                    }
                });

        } else {

            // Check if we are auto logged in with facebook first.
            fbFirstAuthRespDef.promise.then( function(response) {
                var loginRequest = FbUtil.toLoginRequest(response);
                if (loginRequest) {
                    $log.log("logged in via facebook + app authorized. Attempting to authenticate");
                    processInitialLoginRequest(loginRequest);
                } else {
                    $log.log("not logged in via facebook. checking mozilla");
                    personaInitializedDef.promise.then( function() {
                        loginRequest = PersonaUtil.getLoginRequest();
                        if (loginRequest) {
                            $log.log("logged in via mozilla. Attempting to authenticate");
                            processInitialLoginRequest(loginRequest).then(
                                function() {},
                                function() {
                                    // On failure reset any persona state.
                                    PersonaUtil.logout();
                                });
                        } else {
                            $log.log("not logged in via fb or mozilla. Defaulting to logged out.");
                            isInitializedDef.resolve();
                        }
                    });
                }
            });
        }

    }

    function isSessionExpiredRequestFailure(response) {
        return (response.status == 400) &&
            angular.isDefined(response.data) &&
            angular.isDefined(response.data.error) &&
            (response.data.error.type == 'SESSION_EXPIRED');
    }

    function processInitialLoginRequest(loginRequest) {
        return processLoginRequest(loginRequest)
            .then(
                function(user) {
                    return MeUtil.initMandatoryFields(user);
                })
            .then(
                function(user) {
                    isInitializedDef.resolve(user);
                },
                function() {
                    isInitializedDef.resolve();
                    $q.reject();
                });
    }

    function processLoginRequest(loginRequest) {
        var loginRequestDef = $q.defer();

        AuthResource.login(null, loginRequest,
            function (value, responseHeaders) {
                $log.log("SUCCESS /api/auth/login: %o", value);

                loginRequestDef.resolve(value);
            },
            function (response) {
                $log.log("FAILURE /api/auth/login: %o", response);

                loginRequestDef.reject(response);
            });

        return loginRequestDef.promise;
    }

    var SessionManager = {
        isInitialized: function() {
            return isInitialized;
        },

        login: function() {
            var modalInstance = $modal.open({
                backdrop: "static",
                templateUrl: 'template/kex/login-modal.html',
                controller: 'LoginModalInstanceCtrl'
            });
            return modalInstance.result
                .then( function(user) {
                    return MeUtil.initMandatoryFields(user);
                })
                .then( function(user) {
                    $rootScope.isLoggedIn = true;
                    $rootScope.$broadcast("SessionManager.userChanged", user);
                });
        },

        loginRequired: function(showAlert) {
            if ( $rootScope.isLoggedIn ) {
                return $q.when();
            } else {
                var userChangeProcessedDef = $q.defer();
                var userChangeNotifCleanup =
                    $rootScope.$on("SessionManager.userChanged", function (event, response) {
                        userChangeProcessedDef.resolve();
                        userChangeNotifCleanup();
                    });


                return SessionManager.login().then(
                    function() {
                        // Ensure that the user change event has been processed.
                        return userChangeProcessedDef.promise;
                    },
                    function() {
                        userChangeNotifCleanup();
                        if (!angular.isDefined(showAlert) || showAlert) {
                            $rootScope.showAlert('Login required', "danger");
                        }
                        return $q.reject();
                    });
            }
        },

        logout: function() {

            var sessionLoggedOutDef = $q.defer();
            AuthResource.logout(null, null,
                function (value, responseHeaders) {
                    $log.log("SUCCESS /api/auth/logout");
                    sessionLoggedOutDef.resolve();
                },
                function () {
                    sessionLoggedOutDef.reject();
                });

            var fbLogoutPromise = FbUtil.logout();

            PersonaUtil.logout();

            $q.all([fbLogoutPromise, sessionLoggedOutDef.promise])
                .then(completeLogout, completeLogout);

            function completeLogout(result) {
                $log.log("logged out: %o", result);

                // Dealing with in flight ajax requests is tricky. Keep things
                // simple and just reload the page.
                $window.location.href = "/";
            }

        },

        processLoginRequest: processLoginRequest
    };

    return SessionManager;
});

kexApp.controller(
    'LoginModalInstanceCtrl',
    [
        '$scope', '$modalInstance', '$facebook', 'SessionManager', 'FbUtil', '$q', 'PersonaUtil', '$rootScope', '$log',
        function($scope, $modalInstance, $facebook, SessionManager, FbUtil, $q, PersonaUtil, $rootScope, $log) {

            $scope.cancel = cancelLogin;
            var loginTracker = new PromiseTracker();

            var personaAuthChangeUnsubscribe = $rootScope.$on("PersonaUtil.authChanged", function ( event, response ) {
                if (!loginTracker.isPending()) {
                    consumePendingPersonaLoginRequest();
                }
            });

            $scope.$on('$destroy', function() {
                personaAuthChangeUnsubscribe();
            });

            $scope.fbLogin = function() {
                if (!loginTracker.isPending()) {
                    // Need to call this directly since pop-up can
                    // only be spawned on click events.
                    var fbLoginProm = $facebook.login();
                    loginTracker = new PromiseTracker(fbLoginProm);

                    fbLoginProm.then(
                        function(response) {
                            $log.log("Facebook login modal returned: %o", response);

                            var loginRequest = FbUtil.toLoginRequest(response);
                            if (loginRequest) {
                                $log.log("Facebook login successful, converted to credentials");
                                processLoginRequest(loginRequest);
                            } else {
                                consumePendingPersonaLoginRequest();
                            }
                        },
                        function(err) {
                            consumePendingPersonaLoginRequest();
                        });
                }

            }

            $scope.personaLogin = function() {
                if (!loginTracker.isPending()) {
                    if (!consumePendingPersonaLoginRequest()) {
                        PersonaUtil.login();
                    }
                }
            }

            function consumePendingPersonaLoginRequest() {
                var loginRequest = PersonaUtil.getLoginRequest();
                if (loginRequest) {
                    $log.log("Processing persona login...");
                    processLoginRequest(loginRequest, PersonaUtil.logout);
                    return true;
                } else {
                    return false;
                }
            }

            function processLoginRequest(loginRequest, cleanupCallback) {
                var loginRequestPromise = SessionManager.processLoginRequest(loginRequest);
                loginTracker = new PromiseTracker(loginRequestPromise);
                loginRequestPromise.then(
                    function(value) {
                        loginComplete(value);
                    },
                    function() {
                        if (cleanupCallback) {
                            cleanupCallback();
                        }
                        // Close the modal. Error should already be displayed.
                        cancelLogin();
                    });
            }

            function cancelLogin() {
                $modalInstance.dismiss();
            }

            function loginComplete(user) {
                $modalInstance.close(user);
            }
        }
    ]);

/*
 * Webservice factories
 */

kexApp.factory( 'Events', function( AuthDepResource ) {
    return AuthDepResource.create( '/api/event/:id/:registerCtlr/:regType',
        { id : '@id', registerCtlr : '@registerCtlr', regType : '@regType' });
} );
kexApp.factory( 'UserManagedEvents', function( AuthDepResource ) {
    return AuthDepResource.create( '/api/user/:userId/user_managed_event/:userManagedEventId',
        { userId: '@userId', userManagedEventId : '@userManagedEventId' });
} );
kexApp.factory( 'User', function( AuthDepResource ) {
    return AuthDepResource.create( '/api/user/:id/:resource/:filter',
        { id : '@id', resource : '@resource', filter : '@filter' } );
} );
kexApp.factory( 'Me', function( AuthDepResource ) {
    return AuthDepResource.create( '/api/me/:resource/:filter',
        { resource : '@resource', filter : '@filter' } );
} );
kexApp.factory( 'Org', function( AuthDepResource ) {
    return AuthDepResource.create( '/api/org/:id/:resource/:filter',
        { id : '@id', resource : '@resource', filter : '@filter' } );
} );

kexApp.factory('AuthDepResource', function($resource, $q, $rootScope) {
    var wrappedMethods = ['get', 'save', 'query', 'remove', 'delete'];

    var sessionInitializedDef = $q.defer();
    $rootScope.$on("SessionManager.initialized", function (event, response) {
        sessionInitializedDef.resolve();
    });

    return {
        create: function() {
            var wrappedRsrc = {
                _rsrc: $resource.apply(null, arguments)
            };

            for (var mIdx = 0; mIdx < wrappedMethods.length; mIdx++) {
                var m = wrappedMethods[mIdx];
                wrappedRsrc[m] = wrapMethod(m);
            }

            return wrappedRsrc;

            function wrapMethod(m) {
                return function() {
                    var wrappedMethodArgs = wrapMethodArgs(arguments);

                    sessionInitializedDef.promise.then( function() {
                        wrappedRsrc._rsrc[m].apply(
                            wrappedRsrc._rsrc, wrappedMethodArgs.args);
                    });

                    return wrappedMethodArgs.promise;

                    // Until we convert to angular 1.2, wrap the success and failure
                    // callbacks so we can return a promise from this API.
                    function wrapMethodArgs(args) {
                        var wrappedArgs = [];
                        var successCbFound = false;
                        var errorCbFound = false;
                        var retValDefered = $q.defer();

                        for (var argIdx = 0; argIdx < args.length; argIdx++) {
                            if (angular.isFunction(args[argIdx])) {
                                if (!successCbFound) {
                                    successCbFound = true;
                                    wrappedArgs.push(
                                        wrapSuccessCb(retValDefered, args[argIdx]));
                                } else if (!errorCbFound) {
                                    errorCbFound = true;
                                    wrappedArgs.push(
                                        wrapErrorCb(retValDefered, args[argIdx]));
                                } else {
                                    wrappedArgs.push(args[argIdx]);
                                }
                            } else {
                                wrappedArgs.push(args[argIdx]);
                            }
                        }

                        if (!successCbFound) {
                            wrappedArgs.push(wrapSuccessCb(retValDefered));
                        }
                        if (!errorCbFound) {
                            wrappedArgs.push(wrapErrorCb(retValDefered));
                        }

                        return {
                            args: wrappedArgs,
                            promise: retValDefered.promise
                        };

                        function wrapSuccessCb(retValDefered, cbArg) {
                            return function (value, responseHeaders) {
                                retValDefered.resolve(value);
                                if (cbArg) {
                                    cbArg(value, responseHeaders);
                                }
                            }
                        }

                        function wrapErrorCb(retValDefered, cbArg) {
                            return function (httpResponse) {
                                retValDefered.reject(httpResponse);
                                if (cbArg) {
                                    cbArg(httpResponse);
                                }
                            }
                        }
                    }
                };
            }
        }

    };
});

/*
 * Utility API factories
 */

/*
 * This class wraps a promise that can be used prior to the task associated with
 * the promise being issued. Additionally, using the 'recycle' method a new
 * promise is created everytime a new task is issued. The most recently
 * created promise is always returned from the property "promise".
 */
kexApp.factory('RecyclablePromiseFactory', function($q) {

    function RecyclablePromise() {
        this._deferred = $q.defer();
        this._firstRecycle = true;
    }

    Object.defineProperty(RecyclablePromise.prototype, "promise", {
        get: function() {
            return this._deferred.promise;
        }
    });

    RecyclablePromise.prototype.recycle = function() {
        if ( this._firstRecycle ) {
            this._firstRecycle = false;
        } else {
            this._deferred = $q.defer();
        }
        return this._deferred;
    };

    return {
        create: function() {
            return new RecyclablePromise();
        }
    };
});

kexApp.factory('ElementSourceFactory', function($q, $http) {

    function PagedElementSource(resultPromise, processResultCb) {
        this._cachedResults = [];
        this._nextUrl = null;
        this._processResultCb = processResultCb;

        this._processFetchResultBound =
            angular.bind(this, this._processFetchResult);
        this._processFetchErrBound =
            angular.bind(this, this._processFetchErr);

        this._fetchResultDef = $q.defer();
        resultPromise.then(
            this._processFetchResultBound,
            this._processFetchErrBound);
    }

    PagedElementSource.prototype._processFetchResult = function(result) {
        this._fetchResultDef.resolve();
        this._nextUrl =
            result.paging ? result.paging.next : null;
        for (var elIdx = 0; elIdx < result.data.length; elIdx++) {
            var el = result.data[elIdx];
            var processedEl = this._processResultCb ?
                this._processResultCb(el) : el;
            this._cachedResults.push(processedEl);
        }
    }

    PagedElementSource.prototype._processFetchErr = function() {
        this._fetchResultDef.resolve();
        this._nextUrl = null;
    }


    PagedElementSource.prototype.peek = function() {
        return this._fetchResultDef.promise.then( angular.bind(this, function() {
            if (this._cachedResults.length > 0) {
                return this._cachedResults[0];
            } else if (this._nextUrl) {
                this._fetchResultDef = $q.defer();
                $http.get( this._nextUrl )
                    .success(this._processFetchResultBound)
                    .error(this._processFetchErrBound);

                return this.peek();
            } else {
                return null;
            }
        }));
    }

    PagedElementSource.prototype.pop = function() {
        return this.peek().then( angular.bind(this, function (result) {
            if (result == null) {
                return null;
            } else {
                return this._cachedResults.shift();
            }
        }));
    }

    function CompositeElementSource(elementSource1, elementSource2, resultOrderCb) {
        this._elementSource1 = elementSource1;
        this._elementSource2 = elementSource2;
        this._resultOrderCb = resultOrderCb;
    }

    CompositeElementSource.prototype._action = function(action) {
        var es1Result;

        return this._elementSource1.peek()
            .then(
                angular.bind(this, function(result) {
                    es1Result = result;
                    return this._elementSource2.peek();
                }))
            .then(
                angular.bind(this, function(result) {
                    var es2Result = result;

                    if ((es1Result != null) && (es2Result != null)) {
                        if (this._resultOrderCb(es1Result, es2Result) <= 0) {
                            return this._elementSource1[action]();
                        } else {
                            return this._elementSource2[action]();
                        }
                    } else if (es1Result != null) {
                        return this._elementSource1[action]();
                    } else {
                        return this._elementSource2[action]();
                    }
                }));
    }

    CompositeElementSource.prototype.peek = function() {
        return this._action('peek');
    }

    CompositeElementSource.prototype.pop = function() {
        return this._action('pop');
    }

    function SortedArrayElementSource(orderCb) {
        this._elements = [];
        this._orderCb = orderCb;
    }

    SortedArrayElementSource.prototype.peek = function() {
        return $q.when(
            (this._elements.length == 0) ? null : this._elements[0]);
    }

    SortedArrayElementSource.prototype.pop = function() {
        return $q.when(
            (this._elements.length == 0) ? null : this._elements.shift());
    }

    SortedArrayElementSource.prototype.push = function(element) {
        this._elements.orderedInsertExt(element, this._orderCb);
    }

    function PagedElementView(resultPromise, processElementCb, pageLimit, firstPageNumElements) {
        this._elementSource =
            new PagedElementSource(resultPromise, processElementCb);
        this.pageLimit = pageLimit;
        this._displayLimit = pageLimit;
        if (angular.isDefined(firstPageNumElements)) {
            this._displayLimit = firstPageNumElements;
        }
        this.elements = [];
        this.hasMore = false;
        this.firstPageProcessed = false;
        this.isProcessing = true;
        this._isProcessingDef = $q.defer();
        this._updateView();
    }

    PagedElementView.prototype._updateView = function() {
        if (this.elements.length < this._displayLimit) {
            this._elementSource.pop().then(
                angular.bind(this, function(element) {
                    if (element == null) {
                        this._updateHasMore();
                    } else {
                        this.elements.push(element);
                        // Recurse to display the next element.
                        return this._updateView();
                    }
                }));
        } else {
            this._updateHasMore();
        }
    }

    PagedElementView.prototype._updateHasMore = function() {
        this._elementSource.peek().then(
            angular.bind(this, function(element) {
                this.isProcessing = false;
                this._isProcessingDef.resolve();
                this.firstPageProcessed = true;
                this.hasMore = (element != null);
            }));
    }

    PagedElementView.prototype.fetchNextPage = function() {
        if (!this.isProcessing) {
            this._displayLimit += this.pageLimit;
            this.isProcessing = true;
            this._isProcessingDef = $q.defer();
            this._updateView();
        }
    }

    PagedElementView.prototype.isProcessingPromise = function() {
        return this._isProcessingDef.promise;
    }

    return {
        createPagedElementSource: function(resultPromise, processResultCb) {
            return new PagedElementSource(resultPromise, processResultCb);
        },

        createPagedElementView: function(resultPromise, processElementCb, pageLimit, firstPageNumElements) {
            return new PagedElementView(resultPromise, processElementCb, pageLimit, firstPageNumElements);
        },

        createCompositeElementSource: function(elementSource1, elementSource2, resultOrderCb) {
            return new CompositeElementSource(elementSource1, elementSource2, resultOrderCb);
        },

        createSortedArrayElementSource: function(orderCb) {
            return new SortedArrayElementSource(orderCb);
        }
    };
});

kexApp.factory('SnapshotServiceHandshake', function($timeout) {
    var DEFAULT_SNAPSHOT_TIMEOUT = 1000;
    var RENDER_DELAY = 100; /* time to wait for angular to render results. */

    var snapshotOnTimeout = true;
    var snapshotTaken = false;

    var prerenderIoSnapshotService = {
        init: function() {
            window.prerenderReady = false;
        },
        snapshot: function() {
            window.prerenderReady = true;
        }
    };

    var ajaxSnapshotsSnapshotService = {
        init: function() {
        },
        snapshot: function() {
            if (window._AJS) {
                window._AJS.takeSnapshot();
            }
        }
    };

    var snapshotServices = [
        prerenderIoSnapshotService,
        ajaxSnapshotsSnapshotService
    ];

    init();

    function init() {
        angular.forEach(snapshotServices, function(snapshotService) {
            snapshotService.init();
        });

        $timeout(snapshotOnTimeoutCb, DEFAULT_SNAPSHOT_TIMEOUT);
    }

    function snapshotOnTimeoutCb() {
        if (snapshotOnTimeout) {
            snapshot();
        }
    }

    function snapshot() {
        if (!snapshotTaken) {
            snapshotTaken = true;
            angular.forEach(snapshotServices, function(snapshotService) {
                snapshotService.snapshot();
            });
        }
    }

    function snapshotWithRenderDelay() {
        if (!snapshotTaken) {
            $timeout(snapshot, RENDER_DELAY);
        }
    }

    return {
        disableSnapshotOnTimeout: function() {
            snapshotOnTimeout = false;
        },
        snapshot: function() {
            snapshotWithRenderDelay();
        }
    }
});

kexApp.provider('PageProperties', function() {
    var fbAppId;

    this.setFbAppId = function(appId) {
      fbAppId = appId;
      return this;
    };

    this.$get = [
        '$rootScope', 'KexUtil', 'PAGE_TITLE_SUFFIX',
        function($rootScope, KexUtil, PAGE_TITLE_SUFFIX) {

            var DEFAULT_PAGE_TITLE = "Karma Exchange";
            var DEFAULT_PAGE_DESCRIPTION =
                "Karma Exchange is a community of connected, inspired, and engaged volunteers." +
                " Join us and make earning and sharing karma part of your daily routine.";
            var metaPropertyTags, metaNameTags;

            $rootScope.$on('$routeChangeSuccess', function (event, current, previous) {
                reset();
                if (current.$$route && current.$$route.title) {
                    setTitle(current.$$route.title, false);
                }
            });

            reset();

            function reset() {
                metaPropertyTags = {
                    "og:site_name" : "Karma Exchange",
                    "fb:app_id" : fbAppId
                    // Skipping
                    // - og:url - canonical url
                    // - og:type - website is not preferred. Article seems wrong.
                    //      maybe we need a custom type
                    //      https://developers.facebook.com/docs/reference/opengraph/
                    // - twitter:site - need to reserve KarmaExchange on twitter
                };
                metaNameTags =  {};
                setTitle(DEFAULT_PAGE_TITLE, false);
                setPageImage(KexUtil.getBaseUrl() + "/img/kex-logo-1024.png");
                setDescription(DEFAULT_PAGE_DESCRIPTION);
            }

            function setTitle(contentTitle, addSuffixToPageTitle) {
                $rootScope.pageTitle = contentTitle;
                if (addSuffixToPageTitle) {
                    $rootScope.pageTitle += PAGE_TITLE_SUFFIX;
                }
                metaNameTags["twitter:title"] = contentTitle;
                metaPropertyTags["og:title"] = contentTitle;
            }

            function setPageImage(url) {
                metaPropertyTags["og:image"] = url;
                metaNameTags["twitter:image"] = url;
            }

            function setDescription(description) {
                var descrip155 =
                    KexUtil.truncateToWordBoundary(description, 155).str;
                var descrip500 =
                    KexUtil.truncateToWordBoundary(description, 500).str;
                metaNameTags["description"] = descrip155;
                metaNameTags["twitter:card"] = descrip500;
                metaPropertyTags["og:description"] = descrip500;
            }

            return {
                setContentTitle : function(contentTitle) {
                    setTitle(contentTitle, true);
                },
                setPageImage : function(imgUrl, isRelative) {
                    var imgUrlExpanded =
                        isRelative ? (KexUtil.getBaseUrl() + imgUrl) : imgUrl;
                    setPageImage(imgUrlExpanded);
                },
                setDescription: setDescription,

                getMetaPropertyTags : function() {
                    return metaPropertyTags;
                },
                getMetaNameTags : function() {
                    return metaNameTags;
                }
            };

        }];
});

kexApp.factory('IframeUtil', function($rootScope, $location) {

    var iframeParentUrl, iframeView, iframeVersion;

    iframeVersion = $location.search().iframe;
    iframeView = $rootScope.iframeView = !!iframeVersion;
    iframeParentUrl = $location.search().iframeParentUrl;

    var IframeUtil = {

        getEventDetailsHref: function(eventKey) {
            var hrefResult = {};
            if ( iframeView && iframeParentUrl ) {
                hrefResult.href = iframeParentUrl + '?eventId=' + eventKey;
                if (iframeVersion == 'targetAlwaysBlank') {
                    hrefResult.target = '_blank';
                } else {
                    hrefResult.target = '_parent';
                }
            } else {
                hrefResult.href = '#!/event/' + eventKey;
                hrefResult.target = IframeUtil.getLinkTarget();
            }
            return hrefResult;
        },

        getLinkTarget: function() {
            if (iframeView) {
                return '_blank';
            } else {
                return '_self';
            }
        }

    }
    return IframeUtil;
});

kexApp.factory('KexUtil', function($location, $modal, $rootScope) {
    var ANON_USER_IMAGE_URL = "/img/anon_user_256_pd_color.png";

    return {
        getBaseUrl: function() {
            // window.location.host includes the port #.
            return window.location.protocol + '//' + window.location.host;
        },

        getLocation: function() {
            return window.location.href;
        },

        urlStripProtocol: function(url) {
            return url ? url.replace(/^http:/,'') : url;
        },

        stripHashbang: function(url) {
            return (url.indexOf('#!') === 0) ? url.substring(2) : url;
        },

        getSavedResourceKey: function(getResponseHeaderCb) {
            var locationHdr = getResponseHeaderCb('location');
            if (angular.isDefined(locationHdr)) {
                var idx = locationHdr.lastIndexOf('/');
                if ((idx != -1) && ((idx + 1) < locationHdr.length)) {
                    return locationHdr.slice(idx + 1, locationHdr.length);
                }
            }
            return undefined;
        },

        toHours: function(karmaPoints, dec) {
            return this.round(karmaPoints / 60, dec);
        },
        toKarmaPoints: function(hours) {
            return Math.round(hours * 60);
        },

        /*
         * Round 'num' to 'dec' decimal places.
         *
         * Example: round(4.454, 2) = 4.45
         */
        round: function (num, dec) {
           return Math.round(num * Math.pow(10, dec)) / Math.pow(10, dec);
        },

        selectRandom: function(els) {
            return els[Math.floor(Math.random() * els.length)];
        },

        getFirstOfMonth: function(date) {
            return new Date(date.getFullYear(), date.getMonth());
        },

        addMonths: function(date, val) {
            return moment(date).add('months', 1).toDate();
        },

        group: function(els, groupSize) {
            var groupedEls = [];
            if (angular.isDefined(els)) {
                for (var idx = 0; idx < els.length; idx += groupSize) {
                    if ((idx + groupSize) > els.length) {
                        groupedEls.push(els.slice(idx));
                    } else {
                        groupedEls.push(els.slice(idx, idx + groupSize));
                    }
                }
            }
            return groupedEls;
        },

        truncateToWordBoundary: function(str, lim) {
            if (!str) {
                return { str: str, tuncated: false };
            }

            var truncSuffix = "...";
            lim = Math.max(lim, truncSuffix.length);
            var tooLong = str.length > lim;
            if (tooLong) {
                str = str.substr(0, lim - truncSuffix.length);
                var lastSpaceIdx = str.lastIndexOf(' ');
                if (lastSpaceIdx === -1) {
                    lastSpaceIdx = str.length;
                }
                str = str.substr(0, lastSpaceIdx) + "...";
            }
            return { str: str, truncated: tooLong };
        },

        setLocation: function (path) {
            $location.path(this.stripHashbang(path));
        },

        createConfirmationModal: function(modalText) {
            var modalInstance = $modal.open({
                backdrop: false,
                templateUrl: 'template/kex/confirmation-modal.html',
                controller: 'ConfirmationModalInstanceCtrl',
                resolve:
                    {
                        modalText: function () {
                            return modalText;
                        }
                    }
            });

            return modalInstance.result;

        },

        // http://stackoverflow.com/questions/11381673/javascript-solution-to-detect-mobile-browser#comment22236638_11381730
        // http://detectmobilebrowsers.com
        isMobileDevice: function() {
            var result = false;

            (function(a){
                if (  /(android|bb\d+|meego).+mobile|avantgo|bada\/|blackberry|blazer|compal|elaine|fennec|hiptop|iemobile|ip(hone|od)|iris|kindle|lge |maemo|midp|mmp|mobile.+firefox|netfront|opera m(ob|in)i|palm( os)?|phone|p(ixi|re)\/|plucker|pocket|psp|series(4|6)0|symbian|treo|up\.(browser|link)|vodafone|wap|windows (ce|phone)|xda|xiino/i.test(a) ||
                      /1207|6310|6590|3gso|4thp|50[1-6]i|770s|802s|a wa|abac|ac(er|oo|s\-)|ai(ko|rn)|al(av|ca|co)|amoi|an(ex|ny|yw)|aptu|ar(ch|go)|as(te|us)|attw|au(di|\-m|r |s )|avan|be(ck|ll|nq)|bi(lb|rd)|bl(ac|az)|br(e|v)w|bumb|bw\-(n|u)|c55\/|capi|ccwa|cdm\-|cell|chtm|cldc|cmd\-|co(mp|nd)|craw|da(it|ll|ng)|dbte|dc\-s|devi|dica|dmob|do(c|p)o|ds(12|\-d)|el(49|ai)|em(l2|ul)|er(ic|k0)|esl8|ez([4-7]0|os|wa|ze)|fetc|fly(\-|_)|g1 u|g560|gene|gf\-5|g\-mo|go(\.w|od)|gr(ad|un)|haie|hcit|hd\-(m|p|t)|hei\-|hi(pt|ta)|hp( i|ip)|hs\-c|ht(c(\-| |_|a|g|p|s|t)|tp)|hu(aw|tc)|i\-(20|go|ma)|i230|iac( |\-|\/)|ibro|idea|ig01|ikom|im1k|inno|ipaq|iris|ja(t|v)a|jbro|jemu|jigs|kddi|keji|kgt( |\/)|klon|kpt |kwc\-|kyo(c|k)|le(no|xi)|lg( g|\/(k|l|u)|50|54|\-[a-w])|libw|lynx|m1\-w|m3ga|m50\/|ma(te|ui|xo)|mc(01|21|ca)|m\-cr|me(rc|ri)|mi(o8|oa|ts)|mmef|mo(01|02|bi|de|do|t(\-| |o|v)|zz)|mt(50|p1|v )|mwbp|mywa|n10[0-2]|n20[2-3]|n30(0|2)|n50(0|2|5)|n7(0(0|1)|10)|ne((c|m)\-|on|tf|wf|wg|wt)|nok(6|i)|nzph|o2im|op(ti|wv)|oran|owg1|p800|pan(a|d|t)|pdxg|pg(13|\-([1-8]|c))|phil|pire|pl(ay|uc)|pn\-2|po(ck|rt|se)|prox|psio|pt\-g|qa\-a|qc(07|12|21|32|60|\-[2-7]|i\-)|qtek|r380|r600|raks|rim9|ro(ve|zo)|s55\/|sa(ge|ma|mm|ms|ny|va)|sc(01|h\-|oo|p\-)|sdk\/|se(c(\-|0|1)|47|mc|nd|ri)|sgh\-|shar|sie(\-|m)|sk\-0|sl(45|id)|sm(al|ar|b3|it|t5)|so(ft|ny)|sp(01|h\-|v\-|v )|sy(01|mb)|t2(18|50)|t6(00|10|18)|ta(gt|lk)|tcl\-|tdg\-|tel(i|m)|tim\-|t\-mo|to(pl|sh)|ts(70|m\-|m3|m5)|tx\-9|up(\.b|g1|si)|utst|v400|v750|veri|vi(rg|te)|vk(40|5[0-3]|\-v)|vm40|voda|vulc|vx(52|53|60|61|70|80|81|83|85|98)|w3c(\-| )|webc|whit|wi(g |nc|nw)|wmlb|wonu|x700|yas\-|your|zeto|zte\-/i.test(a.substr(0,4))) {
                    result = true;
                }
            })(navigator.userAgent||navigator.vendor||window.opera);

            return result;
        },

        // From: http://detectmobilebrowsers.com/about
        // "To add support for tablets, add |android|ipad|playbook|silk to the first regex."
        isMobileDeviceOrTablet:  function() {
            var result = false;

            (function(a){
                if (  /(android|bb\d+|meego).+mobile|avantgo|bada\/|blackberry|blazer|compal|elaine|fennec|hiptop|iemobile|ip(hone|od)|iris|kindle|lge |maemo|midp|mmp|mobile.+firefox|netfront|opera m(ob|in)i|palm( os)?|phone|p(ixi|re)\/|plucker|pocket|psp|series(4|6)0|symbian|treo|up\.(browser|link)|vodafone|wap|windows (ce|phone)|xda|xiino|android|ipad|playbook|silk/i.test(a) ||
                      /1207|6310|6590|3gso|4thp|50[1-6]i|770s|802s|a wa|abac|ac(er|oo|s\-)|ai(ko|rn)|al(av|ca|co)|amoi|an(ex|ny|yw)|aptu|ar(ch|go)|as(te|us)|attw|au(di|\-m|r |s )|avan|be(ck|ll|nq)|bi(lb|rd)|bl(ac|az)|br(e|v)w|bumb|bw\-(n|u)|c55\/|capi|ccwa|cdm\-|cell|chtm|cldc|cmd\-|co(mp|nd)|craw|da(it|ll|ng)|dbte|dc\-s|devi|dica|dmob|do(c|p)o|ds(12|\-d)|el(49|ai)|em(l2|ul)|er(ic|k0)|esl8|ez([4-7]0|os|wa|ze)|fetc|fly(\-|_)|g1 u|g560|gene|gf\-5|g\-mo|go(\.w|od)|gr(ad|un)|haie|hcit|hd\-(m|p|t)|hei\-|hi(pt|ta)|hp( i|ip)|hs\-c|ht(c(\-| |_|a|g|p|s|t)|tp)|hu(aw|tc)|i\-(20|go|ma)|i230|iac( |\-|\/)|ibro|idea|ig01|ikom|im1k|inno|ipaq|iris|ja(t|v)a|jbro|jemu|jigs|kddi|keji|kgt( |\/)|klon|kpt |kwc\-|kyo(c|k)|le(no|xi)|lg( g|\/(k|l|u)|50|54|\-[a-w])|libw|lynx|m1\-w|m3ga|m50\/|ma(te|ui|xo)|mc(01|21|ca)|m\-cr|me(rc|ri)|mi(o8|oa|ts)|mmef|mo(01|02|bi|de|do|t(\-| |o|v)|zz)|mt(50|p1|v )|mwbp|mywa|n10[0-2]|n20[2-3]|n30(0|2)|n50(0|2|5)|n7(0(0|1)|10)|ne((c|m)\-|on|tf|wf|wg|wt)|nok(6|i)|nzph|o2im|op(ti|wv)|oran|owg1|p800|pan(a|d|t)|pdxg|pg(13|\-([1-8]|c))|phil|pire|pl(ay|uc)|pn\-2|po(ck|rt|se)|prox|psio|pt\-g|qa\-a|qc(07|12|21|32|60|\-[2-7]|i\-)|qtek|r380|r600|raks|rim9|ro(ve|zo)|s55\/|sa(ge|ma|mm|ms|ny|va)|sc(01|h\-|oo|p\-)|sdk\/|se(c(\-|0|1)|47|mc|nd|ri)|sgh\-|shar|sie(\-|m)|sk\-0|sl(45|id)|sm(al|ar|b3|it|t5)|so(ft|ny)|sp(01|h\-|v\-|v )|sy(01|mb)|t2(18|50)|t6(00|10|18)|ta(gt|lk)|tcl\-|tdg\-|tel(i|m)|tim\-|t\-mo|to(pl|sh)|ts(70|m\-|m3|m5)|tx\-9|up(\.b|g1|si)|utst|v400|v750|veri|vi(rg|te)|vk(40|5[0-3]|\-v)|vm40|voda|vulc|vx(52|53|60|61|70|80|81|83|85|98)|w3c(\-| )|webc|whit|wi(g |nc|nw)|wmlb|wonu|x700|yas\-|your|zeto|zte\-/i.test(a.substr(0,4))) {
                    result = true;
                }
            })(navigator.userAgent||navigator.vendor||window.opera);

            return result;
        },

        getUserImageUrl: function(user, imageType) {
            if (!angular.isDefined(user)) {
                return undefined;
            }

            if (user.profileImage) {
                if (imageType != 'large') {
                    imageType = 'square';
                }

                // Currently we only support facebook.
                var urlSuffix = '?type=' + imageType;

                return user.profileImage.url + urlSuffix;
            } else {
                return ANON_USER_IMAGE_URL;
            }
        }

    };
});

kexApp.factory('MeUtil', function($rootScope, $q, Me, KarmaGoalUtil, KexUtil,
        RecyclablePromiseFactory, $modal, $resource) {

    var meRecyclablePromise = RecyclablePromiseFactory.create();
    var goalInfoRecyclablePromise = RecyclablePromiseFactory.create();
    var MeUtil;

    $rootScope.$on("SessionManager.initialized", processUserChange);
    $rootScope.$on("SessionManager.userChanged", processUserChange);

    function processUserChange(event, user) {
        if (user) {
            MeUtil.updateMe(user);
        } else {
            MeUtil.clearMe();
        }
    }

    function mandatoryFieldsInitialized(user) {
        var primaryEmailInfo = MeUtil.getPrimaryEmailInfo(user);
        return user.firstName && user.firstName.trim() &&
            user.lastName && user.lastName.trim() &&
            (primaryEmailInfo != null) && primaryEmailInfo.email && primaryEmailInfo.email.trim();
    }

    MeUtil = {
        // Get the promise corresponding to me
        me: function() {
            return meRecyclablePromise.promise;
        },

        goalInfo: function() {
            return goalInfoRecyclablePromise.promise;
        },

        updateMe: function(user) {
            var meDef = meRecyclablePromise.recycle();
            var goalInfoDef = goalInfoRecyclablePromise.recycle();

            $rootScope.me = user;
            meDef.resolve($rootScope.me);
            updateIsOrganizer();
            updateKarmaGoal(goalInfoDef);

            function updateIsOrganizer() {
                for (var idx = 0; idx < $rootScope.me.organizationMemberships.length; idx++) {
                    var membership = $rootScope.me.organizationMemberships[idx];
                    if ((membership.role == 'ORGANIZER') || (membership.role == 'ADMIN')) {
                        $rootScope.isOrganizer = true;
                        break;
                    }
                }
            }

            function updateKarmaGoal(goalInfoDef) {
                $rootScope.goalInfo = {};
                KarmaGoalUtil.loadKarmaGoalInfo($rootScope.me, KexUtil.getFirstOfMonth(new Date()),
                    $rootScope.goalInfo, goalInfoDef);
            }
        },

        clearMe: function() {
            meRecyclablePromise.recycle().reject();
            goalInfoRecyclablePromise.recycle().reject();
            $rootScope.me = undefined;
            $rootScope.isOrganizer = false;
            $rootScope.goalInfo = {};
        },

        initMandatoryFields: function(user) {
            if (mandatoryFieldsInitialized(user)) {
                return $q.when(user);
            } else {
                var modalInstance = $modal.open({
                    backdrop: "static",
                    templateUrl: 'template/kex/init-mandatory-user-fields-modal.html',
                    controller: 'InitMandatoryUserFieldsCtrl',
                    keyboard: false,
                    resolve:
                    {
                        user: function () {
                            return angular.extend({}, user);
                        }
                    }
                });
                return modalInstance.result
                    .then(
                        function(updatedUser) {
                            var updateDef = $q.defer();
                            // Save the updated user. We directly invoke the api
                            // vs. using the auth dependent Me since we are in the
                            // middle of auth while this is being processed.
                            var MeDirect = $resource('/api/me');
                            MeDirect.save(
                                null,
                                updatedUser,
                                function() {
                                    updateDef.resolve(updatedUser);
                                },
                                function() {
                                    // If the update fails just return the initial user object.
                                    updateDef.resolve(user);
                                } );

                            return updateDef.promise;
                        });
            }
        },

        getPrimaryEmailInfo: function(user) {
            for (var idx = 0; idx < user.registeredEmails.length; idx++) {
                var emailInfo = user.registeredEmails[idx];
                if (emailInfo.primary) {
                    return emailInfo;
                }
            }
            return null;
        }
    };

    return MeUtil;
});


kexApp.controller(
    'InitMandatoryUserFieldsCtrl',
    [
        '$scope', '$modalInstance', 'user', 'MeUtil',
        function($scope, $modalInstance, user, MeUtil) {

            initUser();

            $scope.done = function (mandatoryFieldsForm) {
                $scope.formSubmittedOnce = true;
                if ( !mandatoryFieldsForm.$invalid ) {
                    trimInputFields();
                    $modalInstance.close(user);
                }
            };

            function initUser() {
                $scope.user = user;
                var emailInfo = MeUtil.getPrimaryEmailInfo(user);
                if (emailInfo === null) {
                    emailInfo = {
                        email: undefined,
                        isPrimary: true
                    };
                    user.registeredEmails.push(emailInfo);
                }
                $scope.primaryEmailInfo = emailInfo;
            }

            function trimInputFields() {
                user.firstName = user.firstName.trim();
                user.lastName = user.lastName.trim();
                $scope.primaryEmailInfo.email =
                    $scope.primaryEmailInfo.email.trim();
            }
        }
    ]);

kexApp.factory('KarmaGoalUtil', function($rootScope, $q, User, KexUtil) {
    return {
        loadKarmaGoalInfo: function(user, firstOfMonth, goalInfo, goalInfoDef) {
            var goalStartDate = firstOfMonth;
            var goalEndDate = KexUtil.addMonths(firstOfMonth, 1);

            User.get(
                { id : user.key, resource : 'event', type: "INTERVAL",
                  start_time: goalStartDate.valueOf(), end_time: goalEndDate.valueOf() },
                angular.bind(this, function(result) {
                    var totalKarmaPoints = 0;
                    var completedKarmaPoints = 0;
                    for (var idx = 0; idx < result.data.length; idx++) {
                        var event = result.data[idx];
                        // console_log("Goal tracker processing[%s]: event=%s, reg=%s, date=%s, status=%s, duration=%s",
                        //     user.firstName, event.title, event.userEventSearchInfo.registrationInfo, new Date(event.startTime), event.status,
                        //     KexUtil.toHours(event.karmaPoints, 0));
                        if ((event.userEventSearchInfo.registrationInfo == 'ORGANIZER') ||
                                (event.userEventSearchInfo.registrationInfo == 'REGISTERED') ) {
                            totalKarmaPoints += event.karmaPoints;
                            if (event.status == 'COMPLETED') {
                                completedKarmaPoints += event.karmaPoints;
                            }
                        }
                    }

                    goalInfo.goalStartDate = goalStartDate;
                    goalInfo.goalEndDate = goalEndDate;

                    goalInfo.upcomingKarmaPoints = totalKarmaPoints - completedKarmaPoints;
                    goalInfo.completedKarmaPoints = completedKarmaPoints;

                    this.updateKarmaGoalTarget(user, goalInfo);

                    if (goalInfoDef) {
                        goalInfoDef.resolve(goalInfo);
                    }
                }),
                function() {
                    if (goalInfoDef) {
                        goalInfoDef.reject();
                    }
                } );
        },

        updateCurrentUserKarmaGoalTarget: function(upcomingPtsDelta, upcomingEventDate) {
            this.updateKarmaGoalTarget($rootScope.me, $rootScope.goalInfo,
                upcomingPtsDelta, upcomingEventDate);
        },

        updateKarmaGoalTarget: function(user, goalInfo, upcomingPtsDelta, upcomingEventDate) {
            function capPercentage(pctValue, totalPct) {
                pctValue = Math.round(pctValue);
                if (pctValue + totalPct > 100) {
                    return 100 - totalPct;
                } else {
                    return pctValue;
                }
            }

            function getGoalMsg(totalPct) {
                var msgs;
                if (totalPct === 0) {
                    msgs = ["Volunteering is fun! Sign up for an event"];
                } else if (totalPct < 25) {
                    msgs = ["Ready to earn some more karma?"];
                } else if (totalPct < 75) {
                    msgs = ["Nice job so far"];
                } else if (totalPct < 100) {
                    msgs = ["Almost there!"];
                } else  {
                    msgs = ["High five!", "Karma goal achieved!"];
                }
                return KexUtil.selectRandom(msgs);
            }

            var monthlyGoal = user.karmaGoal.monthlyGoal;

            if (upcomingPtsDelta && upcomingEventDate) {
                if (KexUtil.getFirstOfMonth(upcomingEventDate).getTime() ==
                        goalInfo.goalStartDate.getTime()) {
                    goalInfo.upcomingKarmaPoints =
                        Math.max(0, goalInfo.upcomingKarmaPoints + upcomingPtsDelta);
                }
            }

            goalInfo.registeredForKarmaPoints = goalInfo.upcomingKarmaPoints + goalInfo.completedKarmaPoints;

            goalInfo.upcomingKarmaHours = KexUtil.toHours(goalInfo.upcomingKarmaPoints, 1);
            goalInfo.completedKarmaHours = KexUtil.toHours(goalInfo.completedKarmaPoints, 1);
            goalInfo.registeredForKarmaHours = goalInfo.upcomingKarmaHours + goalInfo.completedKarmaHours;

            var totalPct = 0;
            var completedPct = capPercentage((goalInfo.completedKarmaPoints * 100) / monthlyGoal, totalPct);
            totalPct += completedPct;
            var upcomingPct = capPercentage((goalInfo.upcomingKarmaPoints * 100) / monthlyGoal, totalPct);
            totalPct += upcomingPct;

            goalInfo.msg = getGoalMsg(totalPct);
            goalInfo.pctTotal = totalPct;
            goalInfo.pctCompleted = completedPct;
            goalInfo.pctUpcoming = upcomingPct;
            goalInfo.goalHours = KexUtil.toHours(monthlyGoal, 1);
            goalInfo.barType = this.getGoalBarType(totalPct);
        },

        completionIconStyle: function (registeredPct) {
            if (!angular.isDefined(registeredPct)) {
                registeredPct = 0;
            }
            var ICON_SIZE = 30;
            return {
                clip: 'rect(' + Math.round(ICON_SIZE * (100 - registeredPct) / 100) +
                        'px, 200px, 200px, 0px)'
            };
        },

        getGoalBarType: function(registeredPct) {
            if (registeredPct < 25) {
                return 'danger';
            } else if (registeredPct < 75) {
                return 'warning';
            } else {
                return 'success';
            }
        }

    };
});

kexApp.factory('FbApiCache', function($rootScope) {
    var cache = {};
    var firstUserChange = true;

    $rootScope.$on( "fbUtil.userChanged", function (event) {
        if (!firstUserChange) {
            cache = {};
        }
        firstUserChange = false;
    });

    return {
        lookup: function (key) {
            if (angular.isUndefined(cache[key])) {
                return undefined;
            }
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
            }
            return result + ")";
        }
    };
});

kexApp.factory('FbUtil', function($rootScope, $facebook, $location,
        $window, FbApiCache, $q, $log) {
    var firstAuthResponse = true;
    var fbUserId;

    $rootScope.$on("fb.auth.authResponseChange", function ( event, response ) {
        if ( response.status === 'connected' ) {
            var updateUser = fbUserId != response.authResponse.userID;
            fbUserId = response.authResponse.userID;

            if ( updateUser ) {
                processUserChange(response);
            }
        } else {
            var userLoggedOut = false;
            if ( fbUserId ) {
                userLoggedOut = true;
                fbUserId = undefined;
            }
            if ( userLoggedOut || firstAuthResponse ) {
                processUserChange(response);
            }
        }
        firstAuthResponse = false;

        if ( response.error ) {
            // Ideally we shouldn't show this alert and just silently fail the loginStatus.
            // However, it appears doing Fb.login after facebook has ignored the
            // getLoginStatus sometimes results in a dialog box that just hangs.
            // Need to dig into this more.
            $rootScope.showAlert('Error connecting to facebook: ' + response.error, "danger");
            $log.warn("Error connecting to facebook: %o", response.error);
        }
    });

    function processUserChange( response ) {
        // Fired first time user information is available (post FB.init) and any
        // subsequent time that it changes.
        $rootScope.$broadcast("fbUtil.userChanged", response);
    }

    function isLoggedIn() {
        return !!fbUserId;
    }

    return {
        GRAPH_API_URL: "//graph.facebook.com",

        getImageUrl: function (id, type) {
            return angular.isDefined(id) ? ("//graph.facebook.com/" + id + "/picture?type=" + type) : undefined;
        },

        // Sometimes response.error is passed to this function.
        toLoginRequest: function(response) {
            if (  angular.isDefined(response.status) &&
                  (response.status === 'connected') &&
                  response.authResponse) {
                return {
                    providerType: 'FACEBOOK',
                    credentials: {
                        token: response.authResponse.accessToken
                    }
                };
            } else {
                return null;
            }
        },

        logout: function () {
            if (isLoggedIn()) {
                return $facebook.logout();
            } else {
                return $q.when();
            }
        },

        getAlbumCoverUrl: function (albumId) {
            var cacheKey = FbApiCache.key("getAlbumCoverUrl", albumId);
            var promise = FbApiCache.lookup(cacheKey);
            if (promise !== undefined) {
                return promise;
            }

            promise = $facebook.api("/" + albumId).then(function (response) {
                return $facebook.api("/" + response.cover_photo);
            }).then(function (response) {
                return response.source;
            });

            FbApiCache.update(cacheKey, promise);
            return promise;
        }
    };
});


kexApp.factory('PersonaUtil', function($q, $rootScope) {

    var loginAssertion;

    navigator.id.watch({
        loggedInUser: undefined,
        onlogin: function(assertion) {
            var authChanged = assertion != loginAssertion;
            loginAssertion = assertion;

            if (authChanged) {
                $rootScope.$broadcast("PersonaUtil.authChanged");
                // Persona events are not angular events.
                if (!$rootScope.$$phase) { $rootScope.$apply(); }
            }
        },
        onlogout: function() {
            var authChanged = !!loginAssertion;
            loginAssertion = undefined;
            // Our session is independent from the personna session.
            // So we don't log out when persona is logged out.

            if (authChanged) {
                $rootScope.$broadcast("PersonaUtil.authChanged");
                // Persona events are not angular events.
                if (!$rootScope.$$phase) { $rootScope.$apply(); }
            }
        },
        onready: function() {
            $rootScope.$broadcast("PersonaUtil.initialized");
            // Persona events are not angular events.
            if (!$rootScope.$$phase) { $rootScope.$apply(); }
        }
    });

    var PersonaUtil = {
        // Must be called on a click event
        login: function() {
            navigator.id.request({
                siteName: 'Karma Exchange',
                siteLogo: '/img/kex-logo-1024.png'
            });
        },

        logout: function() {
            if (loginAssertion) {
                navigator.id.logout();
            }
        },

        getLoginRequest: function() {
            if (  loginAssertion ) {
                return {
                    providerType: 'MOZILLA_PERSONA',
                    credentials: {
                        token: loginAssertion
                    }
                };
            } else {
                return null;
            }
        }
    };
    return PersonaUtil;
});


kexApp.factory('EventUtil', function($q, $rootScope, User, Events, KexUtil, KarmaGoalUtil, MeUtil) {
    return {
        postRegistrationTasks: function(event, participantType) {
            this._postRegistrationTasks(event, participantType, true);
        },

        postUnRegistrationTasks: function(event, participantType) {
            this._postRegistrationTasks(event, participantType, false);
        },

        _postRegistrationTasks: function(event, participantType, isRegisterAction) {
            // Organizers don't go through this flow.
            if (participantType == 'REGISTERED') {

                // Make sure goalInfo() is available to update. Must check this since
                // this routine can be called immediately after login.
                MeUtil.goalInfo().then( function() {
                    KarmaGoalUtil.updateCurrentUserKarmaGoalTarget(
                        isRegisterAction ? event.karmaPoints : -event.karmaPoints,
                        new Date(event.startTime));
                });

                if (isRegisterAction) {
                    $rootScope.openShareEventModal(event, "Thank you for volunteering!");
                }

            }
        },

        canWriteReview: function (event) {
            // The registration info is computed with respect to the current user's
            // key. Also, organizers can not write reviews.
            return angular.isDefined(event) && event.registrationInfo == 'REGISTERED';
        },
        getImpactViewUrl: function (event) {
            // TODO(avaliani): the impact url should be different from the event details url.
            return KexUtil.getBaseUrl() + '/#!/event/' + event.key;
        }
    };
});

/*
 * App directives
 */

/*
    ``btn-loading`` attribute.

    This attribute will update the button state using Twitter Bootstrap button plugin and
    according the attribute value.

    The attribute value should be a scope variable.
    If the variable is ``true`` the button will have the ``loading`` state.
    If the variable is ``false`` the button will be reset and displayed normaly.

        Usage:
            <button class="btn" btn-loading="is_loading" data-loading-text="Save in progess ..">Save</button>

    Source: https://gist.github.com/Yukilas/3979293
*/
kexApp.directive("btnLoading", function () {
    return function (scope, element, attrs) {
        scope.$watch(
            function () {
                return scope.$eval(attrs.btnLoading);
            },
            function (loading) {
                if (loading) {
                    return element.button("loading");
                }
                element.button("reset");
            });
    }
});

/*
 * This directive opens all links in an iframe view in a new window / tab
 * since there is no navigation in an iframe.
 */
kexApp.directive('a', function ($rootScope, IframeUtil) {
    return {
        restrict: 'E',
        link: function (scope, element, attrs) {
            // Skip links that have an explicit target specified.
            if (!('target' in attrs)) {
                var target = IframeUtil.getLinkTarget();
                if (target != '_self') {
                    element.attr('target', target);
                }
            }
        }
    };
 });

/* ng-focus was not available in 1.0.8 */
kexApp.directive('ngExFocus', ['$parse', function($parse) {
  return function(scope, element, attr) {
    var fn = $parse(attr['ngExFocus']);
    element.bind('focus', function(event) {
      scope.$apply(function() {
        fn(scope, {$event:event});
      });
    });
  }
}]);

kexApp.directive('stopClickPropagation', function () {
    return {
        restrict: 'A',
        link: function (scope, element, attr) {
            element.bind('click', function (e) {
                e.stopPropagation();
            });
        }
    };
 });

kexApp.directive('dropdownInput', function ($q, $rootScope) {
    return {
        restrict: 'E',
        scope: {
            paceholder: '@',
            inputDisplayValue: '=',
            inputSubmit: '&'
        },
        replace: true,
        transclude: false,
        link: function (scope, element, attr) {
            scope.isMobileDeviceOrTablet = $rootScope.isMobileDeviceOrTablet;

            scope.inputParseTracker = new PromiseTracker();

            var dropdownInputElement = element.find("input").first();
            var dropdownToggleElement = element.find(".dropdown-toggle").first();

            dropdownInputElement.bind('keydown', enterKeyBind);
            function enterKeyBind(evt) {
                if (( evt.which === 13 )) {
                    scope.submit();
                    // The keydown event is not an angular event.
                    if (!$rootScope.$$phase) { $rootScope.$apply(); }
                }
            };
            scope.submit = function() {
                if (scope.inputValue) {
                    var submitResult = scope.inputSubmit({value: scope.inputValue});
                    scope.inputParseTracker = new PromiseTracker(submitResult);
                    submitResult.then(
                        function() {
                            closeDropdown();
                        },
                        function(errorMsg) {
                            scope.errorMsg = errorMsg;
                            // TODO(avaliani): input element is disabled so can't set focus.
                            // dropdownInputElement.focus();
                        })
                }
            }

            scope.$watch(
                function() {return element.attr('class'); },
                function(newValue, oldValue) {
                    if (angular.isDefined(newValue) &&
                        isToggleOpen(newValue) &&
                        ( !angular.isDefined(oldValue) ||
                          !isToggleOpen(oldValue))) {

                        dropdownInputElement.focus();

                    }
                });
            function isToggleOpen(classStr) {
                var classes = classStr.split(/\s+/);
                for (var wIdx = 0; wIdx < classes.length; wIdx++) {
                    if (classes[wIdx] === 'open') {
                        return true;
                    }
                }
                return false;
            }

            function closeDropdown() {
                if (dropdownToggleElement.css('display') !='none') {
                    scope.inputValue = "";
                    scope.errorMsg = "";
                    dropdownToggleElement.trigger( "click" );
                }
            }

        },
        templateUrl: 'template/kex/dropdown-input.html'
    };
 });

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
                scope.gPlace = new google.maps.places.Autocomplete( element[ 0 ], options );
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
        };
} );
kexApp.directive( "gallery", function( ) {
        return function( scope, element, attrs ) {
            var doStuff = function( element, attrs ) {
                $( element ).plusGallery( scope.$eval( attrs.fbgallery ) );
            };
            scope.$watch( attrs.userid, doStuff( element, attrs ) );
            // scope.$watch(attrs.testTwo, doStuff(element,attrs));

        };
} );
kexApp.directive( 'unfocus', function( ) {
    return {
        restrict : 'A',
        link : function( scope, element, attribs ) {
            element[ 0 ].focus( );
            element.bind( "blur", function( ) {
                    scope.$apply( attribs.unfocus );
            } );
        }
    };
} );

kexApp.directive( "eventrepeat", function( ) {
        return function( scope, element, attrs ) {
                $( element ).recurrenceinput();
        };
} );


kexApp.directive('shareButtons', function(KexUtil) {
        return {
            restrict: 'E',
            scope: {
                type: '=',
                title: '=',
                description: '=',
                image: '='
            },
            replace: true,
            transclude: false,
            link: function (scope, element, attrs) {
                scope.KexUtil = KexUtil;
            },
            template:
                '<div>' +
                    '<ul ng-social-buttons ' +
                            'url="KexUtil.getLocation()" ' +
                            'title="title" ' +
                            'description="description" ' +
                            'image="image">' +
                        '<li class="ng-social-facebook">Facebook</li>' +
                        '<li class="ng-social-twitter">Twitter</li>' +
                        '<li class="ng-social-google-plus">Google+</li>' +
                    '</ul>' +
                '</div>'
        };
    });

kexApp.directive('eventParticipantImgsMini', function(KexUtil) {
    return {
        restrict: 'E',
        scope: {
            event: '='
        },
        replace: true,
        transclude: false,
        link: function (scope, element, attrs) {
            scope.KexUtil = KexUtil;
        },
        template:
            '<ul class="list-inline">' +
                '<li ng-repeat="cachedParticipant in event.cachedParticipants">' +
                    '<a href="#!/user/{{cachedParticipant.key}}" stop-click-propagation class="thumbnail-rounded-corners">' +
                        '<img ng-src="{{KexUtil.getUserImageUrl(cachedParticipant, \'mini\')}}"' +
                        '   class="kex-thumbnail-user-mini">' +
                    '</a>' +
                '</li>' +
            '</ul>'
    };
});

kexApp.directive('eventRegistrationInfo', function() {
    return {
        restrict: 'E',
        scope: {
            type: '=',
            registrationInfo: '=',
            userRegistrationInfo: '='
        },
        replace: true,
        transclude: false,
        link: function (scope, element, attrs) {
            scope.showLabel = false;
            scope.$watch('type', function() {
                updateLabel();
            });
            scope.$watch('registrationInfo', function() {
                updateLabel();
            });
            scope.$watch('userRegistrationInfo', function() {
                updateLabel();
            });

            var registrationInfoMapping = {
                ORGANIZER: { text: 'Organizer', labelClass: 'label-success',
                    type: ['UPCOMING', 'USER-PAST', 'USER-UPCOMING', 'ORG-PAST', 'ORG-UPCOMING', 'DETAILS']},
                REGISTERED: { text: 'Registered', labelClass: 'label-success',
                    type: ['UPCOMING', 'ORG-PAST', 'ORG-UPCOMING', 'DETAILS']},
                WAIT_LISTED: { text: 'Waitlisted', labelClass: 'label-warning',
                    type: ['UPCOMING', 'USER-UPCOMING', 'ORG-UPCOMING', 'DETAILS']},
                CAN_WAIT_LIST: { text: 'Waitlist Open', labelClass: 'label-warning',
                    type: ['UPCOMING', 'ORG-UPCOMING']}
            };
            function updateLabel() {
                if (scope.type && scope.registrationInfo) {
                    var registrationInfo =
                        scope.userRegistrationInfo ? scope.userRegistrationInfo : scope.registrationInfo;
                    var mapping = registrationInfoMapping[registrationInfo];
                    if (mapping && ($.inArray(scope.type, mapping.type) != -1)) {
                        scope.showLabel = true;
                        scope.labelText = mapping.text;
                        scope.labelClass = 'label kex-bs-label ' + mapping.labelClass;
                    } else {
                        scope.showLabel = false;
                    }
                } else {
                    scope.showLabel = false;
                }
            }
        },
        template:
            '<span ng-class="labelClass" ng-show="showLabel">{{labelText}}</span>'
    };
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
            '<span>' +
                '<rating-ex value="userRating.value" max="5" readonly="false" on-update="updateUserRating(newValue)"></rating-ex>' +
            '</span>'
    };
});

kexApp.directive('aggregateRating', function() {
    return {
        restrict: 'E',
        scope: {
            value: '='
        },
        replace: true,
        transclude: false,
        template:
            '<span>' +
                '<rating value="value.value" max="5" readonly="true"></rating>' +
                ' ({{value.count}})' +
            '</span>'
    };
});

kexApp.directive('kexFbComments', function($facebook, $rootScope) {
    return {
        restrict: 'E',
        scope: {
            href: '@',
            numPosts: '@'
        },
        replace: true,
        transclude: false,
        compile: function(element, attrs) {
            if (!attrs.numPosts) { attrs.numPosts = 3; }

            return function (scope, element, attrs) {
                function execWhenAttrsResolved(attrNames, execCb) {

                    function createObserveCb(idx) {
                        return function() {
                            resolvedAttrs.set(idx, true);
                            if (resolvedAttrs.count() == attrNames.length) {
                                execCb();
                            }
                        };
                    }

                    var resolvedAttrs = new BitArray(attrNames.length);
                    for (var idx=0; idx < attrNames.length; idx++) {
                        attrs.$observe(attrNames[idx], createObserveCb(idx));
                    }
                }

                execWhenAttrsResolved(['href', 'numPosts'], function() {
                    $facebook.promise.then( function(FB) {
                        element.html('<div class="fb-comments" href="' + scope.href + '" ' +
                            'num-posts="' + scope.numPosts + '"></div>');
                        FB.XFBML.parse(element[0]);
                    });
                });
            };
        },
        template: '<div></div>'
    };
});

kexApp.directive('impactTimeline', function(FbUtil, EventUtil, User,
        Events, UserManagedEvents, KexUtil, ElementSourceFactory, $q) {
    return {
        restrict: 'E',
        scope: {
            org: '=',
            user: '=',
            visible: '='
        },
        replace: true,
        transclude: false,
        templateUrl: 'template/kex/impact-timeline.html',
        link: function (scope, element, attrs) {
            scope.FbUtil = FbUtil;
            scope.EventUtil = EventUtil;
            scope.KexUtil = KexUtil;

            var PAGE_LIMIT = 20;

            var setupDef = $q.defer();
            scope.timelineSetupTracker = new PromiseTracker(setupDef.promise);

            var loaded = false;
            scope.$watch('visible', loadTimeline);
            scope.$watch('user', loadTimeline);
            scope.$watch('org', loadTimeline);

            var displayLimit = PAGE_LIMIT; // The current number of visible timeline elements.

            var displayableEvents = []; // All the displayable events.
            var displayableEventsMap = {}; // A map containing every displayed elment.

            // Processed events. This is different from the displayable events because
            // in order for the html to be correct we need filler / blank events (couldn't
            // figure out a way around this).
            scope.processedEvents = [];
            scope.lastProcessedEvent = undefined;
            var now = new Date().getTime();

            scope.hasMoreEvents = false;
            scope.hasMoreEventsProcessing = true;

            var userCreatedEventsSource =
                ElementSourceFactory.createSortedArrayElementSource(eventDisplayOrderCmp);

            function loadTimeline() {
                if (!loaded && scope.visible && (scope.user || scope.org)) {
                    loaded = true;
                    setupDef.resolve();

                    if (scope.user) {
                        scope.timelineType = 'USER';
                        scope.selfProfileView = (scope.user.permission === 'ALL');

                        var pastEventsPromise =
                            User.get(
                                {
                                    id : scope.user.key,
                                    resource : 'event',
                                    type : 'PAST',
                                    limit: PAGE_LIMIT + 1
                                });

                        var pr1 = ElementSourceFactory.createPagedElementSource(
                            pastEventsPromise, processPastEvent);

                        var userManagedEventsPromise = UserManagedEvents.get(
                                {
                                    userId : scope.user.key,
                                    type: 'DESCENDING',
                                    limit: PAGE_LIMIT + 1
                                });

                        var pr2 = ElementSourceFactory.createPagedElementSource(
                            userManagedEventsPromise, null);

                        // Combine the two event sources.
                        scope.pagedResult = ElementSourceFactory.createCompositeElementSource(
                            pr1, pr2, eventDisplayOrderCmp);

                        scope.timelineSetupTracker.track(pastEventsPromise);
                        scope.timelineSetupTracker.track(userManagedEventsPromise);
                    } else {
                        scope.timelineType = 'ORG';

                        var pastEventsPromise =
                            Events.get(
                                {
                                    type : "PAST",
                                    keywords : "org:" + scope.org.searchTokenSuffix,
                                    limit: PAGE_LIMIT + 1
                                });

                        scope.pagedResult = ElementSourceFactory.createPagedElementSource(
                            pastEventsPromise, processPastEvent);

                        scope.timelineSetupTracker.track(pastEventsPromise);
                    }

                    // Merge any dynamicly created events to the paged input queue. We use
                    // this to ensure that events don't show up between other events when
                    // someone clicks "show more".
                    scope.pagedResult = ElementSourceFactory.createCompositeElementSource(
                        scope.pagedResult, userCreatedEventsSource, eventDisplayOrderCmp);

                    updateDisplayableEvents();
                }
            }

            function processPastEvent(evt) {
                evt.managedEvent = true;
                if (evt.album) {
                    evt.album.coverPhotoUrl = FbUtil.getAlbumCoverUrl(evt.album.id);
                }
                if (!evt.currentUserRating) {
                    evt.currentUserRating = { value: undefined };
                }
                return evt;
            }

            function eventDisplayOrderCmp(ev1, ev2) {
                // if ev1 has a greater start time return a negative value
                return ev2.startTime - ev1.startTime;
            }

            function updateDisplayableEvents() {
                if (displayableEvents.length < displayLimit) {
                    scope.pagedResult.pop().then( function(event) {
                        if (event == null) {
                            updateHasMoreEvents();
                        } else {
                            // Prevent duplicate events from showing up. This can
                            // happen since to avoid doing a full timeline refresh we
                            // add dynamic events immediately to the timeline on save
                            // confirmation.
                            if (!displayableEventsMap[event.key]) {
                                appendEventToKarmaTimeline(event);
                            }

                            // Recurse to display the next element.
                            return updateDisplayableEvents();
                        }
                    });
                } else {
                    updateHasMoreEvents();
                }
            }

            function updateHasMoreEvents() {
                scope.pagedResult.peek().then( function(event) {
                    scope.hasMoreEventsProcessing = false;
                    scope.hasMoreEvents = (event != null);
                });
            }

            scope.fetchMoreEvents = function() {
                if (!scope.hasMoreEventsProcessing) {
                    displayLimit += PAGE_LIMIT;
                    scope.hasMoreEventsProcessing = true;
                    updateDisplayableEvents();
                }
            }


            function appendEventToKarmaTimeline(event) {
                displayableEvents.push(event);
                displayableEventsMap[event.key] = true;

                var idx = displayableEvents.length - 1;
                scope.processedEvents.push(event);
                scope.lastProcessedEvent = event;
                if ((idx % 2) == 1) {
                    // Spacer event.
                    scope.processedEvents.push({});
                }
            }

            function refreshKarmaTimelineEvents() {
                // At the time new events are added old events may no
                // longer be upcoming.
                now = new Date().getTime();

                scope.processedEvents = [];
                scope.lastProcessedEvent = undefined;
                for (var idx = 0; idx < displayableEvents.length; idx++) {
                    var event = displayableEvents[idx];
                    scope.processedEvents.push(event);
                    scope.lastProcessedEvent = event;
                    if ((idx % 2) == 1) {
                        // Spacer event.
                        scope.processedEvents.push({});
                    }
                }
            }

            scope.isUpcomingEvent = function(event) {
                return angular.isDefined(event.startTime) && (event.startTime > now);
            }


            scope.timelineUpdateTracker = new PromiseTracker();
            scope.expandNewEventForm = false;
            scope.newEvent = {};

            scope.post = function() {
                scope.newEventSubmitted = true;
                if (!scope.newEventForm.$invalid) {
                    var processedEvent = processNewEvent(scope.newEvent);
                    var userManagedEventUpdatePromise =
                        UserManagedEvents.save(
                            { userId : scope.user.key },
                            processedEvent,
                            getProcessNewEventSaveCb(processedEvent));

                    scope.timelineUpdateTracker.track(userManagedEventUpdatePromise);
                }
            }

            function processNewEvent(formEvent) {
                var dbEvent = {};
                dbEvent.title = formEvent.title;
                dbEvent.description = formEvent.description;
                var startTime, endTime;
                var karmaPoints = 0;
                if (formEvent.date) {
                    startTime = Date.parse(formEvent.date);
                } else {
                    startTime = new Date().getTime();
                }
                if (formEvent.karmaHours) {
                    karmaPoints = KexUtil.toKarmaPoints(formEvent.karmaHours);
                    endTime =
                        moment(startTime)
                        .add('hours', formEvent.karmaHours)
                        .toDate()
                        .getTime();
                } else {
                    endTime = startTime;
                }
                dbEvent.startTime = startTime;
                dbEvent.endTime = endTime;
                dbEvent.karmaPoints = karmaPoints;
                return dbEvent;
            }

            function getProcessNewEventSaveCb(processedEvent) {
                return function (value, getResponseHeaderCb) {
                    processedEvent.key = KexUtil.getSavedResourceKey(getResponseHeaderCb);
                    addNewEventToTimeline(processedEvent);

                    // On success the timeline will be updated. We should reset
                    // the form.
                    scope.newEventSubmitted = false;
                    scope.newEvent = {};
                    scope.expandNewEventForm=false;
                }
            }

            function addNewEventToTimeline(newEvent) {
                var eventAdded = false;
                if (displayableEvents.length > 0) {
                    var lastEl = displayableEvents[displayableEvents.length - 1];
                    if (eventDisplayOrderCmp(newEvent, lastEl) <= 0) {
                        eventAdded = true;

                        displayableEvents.orderedInsertExt(newEvent, eventDisplayOrderCmp);
                        displayableEventsMap[event.key] = true;

                        displayLimit++;
                        refreshKarmaTimelineEvents();
                    }
                }

                if (!eventAdded) {
                    userCreatedEventsSource.push(newEvent);
                    // At the very least update the has more button. Additionally
                    // however if the users page limit hasn't been reached then the
                    // new element can be immediately displayed.
                    updateDisplayableEvents();
                }
            }

            scope.delete = function(event) {
                KexUtil.createConfirmationModal("Are you sure you want to delete this post?")
                    .then( function() {
                        var evtIdx = displayableEvents.findIndexExt( function(element) {
                            return element.key === event.key;
                        });
                        if (evtIdx != -1) {
                            UserManagedEvents.delete(
                                {
                                    userId : scope.user.key,
                                    userManagedEventId : event.key
                                });

                            /* Assume the delete will be succesful and immediately delete the event. */
                            displayableEvents.splice(evtIdx, 1);
                            delete displayableEventsMap[event.key];
                            // Prevent excessive fetches by reducing the display limit. Never fetch
                            // more than PAGE_LIMIT per source.
                            displayLimit--;
                            refreshKarmaTimelineEvents();
                        }
                    });
            }

        }
    };
});

kexApp.directive('participantsSidebar', function() {
    return {
        restrict: 'E',
        scope: {
            fetchedParticipants: '=',
            header: '@',
            type: '@',
            participantCount: '=',
            fetchMore: '&'
        },
        replace: true,
        transclude: false,
        templateUrl: 'template/kex/participants-sidebar.html'
    };
});

kexApp.directive('orgEventSummary', function(FbUtil) {
    return {
        restrict: 'E',
        scope: {
            org: '='
        },
        replace: true,
        transclude: false,
        link: function (scope, element, attrs) {
            scope.FbUtil = FbUtil;
        },
        templateUrl: 'template/kex/org-event-summary.html'
    };
});

kexApp.directive('orgSummaryPanel', function(FbUtil, $rootScope) {
    return {
        restrict: 'E',
        scope: {
            org: '='
        },
        replace: true,
        transclude: false,
        link: function (scope, element, attrs) {
            scope.FbUtil = FbUtil;
            scope.setLocation = $rootScope.setLocation;
        },
        templateUrl: 'template/kex/org-summary-panel.html'
    };
});

kexApp.directive('eventParticipantSummary', function(KexUtil) {
    return {
        restrict: 'E',
        scope: {
            user: '=',
            type: '=',
            karmaPoints: '=',
            karmaPointsDisplayType: '@'
        },
        replace: true,
        transclude: false,
        compile: function(element, attrs) {
            if (!attrs.karmaPointsDisplayType) { attrs.karmaPointsDisplayType = 'BRIEF'; }

            return function (scope, element, attrs) {
                scope.KexUtil = KexUtil;

                function updateKarmaPoints() {
                    if (angular.isDefined(scope.karmaPoints)) {
                        scope.summaryKarmaPoints = scope.karmaPoints;
                    } else if (angular.isDefined(scope.user)) {
                        scope.summaryKarmaPoints = scope.user.karmaPoints;
                    }
                }

                scope.$watch('user', updateKarmaPoints);
                scope.$watch('karmaPoints', updateKarmaPoints);
            };
        },
        templateUrl: 'template/kex/event-participant-summary.html'
    };
});

kexApp.directive('leaderboardTable', function() {
    return {
        restrict: 'E',
        scope: {
            scores: '=',
            type: '='
        },
        replace: true,
        transclude: false,
        link: function (scope, element, attrs) {
            scope.headerText = {
                THIRTY_DAY: 'Last Thirty Days',
                ALL_TIME: 'All Time'
            };
        },
        templateUrl: 'template/kex/leaderboard-table.html'
    };
});

kexApp.directive('upcomingEvents', function(KexUtil, ElementSourceFactory, $q, User, Events) {
    return {
        restrict: 'E',
        scope: {
            org: '=',
            user: '=',
            visible: '='
        },
        replace: true,
        transclude: false,
        templateUrl: 'template/kex/upcoming-events.html',
        link: function (scope, element, attrs) {
            scope.setLocation = angular.bind(KexUtil, KexUtil.setLocation);

            var EVENTS_PER_PAGE = 20;

            var setupDef = $q.defer();

            var loaded = false;
            scope.$watch('visible', fetchUpcomingEvents);
            scope.$watch('user', fetchUpcomingEvents);
            scope.$watch('org', fetchUpcomingEvents);

            var now = new Date();
            var prevEventDate = new Date( 1001, 1, 1, 1, 1, 1, 0 );

            function fetchUpcomingEvents() {
                if (!loaded && scope.visible && (scope.user || scope.org)) {
                    loaded = true;
                    setupDef.resolve();

                    var eventsPromise;
                    if (scope.user) {
                        scope.searchType = 'USER';
                        eventsPromise = User.get(
                            {
                                type: "UPCOMING",
                                id: scope.user.key,
                                resource: 'event',
                                limit: EVENTS_PER_PAGE + 1
                            });
                    } else {
                        scope.searchType = 'ORG';
                        eventsPromise = Events.get(
                            {
                                type: "UPCOMING",
                                keywords: "org:" + scope.org.searchTokenSuffix,
                                limit: EVENTS_PER_PAGE + 1
                            });
                    }
                    scope.pagedEvents = ElementSourceFactory.createPagedElementView(
                        eventsPromise, processEvent, EVENTS_PER_PAGE);
                }
            }

            function processEvent(event) {
                var dateVal = new Date(event.startTime);
                var showHeader = (dateVal.getDate() != prevEventDate.getDate()) ||
                    (dateVal.getMonth() != prevEventDate.getMonth()) ||
                    (dateVal.getFullYear() != prevEventDate.getFullYear());
                prevEventDate = dateVal;
                event.dateFormat = (now.getFullYear() == dateVal.getFullYear()) ? 'EEEE, MMM d' : 'EEEE, MMM d, y';
                event.showHeader = showHeader;
                event.locationQuickSummary = '';
                if (event.location) {
                    if (event.location.title) {
                        event.locationQuickSummary = event.location.title;
                    }
                    if (event.location.address && event.location.address.city) {
                        if (event.locationQuickSummary) {
                            event.locationQuickSummary += ' ';
                        }
                        event.locationQuickSummary += 'in ' + event.location.address.city;
                        if (event.location.address.state) {
                            event.locationQuickSummary += ', ' + event.location.address.state;
                        }
                    }
                }
                return event;
            }
        }
    };
});

kexApp.directive('goalTrackingBar', function(KarmaGoalUtil) {
    return {
        restrict: 'E',
        scope: {
            pctCompleted: '=',
            pctPending: '='
        },
        replace: true,
        transclude: false,
        link: function (scope, element, attrs) {
            function updateBarType() {
                if (angular.isDefined(scope.pctCompleted) &&
                    angular.isDefined(scope.pctPending)) {

                    scope.barType =
                        KarmaGoalUtil.getGoalBarType(scope.pctCompleted + scope.pctPending);
                }
            }

            scope.$watch('pctCompleted', updateBarType);
            scope.$watch('pctPending', updateBarType);
        },
        templateUrl: 'template/kex/goal-tracking-bar.html'
    };
});


kexApp.directive('karmaBadge', function($rootScope) {
    return {
        restrict: 'E',
        scope: {
            badge: '='
        },
        replace: true,
        transclude: false,
        link: function (scope, element, attrs) {
            $rootScope.badges.then( function (badges) {
                scope.$watch('badge', function() {
                    if (angular.isDefined(scope.badge)) {
                        var badge = badges[scope.badge];
                        scope.iconUrl = badge.icon.url;
                        scope.label = badge.label;
                    }
                });
            });
        },
        templateUrl: 'template/kex/karma-badge.html'
    };
});

kexApp.directive('karmaBadgeSummary', function($rootScope) {
    return {
        restrict: 'E',
        scope: {
            badgeSummary: '='
        },
        replace: true,
        transclude: false,
        templateUrl: 'template/kex/karma-badge-summary.html'
    };
});

// Currently this adds an 's' if num is not one. When needed we
// can add a dictionary to the scope in order to properly pluralize
// different types.
kexApp.directive('numWithType', function() {
    return {
        restrict: 'E',
        scope: {
            num: '=',
            type: '='
        },
        replace: true,
        transclude: false,
        templateUrl: 'template/kex/num-with-type.html'
    };
});

kexApp.directive('floatGeqZero', function() {
  var FLOAT_GEQ_ZERO = /^((\d+(\.(\d+)?)?)|(\.\d+))$/;
  return {
    require: 'ngModel',
    link: function(scope, elm, attrs, ctrl) {
      ctrl.$parsers.unshift(function(viewValue) {
        // Let required take care of empty inputs
        if ((viewValue === undefined) || (viewValue === '')) {
            ctrl.$setValidity('floatGeqZero', true);
            return undefined;
        } else if (FLOAT_GEQ_ZERO.test(viewValue)) {
            // it is valid
            ctrl.$setValidity('floatGeqZero', true);
            return viewValue;
        } else {
            // it is invalid, return undefined (no model update)
            ctrl.$setValidity('floatGeqZero', false);
            return undefined;
        }
      });
    }
  };
});

kexApp.directive('karmaHours', function(KexUtil) {
    return {
        restrict: 'E',
        scope: {
            karmaPoints: '=',
            type: '@'
        },
        replace: true,
        transclude: false,
        link: function (scope, element, attrs) {
            function updateHours() {
                if (angular.isDefined(scope.karmaPoints)) {
                    var karmaHours = KexUtil.toHours(scope.karmaPoints, 1);
                    if (angular.isDefined(scope.type) && (scope.type == 'BRIEF')) {
                        scope.karmaHoursText = "+" + karmaHours + " karma";
                    } else {
                        if (karmaHours == 1) {
                            scope.karmaHoursText = "+1 karma hour";
                        } else {
                            scope.karmaHoursText = "+" + karmaHours + " karma hours";
                        }
                    }
                }
            }

            scope.$watch('karmaPoints', updateHours);
            scope.$watch('type', updateHours);
        },
        templateUrl: 'template/kex/karma-hours.html'
    };
});

kexApp.directive('loadingMessage', function($rootScope) {
    return {
        restrict: 'E',
        scope: {
            msg: '@'
        },
        replace: true,
        transclude: false,
        templateUrl: 'template/kex/loading-message.html'
    };
});

kexApp.directive('truncateAndLink', function($rootScope, KexUtil, IframeUtil) {
    return {
        restrict: 'E',
        scope: {
            text: '=',
            linkText: '@',
            href: '@',
            hrefCb: '&',
            limit: '@'
        },
        replace: true,
        transclude: false,
        templateUrl: 'template/kex/truncate-and-link.html',
        link: function (scope, element, attrs) {
            scope.$watch('text', updateText);
            scope.$watch('href', updateText);
            scope.$watch('limit', updateText);

            function updateText() {
                if (  angular.isDefined(scope.text) &&
                      ( angular.isDefined(scope.href) ||
                        ('hrefCb' in attrs) ) &&
                      angular.isDefined(scope.limit)  ) {
                    var result =
                        KexUtil.truncateToWordBoundary(scope.text, scope.limit);
                    scope.output = result.str;
                    scope.truncated = result.truncated;

                    if ('hrefCb' in attrs) {
                        scope.hrefCtx = scope.hrefCb({});
                    } else {
                        scope.hrefCtx = {
                            href: scope.href,
                            target: IframeUtil.getLinkTarget()
                        }
                    }
                }
            }
        }
    };
});

kexApp.directive('truncateWithToggle', function($rootScope, KexUtil) {
    return {
        restrict: 'E',
        scope: {
            text: '=',
            toggleExpandText: '@',
            limit: '@'
        },
        replace: true,
        transclude: false,
        link: function (scope, element, attrs) {
            scope.$watch('text', updateText);
            scope.$watch('limit', updateText);

            function updateText() {
                if (  angular.isDefined(scope.text) &&
                      angular.isDefined(scope.limit)  ) {
                    var result =
                        KexUtil.truncateToWordBoundary(scope.text, scope.limit);
                    scope.output = result.str;
                    scope.truncated = result.truncated;
                }
            }

            scope.expand = function() {
                scope.output = scope.text;
                scope.truncated = false;
            }
        },
        templateUrl: 'template/kex/truncate-with-toggle.html'
    };
});

/*
 * App controllers
 */

var meViewCtrl = function($scope, $location, User, Me, $rootScope, $routeParams,
        $modal, EventUtil, KexUtil, urlTabsetUtil, KarmaGoalUtil, MeUtil,
        UserManagedEvents, PageProperties, $q) {
    $scope.KexUtil = KexUtil;
    $scope.EventUtil = EventUtil;

    var tabManager = $scope.tabManager = urlTabsetUtil.createTabManager();
    tabManager.addTab('impact', { active: true, onSelectionCb: loadImpactTab });
    tabManager.addTab('upcoming', { active: false, onSelectionCb: loadUpcomingTab });
    tabManager.init();
    $scope.tabs = tabManager.tabs;

    var profileLoadedDef = $q.defer();

    profileLoadedDef.promise.then( function(user) {
        PageProperties.setContentTitle(user.firstName + " " + user.lastName);
        if (user.profileImage && user.profileImage.url) {
            PageProperties.setPageImage(user.profileImage.url + "?type=large");
        }
    })

    function load() {
        if ($location.path() == "/me") {
            $scope.who = 'My';
            MeUtil.me().then(function(meObj) {
                $scope.me = meObj;
                $scope.userKey = $scope.me.key;
                $scope.savedAboutMe = $scope.me.about;
                $scope.userGoalInfo = $rootScope.goalInfo;

                // Tab information depends on the user being fetched first.
                $scope.meLoaded = true;
                profileLoadedDef.resolve($scope.me);
                tabManager.reloadActiveTab();
                postUserKeyResolutionCbs();

            });
        } else {
            $scope.userKey = $routeParams.userId;
            // We shouldn't pick up $rootScope.me.
            $scope.me = undefined;
            User.get(
                {
                    id: $scope.userKey
                },
                function(result) {
                    $scope.me = result;
                    $scope.who = $scope.me.firstName + "'s";
                    $scope.userGoalInfo = {};
                    KarmaGoalUtil.loadKarmaGoalInfo($scope.me, KexUtil.getFirstOfMonth(new Date()), $scope.userGoalInfo);

                    // Tab information depends on the user being fetched first.
                    $scope.meLoaded = true;
                    profileLoadedDef.resolve($scope.me);
                    tabManager.reloadActiveTab();
                });
            postUserKeyResolutionCbs();
        }
    }

    function postUserKeyResolutionCbs() {
        getOtherData();
    }

    $scope.editKarmaGoal = function() {
        var modalInstance = $modal.open({
            backdrop: false,
            templateUrl: 'template/kex/karma-goal-modal.html',
            controller: KarmaGoalModalInstanceCtrl
        });

        modalInstance.result.then(function() {
            KarmaGoalUtil.updateCurrentUserKarmaGoalTarget();
        });
    };

    function getOtherData() {
        User.get(
            {
                id: $scope.userKey,
                resource: 'org'
            },
            function(result) {
                $scope.orgs = result;
            });
    }

    function loadImpactTab() {
        if (!$scope.loadKarmaTab && $scope.meLoaded) {
            $scope.loadKarmaTab = true;
        }
    }

    function loadUpcomingTab() {
        if (!$scope.loadUpcomingEventsTab && $scope.meLoaded) {
            $scope.loadUpcomingEventsTab = true;
        }
    }

    $scope.enableIntroEdit = function() {
        if ($scope.me.permission === "ALL") {
            $scope.edit = true;
        }
    };

    $scope.saveIntroEdit = function() {
        $scope.edit = false;
        $scope.savedAboutMe = $scope.me.about;
        User.save(
            {
                id: $scope.me.key
            },
            $scope.me);
    };

    $scope.disableIntroEdit = function() {
        $scope.edit = false;
        $scope.me.about = $scope.savedAboutMe;
    };

    load();
};

var KarmaGoalModalInstanceCtrl = function ($scope, $modalInstance, $rootScope,
        KexUtil, Me, MeUtil) {
    $scope.saveDisabled = true;

    MeUtil.me().then(function(meObj) {
        $scope.saveDisabled = false;
        $scope.goal = { hours: KexUtil.toHours(meObj.karmaGoal.monthlyGoal, 1) };
        $scope.meObj = meObj;
    });

    $scope.save = function () {
        $scope.saveDisabled = true;
        // TODO(avaliani): if me has recycled, ideally the modal should close. See if
        //   url path changes force the modal to close.
        var meObj = $scope.meObj;
        var originalMonthlyGoal = meObj.karmaGoal.monthlyGoal;
        meObj.karmaGoal.monthlyGoal = KexUtil.toKarmaPoints(Number($scope.goal.hours));
        Me.save(meObj,
            function() {
                $modalInstance.close();
            },
            function() {
                meObj.karmaGoal.monthlyGoal = originalMonthlyGoal;
                $modalInstance.dismiss();
            });
    };

    $scope.cancel = function () {
        $modalInstance.dismiss();
    };
};

var meEditCtrl = function($scope, Me, $rootScope, MeUtil) {

    $scope.load = function() {
        $scope.who = 'My';
        MeUtil.me().then(function(meObj) {
            $scope.me = meObj;
            $scope.origAboutMe = $scope.me.about;
            var primaryEmailInfo = MeUtil.getPrimaryEmailInfo(meObj);
            $scope.primaryEmail = primaryEmailInfo.email;
        });
    };
    $scope.save = function() {
        Me.save($scope.me);
    };


    // $scope.newMail = {
    //     email: null,
    //     primary: null
    // };
    // $scope.addEmail = function() {
    //     //TO-DO check if the new one is marked primary and unmark the current primary one
    //     $scope.me.registeredEmails.push($scope.newMail);
    //     $scope.save();
    // };
    // $scope.removeEmail = function() {
    //     $scope.me.registeredEmails.splice(this.$index, 1);
    //     $scope.save();
    // };
    // $scope.mailPrimaryIndex = {
    //     index: 0
    // };
    $scope.load();
};

var orgDetailCtrl = function($scope, $location, $routeParams, $rootScope, $http, Org,
        Events, FbUtil, KexUtil, EventUtil, urlTabsetUtil, $facebook, PageProperties) {
    $scope.FbUtil = FbUtil;
    $scope.KexUtil = KexUtil;

    var tabManager = $scope.tabManager = urlTabsetUtil.createTabManager();
    tabManager.addTab('impact', { active: true, onSelectionCb: loadImpactTab });
    tabManager.addTab('upcoming', { active: false, onSelectionCb: loadUpcomingTab });
    tabManager.addTab('calendar', { active: false, onSelectionCb: loadCalendarTab });
    tabManager.addTab('topVolunteers', { active: false, onSelectionCb: loadTopVolunteersTab });
    tabManager.init();
    $scope.tabs = tabManager.tabs;
    $scope.dynamicTabs = [];

    $scope.calendarEventSources = [];
    $scope.calendarUiConfig = {
        calendar: {
            height: 450,
            editable: false,
            header: {
                left: 'month agendaWeek agendaDay',
                center: 'title',
                right: 'today prev,next'
            }
        }
    };

    var orgPromise = Org.get(
        { id: $routeParams.orgId },
        function(result) {
            $scope.org = result;
            $scope.orgLoaded = true;
            $facebook.api("/" + $scope.org.page.name).then(function(response) {
                $scope.fbPage = response;
                if ($scope.fbPage.cover && $scope.fbPage.cover.source) {
                    PageProperties.setPageImage($scope.fbPage.cover.source);
                }
            });

            if ($scope.org.parentOrg) {
                Org.get(
                    { id: $scope.org.parentOrg.key },
                    function(result) {
                        $scope.parentOrg = result;
                    });
            }

            // Disable manageOrg tab for now until we have the opportunity to clean up the ui.
            //
            // if ($scope.org.permission === "ALL") {
            //     tabManager.addTab('manageOrg', { active: false, onSelectionCb: loadManageOrgTab });
            //     $scope.dynamicTabs.push({
            //         heading: '<i class="icon-cogs"></i> Manage Organization',
            //         contentUrl: 'partials/manage-org-tab.html',
            //         tabName: 'manageOrg',
            //     });
            // }

            PageProperties.setContentTitle($scope.org.orgName);

            tabManager.reloadActiveTab();
        });

    $scope.calendarEventsFetchTracker = new PromiseTracker(orgPromise);
    $scope.topVolunteersFetchTracker = new PromiseTracker(orgPromise);

    Org.get(
        {
            id: $routeParams.orgId,
            resource: "children"
        },
        function(result) {
            $scope.childOrgs = result;
        });

    function loadImpactTab() {
        if (!$scope.loadKarmaTab && $scope.orgLoaded) {
            $scope.loadKarmaTab = true;
        }
    }

    function loadUpcomingTab() {
        if (!$scope.loadUpcomingEventsTab && $scope.orgLoaded) {
            $scope.loadUpcomingEventsTab = true;
        }
    }

    function loadCalendarTab() {
        function renderCalendar() {
            // This variable is set by the calendar directive which is in
            // a child scope from this controller! In theory there should be no
            // timing issue because a query is required to fetch the event
            // calendar which should give time for the linking and compiling
            // phases to complete and therefore the link function of the
            // calendar directive to execute.
            $scope.eventCalendar.fullCalendar('render');
        }
        if ($scope.calendarEventsLoaded) {
            renderCalendar();
        } else {
            // TODO(avaliani): investigate why deep watch doesn't auto render.
            loadCalendarEvents(renderCalendar);
        }
    }

    function loadCalendarEvents(action) {
        if (!$scope.calendarEventsLoaded && $scope.orgLoaded) {
            $scope.calendarEventsLoaded = true;
            // TODO(avaliani): technically calendar events should be fetched
            //   for the entire month being displayed. Past and future. So
            //   this code needs to be revisited.
            var calendarEventsPromise = Events.get(
                {
                    type: "UPCOMING",
                    keywords: "org:" + $scope.org.searchTokenSuffix
                },
                function(result) {
                    var calendarEvents = [];
                    angular.forEach(result.data, function(event) {
                        calendarEvents.push({
                            title: event.title,
                            start: (new Date(event.startTime)),
                            end: (new Date(event.endTime)),
                            allDay: false,
                            url: "/#!/event/" + event.key
                        });
                    });
                    $scope.calendarEventSources.push(calendarEvents);

                    if (action) {
                        action();
                    }
                });
            $scope.calendarEventsFetchTracker.track(calendarEventsPromise);
        }
    }

    function loadTopVolunteersTab() {
        if (!$scope.topVolunteersTabLoaded) {
            $scope.topVolunteersTabLoaded = true;
            $scope.topVolunteersFetchTracker.track(
                Org.get(
                    {
                        type: "ALL_TIME",
                        id: $routeParams.orgId,
                        resource: "leaderboard"
                    },
                    function(result) {
                        $scope.allTimeLeaders = parseLeaderboardFetch(result);
                    }));
            $scope.topVolunteersFetchTracker.track(
                Org.get(
                    {
                        type: "THIRTY_DAY",
                        id: $routeParams.orgId,
                        resource: "leaderboard"
                    },
                    function(result) {
                        $scope.lastMonthLeaders = parseLeaderboardFetch(result);
                    }));
        }

        function parseLeaderboardFetch(data) {
            if (!angular.isDefined(data) || !angular.isDefined(data.scores)) {
                return { scores: [] };
            } else {
                return data;
            }
        }
    }

    function loadManageOrgTab() {
        if (!$scope.manageOrgTabLoaded) {
            $scope.manageOrgTabLoaded = true;
            Org.get(
                {
                    membership_status: "PENDING",
                    id: $routeParams.orgId,
                    resource: "member"
                },
                function(result) {
                    $scope.pendingMembers = result;
                });
        }
    }
};

kexApp.controller('CalendarCtrl', [
    '$scope', '$log', 'Events', '$http', 'IframeUtil',
    function($scope, $log, Events, $http, IframeUtil) {

        var now = new Date().getTime();
        var calendarEvents = [];
        $scope.calendarEventSources = [calendarEvents];
        $scope.calendarUiConfig = {
            calendar: {
                // height: 450,
                editable: false,
                header: {
                    left: 'month agendaWeek agendaDay',
                    center: 'title',
                    right: 'today prev,next'
                },
                viewRender: function(view, element) {
                    if ($scope.eventRange) {
                        $scope.eventRange.updateVisibleEventRange(view.visStart, view.visEnd);
                    } else {
                        $scope.eventRange = new EventRange(view.visStart, view.visEnd, addEvent);
                    }
                }
            }
        };

        function addEvent(event) {
            // TODO(avaliani): does this handle multi-day events. Need to check.
            var eventHref = IframeUtil.getEventDetailsHref(event.key);
            calendarEvents.push({
                title: event.title,
                start: new Date(event.startTime),
                end: new Date(event.endTime),
                className: (event.startTime < now) ? "fc-event-past" : undefined,
                allDay: false,
                url: eventHref.href,
                urlTarget: eventHref.target
            });
        }

        function EventRange(start, end, addEventCb) {
            var FETCH_LIMIT = 200;
            var events = [];
            var eventsMap = {};

            start = start.getTime();
            end = end.getTime();
            var visStart = start;
            var visEnd = end;
            var fetchPending = false;

            this.updateVisibleEventRange = updateVisibleEventRange;
            this.isFetchPending = isFetchPending;

            fetchEvents(start, end);

            function fetchEvents(fetchStart, fetchEnd) {
                fetchPending = true;
                Events.get(
                    {
                        type: "INTERVAL",
                        start_time: fetchStart,
                        end_time: fetchEnd,
                        limit: FETCH_LIMIT
                        // keywords: "org:" + $scope.org.searchTokenSuffix
                    },
                    processFetchResultSuccess,
                    processFetchResultError);
            }

            function processFetchResultSuccess(result) {
                angular.forEach(result.data, addEventOrdered);

                if (result.paging && result.paging.next) {
                    $http.get( result.paging.next )
                        .success(processFetchResultSuccess)
                        .error(processFetchResultError);
                } else {
                    fetchPending = false;
                    processVisibleEventRangeUpdates();
                }
            }

            function processFetchResultError() {
                fetchPending = false;
            }

            function updateVisibleEventRange(newStart, newEnd) {
                visStart = newStart.getTime();
                visEnd = newEnd.getTime();
                if (!fetchPending) {
                    processVisibleEventRangeUpdates();
                }
            }

            function processVisibleEventRangeUpdates() {
                var fetchStart = visStart;
                var fetchEnd = visEnd;

                if (visStart >= start) {
                    // Handles both visStart being less than end and it being greater than end.
                    fetchStart = end;
                }

                if (visEnd <= end) {
                    fetchEnd = start;
                }

                if (fetchStart < fetchEnd) {
                    if (fetchStart < start) {
                        start = fetchStart;
                    }
                    if (fetchEnd > end) {
                        end = fetchEnd;
                    }
                    fetchEvents(fetchStart, fetchEnd);
                }
            }

            function addEventOrdered(event) {
                if (eventsMap[event.key]) {
                    return;
                }

                var lastEl = (events.length > 0) ?
                    events[events.length - 1] : undefined;

                var addedEvent = false;
                if (lastEl && (event.startTime < lastEl.startTime)) {
                    for (var evIdx = 0; evIdx < events.length; evIdx++) {
                        var curEl = events[evIdx];
                        if (event.startTime < curEl.startTime) {
                            events.splice(evIdx, 0, event);
                            addedEvent = true;
                            break;
                        }
                    }
                }
                if (!addedEvent) {
                    events.push(event);
                }

                eventsMap[event.key] = event;

                addEventCb(event);
            }

            function isFetchPending() {
                return fetchPending;
            }
        }
    }
]);

var orgCtrl = function( $scope, $location, $routeParams, $modal, Org, KexUtil ) {
    $scope.query = "";
    $scope.newOrg = { page : { url : null, urlProvider : "FACEBOOK" }};
    $scope.orgQueryTracker = new PromiseTracker();
    $scope.refresh = function( ) {
        $scope.orgQueryTracker.reset(
            Org.get(
                { name_prefix : $scope.query },
                function(result) {
                    $scope.orgs = result;
                    $scope.orgsGroupSize2 = KexUtil.group(result.data, 2);
                    $scope.orgsGroupSize3 = KexUtil.group(result.data, 3);
                }));
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
};
var eventsCtrl = function( $scope, $location, $routeParams, Events, $rootScope, KexUtil,
        EventUtil, FbUtil, RecyclablePromiseFactory, MeUtil, $q, Geocoder, $log,
        SnapshotServiceHandshake, SessionManager, ElementSourceFactory, IframeUtil ) {
    var EVENTS_PER_PAGE = 20;
    $scope.KexUtil = KexUtil;
    $scope.FbUtil = FbUtil;
    $scope.SessionManager = SessionManager;

    $scope.eventSearchTracker = new PromiseTracker();
    var eventSearchRecyclablePromise = RecyclablePromiseFactory.create();

    $scope.modelOpen = false;

    SnapshotServiceHandshake.disableSnapshotOnTimeout();
    eventSearchRecyclablePromise.promise.then( function() {
        SnapshotServiceHandshake.snapshot();
    });

    var now, prevEventDate;

    $scope.reset = function( numEventsToLoad ) {
        now = new Date();
        prevEventDate = new Date( 1001, 1, 1, 1, 1, 1, 0 );

        var eventSearchDef = eventSearchRecyclablePromise.recycle();
        $scope.eventSearchTracker.reset(eventSearchDef.promise);
        Events.get(
            {
                keywords: ($scope.query ? $scope.query : ""),
                distance: $scope.locationSearch.selectedDistance,
                lat: $scope.locationSearch.selectedLocation.latitude,
                lng: $scope.locationSearch.selectedLocation.longitude,
                limit: EVENTS_PER_PAGE + 1
            },
            function(value) {
                eventSearchDef.resolve(value);
            },
            function() {
                eventSearchDef.reject();
            });
        $scope.pagedEvents = ElementSourceFactory.createPagedElementView(
            eventSearchDef.promise, processEvent, EVENTS_PER_PAGE, numEventsToLoad);
    };

    function processEvent(event) {
        var dateVal = new Date(event.startTime);
        var showHeader = (dateVal.getDate() != prevEventDate.getDate()) ||
            (dateVal.getMonth() != prevEventDate.getMonth()) ||
            (dateVal.getFullYear() != prevEventDate.getFullYear());
        prevEventDate = dateVal;
        event.dateFormat = (now.getFullYear() == dateVal.getFullYear()) ? 'EEEE, MMM d' : 'EEEE, MMM d, y';
        event.showHeader = showHeader;
        event.isCollapsed = true;
        event.locationQuickSummary = '';
        if (event.location) {
            if (event.location.title) {
                event.locationQuickSummary = event.location.title;
            }
            if (event.location.address && event.location.address.city) {
                if (event.locationQuickSummary) {
                    event.locationQuickSummary += ' ';
                }
                event.locationQuickSummary += 'in ' + event.location.address.city;
                if (event.location.address.state) {
                    event.locationQuickSummary += ', ' + event.location.address.state;
                }
            }
        }

        if (event.key == getRouteExpandedEventKey()) {
            toggleEvent(event);
        }

        event.detailsHrefCb = function() {
            return IframeUtil.getEventDetailsHref(event.key);
        }

        return event;
    }

    var query = undefined;
    $scope.$watch('query', function( ) {
        if (query != $scope.query) {
            $scope.reset();
        }
    });

    $scope.$on("SessionManager.userChanged", function (event, response) {
        // Only dependency is the cookie identifying the user.
        var numElemsToReload = ($scope.pagedEvents.elements.length > 0) ?
            $scope.pagedEvents.elements.length : undefined;
        $scope.reset(numElemsToReload);
    });

    $scope.addMarker = function( markerLat, markerLng ) {
        $scope.markers.push( {
            latitude : parseFloat( markerLat ),
            longitude : parseFloat( markerLng )
        } );
    };
    $scope.register = function(event, type) {
        var eventId = event.key;

        var registrationActionDef = $q.defer();
        event.registrationActionTracker = new PromiseTracker(registrationActionDef.promise);

        SessionManager.loginRequired()
            .then(
                function () {
                    // The events array refreshes once a user logs in. Make sure the
                    // refresh is complete and all the elements that were previously loaded
                    // are reloaded.
                    // TODO(avaliani): we should move away from refreshing the entire array and
                    //   instead update the existing array.
                    return $scope.pagedEvents.isProcessingPromise();
                })
            .then(
                function () {
                    // Need to update our event reference post login.
                    event = findEventByKey(eventId);

                    if (  (event == null) ||
                          ( (event.registrationInfo != "CAN_REGISTER") &&
                            (event.registrationInfo != "CAN_WAIT_LIST")) ) {
                        return $q.reject();
                    }

                    // Restore promise tracker in case it was removed during the refresh.
                    event.registrationActionTracker = new PromiseTracker(registrationActionDef.promise);

                    return Events.save(
                        {
                            id: eventId,
                            registerCtlr: 'participants',
                            regType: type
                        },
                        null);
                })
            .then(
                function () {
                    // Also, to update the UI we need the user's key and profile image. So make sure me()
                    // is ready also. Furthermore, if the events refresh then as part of th refresh
                    // the expanded event is fetched once again. Make sure that fetch is complete.
                    return $q.all([
                        MeUtil.me(),
                        event.expandedEventPromise]);
                })
            .then(
                function() {
                    registrationActionDef.resolve();

                    EventUtil.postRegistrationTasks(event.expandedEvent, type);

                    event.expandedEvent.registrationInfo = type;
                    event.registrationInfo = type;
                    if (type === 'REGISTERED') {
                        //$rootScope.showAlert("Your registration is successful!","success");
                        event.expandedEvent.numRegistered++;
                        event.cachedParticipants.push({
                            "key": $rootScope.me.key,
                            profileImage : $rootScope.me.profileImage
                        });
                    } else if (type === 'WAIT_LISTED') {
                        //$rootScope.addAlert("You are added to waitlist!","success");
                    }
                },
                function () {
                    registrationActionDef.reject();
                });
    };

    $scope.delete = function( ) {
        var eventId = this.event.key;
        Events.delete( { id : eventId }, function( ) {
                $( "#event_" + eventId ).fadeOut( );
        } );
    };
    $scope.expandEvent = function() {
        if (this.event.isCollapsed) {
            toggleEvent(this.event);
        }
    };
    $scope.collapseEvent = function($event) {
        if (!this.event.isCollapsed) {
            $event.stopPropagation();
            toggleEvent(this.event);
        }
    };

    function getRouteExpandedEventKey() {
        return $routeParams.expand;
    }

    function toggleEvent(event) {
        if (event) {
            event.isCollapsed = !event.isCollapsed;

            if ( !event.isCollapsed ) {
                event.expandedEventPromise =
                    Events.get( {
                        id : event.key, registerCtlr : 'expanded_search_view' },
                        function(data) {
                            event.expandedEvent = data;
                        } );
                event.expandedEventFetchTracker = new PromiseTracker(event.expandedEventPromise);
            }
        }

        if (event && !event.isCollapsed) {
            $location.search('expand', event.key);
        } else {
            $location.search('expand', null);
        }
    }

    function findEventByKey(eventKey) {
        var events = $scope.pagedEvents.elements;
        for (var idx = 0; idx < events.length; idx++) {
            var eventAtIdx = events[idx];
            if (eventAtIdx.key === eventKey) {
                return eventAtIdx;
            }
        }
        return null;
    }

    function LocationSearch() {
        this.selectedDistance = 50;
        this.distanceChoices = [
            5,
            10,
            50
        ];

        this.selectedLocation =
            { text: "San Francisco, CA", latitude: 37.774929, longitude: -122.419416 };

        this.distanceToDisplayString = function (distance) {
            return distance + " miles";
        }

        this.updateDistance = function (newDistance) {
            if (newDistance != this.selectedDistance) {
                this.selectedDistance = newDistance;
                return true;
            } else {
                return false;
            }
        }

        this.updateLocation = function (newLocation) {
            if ( (this.selectedLocation.latitude != newLocation.lat) ||
                 (this.selectedLocation.longitude != newLocation.lng) ) {

                this.selectedLocation.latitude = newLocation.lat;
                this.selectedLocation.longitude = newLocation.lng;
                if (newLocation.city && newLocation.state) {
                    this.selectedLocation.text = newLocation.city + ", " + newLocation.state;
                } else {
                    this.selectedLocation.text = newLocation.formattedAddress;
                }
                return true;

            } else {
                return false;
            }
        }
    }

    $scope.locationSearch = new LocationSearch();
    $scope.updateDistance = function (selectedDistance) {
        if ($scope.locationSearch.updateDistance(selectedDistance)) {
            $scope.reset();
        }
    }
    $scope.getGeocoding = function (value) {
        var geocodingDef = $q.defer();

        Geocoder.geocodeAddress(value).then(
            function (result) {
                if ($scope.locationSearch.updateLocation(result)) {
                    $scope.reset();
                }
                geocodingDef.resolve();
            },
            function (err) {
                var errMsg;
                if (err.type === 'zero') {
                    errMsg = "Unable to find address";
                } else if (err.type === 'busy') {
                    errMsg = "Server busy, please try again";
                } else {
                    errMsg = "Error processing request, please try again";
                    $log.warn("geocoding error: %s, %s", err.type, err.message);
                }
                geocodingDef.reject(errMsg);
            });


        return geocodingDef.promise;
    }

    $scope.reset();
};

var addEditEventsCtrl =  function( $scope, $rootScope, $routeParams, $filter, $location, Events, $http, FbUtil, EventUtil, KexUtil ) {
    $scope.KexUtil = KexUtil;
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
            if( $scope.event && $scope.event.location.address.street ) {
                geocoder.geocode(
                    { 'address' : $scope.event.location.address.street + ',' + $scope.event.location.address.city + ',' + $scope.event.location.address.state + ',' + $scope.event.location.address.country },
                    function( results, status ) {
                        if( status == google.maps.GeocoderStatus.OK ) {
                            $scope.center.latitude = results [ 0 ].geometry.location.lat( );
                            $scope.center.longitude = results [ 0 ].geometry.location.lng( );
                            $scope.setMarker( $scope.center.latitude, $scope.center.longitude );
                            $scope.zoom = 15;
                            $scope.$apply( );
                        }
                    });
            }
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
            if( type === 'REGISTERED' )
            {
                $http.get( $scope.eventRegistered.paging.next ).success( function( data ) {
                        for( var i = 0; i < data.data.length; i ++ )
                        {
                            $scope.eventRegistered.data.push( data.data [ i ] );
                        }
                        $scope.eventRegistered.paging.next = data.paging ? data.paging.next : null;
                } );
            }
            else if( type === 'ORGANIZER' )
            {
                $http.get( $scope.eventOrganizers.paging.next ).success( function( data ) {
                        for( var i = 0; i < data.data.length; i ++ )
                        {
                            $scope.eventOrganizers.data.push( data.data [ i ] );
                        }
                        $scope.eventOrganizers.paging.next = data.paging ? data.paging.next : null;
                } );
            }
            else if( type === 'WAIT_LISTED' )
            {
                $http.get( $scope.eventWaitListed.paging.next ).success( function( data ) {
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
            Events.get( { id : $routeParams.eventId }, function( result ) {
                    $scope.event = result;
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
                    if ( $scope.event.location.address.geoPt !== null )
                    {
                        $scope.center = {
                            latitude : $scope.event.location.address.geoPt.latitude,
                            longitude : $scope.event.location.address.geoPt.longitude
                        };
                        $scope.setMarker( $scope.center.latitude, $scope.center.longitude );
                        $scope.zoom = 15;
                    }
                    Events.get(
                        { id : $routeParams.eventId, registerCtlr : 'participants', regType : 'ORGANIZER' },
                        function( result ) {
                            $scope.eventOrganizers = result;
                            $scope.eventOrganizers.data.push = function( ) {
                                Events.save( { user : arguments [ 0 ].key }, { id : $routeParams.eventId, registerCtlr : 'participants', regType : 'ORGANIZER' } );
                                return Array.prototype.push.apply( this, arguments );
                            };
                        } );
                    Events.get(
                        { id : $routeParams.eventId, registerCtlr : 'participants', regType : 'REGISTERED' },
                        function( result ) {
                            $scope.eventRegistered = result;
                            $scope.eventRegistered.data.push = function( ) {
                                Events.save( { user : arguments [ 0 ].key }, { id : $routeParams.eventId, registerCtlr : 'participants', regType : 'REGISTERED' } );
                                return Array.prototype.push.apply( this, arguments );
                            };
                        } );
                    Events.get(
                        { id : $routeParams.eventId, registerCtlr : 'participants', regType : 'WAIT_LISTED' },
                        function( result ) {
                            $scope.eventWaitListed = result;
                            $scope.eventWaitListed.data.push = function( ) {
                                Events.save( { user : arguments [ 0 ].key }, { id : $routeParams.eventId, registerCtlr : 'participants', regType : 'WAIT_LISTED' } );
                                return Array.prototype.push.apply( this, arguments );
                            };
                        } );

                    Events.get(
                        { id : $routeParams.eventId, registerCtlr : 'participants', regType : 'REGISTERED_NO_SHOW' },
                        function( result ) {
                            $scope.eventNoShow = result;
                            $scope.eventNoShow.data.push = function( ) {
                                Events.save( { user : arguments [ 0 ].key }, { id : $routeParams.eventId, registerCtlr : 'participants', regType : 'REGISTERED_NO_SHOW' } );
                                return Array.prototype.push.apply( this, arguments );
                            };
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
                    if( $scope.event.status == 'COMPLETED' )
                    {
                        if( $scope.event.album )
                        {
                            // TODO(avaliani): switch to using $facebook.
                            $http.get( FbUtil.GRAPH_API_URL + "/" + $scope.event.album.id + "/photos" ).success( function( data ) {
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
            });
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

var viewEventCtrl = function($scope, $rootScope, $route, $routeParams, $filter, $location,
        Events, $http, EventUtil, KexUtil, $modal, urlTabsetUtil, $facebook,
        RecyclablePromiseFactory, $q, SnapshotServiceHandshake, PageProperties, SessionManager) {
    $scope.KexUtil = KexUtil;
    $scope.EventUtil = EventUtil;
    $scope.currentUserRating = {
        value: undefined
    };

    var tabManager = $scope.tabManager = urlTabsetUtil.createTabManager();
    tabManager.addTab('details', { active: true });
    tabManager.addTab('impact', { disabled: true, onSelectionCb: loadImpactTab });
    tabManager.init();
    var tabs = $scope.tabs = tabManager.tabs;

    var eventDetailsRecyclablePromise = RecyclablePromiseFactory.create();
    $scope.$on("SessionManager.userChanged", function (event, response) {
        refreshEvent(true);
    });

    var pageLoadedDef = $q.defer();
    SnapshotServiceHandshake.disableSnapshotOnTimeout();
    pageLoadedDef.promise.then( function (ctx) {
        SnapshotServiceHandshake.snapshot();
        if (ctx && ctx.pageImageUrl) {
            PageProperties.setPageImage(ctx.pageImageUrl);
        }
    });

    eventDetailsRecyclablePromise.promise.then( function() {
        PageProperties.setContentTitle($scope.event.title +
            " on " + moment($scope.event.startTime).format('l'));
        PageProperties.setDescription($scope.event.description);
    });

    $scope.unregister = function() {
        var unregistrationActionDef = $q.defer();
        $scope.unregistrationActionTracker = new PromiseTracker(unregistrationActionDef.promise);

        var initialRegistrationInfo = $scope.event.registrationInfo;

        Events.delete(
                {
                    id: $scope.event.key,
                    registerCtlr: 'participants'
                })
            .then(
                function () {
                    refreshEvent(false);
                    return eventDetailsRecyclablePromise.promise;
                })
            .then(
                function() {
                    unregistrationActionDef.resolve();
                    EventUtil.postUnRegistrationTasks(
                        $scope.event, initialRegistrationInfo);
                },
                function () {
                    unregistrationActionDef.reject();
                });
    }

    $scope.register = function(type) {

        var registrationActionDef = $q.defer();
        $scope.registrationActionTracker = new PromiseTracker(registrationActionDef.promise);

        SessionManager.loginRequired()
            .then(
                function () {
                    // Login can cause the event to refresh.
                    return eventDetailsRecyclablePromise.promise;
                })
            .then(
                function () {
                    if (  ( ($scope.event.registrationInfo != "CAN_REGISTER") &&
                            ($scope.event.registrationInfo != "CAN_WAIT_LIST")) ) {
                        return $q.reject();
                    }
                    return Events.save(
                        {
                            id: $scope.event.key,
                            registerCtlr: 'participants',
                            regType: type
                        },
                        null);
                })
            .then(
                function () {
                    // This is not the fastest way to update the screen, but it's
                    // good enough for now.
                    refreshEvent(false);
                    return eventDetailsRecyclablePromise.promise;
                })
            .then(
                function () {
                    registrationActionDef.resolve();
                    EventUtil.postRegistrationTasks(
                        $scope.event, type);
                },
                function () {
                    registrationActionDef.reject();
                });
    };

    $scope.getMore = function(type) {
        if (type === 'REGISTERED') {
            $http.get( $scope.eventRegistered.paging.next )
                .success(function(data) {
                    for (var i = 0; i < data.data.length; i++) {
                        $scope.eventRegistered.data.push(data.data[i]);
                    }
                    $scope.eventRegistered.paging.next = data.paging ? data.paging.next : null;
                });
        } else if (type === 'ORGANIZER') {
            $http.get( $scope.eventOrganizers.paging.next )
                .success(function(data) {
                    for (var i = 0; i < data.data.length; i++) {
                        $scope.eventOrganizers.data.push(data.data[i]);
                    }
                    $scope.eventOrganizers.paging.next = data.paging ? data.paging.next : null;
                });
        } else if (type === 'WAIT_LISTED') {
            $http.get( $scope.eventWaitListed.paging.next )
                .success(function(data) {
                    for (var i = 0; i < data.data.length; i++) {
                        $scope.eventWaitListed.data.push(data.data[i]);
                    }
                    $scope.eventWaitListed.paging.next = data.paging ? data.paging.next : null;
                });
        }
    };

    function refreshEvent(refreshDetailsOnly) {
        var eventDetailsDef = eventDetailsRecyclablePromise.recycle();

        Events.get(
            {
                id: $routeParams.eventId
            },
            function( result ) {
                $scope.event = result;
                if ($scope.event.status == 'COMPLETED') {
                    tabs.impact.disabled = false;
                    if (tabs.impact.active) {
                        tabManager.reloadActiveTab();
                    } else if (!tabManager.tabSelectedByUrl) {
                        // Change the current tab if no tab has been explicitly selected.
                        tabManager.markTabActive('impact');
                    } else {
                        pageLoadedDef.resolve();
                    }
                } else {
                    pageLoadedDef.resolve();
                }
                eventDetailsDef.resolve();
            },
            function() {
                eventDetailsDef.reject();
            });

        // Skip other queries of only an event fetch is required.
        if (refreshDetailsOnly) {
            return;
        }

        var eventOrganizersPromise = Events.get(
            {
                id: $routeParams.eventId,
                registerCtlr: 'participants',
                regType: 'ORGANIZER'
            },
            function( result ) {
                $scope.eventOrganizers = result;
            });
        if (!angular.isDefined($scope.eventOrganizersFirstFetchTracker)) {
            $scope.eventOrganizersFirstFetchTracker = new PromiseTracker(eventOrganizersPromise);
        }

        var eventRegisteredPromise = Events.get(
            {
                id: $routeParams.eventId,
                registerCtlr: 'participants',
                regType: 'REGISTERED'
            },
            function( result ) {
                $scope.eventRegistered = result;
            });
        if (!angular.isDefined($scope.eventRegisteredFirstFetchTracker)) {
            $scope.eventRegisteredFirstFetchTracker = new PromiseTracker(eventRegisteredPromise);
        }

        var eventWaitListedPromise = Events.get(
            {
                id: $routeParams.eventId,
                registerCtlr: 'participants',
                regType: 'WAIT_LISTED'
            },
            function( result ) {
                $scope.eventWaitListed = result;
            });
        if (!angular.isDefined($scope.eventWaitListedFirstFetchTracker)) {
            $scope.eventWaitListedFirstFetchTracker = new PromiseTracker(eventWaitListedPromise);
        }
    }

    function loadImpactTab() {
        if (!$scope.impactTabLoaded && $scope.event && ($scope.event.status == 'COMPLETED')) {
            $scope.impactTabLoaded = true;

            if ($scope.event.album) {
                $facebook.cachedApi("/" + $scope.event.album.id + "/photos").then(
                    function (response) {
                        // TODO(avaliani): Bug. For now only fetch the first few images.
                        $scope.impactImgs = [];
                        var fbImgs = response.data;
                        for (var imgIdx = 0; imgIdx < fbImgs.length; imgIdx++) {
                            $scope.impactImgs.push({active: imgIdx == 0, src: fbImgs[imgIdx].source});
                        }

                        pageLoadedDef.resolve(
                            { pageImageUrl: angular.isDefined(fbImgs[0]) ? fbImgs[0].source : undefined }
                        );
                    }, function() {
                        pageLoadedDef.resolve();
                    });
            } else {
                pageLoadedDef.resolve();
            }

            if (EventUtil.canWriteReview($scope.event)) {
                Events.get(
                    {
                        id: $routeParams.eventId,
                        registerCtlr: 'review'
                    },
                    function(value) {
                        $scope.currentUserRating.value =
                            (value && value.rating) ? value.rating.value : undefined;
                    });
            }
        }
    }

    refreshEvent(false);
};

var tourCtrl = function($scope, $location, SessionManager) {
    $scope.login = function () {
        SessionManager.loginRequired(false).then( function () {
            $location.path("/");
        });
    }
};

kexApp.controller('NavbarController',
        [ '$scope', '$location', 'KarmaGoalUtil', 'KexUtil', 'SessionManager',
          function($scope, $location, KarmaGoalUtil, KexUtil, SessionManager) {

    $scope.SessionManager = SessionManager;
    $scope.completionIconStyle = KarmaGoalUtil.completionIconStyle;

    $scope.isActive = function (url) {
        return $location.path() === KexUtil.stripHashbang(url);
    }

    $scope.collapseNavbar = function () {
        if ($('.navbar-toggle').css('display') !='none') {
            $(".navbar-toggle").trigger( "click" );
        }
    }

}]);

kexApp.controller('RootController',
    [
        '$scope', 'PageProperties',
        function($scope, PageProperties) {
            $scope.PageProperties = PageProperties;
        }
    ]);

kexApp.controller(
    'ConfirmationModalInstanceCtrl',
    [
        '$scope', '$modalInstance', 'modalText',
        function($scope, $modalInstance, modalText) {
            $scope.modalText = modalText;
            $scope.confirm = function () {
                $modalInstance.close();
            };

            $scope.cancel = function () {
                $modalInstance.dismiss();
            };
        }
    ]);

var EventModalInstanceCtrl = function ($scope, $modalInstance, event, header, $rootScope) {
    $scope.event = event;
    $scope.header = header;
    $scope.ok = function () {
        $modalInstance.close();
    };
};

function isExternal(url) {
    var match = url.match(/^([^:\/?#]+:)?(?:\/\/([^\/?#]*))?([^?#]+)?(\?[^#]*)?(#.*)?/);
    if (typeof match[1] === "string" && match[1].length > 0 && match[1].toLowerCase() !== location.protocol) return true;
    if (typeof match[2] === "string" && match[2].length > 0 && match[2].replace(new RegExp(":("+{"http:":80,"https:":443}[location.protocol]+")?$"), "") !== location.host) return true;
    return false;
}


/**
 * This class tracks the completion of multiple named events. The completionCb
 * is invoked once all the events are marked complete.
 */
function CompletionTracker(eventArray, completionCb) {
    this._eventIdxMap = {};
    for (var idx = 0; idx < eventArray.length; idx++) {
        this._eventIdxMap[eventArray[idx]] = idx;
    }
    this._tracker = new BitArray(eventArray.length);
    this._completionCb = completionCb;
}

/**
 * Marks a tracked event as completed.
 */
CompletionTracker.prototype.complete = function(eventName) {
    var idx = this._eventIdxMap[eventName];
    this._tracker.set(idx, true);
    if (this._tracker.count() == this._tracker.length) {
        this._completionCb();
    }
}

function PromiseTracker(promise) {
    this.reset(promise);
}

PromiseTracker.prototype.isPending = function() {
    return this._tracked.length > 0;
}

PromiseTracker.prototype.reset = function(promise) {
    this._tracked = [];

    if (angular.isDefined(promise)) {
        this.track(promise);
    }
}

PromiseTracker.prototype.track = function(promise) {
    this._tracked.push(promise);

    promise.then(
        angular.bind(this, onDone),
        angular.bind(this, onDone));

    function onDone() {
        var index = this._tracked.indexOf(promise);
        this._tracked.splice(index, 1);
    }
}


/*
 * App config and run methods
 */

kexApp.config( function( $provide, $routeProvider, $httpProvider, $facebookProvider,
        PagePropertiesProvider, PAGE_TITLE_SUFFIX ) {

    initConditionalLogging();

    $routeProvider
        // .when( '/', { controller : homeCtrl, templateUrl : 'partials/home.html' } )
        // .when( '/home', { controller : homeCtrl, templateUrl : 'partials/home.html' } )
        .when(
            '/me',
            {
                controller : meViewCtrl,
                templateUrl : 'partials/me.html',
                reloadOnSearch: false
            })
        .when(
            '/about',
            {
                title : "About" + PAGE_TITLE_SUFFIX,
                templateUrl : 'partials/about.html',
                reloadOnSearch: false
            })
        .when(
            '/tour',
            {
                title : "Tour" + PAGE_TITLE_SUFFIX,
                controller : tourCtrl,
                templateUrl : 'partials/tour.html',
                reloadOnSearch: false
            })
        .when(
            '/awards',
            {
                title : "Karma Awards" + PAGE_TITLE_SUFFIX,
                templateUrl : 'partials/awards.html',
                reloadOnSearch: false
            })
        .when(
            '/contact',
            {
                title : "Contact" + PAGE_TITLE_SUFFIX,
                templateUrl :
                'partials/contact.html',
                reloadOnSearch: false
            })
        .when(
            '/user/:userId',
            {
                controller : meViewCtrl,
                templateUrl : 'partials/me.html',
                reloadOnSearch: false
            })
        .when(
            '/mysettings',
            {
                title : "User Settings" + PAGE_TITLE_SUFFIX,
                controller : meEditCtrl,
                templateUrl : 'partials/mysettings.html',
                reloadOnSearch: false
            })
        .when(
            '/',
            {
                controller : eventsCtrl,
                templateUrl : 'partials/events.html',
                reloadOnSearch: false
            })
        .when(
            '/events',
            {
                controller : eventsCtrl,
                templateUrl : 'partials/events.html',
                reloadOnSearch: false
            })
        .when(
            '/event/add',
            {
                title : "Add/Edit Event" + PAGE_TITLE_SUFFIX,
                controller : addEditEventsCtrl,
                templateUrl : 'partials/addEditevent.html',
                reloadOnSearch: false
            })
        .when(
            '/event/:eventId/edit',
            {
                title : "Add/Edit Event" + PAGE_TITLE_SUFFIX,
                controller : addEditEventsCtrl,
                templateUrl : 'partials/addEditevent.html',
                reloadOnSearch: false
            })
        .when(
            '/event/:eventId',
            {
                controller : viewEventCtrl,
                templateUrl : 'partials/viewEvent.html',
                reloadOnSearch: false
             })
        .when(
            '/org',
            {
                title : "Volunteer Organizations" + PAGE_TITLE_SUFFIX,
                controller : orgCtrl,
                templateUrl : 'partials/organization.html',
                reloadOnSearch: false
            })
        .when(
            '/org/:orgId',
            {
                controller : orgDetailCtrl,
                templateUrl : 'partials/organizationDetail.html',
                reloadOnSearch: false
            })
        .when(
            '/calendar',
            {
                controller : 'CalendarCtrl',
                templateUrl : 'partials/calendar.html',
                reloadOnSearch: false
            })
        // NOTE: If you're adding a new when you need to either add
        //       a fixed page title, or you need to modify the controller
        //       to invoke PageProperties.setContentTitle().
        .otherwise( { redirectTo : '/' } );
    delete $httpProvider.defaults.headers.common[ 'X-Requested-With' ];
    //$httpProvider.defaults.headers.common['X-'] = 'X';


    var fbAppId;
    if (document.location.hostname === "localhost" ) {
        fbAppId = '276423019167993';
    }
    else if (document.location.hostname === "karmademo.dyndns.dk" ) {
        fbAppId = '1381630838720301';
    }
    else if ( /^kex-latest[-\w]*.appspot.com$/.test(document.location.hostname) ) {
        fbAppId = '166052360247234';
    }
    else {
        fbAppId = '571265879564450';
    }

    PagePropertiesProvider.setFbAppId(fbAppId);

    $facebookProvider.setAppId(fbAppId);
    $facebookProvider.setCustomInit({
        status : true,
        cookie : true,
        xfbml : false });
    $facebookProvider.setPermissions('public_profile,email,user_friends');


    function initConditionalLogging() {
        var LOG_LEVEL_MAP = {
            // Debug is not available in angular 1.0.8
            // debug: 0,
            log: 1,
            info: 2,
            warn: 3,
            error: 4,
        }
        // This is the current logging level.
        var loggingLevel = LOG_LEVEL_MAP['warn'];

        $provide.decorator('$log', function($delegate, $sniffer) {
            var defaultLog = {};

            angular.forEach(LOG_LEVEL_MAP, function(level, logMethod) {
                defaultLog[logMethod] = $delegate[logMethod];
                $delegate[logMethod] = getConditionalLoggingMethod(logMethod);
            });

            function getConditionalLoggingMethod(logMethod) {
                return function() {
                    if (LOG_LEVEL_MAP[logMethod] >= loggingLevel) {
                        defaultLog[logMethod].apply(defaultLog, arguments);
                    }
                }
            }

            return $delegate;
        });
    }
})

// Prevent angular lazy instantation for the following services:
//   - MeUtil
//   - AuthDepResource
//   - SnapshotServiceHandshake
//   - PageProperties
//   - SessionManager
//   - PersonaUtil
//   - IframeUtil
.run( function( $rootScope, Me, $location, FbUtil, $modal, MeUtil, $q, $http,
        AuthDepResource, KexUtil, SnapshotServiceHandshake, PageProperties,
        SessionManager, PersonaUtil, IframeUtil) {

    $rootScope.fbUtil = FbUtil;
    $rootScope.setLocation = angular.bind(KexUtil, KexUtil.setLocation);
    $rootScope.isMobileDevice = KexUtil.isMobileDevice();
    $rootScope.isMobileDeviceOrTablet = KexUtil.isMobileDeviceOrTablet();

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

    $rootScope.displayHttpRequestFailure = function (response) {
        switch (response.status) {
        case 400:
            $rootScope.showAlert(response.data.error.message, "danger");
            break;
        case 401:
            $rootScope.showAlert('Wrong usename or password', "danger");
            break;
        case 403:
            $rootScope.showAlert('Permission denied', "danger");
            break;
        case 404:
            $rootScope.showAlert('Failed to find resource: ' + response.data, "danger");
            break;
        case 500:
            $rootScope.showAlert('Server internal error: ' + response.data, "danger");
            break;
        default:
            $rootScope.showAlert('Error ' + response.status + ': ' + response.data, "danger");
            break;
        }
    }

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
    $rootScope.openShareEventModal = function (event, header) {
        var modalInstance = $modal.open({
            backdrop: false,
            templateUrl: 'template/kex/share-event-modal.html',
            controller: EventModalInstanceCtrl,
            resolve: {
                event: function () {
                    return event;
                },
                header: function () {
                    return header;
                }
            }
        });
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

    function loadBadges() {
        var badgesDef = $q.defer();
        var badgesListDef = $q.defer();

        $http.get('/generated/badges.json').success(function(data) {
            var ctx = { badgesMap: {}, badgesList: [] };
            angular.forEach(data, function(badge) {
                splitBadgeLabel(badge);
                this.badgesMap[badge.name] = badge;
                this.badgesList.push(badge);
            }, ctx);
            badgesDef.resolve(ctx.badgesMap);
            badgesListDef.resolve(ctx.badgesList);
        });

        function splitBadgeLabel(badge) {
            var label = badge.label;
            var firstSpaceBefHalfLength;
            for (var idx = 0; idx < label.length; idx++) {
                if ( (label.charAt(idx) === ' ') &&
                     ( (!angular.isDefined(firstSpaceBefHalfLength)) ||
                       (idx < Math.floor(label.length / 2)) ) ) {
                    firstSpaceBefHalfLength = idx;
                }
            }

            if (!angular.isDefined(firstSpaceBefHalfLength)) {
                badge.labelLine1 = label;
                badge.labelLine2 = undefined;
            } else {
                badge.labelLine1 = label.substring(0, firstSpaceBefHalfLength);
                badge.labelLine2 = label.substring(firstSpaceBefHalfLength + 1);
            }
        }

        $rootScope.badges = badgesDef.promise;
        $rootScope.badgesList = badgesListDef.promise;
    }

    loadBadges();

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

