angular.module( 'bootstrap', ['ngResource'] )

.config( function( $routeProvider ) {
  $routeProvider
      .when(
          '/',
          {
            controller: 'AdminConsoleCtrl',
            templateUrl: 'admin-console.html'
          })
      .when(
          '/configure-org',
          {
            controller: 'ConfigureOrgCtrl',
            templateUrl: 'configure-org.html',
          })
      .otherwise( { redirectTo : '/' } );
})

.factory('UserUsageRes', function( $resource ) {
  return $resource('/api/admin/user_usage');
})

.factory('EventSourceInfoResource', function( $resource ) {
    return $resource('/api/admin/event_source_info');
} )

.controller(
  'AdminConsoleCtrl',
  [
    '$scope', 'UserUsageRes',
    function($scope, UserUsageRes) {
      $scope.fetchUserUsage = function() {
          UserUsageRes.query(function(result) {
              result.sort(function (entry1, entry2) {
                  var name1 = entry1.firstName + ' ' + entry1.lastName;
                  var name2 = entry2.firstName + ' ' + entry2.lastName;
                  return name1.toLowerCase().localeCompare(name2.toLowerCase());
                  // return entry1.lastVisited - entry2.lastVisited;
              });
              $scope.userUsageInfo = result;
          });
      }

    }
  ])

.controller(
  'ConfigureOrgCtrl',
  [
    '$scope', 'EventSourceInfoResource',
    function($scope, EventSourceInfoResource) {

      $scope.eventSourceInfo = {};

      $scope.$watch('orgId', function( ) {
          load($scope.orgId);
      });

      $scope.loadPending = false;
      function load(orgId) {
        if (!$scope.loadPending) {

          $scope.error = undefined;

          if (orgId) {

            $scope.loadPending = true;
            EventSourceInfoResource.get(
              { orgId: orgId },
              function (value, responseHeaders) {
                $scope.eventSourceInfo = value;
                loadComplete();
              },
              function (response) {
                $scope.error = 'Error ' + response.status + ': ' + JSON.stringify(response.data);
                loadComplete();
              });

            function loadComplete() {
              $scope.loadPending = false;
              if (orgId != $scope.orgId) {
                load($scope.orgId);
              }
            }

          } else {

            $scope.eventSourceInfo = {};

          }

        }
      }

      $scope.save = function() {
        $scope.error = undefined;
        $scope.savePending = true;

        EventSourceInfoResource.save(
          { orgId: $scope.orgId },
          $scope.eventSourceInfo,
          function (value, responseHeaders) {
            $scope.eventSourceInfo = value;
            $scope.savePending = false;
          },
          function (response) {
            $scope.error = 'Error ' + response.status + ': ' + JSON.stringify(response.data);
            $scope.savePending = false;
          });
      }

      $scope.newSecret = function() {
        $scope.eventSourceInfo.secret = getUuid();
      }

      function getUuid() {
        // http://stackoverflow.com/questions/105034/how-to-create-a-guid-uuid-in-javascript
        return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function(c) {
            var r = Math.random()*16|0, v = c == 'x' ? r : (r&0x3|0x8);
            return v.toString(16);
        });
      }
    }
  ])

// TODO(avaliani): move to a shared library
.directive("btnLoading", function () {
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
})
;

// This is boilerplate code that is used to initialize the Facebook
// JS SDK.
window.fbAsyncInit = function() {
  FB.init({
    appId : fbAppId, // App ID
    status : true, // check login status
    cookie : true, // enable cookies to allow the server to access the session
    xfbml : true
  // parse page for xfbml or html5 social plugins like login button below
  });

  // Put additional init code here

  FB.getLoginStatus(function(response) {
    onStatus(response); // once on page load
    FB.Event.subscribe('auth.statusChange', onStatus); // every status change
  });
};

// Load the SDK Asynchronously
(function(d, s, id) {
  var js, fjs = d.getElementsByTagName(s)[0];
  if (d.getElementById(id)) {
    return;
  }
  js = d.createElement(s);
  js.id = id;
  js.src = "//connect.facebook.net/en_US/all.js";
  fjs.parentNode.insertBefore(js, fjs);
}(document, 'script', 'facebook-jssdk'));

function showBootstrapCurrentUserButton(response) {
  var uid = response.authResponse.userID;
  var token = response.authResponse.accessToken;
  var url = '/bootstrap/provider/fb/register?' + "uid=" + uid + "&" + "token=" + token;
  var button = '<button onclick="consoleLoggedRequest(' + url + ')"' +
    ' type="submit" class="btn btn-block btn-default btn-admin-op">Bootstrap current user</button>';
  document.getElementById('account-info').innerHTML = (button);
}

function showLoginButton() {
  document.getElementById('account-info').innerHTML = ('<button class="btn btn-block btn-primary btn-admin-op" onclick="FB.login()">Login and connect via facebook</button>');
}

/**
 * This will be called once on page load, and every time the status changes.
 */
function onStatus(response) {
  // Log.info('onStatus', response);
  if (response.status === 'connected') {
    showBootstrapCurrentUserButton(response);
  } else {
    showLoginButton();
  }
}

function consoleLoggedRequest(url) {
  var consoleOutput = '#consoleOutput';
  $(consoleOutput).val('Issuing request url="' + url + '"...');
  $.ajax({
    type: "GET",
    url: url,
    success: function(response) {
      $(consoleOutput).val(response);
    },
    error: function(xhr, status, error) {
      // TODO(avaliani): look into rendering html results in a responsive iframe.
      $(consoleOutput).val(
        "Operation failed:\n" +
        url + "\n" +
        "\n" +
        xhr.responseText);
    }
  });
}
