angular.module( "bootstrap", ["ngResource"] )

.factory('UserUsageRes', function( $resource ) {
    return $resource('/api/admin/user_usage');
})

;

var BootstrapCtrl = function ($scope, UserUsageRes) {
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
};


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
