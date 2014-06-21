angular.module("ui.bootstrap.ex", ["ui.bootstrap.ex.rating", "ui.bootstrap.ex.urltabs"]);

/**
 * ui.bootstrap.ex.rating extends ui.bootstrap.rating by providing an onUpdate() callback.
 */

angular.module('ui.bootstrap.ex.rating', [])

.constant('ratingConfigEx', {
  max: 5,
  stateOn: null,
  stateOff: null
})

.controller('RatingControllerEx', ['$scope', '$attrs', '$parse', 'ratingConfigEx', function($scope, $attrs, $parse, ratingConfigEx) {

  this.maxRange = angular.isDefined($attrs.max) ? $scope.$parent.$eval($attrs.max) : ratingConfigEx.max;
  this.stateOn = angular.isDefined($attrs.stateOn) ? $scope.$parent.$eval($attrs.stateOn) : ratingConfigEx.stateOn;
  this.stateOff = angular.isDefined($attrs.stateOff) ? $scope.$parent.$eval($attrs.stateOff) : ratingConfigEx.stateOff;

  this.createDefaultRange = function(len) {
    var defaultStateObject = {
      stateOn: this.stateOn,
      stateOff: this.stateOff
    };

    var states = new Array(len);
    for (var i = 0; i < len; i++) {
      states[i] = defaultStateObject;
    }
    return states;
  };

  this.normalizeRange = function(states) {
    for (var i = 0, n = states.length; i < n; i++) {
      states[i].stateOn = states[i].stateOn || this.stateOn;
      states[i].stateOff = states[i].stateOff || this.stateOff;
    }
    return states;
  };

  // Get objects used in template
  $scope.range = angular.isDefined($attrs.ratingStates) ?  this.normalizeRange(angular.copy($scope.$parent.$eval($attrs.ratingStates))): this.createDefaultRange(this.maxRange);

  $scope.rate = function(value) {
    if ( $scope.readonly || $scope.value === value) {
      return;
    }

    $scope.value = value;
    $scope.onUpdate({newValue: value});
  };

  $scope.enter = function(value) {
    if ( ! $scope.readonly ) {
      $scope.val = value;
    }
    $scope.onHover({value: value});
  };

  $scope.reset = function() {
    $scope.val = angular.copy($scope.value);
    $scope.onLeave();
  };

  $scope.$watch('value', function(value) {
    $scope.val = value;
  });

  $scope.readonly = false;
  if ($attrs.readonly) {
    $scope.$parent.$watch($parse($attrs.readonly), function(value) {
      $scope.readonly = !!value;
    });
  }
}])

.directive('ratingEx', function() {
  return {
    restrict: 'EA',
    scope: {
      value: '=',
      onHover: '&',
      onLeave: '&',
      onUpdate: '&'
    },
    controller: 'RatingControllerEx',
    template:
      '<span ng-mouseleave="reset()">' +
        '<i ng-repeat="r in range" ng-mouseenter="enter($index + 1)" ng-click="rate($index + 1)" ng-class="$index < val && (r.stateOn || \'icon-star\') || (r.stateOff || \'icon-star-empty\')"></i>' +
      '</span>',
    replace: true
  };
});


/**
 * ui.bootstrap.ex.urltabs extends ui.bootstrap.tabs by tabs that update the url.
 */

angular.module('ui.bootstrap.ex.urltabs', [])

.factory('urlTabsetUtil', ["$location", "$routeParams", function($location, $routeParams) {
    function TabManager() {
      this.tabs = {};
      this.tabSelectedByUrl = false;
      this._apiTabSelect = undefined;
      this._defaultActiveTabName = null;
      this._init = false;
    }

    function Tab(props) {
      this.active = angular.isDefined(props.active) ? props.active : false;
      this.disabled = angular.isDefined(props.disabled) ? props.disabled : false;
      this.onSelectionCb = props.onSelectionCb;
    }

    // Adds pre-init or post-init (a.k.a. dynamic) tabs.
    TabManager.prototype.addTab = function(tabName, props) {
      this.tabs[tabName] = new Tab(props);
      if (this._init) {
        this._checkUrlForDynamicTabName(tabName);
      }
    }

    // Since init determines the default active tab it should be called after adding
    // all non-dynamic tabs.
    TabManager.prototype.init = function() {
      this._init = true;
      this._determineDefaultActiveTabName();
      this._checkUrlForTabName();
    }

    TabManager.prototype._determineDefaultActiveTabName = function() {
      var tabAndName = this._findActiveTab();
      if (tabAndName) {
        this._defaultActiveTabName = tabAndName.tabName;
      }
    }

    TabManager.prototype._findActiveTab = function() {
      var tabAndName = null;
      angular.forEach(this.tabs, angular.bind(this, function(tab, tabName) {
        if (tab.active) {
          tabAndName = { tab: tab, tabName: tabName };
        }
      }));
      return tabAndName;
    }

    TabManager.prototype._checkUrlForTabName = function() {
      if ($routeParams.tab && this.tabs[$routeParams.tab]) {
        this._markTabActive($routeParams.tab);
        this.tabSelectedByUrl = true;
      }
    }

    TabManager.prototype._checkUrlForDynamicTabName = function(tabName) {
      if (!this.tabSelectedByUrl && ($routeParams.tab == tabName)) {
        this._checkUrlForTabName();
      }
    }

    TabManager.prototype.markTabActive = function(tabName) {
      // Internal automatic tab changes should not happen when a tab is explicitly
      // selected by the url.
      if (!this.tabSelectedByUrl) {
        this._markTabActive(tabName);
        // If we update the url when we internally select tabs then the
        // browser back button has strange behavior (no-effect on first click) because
        // (a) we don't reload the page on search parameter changes and (b)
        // even if we did reload the page, internal automatic tab changes would
        // be re-applied.
        this._apiTabSelect = tabName;
      }
    }

    TabManager.prototype._markTabActive = function(tabName) {
      this._deactivateTabs();
      this.tabs[tabName].active = true;
    }

    TabManager.prototype._deactivateTabs = function() {
      angular.forEach(this.tabs, angular.bind(this, function(tab) {
        tab.active = false;
      }));
    }

    TabManager.prototype.select = function(tabName) {
      if (this.tabs[tabName].onSelectionCb) {
        this.tabs[tabName].onSelectionCb();
      }
      if ((this._apiTabSelect != tabName) &&
          ( (tabName != this._defaultActiveTabName) || this.tabSelectedByUrl )) {
        $location.search('tab', tabName);
        this.tabSelectedByUrl = true;
      }
      this._apiTabSelect = undefined;
    }

    TabManager.prototype.reloadActiveTab = function() {
      var tabAndName = this._findActiveTab();
      if (tabAndName && tabAndName.tab.onSelectionCb) {
        tabAndName.tab.onSelectionCb();
      }
    }

    return {
      // NOTE(avaliani): we could have defined TabManager and Tab in the global context.
      // but it seemed cleaner not to. See what the best practices are.
      createTabManager: function() {
        return new TabManager();
      }
    }
}])

;
