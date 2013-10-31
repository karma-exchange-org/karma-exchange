angular.module("ui.bootstrap.ex", ["ui.bootstrap.ex.rating"]);

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
