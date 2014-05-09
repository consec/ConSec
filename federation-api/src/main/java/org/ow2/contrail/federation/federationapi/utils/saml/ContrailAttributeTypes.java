/**
 * 
 */
package org.ow2.contrail.federation.federationapi.utils.saml;

/**
 * @author ales
 *
 */
public enum ContrailAttributeTypes {

	String("contrail:string"),
	Integer("contrail:integer");
	
	private String typeName;
	
	ContrailAttributeTypes(String typeName){
		this.typeName=typeName;
	}
	
}
