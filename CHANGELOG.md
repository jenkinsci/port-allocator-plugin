# History

## Version 1.8 (Jul 25, 2013)

-   Remove remaining ResourceActivity support for Pooled port
    allocation.(see
    [JENKINS-18786](https://issues.jenkins-ci.org/browse/JENKINS-18786))

## Version 1.7 (Jul 24, 2013)

-   [JENKINS-18786](https://issues.jenkins-ci.org/browse/JENKINS-18786)
    Port allocation blocks jobs from executing concurrently

## Version 1.6 (Jul 11, 2013)

-   [JENKINS-11255](https://issues.jenkins-ci.org/browse/JENKINS-11255)
    Pooled port allocation support (+ implement ResourceActivity to
    queue builds instead of blocking after start)
-   Add PortAllocationManager\#allocateConsecutivePortRange (an enabler
    for fixing
    [JENKINS-12821](https://issues.jenkins-ci.org/browse/JENKINS-12821))

## Version 1.5 (Mar 29, 2010)

-   The plugin is made a bit more reusable
    ([report](http://n4.nabble.com/Using-plugin-dependencies-tp1680509p1680509.html))

## Version 1.4 (Dec 28, 2009)

-   Report assigned port in the build log
-   Update uses of deprecated APIs

## Version 1.3 (Jul 10, 2008)

-   Got rid of the dependency on JDK1.6 and make it compatible with
    JDK1.5. The issue was affected only when the preferred port was not
    available.

## Version 1.2 (Jun 04, 2008)

-   Replaced a deprecated API to support Maven2 style job.

## Version 1.0 (Nov 08, 2007)

-   Initial version.
