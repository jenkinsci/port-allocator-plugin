# Port allocator plugin

## TCP port number allocation

When you have several jobs that involve in launching a daemon process
(such as application servers), making sure that each job uses unique TCP
port numbers becomes tedious. If you don't manage this, however, two
jobs that happen to run on the same machine may use the same port, like
8080, and end up interfering each other.

This plugin is written to solve this problem by letting Jenkins do the
book-keeping. With this plugin, you'd just say "I need HTTP\_PORT and
JMS\_PORT for this job", and Jenkins will assign unique available TCP
port numbers to them and expose them to the build as environment
variables.

The following screenshot illustrates this configuration.

![](docs/images/config.png)

## Reserving a fixed port

Sometimes, your job requires a certain port. For example, it's not
unusual to see tests that have hard-coded port numbers, or maybe you are
testing SMTP servers that really do require port 25.

In such a case, you'd just need to say "this job requires port 25, so
don't run it with any other job that requires the same port." Jenkins
will then schedule jobs accordingly so that the collision won't happen.

The following screenshot illustrates this configuration.

![](docs/images/fixed-port.png)

## Shutting down run-away daemons

Often these allocated TCP ports are used to run background daemon
processes, like application servers. A typical problem in this set up is
that when a job fails abnormally, the job may fail to terminate those
servers properly. This interferes with successive builds that use the
same machine.

To fix this problem, when you allocate a port (or reserve a fixed port),
you can choose one of the port types that have the knowledge of shutting
down the process. Currently, two port types are available, and more can
be added as additional plugins.

-   **GlassFish JMX port** that lets Jenkins shut down a run-away GlassFish through JMX.
    You can configure a job to require fixed port 8686, which is the default port number for this.
-   **Tomcat shutdown port** that lets Jenkins shut down a run-away
    Tomcat through the shut down port. See the [Tomcat 9 configuration reference](https://tomcat.apache.org/tomcat-9.0-doc/config/server.html)
    for more about this feature.

The following screenshot illustrates this configuration.

![](docs/images/portType.png)

## Version History

[GitHub releases](https://github.com/jenkinsci/port-allocator-plugin/releases) provide details of recent releases.

[CHANGELOG](CHANGELOG.md) provides releases before 2014
