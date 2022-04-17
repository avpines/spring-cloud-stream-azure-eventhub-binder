# Spring Cloud Stream with EventHubs Binder 

This repository provides multiple examples for Spring Cloud Stream with Spring Cloud Azure's
EventHubs binder.

## Building this project

One of the modules in this project, `eventhub-metrics`, uses another library that needs to be cloned
and built, [dynamic-actuator-meters
](https://github.com/avpines/dynamic-actuator-meters). If you don't wish to use these metrics, you 
can build the projects without them.

### Without metrics

The command to build the project without metrics is a straightforward Maven install:

```bash
mvn clean install
```

This will build the modules except `eventhub-metrics`, and the other modules will not use it as 
well.

### With metrics

To use metrics, we will compile the `eventhub-metrics`, which requires the [dynamic-actuator-meters]
(https://github.com/avpines/dynamic-actuator-meters) library.

1. Clone [dynamic-actuator-meters](https://github.com/avpines/dynamic-actuator-meters)
   ```bash
   git clone https://github.com/avpines/dynamic-actuator-meters.git
   ```
2. Build the library
   ```bash
   mvn clean install
   ```
3. Now that you have the artifact in your local repository, you can go back to this project and run
    ```bash
    mvn clean install -Pmetrics
    ```

This will build all the modules in this library, including `eventhub-metrics`, and the other modules
will use `eventhub-metrics` as a dependency.
