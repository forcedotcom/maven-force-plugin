#Force.com Maven Plugin

The Force.com Maven plugin enables you to generate Force.com JPA POJOs based on the objects already present in your Force.com organization.

##Configuring a Force.com Connection
All plugin configurations require a Force.com connection name. We recommend using an environment variable for configuration. The environment variable name depends on the connection name. For more about connection names, see the [Database.com Java SDK](http://forcedotcom.github.com/java-sdk/connection-url).

For example, if your connection name is `connName`, the associated environment variable must be `FORCE_CONNNAME_URL`.

On Mac or Linux:

<pre>
    <code>
    export FORCE_CONNNAME_URL=https://login.salesforce.com\;user=<em>username</em>\;password=<em>password</em>
    </code>
</pre>

**Note**: Each semi-colon must be escaped with a backslash.

On Windows:

<pre>
    <code>
    set FORCE_CONNNAME_URL=https://login.salesforce.com;user=<em>username</em>;password=<em>password</em>
    </code>
</pre>
    
##Maven Repository
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

##Basic Configuration
The basic configuration requires a Force.com connection name that is referenced in the `<connectionName>` element in a `pom.xml` file.

    <plugin>
      <groupId>com.force</groupId>
      <artifactId>maven-force-plugin</artifactId>
      <version>22.0.2-SNAPSHOT</version>
      <configuration>
        <all>true</all>
        <connectionName>connname</connectionName>
      </configuration>
    </plugin>
    
**Note**: For an explanation of the `<all>` element, see the next configuration section.

##Configuring Force.com JPA Entities for Code Generation
To configure POJO generation, add the following to your `pom.xml` file under the Force.com Maven plugin:

    <executions>
      <execution>
        <id>generate-force-entities</id>
        <goals>
          <goal>codegen</goal>
        </goals>
      </execution>
    </executions>

To generate POJOs for all objects in your organization, edit the `<configuration>` element to include `<all>true</all>`:

    <configuration>
      <all>true</all>
      <connectionName>connname</connectionName>
    </configuration>
    
To only include certain objects, use separate `<include>` elements instead:

    <configuration>
      <connectionName>connname</connectionName>
      <includes>
        <include>Account</include>
        <include>Contact</include>
        ...
      </includes>
    </configuration>

By default, the plugin follows all object references and generates all the necessary files so that generated source will compile. For example, the standard Contact entity has a relationship field to the Account entity. If you generate a Java class for the Contact entity, the code generator generates both Contact and Account classes, as well as classes for any other relationships for Contact.

If you don't want to follow object references, set the `<followReferences>` element to `false`. You can use `<include>` elements for any references that you want to follow.

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

This configuration generates classes for all objects excluding those listed in an `<exclude>` element.

**Note**: Your `<configuration>` element must include one of the following elements:

* `<all>true</all>`
* `<includes>`
* `<excludes>`

The default directory for generated Java source files is `src/main/java`. You can override the default by defining a `<destDir>` element.

The default Java package name is `com.<orgNameDenormalized>.model`, where `<orgNameDenormalized>` is an identifier that is automatically created from your organization name. You can override the default by defining a `<packageName>` element.

    <configuration>
      <all>true</all>
      <connectionName>connname</connectionName>
      <destDir>${basedir}/src/main/java</destDir>
      <packageName>com.mycompany.package.name</packageName>
    </configuration>
    
**Important Note**: If you plan on creating Force.com schema through the JPA provider, you should only ever run JPA code generation *once*.  Failing to do this
might result in conflicts among your Java classes if you manually change the generated code and then run code generation again. If you plan on managing Force.com schema outside of the JPA provider, then you may run JPA code generation as many times as you like.

##Generating Force.com JPA Entities
After you have configured your `pom.xml` file, generate JPE entities by running:

    mvn force:codegen

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

    mvn clean install -DskipTests -DforceCodeGen

##Build
The build requires Maven version 2.2.1 or higher.

    mvn clean install -DskipTests

##Run Tests
First mark the `force-test-connection.properties` file to be ignored by git:

    git update-index --assume-unchanged src/test/resources/force-test-connection.properties
    
This follows our recommended best practices of not checking authentication credentials into source control.    

Add Force.com database credentials to the `force-test-connection.properties` file and run:

    mvn test