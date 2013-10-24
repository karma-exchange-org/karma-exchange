angular.module("template/timepicker/timepicker.html", []).run(["$templateCache", function($templateCache) {
  $templateCache.put("template/timepicker/timepicker.html",
    "<span>\n" +
    "    <div class=\"row\">\n" +
    "        <div class=\"col-xs-4 text-center\">\n" +
    "            <a ng-click=\"incrementHours()\" class=\"btn btn-link\"><i class=\"glyphicon glyphicon-chevron-up\"></i></a>\n" +
    "        </div>\n" +
    "        <div class=\"col-xs-6 text-center\">\n" +
    "            <a ng-click=\"incrementMinutes()\" class=\"btn btn-link\"><i class=\"glyphicon glyphicon-chevron-up\"></i></a>\n" +
    "        </div>\n" +
    "        <div class=\"col-xs-2\"> </div>\n" +
    "    </div>\n" +
    "\n" +
    "    <div class=\"row\">\n" +
    "        <div class=\"col-xs-4\">\n" +
    "            <div class=\"form-group\" ng-class=\"{'has-error': invalidHours}\" style=\"margin-bottom: 0px\">\n" +
    "                <input type=\"text\" ng-model=\"hours\" ng-change=\"updateHours()\" class=\"form-control text-center\" ng-mousewheel=\"incrementHours()\" ng-readonly=\"readonlyInput\" maxlength=\"2\"> \n" +
    "            </div>\n" +
    "        </div>\n" +
    "        <div class=\"col-xs-6\">\n" +
    "            <div class=\"input-group\" ng-class=\"{'has-error': invalidMinutes}\">\n" +
    "                <span class=\"input-group-addon\">:</span>\n" +
    "                <input type=\"text\" ng-model=\"minutes\" ng-change=\"updateMinutes()\" class=\"form-control text-center\" ng-readonly=\"readonlyInput\" maxlength=\"2\">\n" +
    "            </div>\n" +
    "        </div>\n" +
    "        <div class=\"col-xs-2\">\n" +
    "            <button ng-click=\"toggleMeridian()\" class=\"btn btn-default text-center\" ng-show=\"showMeridian\">{{meridian}}</button>\n" +
    "        </div>\n" +
    "    </div>\n" +
    "\n" +
    "    <div class=\"row\">\n" +
    "        <div class=\"col-xs-4 text-center\">\n" +
    "            <a ng-click=\"decrementHours()\" class=\"btn btn-link\"><i class=\"glyphicon glyphicon-chevron-down\"></i></a>\n" +
    "        </div>\n" +
    "        <div class=\"col-xs-6 text-center\">\n" +
    "            <a ng-click=\"decrementMinutes()\" class=\"btn btn-link\"><i class=\"glyphicon glyphicon-chevron-down\"></i></a>\n" +
    "        </div>\n" +
    "        <div class=\"col-xs-2\"> </div>\n" +
    "    </div>\n" +
    "</span>");
}]);
