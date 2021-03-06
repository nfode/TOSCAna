package org.opentosca.toscana.cli;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.opentosca.toscana.cli.commands.Constants;
import org.opentosca.toscana.retrofit.ToscanaApi;
import org.opentosca.toscana.retrofit.model.Csar;
import org.opentosca.toscana.retrofit.model.LifecyclePhase;
import org.opentosca.toscana.retrofit.model.LogEntry;
import org.opentosca.toscana.retrofit.model.Platform;
import org.opentosca.toscana.retrofit.model.Transformation;
import org.opentosca.toscana.retrofit.model.TransformationInputs;
import org.opentosca.toscana.retrofit.model.TransformationLogs;
import org.opentosca.toscana.retrofit.model.TransformationProperty;
import org.opentosca.toscana.retrofit.model.TransformerStatus;
import org.opentosca.toscana.retrofit.model.TransformerStatus.TransformationInformation;
import org.opentosca.toscana.retrofit.model.embedded.CsarResources;
import org.opentosca.toscana.retrofit.model.embedded.PlatformResources;
import org.opentosca.toscana.retrofit.model.embedded.TransformationResources;
import org.opentosca.toscana.retrofit.util.LoggingMode;
import org.opentosca.toscana.retrofit.util.TOSCAnaServerException;

import static org.opentosca.toscana.cli.commands.Constants.SOMETHING_WRONG;

public class ApiController {

    private ToscanaApi toscanaApi;
    private LoggingMode logMode;

    /**
     Constructor for the ApiController, parameters decide if there should be any output of information
     */
    public ApiController(String apiUrl) {
        toscanaApi = new ToscanaApi(apiUrl);
    }

    /**
     Calls the REST API to upload the CSAR, handles different response codes which are returned

     @param file CSAR Archive to upload
     @return output for the CLI
     */
    public String uploadCsar(File file) {
        try {
            toscanaApi.uploadCsar(file.getName(), file);
        } catch (IOException e) {
            System.err.println(String.format(SOMETHING_WRONG + Constants.CSAR_UPLOAD_IO_ERROR
                + Constants.ERROR_PLACEHOLDER, e.getMessage()));
            verboseStack(e);
        } catch (TOSCAnaServerException e) {
            verboseStack(e);
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
            toscanaApi.deleteCsar(csar);
        } catch (IOException e) {
            System.err.println(String.format(SOMETHING_WRONG + Constants.CSAR_DELETE_ERROR
                + Constants.ERROR_PLACEHOLDER, e.getMessage()));
            verboseStack(e);
        } catch (TOSCAnaServerException e) {
            System.err.println(String.format(SOMETHING_WRONG + Constants.CSAR_DELETE_ERROR
                + Constants.SERVER_ERROR_PLACEHOLDER, e.getStatusCode(), e.getErrorResponse().getMessage()));
            verboseStack(e);
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
            csarList = toscanaApi.getCsars();
            List<Csar> list = csarList.getContent();
            if (list != null) {
                for (Csar c : list) {
                    stringCsars.append(c.getName()).append("\n");
                }
                stringCsars.delete(stringCsars.length() - 1, stringCsars.length());
            } else {
                return Constants.CSAR_LIST_EMPTY;
            }
        } catch (IOException e) {
            System.err.println(String.format(SOMETHING_WRONG + Constants.CSAR_LIST_ERROR
                + Constants.ERROR_PLACEHOLDER, e.getMessage()));
            verboseStack(e);
        } catch (TOSCAnaServerException e) {
            System.err.println(String.format(SOMETHING_WRONG + Constants.CSAR_LIST_ERROR
                + Constants.SERVER_ERROR_PLACEHOLDER, e.getStatusCode(), e.getErrorResponse().getMessage()));
            verboseStack(e);
        }
        return stringCsars.toString();
    }

    /**
     Calls the REST API and prints detailed Information for the specified CSAR if it's available

     @param csarName Name of the CSAR which information should be shown
     @return output for the CLI
     */
    public String infoCsar(String csarName) {
        StringBuilder csarInfo = new StringBuilder();
        Csar csar;
        try {
            csar = toscanaApi.getCsarDetails(csarName);
            if (csar != null) {
                csarInfo.append("Name: ").append(csar.getName());

                if (csar.getPhases() != null) {
                    for (LifecyclePhase life : csar.getPhases()) {
                        csarInfo.append("\n").append(life.getName())
                            .append(", ").append(life.getState());
                    }
                }
            } else {
                return Constants.CSAR_INFO_EMPTY;
            }
        } catch (IOException e) {
            System.err.println(String.format(SOMETHING_WRONG + Constants.CSAR_INFO_ERROR
                + Constants.ERROR_PLACEHOLDER, e.getMessage()));
            verboseStack(e);
        } catch (TOSCAnaServerException e) {
            System.err.println(String.format(SOMETHING_WRONG + Constants.CSAR_INFO_ERROR
                + Constants.SERVER_ERROR_PLACEHOLDER, e.getStatusCode(), e.getErrorResponse().getMessage()));
            verboseStack(e);
        }
        return csarInfo.toString();
    }

