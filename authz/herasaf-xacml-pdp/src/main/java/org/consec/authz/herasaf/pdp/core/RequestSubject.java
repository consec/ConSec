package org.consec.authz.herasaf.pdp.core;

import org.herasaf.xacml.core.context.impl.AttributeType;
import org.herasaf.xacml.core.context.impl.AttributeValueType;
import org.herasaf.xacml.core.context.impl.SubjectType;
import org.herasaf.xacml.core.context.transformable.SubjectTransformable;
import org.herasaf.xacml.core.dataTypeAttribute.DataTypeAttribute;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class RequestSubject implements SubjectTransformable {
    private List<SubjectType> subjectTypeList;

    public RequestSubject() {
        subjectTypeList = new ArrayList<SubjectType>();
    }

    public void addSubjectAttr(String attributeId, DataTypeAttribute type, Object value) {
        SubjectType subject = new SubjectType();
        subjectTypeList.add(subject);

        AttributeType attr = new AttributeType();
        subject.getAttributes().add(attr);

        attr.setAttributeId(attributeId);
        attr.setDataType(type);
        AttributeValueType attrValue = new AttributeValueType();
        attrValue.getContent().add(value);
        attr.getAttributeValues().add(attrValue);
    }

    @Override
    public Collection<? extends SubjectType> transform() {
        return subjectTypeList;
    }
}
