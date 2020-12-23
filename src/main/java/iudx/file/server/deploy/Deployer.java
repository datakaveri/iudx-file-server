package iudx.file.server.deploy;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.hazelcast.config.Config;
import com.hazelcast.config.DiscoveryStrategyConfig;
import com.hazelcast.zookeeper.ZookeeperDiscoveryProperties;
import com.hazelcast.zookeeper.ZookeeperDiscoveryStrategyFactory;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics;
import io.micrometer.core.instrument.binder.system.ProcessorMetrics;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.cli.CLI;
import io.vertx.core.cli.CommandLine;
import io.vertx.core.cli.Option;
import io.vertx.core.eventbus.EventBusOptions;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.core.metrics.MetricsOptions;
import io.vertx.core.spi.cluster.ClusterManager;
import io.vertx.micrometer.Label;
import io.vertx.micrometer.MicrometerMetricsOptions;
import io.vertx.micrometer.VertxPrometheusOptions;
import io.vertx.micrometer.backends.BackendRegistries;
import io.vertx.spi.cluster.hazelcast.HazelcastClusterManager;

public class Deployer {
  private static final Logger LOGGER = LogManager.getLogger(Deployer.class);


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

  public static ClusterManager getClusterManager(String host,
                                                  List<String> zookeepers,
                                                  String clusterID) {
    Config config = new Config();
    config.getNetworkConfig().getJoin().getMulticastConfig().setEnabled(false);
    config.getNetworkConfig().setPublicAddress(host);
    config.setProperty("hazelcast.discovery.enabled", "true");
    config.setProperty("hazelcast.logging.type", "log4j2");
    DiscoveryStrategyConfig discoveryStrategyConfig =
        new DiscoveryStrategyConfig(new ZookeeperDiscoveryStrategyFactory());
    discoveryStrategyConfig.addProperty(ZookeeperDiscoveryProperties.ZOOKEEPER_URL.key(),
                                          String.join(",", zookeepers));
    discoveryStrategyConfig.addProperty(ZookeeperDiscoveryProperties.GROUP.key(), clusterID);
    config.getNetworkConfig()
          .getJoin()
          .getDiscoveryConfig()
          .addDiscoveryStrategyConfig(discoveryStrategyConfig);

    return new HazelcastClusterManager(config);
  }

  public static MetricsOptions getMetricsOptions() {
    return new MicrometerMetricsOptions()
        .setPrometheusOptions(
            new VertxPrometheusOptions()
              .setEnabled(true)
              .setStartEmbeddedServer(true)
              .setEmbeddedServerOptions(new HttpServerOptions().setPort(9000)))
        // .setPublishQuantiles(true))
              .setLabels(EnumSet.of(Label.EB_ADDRESS, Label.EB_FAILURE, Label.HTTP_CODE,
                    Label.HTTP_METHOD, Label.HTTP_PATH))
              .setEnabled(true);
  }

  public static void setJVMmetrics() {
    MeterRegistry registry = BackendRegistries.getDefaultNow();
    new JvmMemoryMetrics().bindTo(registry);
    new JvmGcMetrics().bindTo(registry);
    new ProcessorMetrics().bindTo(registry);
    new JvmThreadMetrics().bindTo(registry);

  }

  public static void deploy(String configPath) {
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
    List<String> zookeepers = configuration.getJsonArray("zookeepers").getList();
    String clusterId = configuration.getString("clusterId");
    String host = configuration.getString("host");
    ClusterManager mgr = getClusterManager(host, zookeepers, clusterId);
    EventBusOptions ebOptions = new EventBusOptions().setClustered(true).setHost(host);
    VertxOptions options = new VertxOptions().setClusterManager(mgr).setEventBusOptions(ebOptions)
        .setMetricsOptions(getMetricsOptions());

    Vertx.clusteredVertx(options, res -> {
      if (res.succeeded()) {
        Vertx vertx = res.result();
        setJVMmetrics();
        recursiveDeploy(vertx, configuration, 0);
      } else {
        LOGGER.fatal("Could not join cluster");
      }
    });

  }


  public static void main(String[] args) {
    CLI cli = CLI.create("IUDX File Server")
        .setSummary("A CLI to deploy the file server")
        .addOption(new Option().setLongName("help").setShortName("h").setFlag(true).setDescription("display help"))
        .addOption(new Option().setLongName("config").setShortName("c").setRequired(true).setDescription("configuration file"));

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
