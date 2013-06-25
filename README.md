license-check
=============

[![Build Status](https://travis-ci.org/mrice/license-check.png)](https://travis-ci.org/mrice/license-check)

What is it?
--------------
For now, license-check just checks to make sure that your Maven dependencies have a license declared in the Central Repo. It basically looks at each dependency and runs a query against the Central Repo to see if the dependency declares a license that [depwatch.org](http://depwatch.org) recognizes. If not, then your build will **fail**.

Warning
--------------
If you are using the public version of this plugin, you should know that it will send a list of your dependencies to the server at complykit.org unencrypted (for now). ***This means that you probably shouldn't use this on a super-proprietary project without approval from your management and legal team.***

Isn't there already something like this?
---------------
**No, not really.** There are a few different Maven plugins for doing license "things." But the purpose of this plugin is (or, I should say, will be) to help you make sure you're not including licenses you don't want to. For now, however, all it does is make sure that all the artifacts you've included in the project actually declare a license that depwatch recognizes as one of the [opensource.org](http://www.opensource.org/) registered licenses. 

This doesn't sound like much, but it's critically important. If the license isn't recognized or isn't declared at all, it's very possible that the authors or contributors could claim fully copyright in the library and expose you to a lot of liability. 

How to use it
---------------
For now, you'll need to install the plugin locally (everybody be cool: I'm going to put it in the public repo soon, I'm just waiting for Sonatype right now). Download the code, and cd into the license-check directory. Then type this:

```basic
mvn install
```

This will install the plugin to your local .m2 repo, so it should work locally for now. Obivously, this will create major problems if you want to put it into a continuous build tool like Jenkins or Travis, so you might want to create a profile in your pom. Next, put licence-check into your build process by adding the following to your pom.xml:

```xml

<build>
  <plugins>
    <plugin>
      <groupId>org.complykit</groupId>
      <artifactId>license-check-maven-plugin</artifactId>
      <version>0.1-PREVIEW</version>
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

**NOTE:** As of this writing (late at night on 2013-06-21), the artifact hasn't been pushed into Maven's Central Repository. I hope to have it published by Tuesday (2013-06-25). I did everything I could to get it in there over the weekend, but it takes a business day or two. If you happen to be seeing this over the weekend, please be patient.

Is this it?
---------------
**Absolutely not!** This is just a rough beginning. Stay tuned by signing up my [depwatch.org mailing list](http://depwatch.org). For more about what's on deck, see my [backlog](https://github.com/mrice/license-check/backlog.md).

