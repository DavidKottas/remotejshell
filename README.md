# Remote JShell
Executes java commands from a terminal remotely on the server directly inside your application via JShell.




# How to
Import telnetserver package into your app and instantiate TcpServer class.
That will instantly runs executing threads on the socket and will execute your java commands.
Take a care, this must be your last action, JShell will run in a fork of your application, so it will not reflect any later changes in your application.
Any later object instantiations will not by visible by JShell.


## Application side
Here is a typical example with spring, but you can use this example in any other framework or vanilla java projects.
```java
package com.example.demo;

import cz.kottas.remotejshell.TcpServer;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication
public class Application {
    public static ConfigurableApplicationContext ctx;
    public static TcpServer tcpServer;

    public static void main(final String[] args) throws Exception {
        ctx = SpringApplication.run(Application.class, args);
        tcpServer = new TcpServer(9876, null);
    }
}
```

## Client side - telnet
Previous example is accessible by any telnet terminal application

Here is an example for Ubuntu linux:
```PowerShell
david@ubuntu remotejshell/jlineclient(main)$ telnet 0 9876
Trying 0.0.0.0...
Connected to 0.
Escape character is '^]'.
import com.example.demo.Application;
{"res":null}
Application.ctx
{"res":"org.springframework.boot.web.servlet.context.AnnotationConfigServletWebServerApplicationContext@7ed9499e, started on Tue Apr 09 19:40:25 CEST 2024"}
Application.ctx.getBeanDefinitionNames()
{"res":"String[1007] { \"org.springframework.context.annotation.internalConfigurationAnnotationProcessor\", \"org.springframework.context.annotation.internalAutowiredAnnotationProcessor\", \"org.springframework.context.annotation.internalCommonAnnotationProcessor\", \"org.springframework.context.event.internalEventListenerProcessor\", \"org.springframework.context.event.internalEventListenerFactory\", ...
...
```
As you can see a result of any command is allways json structure

## Client side - jlineclient
Ok, lets try somethink better.

Telnet can not suggest so there is a JLine based application that suggests and emulates telnet.
Any suggestion you can access via TAB key and JLine will autocomplete your command for you.
For this, you have to use my another project called jlineclient

Compile and run it via maven
```PowerShell
david@ubuntu remotejshell/jlineclient(main)$ mvn compile exec:java -Dexec.mainClass="cz.kottas.remotejshell.jlineclient.TelnetClient" -Dexec.args="0 9876"
```
Than use it as a telnet and use TAB key for autocompletion
```PowerShell
0:9876 > import com.example.demo.Application;
{"res":null}
0:9876 > Application.ctx.get
getAliases(                       getBeanDefinitionCount()          getBeanProvider(                  getDisplayName()                  getParentBeanFactory()
getApplicationName()              getBeanDefinitionNames()          getBeansOfType(                   getEnvironment()                  getResource(
getApplicationStartup()           getBeanFactory()                  getBeansWithAnnotation(           getId()                           getResources(
getAutowireCapableBeanFactory()   getBeanNamesForAnnotation(        getClass()                        getMessage(                       getStartupDate()
getBean(                          getBeanNamesForType(              getClassLoader()                  getParent()                       getType(
```
