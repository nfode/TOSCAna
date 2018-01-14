package org.opentosca.toscana.core.parse.graphconverter;

import org.opentosca.toscana.model.BaseToscaElement;
import org.opentosca.toscana.model.util.ToscaKey;

import org.apache.commons.lang3.EnumUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TypeConverter {

    private final Logger logger;
    private final ToscaFactory toscaFactory;

    public TypeConverter(Logger logger) {
        this.logger = logger;
        this.toscaFactory = new ToscaFactory(logger);
    }

    public TypeConverter() {
        this(LoggerFactory.getLogger(TypeConverter.class));
    }

    public <T> T convert(BaseEntity entity, ToscaKey<T> key) {
        if (entity instanceof ScalarEntity) {
            ScalarEntity scalarEntity = (ScalarEntity) entity;
            return convert(scalarEntity.get(), key.getType());
        } else if (BaseToscaElement.class.isAssignableFrom(key.getType())) {
            MappingEntity mappingEntity = (MappingEntity) entity;
            return toscaFactory.wrapEntity(mappingEntity, key.getType());
        } else {
            // TODO currently intrinsic functions get here --> finally implement intrinsic function support.
            throw new UnsupportedOperationException();
        }
    }

    private <T> T convert(String string, Class targetType) {
        if (targetType.getSimpleName().equals("String")) {
            return (T) string;
        } else if (targetType.getSimpleName().equals("Integer")) {
            return (T) Integer.valueOf(string);
        } else if (targetType.isEnum()) {
            T result = (T) EnumUtils.getEnum(targetType, string);
            // TODO handle wrong values
            return result;
        } else {
            throw new UnsupportedOperationException(String.format(
                "Cannot convert value of type %s: currently unsupported", targetType.getSimpleName()));
        }
        // TODO support for Credential
    }
}
