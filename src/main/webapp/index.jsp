<%
    if (request.getParameter("metaonly") != null) {
        //Do server side SEO stuff
        out.println(org.karmaexchange.util.SEOUtil.fetchFBOGTags(request));
    }
    else if (request.getParameter("_escaped_fragment_") != null) {
        //Do server side SEO stuff
        out.println(org.karmaexchange.util.SEOUtil.fetchURL(request));
    } else {
     %>
<!DOCTYPE HTML>
<!--[if lt IE 7]>      <html class="no-js lt-ie9 lt-ie8 lt-ie7"> <![endif]-->
<!--[if IE 7]>         <html class="no-js lt-ie9 lt-ie8"> <![endif]-->
<!--[if IE 8]>         <html class="no-js lt-ie9"> <![endif]-->
<!--[if gt IE 8]><!-->
<html ng-app="kexApp" class="no-js">
    <!--<![endif]-->
    <head>
        <meta charset="utf-8">
        <meta http-equiv="X-UA-Compatible" content="IE=edge,chrome=1">
        <title>Karma Exchange</title>
        <meta name="description" content>
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
        <meta name="fragment" content="!">
        <link rel="stylesheet" href="css/bootstrap.min.css">
        <!-- <link rel="stylesheet" href="//netdna.bootstrapcdn.com/bootstrap/3.0.0/css/bootstrap.min.css"> -->
        <link rel="stylesheet" href="//netdna.bootstrapcdn.com/bootstrap/3.0.0/css/bootstrap-theme.min.css">

        <!-- SVG icons -->
        <link href="//netdna.bootstrapcdn.com/font-awesome/3.1.1/css/font-awesome.css" rel="stylesheet">

        <link href="//code.jquery.com/ui/1.10.3/themes/smoothness/jquery-ui.css">

        <!-- Edit about me++ -->
        <link href="css/bootstrap-editable.css" rel="stylesheet">

        <!-- File upload. Future: rowlink++ -->
        <link href="css/jasny-bootstrap.min.css" rel="stylesheet">
        <link href="css/jasny-bootstrap-responsive.min.css" rel="stylesheet">

        <link href="css/jquery.timepicker.css" rel="stylesheet">

        <link href="css/fullcalendar.css" rel="stylesheet">

        <!-- Recurring events -->
        <link href="css/jquery.recurrenceinput.css" rel="stylesheet">
        <link href="css/overlays.css" rel="stylesheet">


        <!-- Good for facebook albums.
            <link href="css/plusgallery.css" rel="stylesheet">
        -->
        <link href="css/angular-social.css" rel="stylesheet">

        <link rel="stylesheet" href="css/main.css">
        <style>
            .ng-cloak { display: none; }
        </style>
        <!--[if lt IE 9]>
            <script src="//html5shiv.googlecode.com/svn/trunk/html5.js"></script>
            <script>window.html5 || document.write('<script src="js/vendor/html5shiv.js"><\/script>')</script>
        <![endif]-->
    </head>

    <body>
        <!--[if lt IE 7]>
            <p class="chromeframe">You are using an <strong>outdated</strong> browser. Please <a href="http://browsehappy.com/">upgrade your browser</a> or <a href="http://www.google.com/chromeframe/?redirect=true">activate Google Chrome Frame</a> to improve your experience.</p>
        <![endif]-->
        <!-- This code is taken from http://twitter.github.com/bootstrap/examples/hero.html -->
        <script type="text/javascript" src="//www.google.com/jsapi"></script>

        <script src="//ajax.googleapis.com/ajax/libs/jquery/1.8.3/jquery.min.js"></script>
        <script src="//ajax.googleapis.com/ajax/libs/angularjs/1.0.8/angular.min.js"></script>

        <script src="//ajax.googleapis.com/ajax/libs/jqueryui/1.10.3/jquery-ui.min.js"></script>

        <script src="js/vendor/angular-resource.min.js"></script>
        <script src="js/vendor/angular-cookies-min.js"></script>

        <script src="js/vendor/angular-social.js"></script>

        <script src="js/vendor/ngFacebook.js"></script>

        <!-- We can get rid of this once we start using angular cookies. -->
        <script src="js/vendor/jquery-cookie-min.js"></script>

        <script src="js/vendor/bootstrap.min.js"></script>
        <!-- <script src="//netdna.bootstrapcdn.com/bootstrap/3.0.0/js/bootstrap.min.js"></script> -->

        <script src="//maps.googleapis.com/maps/api/js?libraries=places&sensor=false&language=en"></script>
        <script src="js/vendor/angular-googlemaps-min.js"></script>

        <script src="js/vendor/bootstrap-datetimepicker.min.js"></script>

        <!-- Edit about me -->
        <script src="js/vendor/bootstrap-editable.min.js"></script>

        <script src="js/vendor/jasny-bootstrap.min.js"></script>

        <!-- angular-ui boostrap 3.0 compatabile -->
        <!-- <script src="js/vendor/ui-bootstrap-0.6.0-SNAPSHOT.js"></script> -->
        <script src="js/vendor/ui-bootstrap-tpls-0.6.0-SNAPSHOT.js"></script>
        <script src="js/ui-bootstrap-ex.js"></script>

        <script src="js/vendor/jquery.timepicker.min.js"></script>

        <script src="js/vendor/fullcalendar.min.js"></script>

        <!-- Recurring events. -->
        <script type="text/javascript" src="js/vendor/jquery.tools.min.js"></script>
        <script type="text/javascript" src="js/vendor/jquery.tmpl-beta1.js"></script>
        <script type="text/javascript" src="js/vendor/jquery.recurrenceinput.js"></script>

        <script src="js/vendor/calendar.js"></script>

        <script src="js/vendor/bit-array.js"></script>

        <!-- For date time manipulation -->
        <script src="js/vendor/moment.min.js"></script>

        <!-- Bootstrap related files. -->
        <script src="js/app.js"></script>
        <script src="js/plugins.js"></script>

        <!-- Good for displaying facebook galleries.
            <script src="js/vendor/plusgallery.js">
            </script>
         -->
        <!-- Might be required for recurring events.
            <script src="//ajax.googleapis.com/ajax/libs/mootools/1.4.5/mootools-yui-compressed.js"></script>
        -->
        <!-- Javascript based grid layout.
            <script src="js/vendor/jquery.masonry.min.js"></script>
        -->

        <!-- Google analytics placeholder. -->
        <script>
          (function(i,s,o,g,r,a,m){i['GoogleAnalyticsObject']=r;i[r]=i[r]||function(){
          (i[r].q=i[r].q||[]).push(arguments)},i[r].l=1*new Date();a=s.createElement(o),
          m=s.getElementsByTagName(o)[0];a.async=1;a.src=g;m.parentNode.insertBefore(a,m)
          })(window,document,'script','//www.google-analytics.com/analytics.js','ga');

          ga('create', 'UA-45011314-1', 'karmaexchange.org');
          ga('send', 'pageview');

        </script>


        <nav class="navbar navbar-fixed-top navbar-inverse" role="navigation">
            <!-- Keep the navbar content at container width. The navbar background extends across the screen. -->
            <div class="container">
                <div class="navbar-header">
                    <button type="button" class="navbar-toggle" toggle="collapse" target=".navbar-responsive-collapse">
                        <span class="icon-bar">
                        </span>
                        <span class="icon-bar">
                        </span>
                        <span class="icon-bar">
                        </span>
                    </button>
                    <a class="navbar-brand" href="#!/event">KarmaExchange </a>
                </div>
                <div class="navbar-collapse collapse navbar-responsive-collapse">
                    <ul class="nav navbar-nav">
                        <li>
                            <a href="#!/event"><i class="icon-search icon-white"></i> Find Events</a>
                        </li>
                        <li ng-show="me">
                            <a href="#!/me"><i class="icon-calendar icon-white"></i> My Events</a>
                        </li>
                        <li>
                            <a href="#!/org"><i class="icon-group icon-white"></i> Organizations</a>
                        </li>
                        <li ng-show="isOrganizer">
                            <a href="#!/event/add"><i class="icon-plus icon-white"></i> Create Events</a>
                        </li>
                    </ul>
                    <ul class="nav navbar-nav navbar-right">
                        <!--
                            <li ng-hide="me">
                                    <fb:login-button scope="{{fbUtil.LOGIN_SCOPE}}" width="200" max-rows="1">
                                    </fb:login-button>
                            </li>
                        -->
                        <li ng-show="goalInfo.msg">
                            <div class="navbar-goal-pct navbar-goal-pct-{{goalInfo.barType}}"
                                    popover-placement="bottom"
                                    popover="Karma&nbsp;Goal Completion&nbsp;% for&nbsp;{{ goalInfo.goalStartDate | date:'MMM-yyyy' }}"
                                    popover-trigger="mouseenter"
                                    popover-popup-delay="250">
                                <a href="#!/me">
                                    {{goalInfo.pctTotal}} %
                                </a>
                            </div>
                        </li>
                        <li class="dropdown">
                            <a ng-show="me" href="#" class="dropdown-toggle" toggle="dropdown">Welcome, {{me.firstName}} <b class="caret"></b></a>
                            <a ng-hide="me" href="" class="dropdown-toggle" toggle="dropdown">Info <b class="caret"></b></a>
                            <ul class="dropdown-menu">
                                <li ng-show="me">
                                    <a href="#!/mysettings"><i class="icon-cog"></i> Profile</a>
                                </li>
                                <li ng-show="me">
                                    <a href="/#" ng-click="fbUtil.logout()"><i class="icon-off"></i> Logout</a>
                                </li>
                                <li ng-show="me" class="divider"></li>
                                <li>
                                    <a href="#!/about"><i class="icon-info"></i> About us</a>
                                </li>
                                <li>
                                    <a href="#!/contact"><i class="icon-envelope"></i> Contact us</a>
                                </li>
                            </ul>
                        </li>
                        <li ng-hide="me">
                            <button type="button" ng-click="fbUtil.login()" class="btn btn-default navbar-btn">
                                <img src="/img/facebook.png" class="kex-icon"> Login
                            </button>
                        </li>
                    </ul>
                </div>
            </div>
        </nav>
        <div class="container">
            <div class="messagesList" app-messages>
            </div>
            <div class="row">
                <alert ng-repeat="alert in alerts" type="alert.type" close="closeAlert($index)">
                    {{alert.msg}}
                </alert>
            </div>
            <div ng-view>
            </div>
            <div>
                <form ng-show="isMessageOpen" class="add-window" ng-submit="sendMessage()">
            <div class="header">
                Send a message
                <a ng-click="cancelMessage()"><i class="icon-remove pull-right"></i></a>
            </div>
            <div class="inner">
                <div>
                <input class="row form-control" type="text" ng-model="message.subject" required placeholder="Subject">
                <textarea class="row" ng-model="message.body" rows="5" placeholder="Message">
                </textarea>
                </div>
                <div class="button-bar">
                <input type="submit" value="Send" class="btn btn-primary form-control">
                <button type="button" class="btn btn-default" ng-click="cancelMessage()">
                    Cancel
                </button>
                </div>
            </div>
            </form>
            </div>

        </div>
        <div>
            <div id="fb-root">
            </div>
        </div>

    </body>

</html>
<% } %>
