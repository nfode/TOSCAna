package org.opentosca.toscana.cli;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.opentosca.toscana.cli.commands.Constants;
import org.opentosca.toscana.retrofit.TOSCAnaAPI;
import org.opentosca.toscana.retrofit.model.Csar;
import org.opentosca.toscana.retrofit.model.LogEntry;
import org.opentosca.toscana.retrofit.model.Platform;
import org.opentosca.toscana.retrofit.model.Transformation;
import org.opentosca.toscana.retrofit.model.TransformationLogs;
import org.opentosca.toscana.retrofit.model.TransformationProperties;
import org.opentosca.toscana.retrofit.model.TransformationProperty;
import org.opentosca.toscana.retrofit.model.TransformerStatus;
import org.opentosca.toscana.retrofit.model.embedded.CsarResources;
import org.opentosca.toscana.retrofit.model.embedded.PlatformResources;
import org.opentosca.toscana.retrofit.model.embedded.TransformationResources;
import org.opentosca.toscana.retrofit.util.LoggingMode;
import org.opentosca.toscana.retrofit.util.TOSCAnaServerException;

public class ApiController {

    private Constants con;
    private TOSCAnaAPI toscAnaAPI;

    /**
     Constructor for the ApiController, parameters decide if there should be any output of information
     */
    public ApiController(Mode modeName) {
        con = new Constants();
        CliProperties prop = new CliProperties();
        final String API_URL = prop.getApiUrl();

        //starts the retrofit client with the chosen loglevel
        try {
            if (modeName == Mode.LOW) {
                toscAnaAPI = new TOSCAnaAPI(API_URL, LoggingMode.LOW);
            } else if (modeName == Mode.HIGH) {
                toscAnaAPI = new TOSCAnaAPI(API_URL, LoggingMode.HIGH);
            } else if (modeName == Mode.NONE) {
                toscAnaAPI = new TOSCAnaAPI(API_URL);
            }
        } catch (IllegalArgumentException e) {
            System.out.println(e.getMessage());
            System.exit(1);
        }
    }

    /**
     Calls the REST API to upload the CSAR, handles different response codes which are returned

     @param file CSAR Archive to upload
     @return output for the CLI
     */
    public String uploadCsar(File file) {
        try {
            toscAnaAPI.uploadCsar(file.getName(), file);
        } catch (IOException e) {
            System.err.println(String.format(con.CSAR_UPLOAD_IO_ERROR + " '%s'", e.getMessage()));
        } catch (TOSCAnaServerException e) {
            System.err.println(String.format(con.CSAR_UPLOAD_RESPONSE_ERROR + " %s '%s'", e.getStatusCode(), e.getErrorResponse().getMessage()));
        }
        return "";
    }

    /**
     Calls the REST API and deletes the specified CSAR if it's available, handles different response codes

     @param csar CSAR to delete from the Transformator
     @return output for the CLI
     */
    public String deleteCsar(String csar) {
        try {
            toscAnaAPI.deleteCsar(csar);
        } catch (IOException e) {
            System.err.println(String.format(con.CSAR_DELETE_ERROR + " '%s'", e.getMessage()));
        } catch (TOSCAnaServerException e) {
            System.err.println(String.format(con.CSAR_DELETE_ERROR + " %s '%s'", e.getStatusCode(), e.getErrorResponse().getMessage()));
        }
        return "";
    }

    /**
     Calls the REST API and lists all available CSARs, only handles code 200 or exception responses

     @return output for the CLI
     */
    public String listCsar() {
        CsarResources csarList;
        StringBuilder stringCsars = new StringBuilder();
        try {
            csarList = toscAnaAPI.getCsars();
            List<Csar> list = csarList.getContent();

            for (Csar c : list) {
                stringCsars.append(c.getName()).append("\n");
            }
            stringCsars.delete(stringCsars.length() - 1, stringCsars.length());
        } catch (IOException e) {
            System.err.println(String.format(con.CSAR_LIST_ERROR + " '%s'", e.getMessage()));
        } catch (TOSCAnaServerException e) {
            System.err.println(String.format(con.CSAR_LIST_ERROR + " %s '%s'", e.getStatusCode(), e.getErrorResponse().getMessage()));
        }
        return stringCsars.toString();
    }

