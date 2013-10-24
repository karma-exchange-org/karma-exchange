angular.module("template/datepicker/datepicker.html", []).run(["$templateCache", function($templateCache) {
  $templateCache.put("template/datepicker/datepicker.html",
    "<table style=\"table-layout:fixed;\" class=\"table-condensed\">\n" +
    "  <!-- secondary: last month, disabled: disabled -->\n" +
    "  <thead class=\"text-center\">\n" +
    "    <tr>\n" +
    "      <th style=\"overflow: hidden; min-width: 26px\">\n" +
    "        <button type=\"button\" class=\"btn btn-xs btn-link\" ng-click=\"move(-1)\"> \n" +
    "          <span class=\"glyphicon glyphicon-chevron-left\"> </span> \n" +
    "        </button>\n" +
    "      </th>\n" +
    "      <th colspan=\"{{rows[0].length - 2 + showWeekNumbers}}\"><button type=\"button\" class=\"btn btn-md btn-link btn-block\" ng-click=\"toggleMode()\"><strong>{{title}}</strong></button></th>\n" +
    "      <th style=\"overflow: hidden; min-width: 26px\">\n" +
    "        <button type=\"button\" class=\"btn btn-xs btn-link\" ng-click=\"move(1)\"> \n" +
    "          <span class=\"glyphicon glyphicon-chevron-right\"> </span> \n" +
    "        </button>\n" +
    "      </th>\n" +
    "    </tr>\n" +
    "    <tr ng-show=\"labels.length > 0\">\n" +
    "      <th class=\"text-center\" ng-show=\"showWeekNumbers\" style=\"overflow: hidden; min-width: 26px\"><h6>#</h6></th>\n" +
    "      <th class=\"text-center\" ng-repeat=\"label in labels\" style=\"overflow: hidden; min-width: 26px\"><h6>{{label}}</h6></th>\n" +
    "    </tr>\n" +
    "  </thead>\n" +
    "  <tbody>\n" +
    "    <tr ng-repeat=\"row in rows\">\n" +
    "      <td ng-show=\"showWeekNumbers\" class=\"text-center\" style=\"overflow: hidden; min-width: 26px\"><button type=\"button\" class=\"btn btn-xs btn-link\" disabled><strong><em>{{ getWeekNumber(row) }}</em></strong></button></td>\n" +
    "      <td ng-repeat=\"dt in row\" class=\"text-center\" style=\"overflow: hidden; min-width: 26px\">\n" +
    "        <button type=\"button\" style=\"width: 100%; border: 0px\" class=\"btn btn-xs\" ng-class=\"{'btn-primary': dt.selected, 'btn-default': !dt.selected}\" ng-click=\"select(dt.date)\" ng-disabled=\"dt.disabled\"><span ng-class=\"{'text-muted': dt.secondary && !dt.selected}\">{{dt.label}}</span></button>\n" +
    "      </td>\n" +
    "    </tr>\n" +
    "  </tbody>\n" +
    "</table>");
}]);
