*This project is no longer maintained.*

Tachyon
========

An Android library that provides a customizable calendar day view.

[Click here for the iOS version.](https://github.com/linkedin/Tachyon-iOS)

![Alt text](screenshot.png "Tachyon Sample")

Tachyon is designed to provide a familiar visualization of a calendar day. The rendering is done by the `DayView` class, which takes a list of events and displays them using a custom layout algorithm.

Usage
-----

To use Tachyon, you can either directly reference the `DayView` class in your layout files/code, or you can subclass `DayView` to customize the experience.

Sample App
----------

The ''tachyon-sample'' app contains an example of using the library.

Testing
-------

We use Mockito for our unit tests. You can run them via the `clean test` Gradle tasks.