    /**
     Calls the REST API and prints detailed Information for the specified CSAR if it's available

     @param csarName Name of the CSAR which information should be shown
     @return output for the CLI
     */
    public String infoCsar(String csarName) {
        String cName = "";
        Csar csar;
        try {
            csar = toscAnaAPI.getCsarDetails(csarName);
            cName = csar.getName();
        } catch (IOException e) {
            System.err.println(String.format(con.CSAR_INFO_ERROR + " '%s'", e.getMessage()));
        } catch (TOSCAnaServerException e) {
            System.err.println(String.format(con.CSAR_INFO_ERROR + " %s '%s'", e.getStatusCode(), e.getErrorResponse().getMessage()));
        }
        return cName;
    }

    /**
     Calls the REST API and starts the Transformation, handles response codes

     @param csar CSAR for which a transformation should be started
     @param plat platform for which a transformation should be started
     @return output for the CLI
     */
    public String startTransformation(String csar, String plat) {
        try {
            toscAnaAPI.createTransformation(csar, plat);
            launchTransformation(csar, plat);
        } catch (IOException e) {
            System.err.println(String.format(con.TRANSFORMATION_CREATE_ERROR + " '%s'", e.getMessage()));
        } catch (TOSCAnaServerException e) {
            System.err.println(String.format(con.TRANSFORMATION_CREATE_ERROR + " %s '%s'", e.getStatusCode(), e.getErrorResponse().getMessage()));
        }
        return "";
    }

    /**
     Calls the REST API and launches the Transformation, handles response codes

     @param csar     CSAR for which a transformation should be launched
     @param platform platform for which a transformation should be launched
     @return output for the CLI
     */
    private String launchTransformation(String csar, String platform) {
        try {
            toscAnaAPI.startTransformation(csar, platform);
        } catch (IOException e) {
            System.err.println(String.format(con.TRANSFORMATION_START_ERROR + " '%s'", e.getMessage()));
        } catch (TOSCAnaServerException e) {
            System.err.println(String.format(con.TRANSFORMATION_START_ERROR + " %s '%s'", e.getStatusCode(), e.getErrorResponse().getMessage()));
        }
        return "";
    }

    /**
     TODO: Implement functionality Calls the REST API and stops the currently running Transformation if it's running

     @param csar CSAR to stop transformation for
     @param plat platform to stop transformation for
     @return output for the CLI
     */
    public String stopTransformation(String csar, String plat) {
        return con.TRANSFORMATION_STOP;
    }

    /**
     Calls the REST API and deletes the specified Transformation, handles response codes

     @param csar CSAR for which transformation should be deleted
     @param plat platform for which the transformation should be deleted
     @return output for the CLI
     */
    public String deleteTransformation(String csar, String plat) {
        try {
            toscAnaAPI.deleteTransformation(csar, plat);
        } catch (IOException e) {
            System.err.println(String.format(con.TRANSFORMATION_DELETE_ERROR + " '%s'", e.getMessage()));
        } catch (TOSCAnaServerException e) {
            System.err.println(String.format(con.TRANSFORMATION_DELETE_ERROR + " %s '%s'", e.getStatusCode(), e.getErrorResponse().getMessage()));
        }
        return "";
    }

    /**
     Calls the REST API to download an Artifact for the specified finished Transformation, handles response codes

     @param csar CSAR for which to download an Artifact
     @param plat Platform for which the Artifact should be downloaded
     @return output for the CLI
     */
    public String downloadTransformation(String csar, String plat) {
        String downloadUrl = "";
        downloadUrl = toscAnaAPI.getArtifactDownloadUrl(csar, plat);

        return downloadUrl;
    }

    /**
     Calls the REST API and lists all available Transformations for the CSAR

     @param csar CSAR, for which transformations should be shown
     @return output for the CLI
     */
    public String listTransformation(String csar) {
        TransformationResources transformationList;
        StringBuilder stringTransformations = new StringBuilder();
        try {
            transformationList = toscAnaAPI.getTransformations(csar);
            List<Transformation> list = transformationList.getContent();

            for (Transformation t : list) {
                stringTransformations.append(t.getPlatform()).append("\n");
            }
            stringTransformations.delete(stringTransformations.length() - 1, stringTransformations.length());
        } catch (IOException e) {
            System.err.println(String.format(con.TRANSFORMATION_LIST_ERROR + " '%s'", e.getMessage()));
        } catch (TOSCAnaServerException e) {
            System.err.println(String.format(con.TRANSFORMATION_LIST_ERROR + " %s '%s'", e.getStatusCode(), e.getErrorResponse().getMessage()));
        }
        return stringTransformations.toString();
    }

