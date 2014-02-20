angular.module('globalErrors', []).config(function($provide, $httpProvider, $compileProvider) {
    $httpProvider.defaults.headers.post["Content-Type"] = "application/json;charset=UTF-8";
    $httpProvider.defaults.transformRequest.push(function(data, headersGetter) {
        // console.log(angular.toJson(headersGetter()));
        // Check if it is a post request. Mutations require authentication.
        // if ((headersGetter()["Content-Type"] !== null) && !isLoggedIn()) {
        //     // Temporarily disabling until this functionality is completed to allow for login.
        //     // alert("login required");
        // }
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

angular.module('HashBangURLs', []).config(['$locationProvider', function($location) {
  $location.hashPrefix('!');
}]);

kexApp = angular.module( "kexApp",
    ["ngResource", "ngCookies", "google-maps", "ui.bootstrap", "ui.bootstrap.ex", "ngFacebook",
     "globalErrors" ,"ui.calendar", "ngSocial","HashBangURLs"] )

.filter( 'newlines', function( ) {
    return function ( text ) {
        if ( text ) {
            return text.replace( /\n/g, '<br/>' );
        }
    };
})

.filter( 'noHTML', function( ) {
    return function ( text ) {
        if( text ) {
            return text.replace( /&/g, '&amp;' ).replace( />/g, '&gt;' ).replace( /</g, '&lt;' );
        }
    };
} )

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
});


/*
 * Webservice factories
 */

kexApp.factory( 'Events', function( FbAuthDepResource ) {
    return FbAuthDepResource.create( '/api/event/:id/:registerCtlr/:regType',
        { id : '@id', registerCtlr : '@registerCtlr', regType : '@regType' });
} );
kexApp.factory( 'User', function( FbAuthDepResource ) {
    return FbAuthDepResource.create( '/api/user/:id/:resource/:filter',
        { id : '@id', resource : '@resource', filter : '@filter' } );
} );
kexApp.factory( 'Me', function( FbAuthDepResource ) {
    return FbAuthDepResource.create( '/api/me/:resource/:filter',
        { resource : '@resource', filter : '@filter' } );
} );
kexApp.factory( 'Org', function( FbAuthDepResource ) {
    return FbAuthDepResource.create( '/api/org/:id/:resource/:filter',
        { id : '@id', resource : '@resource', filter : '@filter' } );
} );

kexApp.factory('FbAuthDepResource', function($resource, FbUtil, $q, $rootScope) {
    var wrappedMethods = ['get', 'save', 'query', 'remove', 'delete'];

    var authRespDef = $q.defer();
    var isFirstAuthResp = true;
    $rootScope.$on("fbUtil.userChanged", function (event, response) {
        if ( isFirstAuthResp ) {
            isFirstAuthResp = false;
            authRespDef.resolve();
        } else {
            // If any resource depends on handling user changes after the
            // first auth response this event must be listened to in order
            // to update the resource.
            $rootScope.$broadcast("FbAuthDepResource.userChanged", response);
        }
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

                    authRespDef.promise.then( function() {

                        if (FbUtil.authTokenRefreshRequired()) {
                            FbUtil.refreshAuthToken().then(
                                invokeMethod, invokeMethod);
                        } else {
                            invokeMethod();
                        }

                        function invokeMethod() {
                            wrappedRsrc._rsrc[m].apply(
                                wrappedRsrc._rsrc, wrappedMethodArgs.args);
                        }

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

kexApp.factory('KexUtil', function($rootScope) {
    return {
        getBaseUrl: function() {
            return window.location.protocol + '//' + window.location.host;
        },
        getLocation: function() {
            return window.location.href;
        },
        urlStripProtocol: function(url) {
            return url ? url.replace(/^http:/,'') : url;
        },
        strConcat: function(str1, str2) {
            return (angular.isDefined(str1) && angular.isDefined(str2)) ? (str1 + str2) : undefined;
        },
        stripHashbang: function(url) {
            return (url.indexOf('#!') === 0) ? url.substring(2) : url;
        },
        getSEOUrl: function(){
            return window.location.protocol + '//' + window.location.host + "?_escaped_fragment_=" + window.location.hash.replace('#!','');
        },
        getOGMetaTagUrl: function(ogtype, ogtitle, ogimage){
            return window.location.protocol + '//' + window.location.host + "?metaonly=true&ogtype="+ogtype+"&ogtitle="+encodeURIComponent(ogtitle)+"&ogimage="+encodeURIComponent(ogimage)+"&ogurl="+encodeURIComponent(window.location.href);
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

        truncateToWordBoundary: function(str, lim) {
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
        }
    };
});

kexApp.factory('MeUtil', function($rootScope, $q, Me, KarmaGoalUtil, KexUtil, RecyclablePromiseFactory) {

    var meRecyclablePromise = RecyclablePromiseFactory.create();
    var goalInfoRecyclablePromise = RecyclablePromiseFactory.create();

    $rootScope.$on("fbUtil.userChanged", function (event, response) {
        if ( response.status === 'connected' ) {
            MeUtil.updateMe();
        } else {
            MeUtil.clearMe();
        }
    });

    var MeUtil = {
        // Get the promise corresponding to me
        me: function() {
            return meRecyclablePromise.promise;
        },

        goalInfo: function() {
            return goalInfoRecyclablePromise.promise;
        },

        updateMe: function() {
            var meDef = meRecyclablePromise.recycle();
            var goalInfoDef = goalInfoRecyclablePromise.recycle();
            Me.get(
                angular.bind(this, function(value) {
                    $rootScope.me = value;
                    meDef.resolve($rootScope.me);
                    updateIsOrganizer();
                    updateKarmaGoal(goalInfoDef);
                }),
                angular.bind(this, function(httpResponse) {
                    this._clearMe(meDef, goalInfoDef);
                }));

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
            this._clearMe(
                meRecyclablePromise.recycle(),
                goalInfoRecyclablePromise.recycle());
        },

        _clearMe: function(meDef, goalInfoDef) {
            meDef.reject();
            goalInfoDef.reject();
            $rootScope.me = undefined;
            $rootScope.isOrganizer = false;
            $rootScope.goalInfo = {};
        }
    };

    return MeUtil;
});


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

kexApp.factory('ApiCache', function($rootScope) {
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
        $window, ApiCache, $q) {
    var firstAuthResponse = true;
    var authTokenRefreshTime;
    var authTokenRefreshPromise;
    var authTokenIsBeingRefreshed = false;

    $rootScope.$on("fb.auth.authResponseChange", function ( event, response ) {
        if ( response.status === 'connected' ) {
            setCookies(response.authResponse);

            var updateUser = $rootScope.fbUserId != response.authResponse.userID;
            $rootScope.fbUserId = response.authResponse.userID;

            if ( updateUser ) {
                processUserChange(response);
            }
        } else {
            removeCookies();
            var userLoggedOut = false;
            if ( $rootScope.fbUserId ) {
                userLoggedOut = true;
                $rootScope.fbUserId = undefined;
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
            console.log("Error connecting to facebook: %o", response.error);
        }
    });

    function processUserChange( response ) {
        // Fired first time user information is available (post FB.init) and any
        // subsequent time that it changes.
        $rootScope.$broadcast("fbUtil.userChanged", response);
    }

    function setCookies(authResponse) {
        $.cookie( "facebook-uid", authResponse.userID );
        $.cookie( "facebook-token", authResponse.accessToken );
        $.cookie( "login", "facebook" );
        $rootScope.isLoggedIn = true;
        // Force a refresh of the token if it will expire in under 60 seconds.
        authTokenRefreshTime = moment().add('seconds', authResponse.expiresIn - 60);
    }

    function removeCookies() {
        $.removeCookie( "facebook-uid" );
        $.removeCookie( "facebook-token" );
        $.removeCookie( "login" );
        $rootScope.isLoggedIn = false;
        authTokenRefreshTime = undefined;
    }

    return {
        GRAPH_API_URL: "//graph.facebook.com",
        LOGIN_SCOPE: "email,user_location",

        getImageUrl: function (id, type) {
            return angular.isDefined(id) ? ("//graph.facebook.com/" + id + "/picture?type=" + type) : undefined;
        },

        login: function () {
            return $facebook.login();
        },

        loginRequired: function(showAlert) {
            var def = $q.defer();

            if ( isLoggedIn() ) {
                def.resolve();
            } else {
                this.login().then(
                    function() {
                        if (isLoggedIn()) {
                            def.resolve();
                        } else {
                            def.reject();
                            if (!angular.isDefined(showAlert) || showAlert) {
                                $rootScope.showAlert('Login required', "danger");
                            }
                        }
                    },
                    function() {
                        def.reject();
                        if (!angular.isDefined(showAlert) || showAlert) {
                            $rootScope.showAlert('Login required', "danger");
                        }
                    });
            }

            return def.promise;
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

        authTokenRefreshRequired: function () {
            return angular.isDefined(authTokenRefreshTime) &&
                (authTokenRefreshTime.diff(moment()) < 0);
        },

        refreshAuthToken: function () {
            if (authTokenIsBeingRefreshed) {
                return authTokenRefreshPromise;
            }

            authTokenIsBeingRefreshed = true;
            authTokenRefreshPromise = $facebook.getLoginStatus(true).then(
                function (response) {
                    authTokenIsBeingRefreshed = false;
                    if (response.status === 'connected') {
                        // This ensures that there is no ordering issue between when the
                        // facebook auth response event fires and the login status promise
                        // is resolved.
                        setCookies(response.authResponse);
                    }
                },
                function (err) {
                    authTokenIsBeingRefreshed = false;
                });
            return authTokenRefreshPromise;
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

kexApp.factory('EventUtil', function($q, $rootScope, User, Events, KexUtil, FbUtil, KarmaGoalUtil, MeUtil) {
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
        },
        getImpactTimelineEvents: function(eventFilter) {
            var impactTimelineEventsGrouped = $q.defer();

            if (eventFilter.userKey) {
                User.get( { id : eventFilter.userKey, resource : 'event', type : 'PAST'},
                    processImpactTimeline);
            } else {
                Events.get( { type : "PAST", keywords : eventFilter.keywords },
                    processImpactTimeline);
            }

            return impactTimelineEventsGrouped.promise;

            function processImpactTimeline(pastEvents) {
                var eventsGrouped = [];
                var curGroup = [];
                for (var idx = 0; idx < pastEvents.data.length; idx++) {
                    var event = pastEvents.data[idx];
                    if (event.album) {
                        event.album.coverPhotoUrl = FbUtil.getAlbumCoverUrl(event.album.id);
                    }
                    if (!event.currentUserRating) {
                        event.currentUserRating = { value: undefined };
                    }

                    curGroup.push(event);
                    if (curGroup.length == 2) {
                        eventsGrouped.push(curGroup);
                        curGroup = [];
                    }
                }
                if (curGroup.length) {
                    eventsGrouped.push(curGroup);
                }

                impactTimelineEventsGrouped.resolve(eventsGrouped);
            }
        }
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
                            'url="KexUtil.getOGMetaTagUrl(type,title,image)" ' +
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

kexApp.directive('eventParticipantImgsMini', function() {
    return {
        restrict: 'E',
        scope: {
            event: '='
        },
        replace: true,
        transclude: false,
        template:
            '<ul class="list-inline">' +
                '<li ng-repeat="userImage in event.cachedParticipantImages">' +
                    '<a href="#!/user/{{userImage.participant.key}}">' +
                        '<img ng-src="{{userImage.imageUrl}}?type=square" class="kex-thumbnail-user-mini">' +
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

kexApp.directive('impactTimeline', function(FbUtil, EventUtil) {
    return {
        restrict: 'E',
        scope: {
            timelineEvents: '=',
            selfProfileView: '=',
            timelineType: '='
        },
        replace: true,
        transclude: false,
        link: function (scope, element, attrs) {
            scope.FbUtil = FbUtil;
            scope.EventUtil = EventUtil;
        },
        templateUrl: 'template/kex/impact-timeline.html'
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

kexApp.directive('eventParticipantSummary', function() {
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

kexApp.directive('upcomingEvents', function() {
    return {
        restrict: 'E',
        scope: {
            events: '=',
            searchType: '='
        },
        replace: true,
        transclude: false,
        templateUrl: 'template/kex/upcoming-events.html'
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

var FLOAT_GEQ_ZERO = /^((\d+(\.(\d+)?)?)|(\.\d+))$/;
kexApp.directive('floatGeqZero', function() {
  return {
    require: 'ngModel',
    link: function(scope, elm, attrs, ctrl) {
      ctrl.$parsers.unshift(function(viewValue) {
        if (FLOAT_GEQ_ZERO.test(viewValue)) {
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

kexApp.directive('truncateAndLink', function($rootScope, KexUtil) {
    return {
        restrict: 'E',
        scope: {
            text: '=',
            linkText: '@',
            href: '@',
            limit: '@'
        },
        replace: true,
        transclude: false,
        link: function (scope, element, attrs) {
            scope.$watch('text', updateText);
            scope.$watch('href', updateText);
            scope.$watch('limit', updateText);

            function updateText() {
                if (  angular.isDefined(scope.text) &&
                      angular.isDefined(scope.href) &&
                      angular.isDefined(scope.limit)  ) {
                    var result =
                        KexUtil.truncateToWordBoundary(scope.text, scope.limit);
                    scope.output = result.str;
                    scope.truncated = result.truncated;
                }
            }
        },
        templateUrl: 'template/kex/truncate-and-link.html'
    };
});

/*
 * App controllers
 */

var meViewCtrl = function($scope, $location, User, Me, $rootScope, $routeParams,
        $modal, EventUtil, KexUtil, urlTabsetUtil, KarmaGoalUtil, MeUtil) {
    $scope.KexUtil = KexUtil;
    $scope.EventUtil = EventUtil;

    var tabManager = $scope.tabManager = urlTabsetUtil.createTabManager();
    tabManager.addTab('impact', { active: true, onSelectionCb: loadImpactTab });
    tabManager.addTab('upcoming', { active: false, onSelectionCb: loadUpcomingTab });
    tabManager.init();
    $scope.tabs = tabManager.tabs;

    $scope.impactTimelineFetchTracker = new PromiseTracker();
    $scope.upcomingEventsFetchTracker = new PromiseTracker();

    function load() {
        if ($location.path() == "/me") {
            $scope.who = 'My';
            $scope.impactTimelineFetchTracker.track(MeUtil.me());
            $scope.upcomingEventsFetchTracker.track(MeUtil.me());
            MeUtil.me().then(function(meObj) {
                $scope.me = meObj;
                $scope.userKey = $scope.me.key;
                $scope.savedAboutMe = $scope.me.about;
                $scope.userGoalInfo = $rootScope.goalInfo;

                // Tab information depends on the user being fetched first.
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
        if (!$scope.impactTabLoaded && $scope.userKey) {
            $scope.impactTabLoaded = true;
            $scope.impactTimelineEvents = EventUtil.getImpactTimelineEvents({
                userKey: $scope.userKey
            });
            $scope.impactTimelineFetchTracker.track($scope.impactTimelineEvents);
        }
    }

    function loadUpcomingTab() {
        if (!$scope.upcomingTabLoaded && $scope.userKey) {
            $scope.upcomingTabLoaded = true;
            var upcomingEventsPromise = User.get(
                {
                    id: $scope.userKey,
                    resource: 'event'
                },
                function(result) {
                    $scope.events = result;
                });
            $scope.upcomingEventsFetchTracker.track(upcomingEventsPromise);
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
    $scope.newMail = {
        email: null,
        primary: null
    };
    $scope.load = function() {
        $scope.who = 'My';
        MeUtil.me().then(function(meObj) {
            $scope.me = meObj;
            $scope.origAboutMe = $scope.me.about;
        });
    };
    $scope.save = function() {
        Me.save($scope.me);
    };
    $scope.addEmail = function() {
        //TO-DO check if the new one is marked primary and unmark the current primary one
        $scope.me.registeredEmails.push($scope.newMail);
        $scope.save();
    };
    $scope.removeEmail = function() {
        $scope.me.registeredEmails.splice(this.$index, 1);
        $scope.save();
    };
    $scope.mailPrimaryIndex = {
        index: 0
    };
    $scope.load();
};

var orgDetailCtrl = function($scope, $location, $routeParams, $rootScope, $http, Org,
        Events, FbUtil, KexUtil, EventUtil, urlTabsetUtil, $facebook) {
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

    var calendarEvents = [];
    $scope.calendarEventSources = [ calendarEvents ];
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
            });

            if ($scope.org.parentOrg) {
                Org.get(
                    { id: $scope.org.parentOrg.key },
                    function(result) {
                        $scope.parentOrg = result;
                    });
            }

            if ($scope.org.permission === "ALL") {
                tabManager.addTab('manageOrg', { active: false, onSelectionCb: loadManageOrgTab });
                $scope.dynamicTabs.push({
                    heading: '<i class="icon-cogs"></i> Manage Organization',
                    contentUrl: 'partials/manage-org-tab.html',
                    tabName: 'manageOrg',
                });
            }

            tabManager.reloadActiveTab();
        });

    $scope.impactTimelineFetchTracker = new PromiseTracker(orgPromise);
    $scope.upcomingEventsFetchTracker = new PromiseTracker(orgPromise);
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
        if (!$scope.impactTabLoaded && $scope.orgLoaded) {
            $scope.impactTabLoaded = true;
            $scope.impactTimelineEvents = EventUtil.getImpactTimelineEvents({
                keywords: "org:" + $scope.org.searchTokenSuffix
            });
            $scope.impactTimelineFetchTracker.track($scope.impactTimelineEvents);
        }
    }

    function loadUpcomingTab() {
        loadUpcomingEvents();
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
        if ($scope.upcomingEventsLoaded) {
            renderCalendar();
        } else {
            // TODO(avaliani): investigate why deep watch doesn't auto render.
            loadUpcomingEvents(renderCalendar);
        }
    }

    function loadUpcomingEvents(action) {
        if (!$scope.upcomingEventsLoaded && $scope.orgLoaded) {
            $scope.upcomingEventsLoaded = true;
            var upcomingEventsPromise = Events.get(
                {
                    type: "UPCOMING",
                    keywords: "org:" + $scope.org.searchTokenSuffix
                },
                function(result) {
                    $scope.upcomingEvents = result;
                    angular.forEach($scope.upcomingEvents.data, function(event) {
                        calendarEvents.push({
                            title: event.title,
                            start: (new Date(event.startTime)),
                            end: (new Date(event.endTime)),
                            allDay: false,
                            url: "/#!/event/" + event.key
                        });
                    });
                    if (action) {
                        action();
                    }
                });
            $scope.upcomingEventsFetchTracker.track(upcomingEventsPromise);
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
                        $scope.allTimeLeaders = result;
                    }));
            $scope.topVolunteersFetchTracker.track(
                Org.get(
                    {
                        type: "THIRTY_DAY",
                        id: $routeParams.orgId,
                        resource: "leaderboard"
                    },
                    function(result) {
                        $scope.lastMonthLeaders = result;
                    }));
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

var orgCtrl = function( $scope, $location, $routeParams, $modal, Org ) {
    $scope.query = "";
    $scope.newOrg = { page : { url : null, urlProvider : "FACEBOOK" }};
    $scope.orgQueryTracker = new PromiseTracker();
    $scope.refresh = function( ) {
        $scope.orgQueryTracker.reset(
            Org.get(
                { name_prefix : $scope.query },
                function(result) {
                    $scope.orgs = result;
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
        EventUtil, FbUtil, RecyclablePromiseFactory, MeUtil, $q ) {
    $scope.KexUtil = KexUtil;
    $scope.FbUtil = FbUtil;

    $scope.eventSearchTracker = new PromiseTracker();
    var eventSearchRecyclablePromise = RecyclablePromiseFactory.create();

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
            markers : [{}]

    } );

    $scope.isMap=false;
    $scope.showMap = function()
    {
        $scope.isMap=true;
        angular.forEach($scope.events.data, function(event){
                $scope.addMarker(event.location.address.geoPt.latitude,event.location.address.geoPt.longitude);
        });
        $scope.zoom = 10;
    };
    $scope.showList = function(){
        $scope.isMap=false;
    };
    $scope.reset = function( ) {
        var eventSearchDef = eventSearchRecyclablePromise.recycle();
        $scope.eventSearchTracker.reset(eventSearchDef.promise);
        Events.get(
            {
                keywords: ($scope.query ? $scope.query : ""),
                lat: $scope.center.latitude,
                long:$scope.center.longitude
            },
            function(value) {
                processEvents(value);
                eventSearchDef.resolve();
            },
            function() {
                eventSearchDef.reject();
            });
    };
    function processEvents(value) {
        $scope.events = value;
        var now = new Date();
        var currentDate = new Date( 1001, 1, 1, 1, 1, 1, 0 );
        for (var idx = 0; idx < $scope.events.data.length; idx++) {
            var event = $scope.events.data[idx];
            var dateVal = new Date(event.startTime);
            var showHeader = (dateVal.getDate() != currentDate.getDate()) ||
                (dateVal.getMonth() != currentDate.getMonth()) ||
                (dateVal.getFullYear() != currentDate.getFullYear());
            currentDate = dateVal;
            event.dateFormat = (now.getFullYear() == dateVal.getFullYear()) ? 'EEEE, MMM d' : 'EEEE, MMM d, y';
            event.showHeader = showHeader;
            event.isCollapsed = true;
        }
        expandEventOnReset();
    }

    var query = undefined;
    $scope.$watch('query', function( ) {
        if (query != $scope.query) {
            $scope.reset();
        }
    });

    $scope.$on("FbAuthDepResource.userChanged", function (event, response) {
        // Only dependency is the cookie identifying the user.
        $scope.reset();
    });

    $scope.addMarker = function( markerLat, markerLng ) {
        $scope.markers.push( {
            latitude : parseFloat( markerLat ),
            longitude : parseFloat( markerLng )
        } );
    };
    $scope.register = function(type) {
        // TODO(avaliani): If the events are refreshed in the course of login, then $index is not valid.
        $scope.modelIndex = this.$index;
        var eventId = $scope.modelEvent.key;

        FbUtil.loginRequired().then( function () {

            Events.save(
                {
                    id: eventId,
                    registerCtlr: 'participants',
                    regType: type
                },
                null,
                function() {
                    EventUtil.postRegistrationTasks($scope.modelEvent, type);

                    // The events array refreshes once a user logs in. Make sure it's ready.
                    // Also, to update the UI we need the user's key and profile image. So make sure me()
                    // is ready also.
                    $q.all([eventSearchRecyclablePromise.promise, MeUtil.me()])
                        .then( function() {

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
                });

        });
    };

    $scope.delete = function( ) {
        var eventId = this.event.key;
        Events.delete( { id : eventId }, function( ) {
                $( "#event_" + eventId ).fadeOut( );
        } );
    };
    $scope.modelEvent = {};
    $scope.modelEventFetchTracker = new PromiseTracker();
    $scope.toggleEvent = function() {
        toggleEventByKey(this.event.key);
    };

    function expandEventOnReset() {
        if ($routeParams.expand) {
            toggleEventByKey($routeParams.expand);
        }
    }

    function toggleEventByKey(eventKey) {
        var event;

        // Collapse all other events.
        for (var idx = 0; idx < $scope.events.data.length; idx++) {
            var eventAtIdx = $scope.events.data[idx];
            if (eventAtIdx.key === eventKey) {
                event = eventAtIdx;
            } else {
                eventAtIdx.isCollapsed = true;
            }
        }

        if (event) {
            event.isCollapsed = !event.isCollapsed;

            if ( !event.isCollapsed ) {
                $scope.modelEventFetchTracker.reset(
                    Events.get( { id : event.key, registerCtlr : 'expanded_search_view' }, function(data) {
                        $scope.modelEvent = data;
                    } ));
            }
        }

        if (event && !event.isCollapsed) {
            $location.search('expand', event.key);
        } else {
            $location.search('expand', null);
        }
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
        Events, $http, FbUtil, EventUtil, KexUtil, $modal, urlTabsetUtil, $facebook,
        RecyclablePromiseFactory) {
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

    var eventViewRecyclablePromise = RecyclablePromiseFactory.create();
    $scope.$on("FbAuthDepResource.userChanged", function (event, response) {
        refreshEvent();
    });

    $scope.unregister = function() {
        Events.delete(
            {
                id: $scope.event.key,
                registerCtlr: 'participants'
            },
            function() {
                EventUtil.postUnRegistrationTasks(
                    $scope.event, $scope.event.registrationInfo);
                refreshEvent();
            });
    }

    $scope.register = function(type) {
        FbUtil.loginRequired().then( function () {

            eventViewRecyclablePromise.promise.then(function () {

                Events.save(
                    {
                        id: $scope.event.key,
                        registerCtlr: 'participants',
                        regType: type
                    },
                    null,
                    function() {
                        EventUtil.postRegistrationTasks(
                            $scope.event, type);
                        refreshEvent();
                    });

            });

        });
    };

    $scope.getMore = function(type) {
        if (type === 'REGISTERED') {
            $http({
                method: 'GET',
                url: $scope.eventRegistered.paging.next
            }).success(function(data) {
                for (var i = 0; i < data.data.length; i++) {
                    $scope.eventRegistered.data.push(data.data[i]);
                }
                $scope.eventRegistered.paging.next = data.paging ? data.paging.next : null;
            });
        } else if (type === 'ORGANIZER') {
            $http({
                method: 'GET',
                url: $scope.eventOrganizers.paging.next
            }).success(function(data) {
                for (var i = 0; i < data.data.length; i++) {
                    $scope.eventOrganizers.data.push(data.data[i]);
                }
                $scope.eventOrganizers.paging.next = data.paging ? data.paging.next : null;
            });
        } else if (type === 'WAIT_LISTED') {
            $http({
                method: 'GET',
                url: $scope.eventWaitListed.paging.next
            }).success(function(data) {
                for (var i = 0; i < data.data.length; i++) {
                    $scope.eventWaitListed.data.push(data.data[i]);
                }
                $scope.eventWaitListed.paging.next = data.paging ? data.paging.next : null;
            });
        }
    };

    function refreshEvent() {
        var eventViewDef = eventViewRecyclablePromise.recycle();
        var refreshCompletionTracker = new CompletionTracker(
            ['event', 'eventOrganizers', 'eventRegistered', 'eventWaitListed'],
            refreshComplete)

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
                    }
                }
                refreshCompletionTracker.complete('event');
            },
            function() {
                refreshCompletionTracker.complete('event');
            });

        var eventOrganizersPromise = Events.get(
            {
                id: $routeParams.eventId,
                registerCtlr: 'participants',
                regType: 'ORGANIZER'
            },
            function( result ) {
                $scope.eventOrganizers = result;
                refreshCompletionTracker.complete('eventOrganizers');
            },
            function() {
                refreshCompletionTracker.complete('eventOrganizers');
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
                refreshCompletionTracker.complete('eventRegistered');
            },
            function() {
                refreshCompletionTracker.complete('eventRegistered');
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
                refreshCompletionTracker.complete('eventWaitListed');
            },
            function() {
                refreshCompletionTracker.complete('eventWaitListed');
            });
        if (!angular.isDefined($scope.eventWaitListedFirstFetchTracker)) {
            $scope.eventWaitListedFirstFetchTracker = new PromiseTracker(eventWaitListedPromise);
        }


        function refreshComplete() {
            eventViewDef.resolve();
        }
    }

    function loadImpactTab() {
        if (!$scope.impactTabLoaded && $scope.event && ($scope.event.status == 'COMPLETED')) {
            $scope.impactTabLoaded = true;

            if ($scope.event.album) {
                $facebook.cachedApi("/" + $scope.event.album.id + "/photos").then(function (response) {
                    // TODO(avaliani): Bug. For now only fetch the first few images.
                    $scope.impactImgs = [];
                    var fbImgs = response.data;
                    for (var imgIdx = 0; imgIdx < fbImgs.length; imgIdx++) {
                        $scope.impactImgs.push({active: imgIdx == 0, src: fbImgs[imgIdx].source});
                    }
                });
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

    refreshEvent();
};

var tourCtrl = function($scope, FbUtil, $location) {
    $scope.login = function () {
        FbUtil.loginRequired(false).then( function () {
            $location.path("/");
        });
    }
};

kexApp.controller('NavbarController',
        [ '$scope', '$location', 'KarmaGoalUtil', 'KexUtil',
          function($scope, $location, KarmaGoalUtil, KexUtil) {
    $scope.isActive = function (url) {
        return $location.path() === KexUtil.stripHashbang(url);
    }

    $scope.completionIconStyle = KarmaGoalUtil.completionIconStyle;
}]);

var EventModalInstanceCtrl = function ($scope, $modalInstance, event, header, $rootScope) {
    $scope.event = event;
    $scope.header = header;
    $scope.ok = function () {
        $modalInstance.close();
    };
};

// TODO(avaliani): refactor this dependency
function isLoggedIn() {
    if( $.cookie( "facebook-token" ) ) {
        return true;
    } else {
        return false;
    }
}

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

kexApp.config( function( $routeProvider, $httpProvider, $facebookProvider ) {
    $routeProvider
        // .when( '/', { controller : homeCtrl, templateUrl : 'partials/home.html' } )
        // .when( '/home', { controller : homeCtrl, templateUrl : 'partials/home.html' } )
        .when( '/me', { controller : meViewCtrl, templateUrl : 'partials/me.html', reloadOnSearch: false } )
        .when( '/about', { templateUrl : 'partials/about.html', reloadOnSearch: false } )
        .when( '/tour', { controller : tourCtrl, templateUrl : 'partials/tour.html', reloadOnSearch: false } )
        .when( '/awards', { templateUrl : 'partials/awards.html', reloadOnSearch: false } )
        .when( '/contact', { templateUrl : 'partials/contact.html', reloadOnSearch: false } )
        .when( '/user/:userId', { controller : meViewCtrl, templateUrl : 'partials/me.html', reloadOnSearch: false } )
        .when( '/mysettings', { controller : meEditCtrl, templateUrl : 'partials/mysettings.html', reloadOnSearch: false } )
        .when( '/event', { controller : eventsCtrl, templateUrl : 'partials/events.html', reloadOnSearch: false } )
        .when( '/event/add', { controller : addEditEventsCtrl, templateUrl : 'partials/addEditevent.html', reloadOnSearch: false } )
        .when( '/event/:eventId/edit', { controller : addEditEventsCtrl, templateUrl : 'partials/addEditevent.html', reloadOnSearch: false } )
        .when( '/event/:eventId', { controller : viewEventCtrl, templateUrl : 'partials/viewEvent.html', reloadOnSearch: false } )
        .when( '/org', { controller : orgCtrl, templateUrl : 'partials/organization.html', reloadOnSearch: false } )
        .when( '/org/:orgId', { controller : orgDetailCtrl, templateUrl : 'partials/organizationDetail.html', reloadOnSearch: false } )
        .otherwise( { redirectTo : '/event' } );
    delete $httpProvider.defaults.headers.common[ 'X-Requested-With' ];
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
        xfbml : false });

})

// Force service instantiation to handle angular lazy instantation for the
// following services:
//   - MeUtil
//   - FbAuthDepResource
.run( function( $rootScope, Me, $location, FbUtil, $modal, MeUtil, $q, $http,
        FbAuthDepResource, KexUtil ) {
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

    $rootScope.setLocation = function (path) {
        $location.path(KexUtil.stripHashbang(path));
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

