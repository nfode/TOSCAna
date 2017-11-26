package org.opentosca.toscana.model.capability;

import java.util.Optional;
import java.util.Set;

import org.opentosca.toscana.model.datatype.Range;
import org.opentosca.toscana.model.node.RootNode;

import lombok.Builder;
import lombok.Data;
import lombok.Singular;

/**
 The default TOSCA type to express an Operating System capability for a node.
 (TOSCA Simple Profile in YAML Version 1.1, p. 157)
 */
@Data
public class OsCapability extends Capability {

    public enum Architecture {
        x86_32,
        x86_64,
        // might grow
    }

    public enum Type {
        AIX,
        LINUX,
        MAC,
        WINDOWS,
        // might grow
    }

    public enum Distribution {
        ARCH,
        DEBIAN,
        FEDORA,
        RHEL,
        UBUNTU,
        // might grow
    }

    /**
     The Operating System architecture.
     (TOSCA Simple Profile in YAML Version 1.1, p. 157)
     */
    private final Architecture architecture;

    /**
     The Operating System type.
     (TOSCA Simple Profile in YAML Version 1.1, p. 157)
     */
    private final Type type;

    /**
     The Operating System distribution.
     (TOSCA Simple Profile in YAML Version 1.1, p. 157)
     */
    private final Distribution distribution;

    /**
     The Operating System version.
     (TOSCA Simple Profile in YAML Version 1.1, p. 157)
     */
    private final String version;

    @Builder
    protected OsCapability(Architecture architecture,
                           Type type,
                           Distribution distribution,
                           String version,
                           @Singular Set<Class<? extends RootNode>> validSourceTypes,
                           Range occurence,
                           String description) {
        super(validSourceTypes, occurence, description);
        this.architecture = architecture;
        this.type = type;
        this.distribution = distribution;
        this.version = version;
    }

    /**
     @return {@link #architecture}
     */
    public Optional<Architecture> getArchitecture() {
        return Optional.ofNullable(architecture);
    }

    /**
     @return {@link #type}
     */
    public Optional<Type> getType() {
        return Optional.ofNullable(type);
    }

    /**
     @return {@link #distribution}
     */
    public Optional<Distribution> getDistribution() {
        return Optional.ofNullable(distribution);
    }

    /**
     @return {@link #version}
     */
    public Optional<String> getVersion() {
        return Optional.ofNullable(version);
    }

    public static class OsCapabilityBuilder extends CapabilityBuilder {
    }
}