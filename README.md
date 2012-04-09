This fork externalizes database properties so that the unmodified application can run in <a href="http://cloudfoundry.org">Cloud Foundry</a>

### About

This is the demo application that we're building to support the Lift Book.

[Lift Book repo](http://www.github.com/tjweir/liftbook/)

[LiftWeb Source](http://www.github.com/lift/framework)

- Derek, Marius and Tyler

<a rel="license" href="http://creativecommons.org/licenses/by-sa/3.0/"><img alt="Creative Commons License" style="border-width:0" src="http://i.creativecommons.org/l/by-sa/3.0/88x31.png" /></a><br /><span xmlns:dct="http://purl.org/dc/terms/" property="dct:title">PocketChange</span> is licensed under a <a rel="license" href="http://creativecommons.org/licenses/by-sa/3.0/">Creative Commons Attribution-ShareAlike 3.0 Unported License</a>.

### Usage

To run the application, you'll need [SBT 0.11.2](https://github.com/harrah/xsbt/wiki/Getting-Started-Setup).

Once, you have SBT set up, to start the application with [Jety on port 8080](http://localhost:8080) use this in console:

    sbt container:start

To stop the application use this in console:

    sbt container:stop