    /**
     Calls the REST API and starts the Transformation, handles response codes

     @param csar CSAR for which a transformation should be started
     @param plat platform for which a transformation should be started
     @return output for the CLI
     */
    public String startTransformation(String csar, String plat) {
        try {
            toscanaApi.createTransformation(csar, plat);
            launchTransformation(csar, plat);
        } catch (IOException e) {
            System.err.println(String.format(SOMETHING_WRONG + Constants.TRANSFORMATION_CREATE_ERROR
                + Constants.ERROR_PLACEHOLDER, e.getMessage()));
            verboseStack(e);
        } catch (TOSCAnaServerException e) {
            System.err.println(String.format(SOMETHING_WRONG + Constants.TRANSFORMATION_CREATE_ERROR
                + Constants.SERVER_ERROR_PLACEHOLDER, e.getStatusCode(), e.getErrorResponse().getMessage()));
            verboseStack(e);
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
            toscanaApi.startTransformation(csar, platform);
        } catch (IOException e) {
            System.err.println(String.format(SOMETHING_WRONG + Constants.TRANSFORMATION_START_ERROR
                + Constants.ERROR_PLACEHOLDER, e.getMessage()));
            verboseStack(e);
        } catch (TOSCAnaServerException e) {
            System.err.println(String.format(SOMETHING_WRONG + Constants.TRANSFORMATION_START_ERROR
                + Constants.SERVER_ERROR_PLACEHOLDER, e.getStatusCode(), e.getErrorResponse().getMessage()));
            verboseStack(e);
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
        return Constants.TRANSFORMATION_STOP;
    }

    /**
     Calls the REST API and deletes the specified Transformation, handles response codes

     @param csar CSAR for which transformation should be deleted
     @param plat platform for which the transformation should be deleted
     @return output for the CLI
     */
    public String deleteTransformation(String csar, String plat) {
        try {
            toscanaApi.deleteTransformation(csar, plat);
        } catch (IOException e) {
            System.err.println(String.format(SOMETHING_WRONG + Constants.TRANSFORMATION_DELETE_ERROR
                + Constants.ERROR_PLACEHOLDER, e.getMessage()));
            verboseStack(e);
        } catch (TOSCAnaServerException e) {
            System.err.println(String.format(SOMETHING_WRONG + Constants.TRANSFORMATION_DELETE_ERROR
                + Constants.SERVER_ERROR_PLACEHOLDER, e.getStatusCode(), e.getErrorResponse().getMessage()));
            verboseStack(e);
        }
        return "";
    }

    /**
     Calls the REST API to download an Artifact for the specified finished Transformation, handles response codes

     @param csar CSAR for which to download an Artifact
     @param plat Platform for which the Artifact should be downloaded
     @return output for the CLI
     */
    public String downloadTransformationUrl(String csar, String plat) {
        return toscanaApi.getArtifactDownloadUrl(csar, plat);
    }

    /**
     Calls the REST API to download an Artifact for the specified finished Transformation, output is stored at
     specified location

     @param csar CSAR for which to download an Artifact
     @param plat Platform for which the Artifact should be downloaded
     @return output for the CLI
     */
    public String downloadTransformationStream(String csar, String plat, File destinationFile) {
        try {
            InputStream input = toscanaApi.downloadArtifactAsStream(csar, plat);
            java.nio.file.Files.copy(input, destinationFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            input.close();
        } catch (IOException e) {
            System.err.println(String.format(SOMETHING_WRONG + Constants.TRANSFORMATION_DOWNLOAD_ERROR
                + Constants.ERROR_PLACEHOLDER, e.getMessage()));
            verboseStack(e);
        } catch (TOSCAnaServerException e) {
            System.err.println(String.format(SOMETHING_WRONG + Constants.TRANSFORMATION_DOWNLOAD_ERROR
                + Constants.SERVER_ERROR_PLACEHOLDER, e.getStatusCode(), e.getErrorResponse().getMessage()));
            verboseStack(e);
        }
        return "";
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
            transformationList = toscanaApi.getTransformations(csar);
            List<Transformation> list = transformationList.getContent();
            if (list != null) {
                for (Transformation t : list) {
                    stringTransformations.append("Platform: ").append(t.getPlatform()).append("\n");
                }
                stringTransformations.delete(stringTransformations.length() - 1, stringTransformations.length());
            } else {
                return Constants.TRANSFORMATION_LIST_EMPTY;
            }
        } catch (IOException e) {
            System.err.println(String.format(SOMETHING_WRONG + Constants.TRANSFORMATION_LIST_ERROR
                + Constants.ERROR_PLACEHOLDER, e.getMessage()));
            verboseStack(e);
        } catch (TOSCAnaServerException e) {
            System.err.println(String.format(SOMETHING_WRONG + Constants.TRANSFORMATION_LIST_ERROR
                + Constants.SERVER_ERROR_PLACEHOLDER, e.getStatusCode(), e.getErrorResponse().getMessage()));
            verboseStack(e);
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
            transformation = toscanaApi.getTransformation(csar, plat);
            if (transformation != null) {

                stringTransformation.append("Platform: ").append(transformation.getPlatform())
                    .append(", Overall State: ").append(transformation.getState());

                if (transformation.getPhases() != null) {
                    for (LifecyclePhase life : transformation.getPhases()) {
                        stringTransformation.append("\n").append(life.getName())
                            .append(", ").append(life.getState());
                    }
                }
            } else {
                return Constants.TRANSFORMATION_INFO_EMPTY;
            }
        } catch (IOException e) {
            System.err.println(String.format(SOMETHING_WRONG + Constants.TRANSFORMATION_INFO_ERROR
                + Constants.ERROR_PLACEHOLDER, e.getMessage()));
            verboseStack(e);
        } catch (TOSCAnaServerException e) {
            System.err.println(String.format(SOMETHING_WRONG + Constants.TRANSFORMATION_INFO_ERROR
                + Constants.SERVER_ERROR_PLACEHOLDER, e.getStatusCode(), e.getErrorResponse().getMessage()));
            verboseStack(e);
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
            logsList = toscanaApi.getLogs(csar, plat, start);
            List<LogEntry> list = logsList.getLogEntries();
            if (list != null) {
                for (LogEntry l : list) {
                    Date date = new Date(l.getTimestamp());
                    stringLogs.append(date).append(", ")
                        .append(l.getLevel()).append(", ")
                        .append(l.getMessage()).append("\n");
                }
                stringLogs.delete(stringLogs.length() - 1, stringLogs.length());
            } else {
                return Constants.TRANSFORMATION_LOGS_EMPTY;
            }
        } catch (IOException e) {
            System.err.println(String.format(SOMETHING_WRONG + Constants.TRANSFORMATION_LOGS_ERROR
                + Constants.ERROR_PLACEHOLDER, e.getMessage()));
            verboseStack(e);
        } catch (TOSCAnaServerException e) {
            System.err.println(String.format(SOMETHING_WRONG + Constants.TRANSFORMATION_LOGS_ERROR
                + Constants.SERVER_ERROR_PLACEHOLDER, e.getStatusCode(), e.getErrorResponse().getMessage()));
            verboseStack(e);
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
        TransformationInputs propertiesList;
        StringBuilder stringProperties = new StringBuilder();
        try {
            propertiesList = toscanaApi.getInputs(csar, plat);
            List<TransformationProperty> list = propertiesList.getInputs();
            if (list != null) {
                for (TransformationProperty p : list) {
                    stringProperties.append("Key: ").append(p.getKey())
                        .append(", Type: ").append(p.getType())
                        .append(", Required: ").append(p.isRequired())
                        .append(", Description: ").append(p.getDescription()).append("\n");
                }
                stringProperties.delete(stringProperties.length() - 1, stringProperties.length());
            } else {
                return Constants.INPUT_LIST_EMPTY;
            }
        } catch (IOException e) {
            System.err.println(String.format(SOMETHING_WRONG + Constants.INPUT_LIST_ERROR
                + Constants.ERROR_PLACEHOLDER, e.getMessage()));
            verboseStack(e);
        } catch (TOSCAnaServerException e) {
            System.err.println(String.format(SOMETHING_WRONG + Constants.INPUT_LIST_ERROR
                + Constants.SERVER_ERROR_PLACEHOLDER, e.getStatusCode(), e.getErrorResponse().getMessage()));
            verboseStack(e);
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
        TransformationInputs sendProp = new TransformationInputs(properties);

        //get Return of the update if it was successfull
        Map<String, Boolean> propertiesReturn;
        StringBuilder stringProperties = new StringBuilder();
        try {
            propertiesReturn = toscanaApi.updateProperties(csar, plat, sendProp);
            if (propertiesReturn != null) {
                for (String s : propertiesReturn.keySet()) {
                    stringProperties.append(s).append(", accepted: ")
                        .append(propertiesReturn.get(s)).append("\n");
                }
                stringProperties.delete(stringProperties.length() - 1, stringProperties.length());
            } else {
                return Constants.INPUT_SET_ERROR;
            }
        } catch (IOException e) {
            System.err.println(String.format(SOMETHING_WRONG + Constants.INPUT_SET_ERROR
                + Constants.ERROR_PLACEHOLDER, e.getMessage()));
            verboseStack(e);
        } catch (TOSCAnaServerException e) {
            System.err.println(String.format(SOMETHING_WRONG + Constants.INPUT_SET_ERROR
                + Constants.SERVER_ERROR_PLACEHOLDER, e.getStatusCode(), e.getErrorResponse().getMessage()));
            verboseStack(e);
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
            platformList = toscanaApi.getPlatforms();
            List<Platform> list = platformList.getContent();
            if (list != null) {
                for (Platform p : list) {
                    stringPlatforms.append("ID: ").append(p.getId()).append(", Name: ")
                        .append(p.getName()).append("\n");
                }
                stringPlatforms.delete(stringPlatforms.length() - 1, stringPlatforms.length());
            } else {
                return Constants.PLATFORM_LIST_EMPTY;
            }
        } catch (IOException e) {
            System.err.println(String.format(SOMETHING_WRONG + Constants.PLATFORM_LIST_ERROR
                + Constants.ERROR_PLACEHOLDER, e.getMessage()));
            verboseStack(e);
        } catch (TOSCAnaServerException e) {
            System.err.println(String.format(SOMETHING_WRONG + Constants.PLATFORM_LIST_ERROR
                + Constants.SERVER_ERROR_PLACEHOLDER, e.getStatusCode(), e.getErrorResponse().getMessage()));
            verboseStack(e);
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
            platform = toscanaApi.getPlatformDetails(plat);
            if (platform != null) {
                stringPlatform.append("ID: ").append(platform.getId()).append(", Name: ")
                    .append(platform.getName()).append(", supports Deployment: ").append(platform.supportsDeployment());
            } else {
                return Constants.PLATFORM_INFO_EMPTY;
            }
        } catch (IOException e) {
            System.err.println(String.format(SOMETHING_WRONG + Constants.PLATFORM_INFO_ERROR
                + Constants.ERROR_PLACEHOLDER, e.getMessage()));
            verboseStack(e);
        } catch (TOSCAnaServerException e) {
            System.err.println(String.format(SOMETHING_WRONG + Constants.PLATFORM_INFO_ERROR
                + Constants.SERVER_ERROR_PLACEHOLDER, e.getStatusCode(), e.getErrorResponse().getMessage()));
            verboseStack(e);
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
            status = toscanaApi.getServerStatus();
            if (status != null) {
                List<TransformationInformation> errorTrans = status.getTransformerHealth().getErroredTransformations();
                List<TransformationInformation> runTrans = status.getTransformerHealth().getRunningTransformations();

                stringStatus.append("System: ").append(status.getStatus()).append(", Space free: ")
                    .append(status.getFileSystemHealth().getFreeBytes()).append(", Transformer: ")
                    .append(status.getTransformerHealth().getStatus());

                if (errorTrans != null && errorTrans.size() > 0) {
                    stringStatus.append("\nTransformation errored: ");
                    for (TransformationInformation info : errorTrans) {
                        stringStatus.append("\nName: ").append(info.getCsarName())
                            .append(", Platform: ").append(info.getPlatformName());
                    }
                }

                if (runTrans != null && runTrans.size() > 0) {
                    stringStatus.append("\nTransformation running: ");
                    for (TransformationInformation info : runTrans) {
                        stringStatus.append("\nName: ").append(info.getCsarName())
                            .append(", Platform: ").append(info.getPlatformName());
                    }
                }
            } else {
                return Constants.STATUS_EMPTY;
            }
        } catch (IOException e) {
            System.err.println(String.format(SOMETHING_WRONG + Constants.STATUS_ERROR
                + Constants.ERROR_PLACEHOLDER, e.getMessage()));
            verboseStack(e);
            System.exit(1);
        } catch (TOSCAnaServerException e) {
            System.err.println(String.format(SOMETHING_WRONG + Constants.STATUS_ERROR
                + Constants.SERVER_ERROR_PLACEHOLDER, e.getStatusCode(), e.getErrorResponse().getMessage()));
            verboseStack(e);
            System.exit(1);
        }
        return stringStatus.toString();
    }

    /**
     Prints the Stacktrace for the Error when the LoggingMode is set to High, "-m" Option is called

     @param e specific Error
     */
    private void verboseStack(Exception e) {
        if (logMode.name().equals("HIGH")) {
            e.printStackTrace();
        }
    }

    public void setLoggingMode(LoggingMode loggingMode) {
        logMode = loggingMode;
        toscanaApi.setLoggingMode(loggingMode);
    }
}
