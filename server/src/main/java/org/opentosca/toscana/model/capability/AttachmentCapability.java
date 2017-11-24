package org.opentosca.toscana.model.capability;

import java.util.Set;

import org.opentosca.toscana.model.datatype.Range;
import org.opentosca.toscana.model.node.BlockStorage;
import org.opentosca.toscana.model.node.RootNode;

import lombok.Builder;
import lombok.Data;
import lombok.Singular;

/**
 The default TOSCA type that should be used or extended to define an attachment capability of a
 (logical) infrastructure device node (e.g., {@link BlockStorage} node).
 */
@Data
public class AttachmentCapability extends Capability {

    @Builder
    protected AttachmentCapability(@Singular Set<Class<? extends RootNode>> validSourceTypes,
                                   Range occurence,
                                   String description) {
        super(validSourceTypes, occurence, description);
    }

    public static class AttachmentCapabilityBuilder extends CapabilityBuilder {
    }
}