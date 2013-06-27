license-check
=============

[![Build Status](https://travis-ci.org/mrice/license-check.png)](https://travis-ci.org/mrice/license-check)

Current version: 0.4 (June 26, 2013)

What is it?
--------------
For now, **license-check** just checks to make sure that your Maven dependencies have a license declared in the [Maven Central Repository](http://search.maven.org) and that you aren't including a license on your project's blacklist. There's more on the horizon, but this is an early release. 

How it works
--------------
License-check looks at each dependency and runs a query against the Central Repo to see if the dependency declares a license that [complykit.org](http://complykit.org) recognizes. If not, then your build will **fail**. (Don't worry, there's a way around this if your dependency isn't in the public repo. See the configuration options below.)

Warning
--------------
If you are using the public version of this plugin, you should know that it will send a list of your dependencies to the server at complykit.org unencrypted (for now). ***This means that you probably shouldn't use this on a super-proprietary project without approval from your management and legal teams.***

Isn't there already something like this?
---------------
**No, not really.** There are a few different Maven plugins for doing license "things." But the purpose of this plugin is (or, I should say, will be) to help you make sure you're not including licenses you don't want to. For now, however, it makes sure that all the artifacts you've included in the project actually declare a license that [complykit.org](http://complykit.org) recognizes as one of the [opensource.org](http://www.opensource.org/) registered licenses. 

This doesn't sound like much, but it's critically important. If the license isn't recognized or isn't declared at all, it's very possible that the authors or contributors could claim fully copyright in the library and expose you to a lot of liability. 

How to use it
---------------
Put license-check into your build process by adding the following to your pom.xml:

```xml

<build>
  <plugins>
    <plugin>
      <groupId>org.complykit</groupId>
      <artifactId>license-check-maven-plugin</artifactId>
      <version>0.4</version>
      <executions>
        <execution>
          <phase>verify</phase>
          <goals>
            <goal>check</goal>
          </goals>
        </execution>
      </executions>
    </plugin>
  </plugins>
</build>

```

When you do this, your builds will start failing if you include a dependency with a license that depwatch doesn't recognize (or, worse, the dependency doesn't declare a license at all).

Configuration options
---------------
**Create a blacklist of licenses:** Right or wrong, many organizations and their legal teams have declared certain licenses to be incompatible with their own licenseing goals. Detecting those licenses and failing your build when you accidentally include one is one of the principal goals of this project. For now, you'll need to add licenses to your blacklist manually. Add a configuration setting to your plugin such as the following:

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

Note that you need to enter the exact license code that complykit.org uses. Until I can get that documented, refer to the short codes used by the (Open Source Initiative)[http://opensource.org]--those are the basis of the codes I'm using.

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

Notice you need to add all three coordinates to the artifact. They should be familiar, and the correspond to the groupId, artifactId, and version that are the common elements of most poms. To add more than just one artifact to your exclude list, just add multiple param elements.


Is this it?
---------------
**Absolutely not!** This is just a rough beginning. Stay tuned by signing up my [complykit.org mailing list](http://complykit.org). For more about what's on deck, see my [backlog](https://github.com/mrice/license-check/backlog.md).


Trust me, I'm a lawyer.
---------------
I hope you'll contact me with any questions or issues (or use the github issue tracker). I **desperately** hope you'll give me some feedback, good or bad. And yes, I really am a lawyer (licensed in Washington State and probably California very soon). Obvious disclaimer: the purpose of this tool is not to give you legal advice, duh.