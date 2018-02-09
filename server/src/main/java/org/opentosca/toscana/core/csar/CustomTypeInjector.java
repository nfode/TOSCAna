package org.opentosca.toscana.core.csar;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import org.opentosca.toscana.core.parse.EntrypointDetector;
import org.opentosca.toscana.core.parse.InvalidCsarException;
import org.opentosca.toscana.core.transformation.logging.Log;

import com.google.common.collect.Lists;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;

public class CustomTypeInjector {

    private final static File CUSTOM_TYPE_DEFINITION_ROOT = new File(CustomTypeInjector.class.getResource("/tosca_definitions").getFile());
    private final static File NODE_TYPE_DEFINITIONS = new File(CUSTOM_TYPE_DEFINITION_ROOT, "node");
    private final static String NOTE = "  # autogenerated by toscana";

    private final Log log;
    private final Logger logger;

    public CustomTypeInjector(Log log) {
        this.log = log;
        this.logger = log.getLogger(CustomTypeInjector.class);
    }

    /**
     Injects custom TOSCA node type definitions into service templates, if necessary
     */
    public void inject(Csar csar) {
        try {
            File serviceTemplate = new EntrypointDetector(log).findEntryPoint(csar.getContentDir());
            String templateContent = FileUtils.readFileToString(serviceTemplate);
            List<String> template = Lists.newArrayList(templateContent.split("\n"));

            for (File nodeTypeDefinition : NODE_TYPE_DEFINITIONS.listFiles()) {
                if (isRequired(nodeTypeDefinition, templateContent)) {
                    template = inject(template, nodeTypeDefinition, "node_types:");
                }
            }
            FileUtils.writeLines(serviceTemplate, template);
        } catch (InvalidCsarException | IOException e) {
            logger.error("Failed to inject custom types into csar", e);
        }
    }

    private boolean isRequired(File nodeTypeDefinition, String templateContent) {
        String typeName = nodeTypeDefinition.getName().replace(".yaml", "");
        String typeAssignment = "type: " + typeName;
        return (templateContent.contains(typeAssignment));
    }

    private List<String> inject(List<String> template, File typeDefinition, String location) throws IOException {
        String typeDefinitionString = FileUtils.readFileToString(typeDefinition);
        List<String> definitionContent = Lists.newArrayList(typeDefinitionString.split("\n"));
        definitionContent.add(0, NOTE);
        definitionContent.add("");
        List<String> injectedTemplate = new LinkedList<>();
        boolean injected = false;
        for (String line : template) {
            injectedTemplate.add(line);
            if (line.startsWith(location) && !injected) {
                injectedTemplate.addAll(definitionContent);
                injected = true;
            }
        }
        if (!injected) {
            injectedTemplate.add("");
            injectedTemplate.add(location);
            injectedTemplate.addAll(definitionContent);
        }
        return injectedTemplate;
    }
}
