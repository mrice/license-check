license-check
=============

[![Build Status](https://travis-ci.org/mrice/license-check.png)](https://travis-ci.org/mrice/license-check)

Current version: 0.5.3 (Sep 5, 2015)

What is it?
--------------
For now, **license-check** just checks to make sure that your Maven dependencies have a license declared in their POM
files and that you aren't including a license on your project's blacklist. There's more on the horizon but this is an
early release.

How it works
--------------
License-check looks at each dependency and runs a query against your Maven respository to see if the dependency declares
a license that it recognizes. If not, then your build will **fail**. (Don't worry if you're hoping for a different
result, there's a way around this if your dependency isn't clear on its licensing. See the configuration options below.)

Isn't there already something like this?
---------------
**Eh, not really.** There are a few different Maven plugins for doing license "things." But the purpose of this plugin
is (or, I should say, will be) to help you make sure you're not including licenses you don't want to. For now, however,
the plugin just makes sure it recognizes the declared licenses as one of the [opensource.org](http://www.opensource.org/) registered licenses.

This doesn't sound like much, but it's critically important. If the license isn't recognized or isn't declared at all,
it's very possible that the authors or contributors could claim fully copyright in the library and expose you to a lot
of liability.

How to use it
---------------
Put license-check into your build process by adding the following to your pom.xml:

```xml

<build>
  <plugins>
    <plugin>
      <groupId>org.complykit</groupId>
      <artifactId>license-check-maven-plugin</artifactId>
      <version>0.5.3</version>
      <executions>
        <execution>
          <phase>verify</phase>
          <goals>
            <goal>os-check</goal>
          </goals>
        </execution>
      </executions>
    </plugin>
  </plugins>
</build>

```

When you do this, **your builds will start failing** if you include a dependency with an unrecognized license (or,
worse, an undeclared a license).

Configuration options
---------------
**Create a blacklist of licenses:** Right or wrong, many organizations and their legal teams have declared certain
licenses to be incompatible with their own licenseing goals. Detecting those licenses and failing your build when you
accidentally include one is one of the principal goals of this project. For now, you'll need to add licenses to your
blacklist manually. Add a configuration setting to your plugin such as the following:

```xml
  <plugin>
    ...
    <configuration>
      <blacklist>
        <param>agpl-3.0</param> <!--exclude affero-->
        <param>gpl-2.0</param> <!--exclude gpl 2-->
        <param>gpl-3.0</param> <!--exclude gpl 3-->
      </blacklist>
    </configuration>
  </plugin>
```

**To exclude artifacts:** Add the following configuration setting to the plugin:

```xml
  <plugin>
    ...
    <configuration>
      <excludes>
        <param>com.bigco.webapp:internal-common-library:1.0.23</param>
      </excludes>
    </configuration>
  </plugin>
```

Notice you need to add all three coordinates to the artifact. They should be familiar, and the correspond to the
groupId, artifactId, and version that are the common elements of most poms. To add more than just one artifact to your
exclude list, just add multiple param elements.

**To exclude scope:** if you don't want to consider dependencies from the pom with certain scopes, especially provided
or test, then you can exclude them:

```xml
  <plugin>
    ...
    <configuration>
      <excludedScopes>
        <param>test</param>
        <param>provided</param>
      </excludedScopes>
    </configuration>
  </plugin>
```

The idea here is that you may feel comfortable excluding some artifacts from considering. Not clear at all whether this
solves difficult licensing issues, but you may want to do it.

W/R/T IANAL
---
For the record, the original author actually is a lawyer -- but the usual qualifications about "this is not legal advice" apply with full force. Of course.


License
---
Open source licensed under the [MIT License](https://opensource.org/licenses/MIT).
