package org.opentosca.toscana.core.parse.model;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.opentosca.toscana.core.parse.converter.GraphNormalizer;
import org.opentosca.toscana.core.parse.converter.LinkResolver;
import org.opentosca.toscana.core.parse.converter.TypeWrapper;
import org.opentosca.toscana.core.parse.converter.util.ToscaStructure;
import org.opentosca.toscana.core.transformation.logging.Log;
import org.opentosca.toscana.core.transformation.properties.Property;
import org.opentosca.toscana.model.EntityId;
import org.opentosca.toscana.model.Parameter;

import org.slf4j.Logger;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.nodes.Node;

public class ServiceModel {

    private final Log log;
    private final Logger logger;
    private final ServiceGraph graph;
    private Map<String, Property> inputs;

    public ServiceModel(File template, Log log) {
        this.log = log;
        this.logger = log.getLogger(getClass());
        Yaml yaml = new Yaml();
        try {
            Node snakeNode = yaml.compose(new FileReader(template));
            this.graph = new ServiceGraph(snakeNode, log);
            ToscaStructure.buildBasicStructure(this.graph); // in case this has not already been established
            GraphNormalizer.normalize(this);
            new LinkResolver(this).resolveLinks();
        } catch (FileNotFoundException e) {
            logger.error(String.format("Template '%s' does not exist - failed to construct ServiceModel", template), e);
            throw new IllegalStateException();
        }
    }

    public Optional<Entity> getEntity(List<String> context) {
        return graph.getEntity(context);
    }

    public Optional<Entity> getEntity(EntityId context) {
        return graph.getEntity(context);
    }

    public Entity getEntityOrThrow(EntityId entityId) {
        return graph.getEntityOrThrow(entityId);
    }

    public void addEntity(Entity entity) {
        graph.addEntity(entity);
    }

    /**
     Returns an iterator for a set of entities referenced by given EntityId.
     */
    public Iterator<Entity> iterator(EntityId id) {
        Entity entities = this.getEntityOrThrow(id);
        return entities.getChildren().iterator();
    }

    public Map<String, Property> getInputs() {
        if (inputs == null) {
            inputs = new HashMap<>();
            Set<Entity> inputEntities = graph.getChildren(ToscaStructure.INPUTS);
            for (Entity inputEntity : inputEntities) {
                Parameter input = TypeWrapper.wrapEntity((MappingEntity) inputEntity, Parameter.class);
                inputs.put(input.getEntityName(), input);
            }
        }
        return inputs;
    }

    public ServiceGraph getGraph() {
        return graph;
    }
}