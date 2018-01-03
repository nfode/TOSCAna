package org.opentosca.toscana.plugins.cloudfoundry;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.opentosca.toscana.core.plugin.PluginFileAccess;
import org.opentosca.toscana.plugins.cloudfoundry.application.CloudFoundryApplication;
import org.opentosca.toscana.plugins.cloudfoundry.application.CloudFoundryProvider;
import org.opentosca.toscana.plugins.cloudfoundry.application.CloudFoundryService;
import org.opentosca.toscana.plugins.cloudfoundry.application.CloudFoundryServiceType;
import org.opentosca.toscana.plugins.scripts.BashScript;
import org.opentosca.toscana.plugins.scripts.EnvironmentCheck;

import org.cloudfoundry.operations.services.ServiceOffering;
import org.cloudfoundry.operations.services.ServicePlan;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import static org.opentosca.toscana.plugins.cloudfoundry.application.CloudFoundryManifestAttribute.ENVIRONMENT;
import static org.opentosca.toscana.plugins.cloudfoundry.application.CloudFoundryManifestAttribute.PATH;
import static org.opentosca.toscana.plugins.cloudfoundry.application.CloudFoundryManifestAttribute.SERVICE;
import static org.opentosca.toscana.plugins.lifecycle.AbstractLifecycle.OUTPUT_DIR;

/**
 Creates all files which are necessary to deploy the application
 Files: manifest.yml, builpack additions, deployScript
 */
public class CloudFoundryFileCreator {

    public static final String MANIFEST_NAME = "manifest.yml";
    public static final String MANIFEST_PATH = OUTPUT_DIR + MANIFEST_NAME;
    public static final String MANIFESTHEAD = "---\napplications:\n";
    public static final String NAMEBLOCK = "name";
    public static final String CLI_CREATE_SERVICE_DEFAULT = "cf create-service {plan} {service} ";
    public static final String CLI_CREATE_SERVICE = "cf create-service ";
    public static final String CLI_PUSH = "cf push ";
    public static final String CLI_PATH_TO_MANIFEST = " -f ../";
    public static final String FILEPRAEFIX_DEPLOY = "deploy_";
    public static final String FILESUFFIX_DEPLOY = ".sh";
    public static final String BUILDPACK_OBJECT_PHP = "PHP_EXTENSIONS";
    public static final String BUILDPACK_FILEPATH_PHP = ".bp-config/options.json";

    private final PluginFileAccess fileAccess;
    private final CloudFoundryApplication app;

    public CloudFoundryFileCreator(PluginFileAccess fileAccess, CloudFoundryApplication app) {
        this.fileAccess = fileAccess;
        this.app = app;
    }

    public void createFiles() throws IOException, JSONException {
        createManifest();
        createBuildpackAdditionsFile();
        createDeployScript();
        insertFiles();
    }

    private void createManifest() throws IOException {
        createManifestHead();
        addPathToApplication();
        createAttributes();
        createEnvironmentVariables();
        createService();
    }

    private void createManifestHead() throws IOException {
        String manifestHead = String.format("%s- %s: %s", MANIFESTHEAD, NAMEBLOCK, app.getName());
        fileAccess.access(MANIFEST_PATH).appendln(manifestHead).close();
    }

    private void addPathToApplication() throws IOException {
        String mainApplicationPath = app.getPathToApplication();
        if (mainApplicationPath != null) {
            String pathAddition = String.format("  %s: ../%s", PATH.getName(), app.getPathToApplication());
            fileAccess.access(MANIFEST_PATH).appendln(pathAddition).close();
        }
    }

    private void createEnvironmentVariables() throws IOException {
        Map<String, String> envVariables = app.getEnvironmentVariables();
        if (!envVariables.isEmpty()) {
            ArrayList<String> environmentVariables = new ArrayList<>();
            environmentVariables.add(String.format("  %s:", ENVIRONMENT.getName()));
            for (Map.Entry<String, String> entry : envVariables.entrySet()) {
                environmentVariables.add(String.format("    %s: %s", entry.getKey(), entry.getValue()));
            }
            for (String env : environmentVariables) {
                fileAccess.access(MANIFEST_PATH).appendln(env).close();
            }
        }
    }

    /**
     creates a service which depends on the template
     add it to the manifest
     */
    private void createService() throws IOException {
        Map<String, CloudFoundryServiceType> appServices = app.getServices();
        if (!appServices.isEmpty()) {
            ArrayList<String> services = new ArrayList<>();
            services.add(String.format("  %s:", SERVICE.getName()));
            for (Map.Entry<String, CloudFoundryServiceType> service : appServices.entrySet()) {
                services.add(String.format("    - %s", service.getKey()));
            }
            for (String service : services) {
                fileAccess.access(MANIFEST_PATH).appendln(service).close();
            }
        }
    }

