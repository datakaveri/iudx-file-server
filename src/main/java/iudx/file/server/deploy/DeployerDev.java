package iudx.file.server.deploy;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.cli.CLI;
import io.vertx.core.cli.CommandLine;
import io.vertx.core.cli.Option;
import io.vertx.core.eventbus.EventBusOptions;
import io.vertx.core.json.JsonObject;

public class DeployerDev {
  private static final Logger LOGGER = LogManager.getLogger(DeployerDev.class);

  public static void recursiveDeploy(Vertx vertx, JsonObject configs, int i) {
    if (i >= configs.getJsonArray("modules").size()) {
      LOGGER.info("Deployed all");
      return;
    }
    JsonObject config = configs.getJsonArray("modules").getJsonObject(i);
    String moduleName = config.getString("id");
    int numInstances = config.getInteger("verticleInstances");
    vertx.deployVerticle(moduleName,
                          new DeploymentOptions()
                            .setInstances(numInstances)
                            .setConfig(config),
                          ar -> {
      if (ar.succeeded()) {
        LOGGER.info("Deployed " + moduleName);
        recursiveDeploy(vertx, configs, i+1);
      } else {
        LOGGER.fatal("Failed to deploy " + moduleName + " cause:", ar.cause());
      }
    });
  }

  public static void deploy(String configPath) {
    EventBusOptions ebOptions = new EventBusOptions();
    VertxOptions options = new VertxOptions().setEventBusOptions(ebOptions);

    String config;
    try {
     config = new String(Files.readAllBytes(Paths.get(configPath)), StandardCharsets.UTF_8);
    } catch (Exception e) {
      LOGGER.fatal("Couldn't read configuration file");
      return;
    }
    if (config.length() < 1) {
      LOGGER.fatal("Couldn't read configuration file");
      return;
    }
    JsonObject configuration = new JsonObject(config);
    Vertx vertx = Vertx.vertx(options);
    recursiveDeploy(vertx, configuration, 0);
  }

  public static void main(String[] args) {
    CLI cli = CLI.create("IUDX File Server").setSummary("A CLI to deploy the resource server")
        .addOption(new Option().setLongName("help").setShortName("h").setFlag(true)
            .setDescription("display help"))
        .addOption(new Option().setLongName("config").setShortName("c")
            .setRequired(true).setDescription("configuration file"));

    StringBuilder usageString = new StringBuilder();
    cli.usage(usageString);
    CommandLine commandLine = cli.parse(Arrays.asList(args), false);
    if (commandLine.isValid() && !commandLine.isFlagEnabled("help")) {
      String configPath = commandLine.getOptionValue("config");
      deploy(configPath);
    } else {
      LOGGER.info(usageString);
    }
  }
}
