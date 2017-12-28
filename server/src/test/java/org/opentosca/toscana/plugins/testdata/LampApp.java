package org.opentosca.toscana.plugins.testdata;

import java.util.HashSet;
import java.util.Set;

import org.opentosca.toscana.model.artifact.Artifact;
import org.opentosca.toscana.model.capability.ContainerCapability;
import org.opentosca.toscana.model.capability.ContainerCapability.ContainerCapabilityBuilder;
import org.opentosca.toscana.model.capability.OsCapability;
import org.opentosca.toscana.model.node.Apache;
import org.opentosca.toscana.model.node.Compute;
import org.opentosca.toscana.model.node.MysqlDatabase;
import org.opentosca.toscana.model.node.MysqlDbms;
import org.opentosca.toscana.model.node.RootNode;
import org.opentosca.toscana.model.node.WebApplication;
import org.opentosca.toscana.model.operation.Operation;
import org.opentosca.toscana.model.operation.OperationVariable;
import org.opentosca.toscana.model.operation.StandardLifecycle;

public class LampApp {

    private final Set<RootNode> testNodes = new HashSet<>();

    public Set<RootNode> createLampApp() {
        createLampModel();
        return testNodes;
    }

    public static Set<RootNode> getLampModel() {
        return new LampApp().createLampApp();
    }

    private void createLampModel() {
        testNodes.add(createComputeNode());
        testNodes.add(createMysqlDbms());
        testNodes.add(createMysqlDatabase());
        testNodes.add(createApache());
        testNodes.add(createWebApplication());
    }

    private Compute createComputeNode() {
        OsCapability osCapability = OsCapability
            .builder()
            .distribution(OsCapability.Distribution.UBUNTU)
            .type(OsCapability.Type.LINUX)
            .version("16.04")
            .build();
        Compute computeNode = Compute
            .builder("server")
            .os(osCapability)
            .host(createContainerCapability())
            .build();
        return computeNode;
    }

    private ContainerCapability createContainerCapability() {
        Set<Class<? extends RootNode>> validSourceTypes = new HashSet<>();
        validSourceTypes.add(Compute.class);
        validSourceTypes.add(MysqlDbms.class);

        ContainerCapabilityBuilder containerCapabilityBuilder = ContainerCapability.builder()
            .memSizeInMB(1024)
            .diskSizeInMB(2000)
            .numCpus(1)
            .validSourceTypes(validSourceTypes);

        return containerCapabilityBuilder.build();
    }

    private MysqlDbms createMysqlDbms() {
        Operation dbmsOperation = Operation.builder()
            .artifact(Artifact.builder("artifact", "mysql_dbms/mysql_dbms_configure.sh").build())
            .input(new OperationVariable("db_root_password"))
            .build();

        StandardLifecycle lifecycle = StandardLifecycle.builder()
            .configure(dbmsOperation)
            .build();

        return MysqlDbms.builder(
            "mysql_dbms",
            "geheim")
            .port(3306)
            .standardLifecycle(lifecycle)
            .build();
    }

    private MysqlDatabase createMysqlDatabase() {
        return MysqlDatabase
            .builder("my_db", "DBNAME")
            .build();
    }

    private Apache createApache() {
        ContainerCapability containerCapability = createContainerCapability();
        return Apache
            .builder("apache_web_server")
            .containerHost(containerCapability)
            .build();
    }

    private WebApplication createWebApplication() {
        Set<String> appDependencies = new HashSet<>();
        appDependencies.add("my_app/myphpapp.php");
        appDependencies.add("my_app/mysql-credentials.php");
        Operation appCreate = Operation.builder()
            .artifact(Artifact.builder("artifact", "my_app/create_myphpapp.sh").build())
            .dependencies(appDependencies)
            .build();

        Set<OperationVariable> appInputs = new HashSet<>();
        appInputs.add(new OperationVariable("database_host"));
        appInputs.add(new OperationVariable("database_password"));
        appInputs.add(new OperationVariable("database_name"));
        appInputs.add(new OperationVariable("database_user"));
        OperationVariable dbPort = new OperationVariable("database_port");
        dbPort.setValue("3306");
        appInputs.add(dbPort);

        Operation appConfigure = Operation.builder()
            .artifact(Artifact.builder("artifact", "my_app/configure_myphpapp.sh").build())
            .inputs(appInputs)
            .build();

        StandardLifecycle webAppLifecycle = StandardLifecycle.builder()
            .create(appCreate)
            .configure(appConfigure)
            .build();
        return WebApplication
            .builder("my_app")
            .standardLifecycle(webAppLifecycle)
            .build();
    }
}
