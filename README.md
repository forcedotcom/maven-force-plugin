#Force.com Maven plugin.

##Using the Plugin
All plugin configurations require a Force.com connection name.  For more about connection names see the [Database.com Java SDK](http://forcedotcom.github.com/java-sdk/connection-url).
We recommended using an environment variable for configuration.  For example:

    $ export FORCE_CONNNAME_URL=https://login.salesforce.com\;user=username\;password=password
    
###Maven Repository
The Force.com Maven plugin currently only has SNAPSHOT versions.  These are hosted on Salesforce.com's Maven repository.

To use the repository, add the following to your `pom.xml` file:

    <pluginRepositories>
      <pluginRepository>
        <id>force.repo.snapshot</id>
        <name>Force.com Snapshot Repository</name>
        <url>http://repo.t.salesforce.com/archiva/repository/snapshots</url>
        <snapshots>
          <enabled>true</enabled>
        </snapshots>
        <releases>
          <enabled>false</enabled>
        </releases>
      </pluginRepository>
    </pluginRepositories> 

###Basic Configuration
The basic configuration requires a Force.com connection name that is referenced in the `<connectionName>` element.

    <plugin>
      <groupId>com.force</groupId>
      <artifactId>maven-force-plugin</artifactId>
      <version>22.0.2-SNAPSHOT</version>
      <configuration>
        <connectionName>connname</connectionName>
      </configuration>
    </plugin>
    
###Generating Force.com JPA Entities
The Force.com Maven plugin enables you to generate Force.com JPA POJOs based on the objects already present in your Force.com organization.

To generate POJOs, add the following to your `pom.xml` file:

    <executions>
      <execution>
        <id>generate-force-entities</id>
        <goals>
          <goal>codegen</goal>
        </goals>
      </execution>
    </executions>

To generate POJOs for all objects in your organization, edit the `<configuration>` element:

    <configuration>
      <all>true</all>
      <connectionName>connname</connectionName>
    </configuration>
    
To only include certain objects, use separate `<include>` elements:

    <configuration>
      <connectionName>connname</connectionName>
      <includes>
        <include>Account</include>
        <include>Contact</include>
        ...
      </includes>
    </configuration>

By default, the plugin follows all object references and generates all the necessary files so that generated source will compile.  If you do not wish to
follow object references, this can be turned off by setting the `<followReferences>` element to `false`:

    <configuration>
      <connectionName>connname</connectionName>
      <followReferences>false</followReferences>
      <includes>
        <include>Account</include>
        <include>Contact</include>
        ...
      </includes>
    </configuration>

To exclude certain objects:

    <configuration>
      <connectionName>connname</connectionName>
      <excludes>
        <exclude>Opportunity</exclude>
        <exclude>CustomObject__c</exclude>
        ...
      </excludes>
    </configuration>

You can also override the default Java package name and destination directory:

    <configuration>
      <all>true</all>
      <connectionName>connname</connectionName>
      <destDir>${basedir}/src/main/java</destDir>
      <packageName>com.mycompany.package.name</packageName>
    </configuration>
    
**Important Note:** If you plan on creating Force.com schema through the JPA provider then you should only ever run JPA code generation *once*.  Failing to do this
might result in conflicts among your Java classes.  If you plan on managing Force.com schema outside of the JPA provider, then you may run JPA code generation as
many times as you'd like.

###Generating Force.com JPA Entities on Heroku
Heroku does not allow environment variables at build time.  This means that your Force.com database credentials will not be available at
build time for the JPA code generation to run.  There are two options:

1. Check in Force.com database credentials (*not recommended*)
2. Pre-run the JPA code generation and check in the resulting source code files.

The latter requires an opt-in strategy for running code generation.  Here's an example:

    <profile>
      <id>force-codegen</id>
      <activation>
        <property>
          <name>forceCodeGen</name>
          <value>true</value>
        </property>
      </activation>
      
      <build>
        <plugins>
        
          <plugin>
            <groupId>com.force</groupId>
            <artifactId>maven-force-plugin</artifactId>
            <version>22.0.2-SNAPSHOT</version>
            <executions>
              <execution>
                <id>generate-force-entities</id>
                <goals>
                  <goal>codegen</goal>
                </goals>
              </execution>
            </executions>
            <configuration>
              <all>true</all>
              <connectionName>connname</connectionName>
              <destDir>${basedir}/src/main/java</destDir>
              <packageName>com.mycompany.package.name</packageName>
            </configuration>
          </plugin>
          
        </plugins>
      </build>
    </profile>
    
Now you can opt-in to code generation with the following:

    $ mvn clean install -DskipTests -DforceCodeGen

##Build
The build requires Maven version 2.2.1 or higher.

    $ mvn clean install -DskipTests

##Run Tests
First mark the force-test-connection.properties file to be ignored by git:

    $ git update-index --assume-unchanged src/test/resources/force-test-connection.properties

Add Force.com database credentials to the force-test-connection.properties file and run:

    $ mvn test