    /**
     Calls the REST API and returns all Information about the Transformation

     @param csar CSAR, for which Transformation Info should be shown
     @param plat Platform, for which Information should be shown
     @return output for the CLI
     */
    public String infoTransformation(String csar, String plat) {
        Transformation transformation;
        StringBuilder stringTransformation = new StringBuilder();
        try {
            transformation = toscAnaAPI.getTransformation(csar, plat);

            stringTransformation.append(transformation.getPlatform()).append(", ").append(transformation.getProgress()).append(", ").append(transformation.getStatus());
        } catch (IOException e) {
            System.err.println(String.format(con.TRANSFORMATION_INFO_ERROR + " '%s'", e.getMessage()));
        } catch (TOSCAnaServerException e) {
            System.err.println(String.format(con.TRANSFORMATION_INFO_ERROR + " %s '%s'", e.getStatusCode(), e.getErrorResponse().getMessage()));
        }
        return stringTransformation.toString();
    }

    /**
     Calls the REST API and returns logs for the specified Transformation

     @param csar  CSAR for which a transformation is available
     @param plat  Platform for which a transformation is available
     @param start where to start with log output, default is start position 0
     @return output for the CLI
     */
    public String logsTransformation(String csar, String plat, int start) {
        TransformationLogs logsList;
        StringBuilder stringLogs = new StringBuilder();
        try {
            logsList = toscAnaAPI.getLogs(csar, plat, start);
            List<LogEntry> list = logsList.getLogEntries();

            for (LogEntry l : list) {
                stringLogs.append(l.getTimestamp()).append(", ").append(l.getMessage()).append(", ").append(l.getLevel());
            }
        } catch (IOException e) {
            System.err.println(String.format(con.TRANSFORMATION_LOGS_ERROR + " '%s'", e.getMessage()));
        } catch (TOSCAnaServerException e) {
            System.err.println(String.format(con.TRANSFORMATION_LOGS_ERROR + " %s '%s'", e.getStatusCode(), e.getErrorResponse().getMessage()));
        }
        return stringLogs.toString();
    }

    /**
     Calls the REST API and shows every needed Input, that must be set before a transformation can be started

     @param csar CSAR for which required inputs should be shown
     @param plat Platform for which required inputs should be shown
     @return output for the CLI
     */
    public String inputList(String csar, String plat) {
        TransformationProperties propertiesList;
        StringBuilder stringProperties = new StringBuilder();
        try {
            propertiesList = toscAnaAPI.getProperties(csar, plat);
            List<TransformationProperty> list = propertiesList.getProperties();

            for (TransformationProperty p : list) {
                stringProperties.append(p.getKey()).append(", ").append(p.getValue()).append(", ").append(p.getDescription());
            }
        } catch (IOException e) {
            System.err.println(String.format(con.INPUT_LIST_ERROR + " '%s'", e.getMessage()));
        } catch (TOSCAnaServerException e) {
            System.err.println(String.format(con.INPUT_LIST_ERROR + " %s '%s'", e.getStatusCode(), e.getErrorResponse().getMessage()));
        }
        return stringProperties.toString();
    }

