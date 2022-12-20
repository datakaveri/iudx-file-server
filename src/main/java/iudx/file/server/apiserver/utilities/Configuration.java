package iudx.file.server.apiserver.utilities;

import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.file.FileSystem;
import io.vertx.core.json.JsonObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;

public class Configuration {
    private static final Logger LOG = LogManager.getLogger(Configuration.class);
    private static FileSystem fileSystem;
    private static final String CONFIG_PATH = "./secrets/all-verticles-configs/config-dev.json";
    private static File file;
    private static Vertx vertx;
    public static final String NGSILD_BASE_PATH = "ngsildBasePath";
    public static final String IUDX_V1_BASE_PATH = "dxV1BasePath";

    /**
     * Get FileServerVerticle config to retrieve base path from config-dev
     */

    public static JsonObject getConfiguration() {
        vertx = Vertx.vertx();
        fileSystem = vertx.fileSystem();
        file = new File(CONFIG_PATH);
        if (file.exists()) {
            Buffer buffer = fileSystem.readFileBlocking(CONFIG_PATH);
            JsonObject basePathJson = buffer.toJsonObject().getJsonObject("commonConfig");
            return basePathJson;
        } else {
            LOG.error("Couldn't read configuration file : " + CONFIG_PATH);
            return null;
        }
    }


    public static String getBasePath(String path) {
        JsonObject jsonObject = getConfiguration();
        if (jsonObject != null) {
            return jsonObject.getString(path);
        } else {
            return null;
        }
    }
}
