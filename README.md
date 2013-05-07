Karma Exchange

Copyright (C) 2013 Karma Exchange

## Maven details:

Requires [Apache Maven](http://maven.apache.org) 3.0 or greater, and JDK 6+ in order to run.

To build, run

    mvn package

Building will run the tests, but to explicitly run tests you can use the test target

    mvn test

To start the app, use the [App Engine Maven Plugin](http://code.google.com/p/appengine-maven-plugin/) that is already included in this demo.  Just run the command.

    mvn appengine:devserver

For further information, consult the [Java App Engine](https://developers.google.com/appengine/docs/java/overview) documentation.

To see all the available goals for the App Engine plugin, run

    mvn help:describe -Dplugin=appengine

Further reading [App Engine Maven Integeration Wiki](https://developers.google.com/appengine/docs/java/tools/maven)

## Eclipse details:

Make sure you're using eclipse juno (3.8 / 4.2) or a more recent version.

1. Close eclipse and install [lombok](http://projectlombok.org/). Download the jar and execute it / double click it to add it to eclipse.ini.

2. Install the maven eclipse plugins from http://download.jboss.org/jbosstools/updates/m2eclipse-wtp (all of them) and restart eclipse.

3. Open your workspace / create a new workspace and import the maven project: File --> Import --> Maven --> Existing Maven Project.

4. To run the tests: F11 --> Maven Test

5. To run the dev app server: F11 --> Maven Build --> DevAppServer: appengine:devserver

Further reading [Google Plugin Maven Integeration Wiki](https://code.google.com/p/google-web-toolkit/wiki/WorkingWithMaven)

