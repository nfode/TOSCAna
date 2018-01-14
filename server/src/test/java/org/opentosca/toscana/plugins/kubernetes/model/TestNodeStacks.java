package org.opentosca.toscana.plugins.kubernetes.model;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.opentosca.toscana.core.testdata.TestCsars;
import org.opentosca.toscana.model.EffectiveModel;
import org.opentosca.toscana.model.node.RootNode;
import org.opentosca.toscana.plugins.kubernetes.util.KubernetesNodeContainer;
import org.opentosca.toscana.plugins.kubernetes.util.NodeStack;

import com.google.common.collect.Sets;

public class TestNodeStacks {
    public static Set<NodeStack> getLampNodeStacks() {
        return new TestNodeStacks().createLampNodeStacks();
    }

    private HashSet<NodeStack> createLampNodeStacks() {
        Map<String, RootNode> map = new EffectiveModel(TestCsars.VALID_LAMP_INPUT_TEMPLATE).getNodeMap();

        List<KubernetesNodeContainer> webAppNodes = new LinkedList<>();
        KubernetesNodeContainer computeContainer = new KubernetesNodeContainer(map.get("server"));
        computeContainer.hasParentComputeNode();
        webAppNodes.add(new KubernetesNodeContainer(map.get("my_app")));
        webAppNodes.add(new KubernetesNodeContainer(map.get("apache_web_server")));
        webAppNodes.add(computeContainer);

        NodeStack webAppNodeStack = new NodeStack(webAppNodes);
        //Manualy set the docker image tag (used for testing the ResourceFileCreator)
        webAppNodeStack.setDockerImageTag("my-app");
        return Sets.newHashSet(webAppNodeStack);
    }
}
