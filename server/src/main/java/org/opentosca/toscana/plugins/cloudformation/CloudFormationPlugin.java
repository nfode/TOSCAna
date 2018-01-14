package org.opentosca.toscana.plugins.cloudformation;

import java.util.HashSet;
import java.util.Set;

import org.opentosca.toscana.core.transformation.TransformationContext;
import org.opentosca.toscana.core.transformation.platform.Platform;
import org.opentosca.toscana.core.transformation.properties.Property;
import org.opentosca.toscana.core.transformation.properties.PropertyType;
import org.opentosca.toscana.plugins.lifecycle.LifecycleAwarePlugin;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class CloudFormationPlugin extends LifecycleAwarePlugin<CloudFormationLifecycle> {
    private final static Logger logger = LoggerFactory.getLogger(CloudFormationPlugin.class);

    public CloudFormationPlugin() {
        super(getPlatformDetails());
    }

    private static Platform getPlatformDetails() {
        String platformId = "cloudformation";
        String platformName = "AWS CloudFormation";
        Set<Property> platformProperties = new HashSet<>();
        
        //TODO change to other PropertyType?
        Property awsAccessKey = new Property("access-key", PropertyType.SECRET,
            "AccesKey needed for AWS authentication.",
            false,
            "");
            
        Property awsSecretKey = new Property("secret-key", PropertyType.SECRET,
            "SecretKey needed for AWS authentication.",
            false,
            "");
        
        platformProperties.add(awsAccessKey);
        platformProperties.add(awsSecretKey);
        
        return new Platform(platformId, platformName, platformProperties);
    }

    @Override
    protected CloudFormationLifecycle getInstance(TransformationContext context) throws Exception {
        return new CloudFormationLifecycle(context);
    }
}