    /**
     creates a deploy shell script
     */
    private void createDeployScript() throws IOException {
        BashScript deployScript = new BashScript(fileAccess, FILEPRAEFIX_DEPLOY + app.getName());
        deployScript.append(EnvironmentCheck.checkEnvironment("cf"));

        if (app.getProvider() != null && !app.getServices().isEmpty()) {
            addProviderServiceOfferings(deployScript);
            for (Map.Entry<String, CloudFoundryServiceType> service : app.getServices().entrySet()) {
                String description = service.getValue().getName();
                CloudFoundryProvider provider = app.getProvider();
                List<ServiceOffering> services = provider.getOfferedService();
                Boolean isSet = false;

                //checks if a offered service of the provider contains the description of the needed service
                //if yes then add the service to the script with a free plan
                for (ServiceOffering offeredService : services) {
                    if (offeredService.getDescription().toLowerCase().indexOf(description.toLowerCase()) != -1) {
                        for (ServicePlan plan : offeredService.getServicePlans()) {
                            if (plan.getFree()) {
                                String serviceName = offeredService.getLabel();
                                String planName = plan.getName();
                                String serviceInstanceName = service.getKey();
                                deployScript.append(String.format("%s%s %s %s", CLI_CREATE_SERVICE,
                                    serviceName, planName, serviceInstanceName));
                                app.addMatchedService(
                                    new CloudFoundryService(serviceName, serviceInstanceName, planName));
                                isSet = true;
                                break;
                            }
                        }
                    }
                }

                if (!isSet) {
                    deployScript.append(CLI_CREATE_SERVICE_DEFAULT + service);
                }
            }
        } else {
            for (Map.Entry<String, CloudFoundryServiceType> service : app.getServices().entrySet()) {
                deployScript.append(CLI_CREATE_SERVICE_DEFAULT + service.getKey());
            }
        }

        deployScript.append(CLI_PUSH + app.getName() + CLI_PATH_TO_MANIFEST + MANIFEST_NAME);
    }

    //only for PHP 
    //TODO: check if PHP and mysql then add "mysql" & "mysqli"
    private void createBuildpackAdditionsFile() throws IOException, JSONException {
        JSONObject buildPackAdditionsJson = new JSONObject();
        JSONArray buildPacks = new JSONArray();
        for (String buildPack : app.getBuildpackAdditions()) {
            buildPacks.put(buildPack);
        }
        buildPackAdditionsJson.put(BUILDPACK_OBJECT_PHP, buildPacks);
        String path;
        if (app.getPathToApplication() != null) {
            path = String.format("%s/%s", app.getPathToApplication(), BUILDPACK_FILEPATH_PHP);
        } else {
            path = BUILDPACK_FILEPATH_PHP;
        }
        fileAccess.access(path).append(buildPackAdditionsJson.toString(4)).close();
    }

    private void createAttributes() throws IOException {

        if (!app.getAttributes().isEmpty()) {
            ArrayList<String> attributes = new ArrayList<>();
            for (Map.Entry<String, String> attribute : app.getAttributes().entrySet()) {
                attributes.add(String.format("  %s: %s", attribute.getKey(), attribute.getValue()));
            }
            for (String attribute : attributes) {
                fileAccess.access(MANIFEST_PATH).appendln(attribute).close();
            }
        }
    }

    private void insertFiles() throws IOException {
        for (String filePath : app.getFilePaths()) {
            fileAccess.copy(filePath);
        }
    }

    private void addProviderServiceOfferings(BashScript deployScript) throws IOException {
        CloudFoundryProvider provider = app.getProvider();
        List<ServiceOffering> services = provider.getOfferedService();

        deployScript.append("# following services you could choose:");
        deployScript.append(String.format("# %-20s %-40s %-50s\n", "Name", " Plans", "Description"));

        for (ServiceOffering service : services) {
            String plans = "";
            for (ServicePlan plan : service.getServicePlans()) {
                String currentPlan;
                if (plan.getFree()) {
                    currentPlan = plan.getName();
                } else {
                    currentPlan = plan.getName() + "*";
                }

                plans = String.format("%s %s ", plans, currentPlan);
            }
            deployScript.append(String.format("# %-20s %-40s %-50s ", service.getLabel(), plans, service.getDescription()));
        }
        deployScript.append("\n* These service plans have an associated cost. Creating a service instance will incur this cost.");
    }
}
