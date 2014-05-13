/**
 *
 */
package eu.contrail.security.servercommons;

/**
 * @author ales
 */
public enum ContrailAttributeTypes {

    String("contrail:string"),
    Integer("contrail:integer");

    private String typeName;

    ContrailAttributeTypes(String typeName) {
        this.typeName = typeName;
    }

}