    /**
     Calls the REST API, and trys to set the required Inputs. After they are set successfully a transformation can be
     started

     @param csar   CSAR for which to set Inputs
     @param plat   Platform for which to set Inputs
     @param inputs the required inputs, format is key=value, = is not allowed as an identifier
     @return output for the CLI
     */
    public String placeInput(String csar, String plat, Map<String, String> inputs) {
        //prepare Inputs which should be updated
        List<TransformationProperty> properties = new ArrayList<>();
        for (Map.Entry<String, String> mapEntry : inputs.entrySet()) {
            TransformationProperty p = new TransformationProperty();
            p.setKey(mapEntry.getKey());
            p.setValue(mapEntry.getValue());
            properties.add(p);
        }
        TransformationProperties sendProp = new TransformationProperties(properties);

        //get Return of the update if it was successfull
        Map<String, Boolean> propertiesReturn;
        StringBuilder stringProperties = new StringBuilder();
        try {
            propertiesReturn = toscAnaAPI.updateProperties(csar, plat, sendProp);
            for (String s : propertiesReturn.keySet()) {
                stringProperties.append("%n").append(s).append(" ").append(propertiesReturn.get(s));
            }
        } catch (IOException e) {
            System.err.println(String.format(con.INPUT_SET_ERROR + " '%s'", e.getMessage()));
        } catch (TOSCAnaServerException e) {
            System.err.println(String.format(con.INPUT_SET_ERROR + " %s '%s'", e.getStatusCode(), e.getErrorResponse().getMessage()));
        }
        return stringProperties.toString();
    }

    /**
     Calls the REST API and returns all Platforms, that are available for a transformation

     @return output for the CLI
     */
    public String listPlatform() {
        PlatformResources platformList;
        StringBuilder stringPlatforms = new StringBuilder();
        try {
            platformList = toscAnaAPI.getPlatforms();
            List<Platform> list = platformList.getContent();

            for (Platform p : list) {
                stringPlatforms.append(p.getId()).append(", ").append(p.getName()).append("\n");
            }
            stringPlatforms.delete(stringPlatforms.length() - 1, stringPlatforms.length());
        } catch (IOException e) {
            System.err.println(String.format(con.PLATFORM_LIST_ERROR + " '%s'", e.getMessage()));
        } catch (TOSCAnaServerException e) {
            System.err.println(String.format(con.PLATFORM_LIST_ERROR + " %s '%s'", e.getStatusCode(), e.getErrorResponse().getMessage()));
        }
        return stringPlatforms.toString();
    }

    /**
     Calls the REST API and returns all Information about the Platform

     @param plat Platform for which all it's information should be shown
     @return output for the CLI
     */
    public String infoPlatform(String plat) {
        Platform platform;
        StringBuilder stringPlatform = new StringBuilder();
        try {
            platform = toscAnaAPI.getPlatformDetails(plat);
            stringPlatform.append(platform.getId()).append(", ").append(platform.getName());
        } catch (IOException e) {
            System.err.println(String.format(con.PLATFORM_INFO_ERROR + " '%s'", e.getMessage()));
        } catch (TOSCAnaServerException e) {
            System.err.println(String.format(con.PLATFORM_INFO_ERROR + " %s '%s'", e.getStatusCode(), e.getErrorResponse().getMessage()));
        }
        return stringPlatform.toString();
    }

    /**
     Calls the REST API and returns the current state of the system

     @return output for the CLI
     */
    public String showStatus() {
        TransformerStatus status;
        StringBuilder stringStatus = new StringBuilder();
        try {
            status = toscAnaAPI.getServerStatus();

            stringStatus.append(status.getStatus()).append(", ").append(status.getFileSystemHealth()).append(", ").append(status.getTransformerHealth());
        } catch (IOException e) {
            System.err.println(String.format(con.STATUS_ERROR + " '%s'", e.getMessage()));
        } catch (TOSCAnaServerException e) {
            System.err.println(String.format(con.STATUS_ERROR + " %s '%s'", e.getStatusCode(), e.getErrorResponse().getMessage()));
        }
        return stringStatus.toString();
    }

    public String showMetrics() {
        Map<String, Object> status;
        StringBuilder stringMetrics = new StringBuilder();
        try {
            status = toscAnaAPI.getTransformerMetrics();

            stringMetrics.append(status.keySet());
        } catch (IOException e) {
            System.err.println(String.format(con.STATUS_METRIC_ERROR + " '%s'", e.getMessage()));
        } catch (TOSCAnaServerException e) {
            System.err.println(String.format(con.STATUS_METRIC_ERROR + " %s '%s'", e.getStatusCode(), e.getErrorResponse().getMessage()));
        }
        return stringMetrics.toString();
    }

    public enum Mode {
        HIGH, LOW, NONE
    }
}
