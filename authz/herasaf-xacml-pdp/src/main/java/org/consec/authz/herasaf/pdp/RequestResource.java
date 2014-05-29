package org.consec.authz.herasaf.pdp;

import org.herasaf.xacml.core.context.impl.AttributeType;
import org.herasaf.xacml.core.context.impl.AttributeValueType;
import org.herasaf.xacml.core.context.impl.ResourceType;
import org.herasaf.xacml.core.context.transformable.ResourceTransformable;
import org.herasaf.xacml.core.dataTypeAttribute.DataTypeAttribute;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class RequestResource implements ResourceTransformable {
    private List<ResourceType> resourceTypeList;

    public RequestResource() {
        resourceTypeList = new ArrayList<ResourceType>();
    }

    public void addResourceAttr(String attributeId, DataTypeAttribute type, Object value) {
        ResourceType resource = new ResourceType();
        resourceTypeList.add(resource);

        AttributeType attr = new AttributeType();
        resource.getAttributes().add(attr);

        attr.setAttributeId(attributeId);
        attr.setDataType(type);
        AttributeValueType attrValue = new AttributeValueType();
        attrValue.getContent().add(value);
        attr.getAttributeValues().add(attrValue);
    }

    @Override
    public Collection<? extends ResourceType> transform() {
        return resourceTypeList;
    }
}
