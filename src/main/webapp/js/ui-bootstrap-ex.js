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
      this._defaultActiveTabName = null;
    }

    function Tab(props) {
      this.active = angular.isDefined(props.active) ? props.active : false;
      this.disabled = angular.isDefined(props.disabled) ? props.disabled : false;
      this.onSelectionCb = props.onSelectionCb;
    }

    TabManager.prototype.addTab = function(tabName, props) {
      this.tabs[tabName] = new Tab(props);
    }

    TabManager.prototype.init = function() {
      this._determineDefaultActiveTabName();
      this._checkUrlForTabName();
    }

    TabManager.prototype._determineDefaultActiveTabName = function() {
      var activeTabAndTabName = this._findActiveTab();
      if (activeTabAndTabName) {
        this._defaultActiveTabName = activeTabAndTabName.tabName;
      }
    }

    TabManager.prototype._findActiveTab = function() {
      var tabAndTabName = null;
      angular.forEach(this.tabs, angular.bind(this, function(tab, tabName) {
        if (tab.active) {
          tabAndTabName = { tab: tab, tabName: tabName };
        }
      }));
      return tabAndTabName;
    }

    TabManager.prototype._checkUrlForTabName = function() {
      if ($routeParams.tab && this.tabs[$routeParams.tab]) {
        this.markTabActive($routeParams.tab);
        this.tabSelectedByUrl = true;
      }
    }

    TabManager.prototype.markTabActive = function(tabName) {
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
      if ((tabName != this._defaultActiveTabName) || this.tabSelectedByUrl) {
        $location.search('tab', tabName);
        this.tabSelectedByUrl = true;
      }
    }

    TabManager.prototype.reloadActiveTab = function() {
      var activeTabAndTabName = this._findActiveTab();
      if (activeTabAndTabName && activeTabAndTabName.tab.onSelectionCb) {
        activeTabAndTabName.tab.onSelectionCb();
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
