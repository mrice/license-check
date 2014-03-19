license-check
=============

[![Build Status](https://travis-ci.org/mrice/license-check.png)](https://travis-ci.org/mrice/license-check)

Current version: 0.5.3 (**TBD**)

What is it?
--------------
For now, **license-check** just checks to make sure that your Maven dependencies have a license declared in their POM files and that you aren't including a license on your project's blacklist. There's more on the horizon but this is an early release. 

How does it work?
--------------
License-check looks at each dependency and runs a query against your Maven respository to see if the dependency declares a license the plugin can recognize using a local, stored collection of regular expressions of common licenses. 

If the plugin cannot find a match, then your build will **fail**. (Don't worry, there's a way around this if your dependency isn't in the public repo. See the configuration options below.)

Isn't there already something like this?
---------------
**No, not really.** There are a few different Maven plugins for doing license "things." But the purpose of this plugin is  to help you make sure you're not including licenses without realizing what you're doing. It does this by making sure that all the artifacts you've included in the project actually declare a license that [complykit.org](http://complykit.org) recognizes as one of the [opensource.org](http://www.opensource.org/) registered licenses. 

This doesn't sound like much, but **it's critically important**. If the license isn't recognized or isn't declared at all, it's very possible that the authors or contributors could claim full copyright in the library and expose you to a lot of liability (* that's not legal advice, that's just reality). 

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

As mentioned, when you do this, your mvn install builds will start failing if you include a dependency with a license that the plugin doesn't recognize (or, worse, the dependency doesn't declare a license at all).

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
**Absolutely not!** This is just a rough beginning. Stay tuned by signing up my [complykit.org mailing list](http://complykit.org). For more about what's on deck, see my [backlog](https://github.com/mrice/license-check/wiki/Backlog).


Trust me, I'm a lawyer.
---------------
I hope you'll contact me with any questions or issues (or use the github issue tracker). I really (really!) hope you'll give me some feedback, good or bad. And yes, I really am a lawyer (licensed in Washington State and California). Obvious disclaimer: the purpose of this tool is not to give you legal advice, duh. (Oh, in case someone clones the original repository, this message is only from [Michael Rice](http://michaelrice.com).)