package it.helper730.api;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.info.BuildProperties;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/v1/health")
public class HealthController {

    private final String version;

    /**
     * Constructor for HealthController.
     *
     * @param buildProperties build metadata provided by Maven;
     *                        absent when the applicaiton runs in development mode.
     */
    public HealthController(ObjectProvider<BuildProperties> buildProperties) {
        BuildProperties props = buildProperties.getIfAvailable();
        this.version = props != null ? props.getVersion() : "dev";
    }

    /**
     * Returns the health status of the application.
     *
     * @return the health response
     */
    @GetMapping()
    public HealthResponse health() {
        return new HealthResponse("UP", this.version);
    }

    /**
     * The Health Check response.
     *
     * @param status  the health status of the application
     * @param version the version of the application
     */
    public record HealthResponse(String status, String version) {}
}
