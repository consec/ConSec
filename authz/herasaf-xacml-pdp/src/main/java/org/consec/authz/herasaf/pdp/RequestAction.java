package org.consec.authz.herasaf.pdp;

import org.herasaf.xacml.core.context.impl.ActionType;
import org.herasaf.xacml.core.context.impl.AttributeType;
import org.herasaf.xacml.core.context.impl.AttributeValueType;
import org.herasaf.xacml.core.context.transformable.ActionTransformable;
import org.herasaf.xacml.core.dataTypeAttribute.DataTypeAttribute;

public class RequestAction implements ActionTransformable {
    private ActionType action;

    public RequestAction() {
        action = null;
    }

    public RequestAction(String attributeId, DataTypeAttribute type, Object value) {
        action = new ActionType();

        AttributeType attr = new AttributeType();
        action.getAttributes().add(attr);

        attr.setAttributeId(attributeId);
        attr.setDataType(type);
        AttributeValueType attrValue = new AttributeValueType();
        attrValue.getContent().add(value);
        attr.getAttributeValues().add(attrValue);
    }

    @Override
    public ActionType transform() {
        return action;
    }
}
