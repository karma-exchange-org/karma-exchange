## Karma Exchange

Copyright (C) 2013 Karma Exchange

### Building and Running

Requires 
* [Apache Maven](http://maven.apache.org) 3.1
* JDK 7 in order to run.

You must also specify the facebook app id and secret key in **src/main/webapp/WEB-INF/app-private.properties**. This file is not on github for security reasons. File format:

    [app-domain]-facebook-app-id = [faceboo-app-id]
    [app-domain]-facebook-app-secret = [facebook-app-secret]
    ajax-snapshots-snapshot-service-token = [token]
    prerender-snapshot-service-token = [token]

To run the app locally, use the [App Engine Maven Plugin](http://code.google.com/p/appengine-maven-plugin/)

    mvn appengine:devserver

To create test content go to **[app-domain:app-port]/bootstrap**, login as an admin, click "delete all resources", and then click "Bootstrap Test Resources". Wait for both to complete.

To generate javadocs, run

    mvn lombok:delombok
    mvn javadoc:javadoc

To see all the available goals for the App Engine plugin, run

    mvn help:describe -Dplugin=appengine

For more information on maven features read the [App Engine Maven Integeration Wiki](https://developers.google.com/appengine/docs/java/tools/maven)

### Eclipse IDE Support

Make sure you're using eclipse juno (3.8 / 4.2) or a more recent version.

1. Close eclipse and install [lombok](http://projectlombok.org/). 
  * Download the jar.
  * Execute it by double clicking on it. Doing this will add lombok to eclipse.ini.

2. Install the maven eclipse plugins from http://download.jboss.org/jbosstools/updates/m2eclipse-wtp (all of them) and restart eclipse.

3. Open your workspace / create a new workspace and import the maven project: File --> Import --> Maven --> Existing Maven Project.

4. Disable eclipse's javascript validator (it doesn't seem to handle angular well): Project->Properties->Builders then uncheck the‘Javascript Validator’.

5. To run the tests: F11 --> Maven Test

6. To run the dev app server: F11 --> Maven Build --> DevAppServer: appengine:devserver

For more information see the [Google Plugin Maven Integeration Wiki](https://code.google.com/p/google-web-toolkit/wiki/WorkingWithMaven)

