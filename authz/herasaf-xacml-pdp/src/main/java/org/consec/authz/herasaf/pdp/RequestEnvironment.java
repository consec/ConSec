package org.consec.authz.herasaf.pdp;

import org.herasaf.xacml.core.context.impl.AttributeType;
import org.herasaf.xacml.core.context.impl.AttributeValueType;
import org.herasaf.xacml.core.context.impl.EnvironmentType;
import org.herasaf.xacml.core.context.transformable.EnvironmentTransformable;
import org.herasaf.xacml.core.dataTypeAttribute.DataTypeAttribute;

public class RequestEnvironment implements EnvironmentTransformable {
    private EnvironmentType environment;

    public RequestEnvironment() {
        environment = null;
    }

    public RequestEnvironment(String attributeId, DataTypeAttribute type, Object value) {
        environment = new EnvironmentType();

        AttributeType attr = new AttributeType();
        environment.getAttributes().add(attr);

        attr.setAttributeId(attributeId);
        attr.setDataType(type);
        AttributeValueType attrValue = new AttributeValueType();
        attrValue.getContent().add(value);
        attr.getAttributeValues().add(attrValue);
    }

    @Override
    public EnvironmentType transform() {
        return environment;
    }
}
