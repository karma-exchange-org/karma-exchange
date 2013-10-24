angular.module("template/datepicker/popup.html", []).run(["$templateCache", function($templateCache) {
  $templateCache.put("template/datepicker/popup.html",
    "<ul class=\"dropdown-menu\" ng-style=\"{display: (isOpen && 'block') || 'none', top: position.top+'px', left: position.left+'px'}\" class=\"dropdown-menu\">\n" +
    "	<li ng-transclude></li>\n" +
    "	<li class=\"divider\"></li>\n" +
    "	<li style=\"padding: 9px;\">\n" +
    "		<span class=\"btn-group\">\n" +
    "			<button class=\"btn btn-xs btn-default\" ng-click=\"today()\">Today</button>\n" +
    "			<button class=\"btn btn-xs btn-info\" ng-click=\"showWeeks = ! showWeeks\" ng-class=\"{active: showWeeks}\">Weeks</button>\n" +
    "			<button class=\"btn btn-xs btn-danger\" ng-click=\"clear()\">Clear</button>\n" +
    "		</span>\n" +
    "		<button class=\"btn btn-xs btn-success pull-right\" ng-click=\"isOpen = false\">Close</button>\n" +
    "	</li>\n" +
    "</ul>");
}]);
