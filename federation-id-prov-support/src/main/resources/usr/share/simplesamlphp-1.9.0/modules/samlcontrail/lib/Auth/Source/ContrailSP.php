<?php

//class sspmod_samlcontrail_Auth_Source_ContrailSP extends SimpleSAML_Auth_Source {
class sspmod_samlcontrail_Auth_Source_ContrailSP extends sspmod_saml_Auth_Source_SP {

	private $idp_name

	/* The database DSN.
	* See the documentation for the various database drivers for information about the syntax:
	*     http://www.php.net/manual/en/pdo.drivers.php
	*/
	private $dsn;

	/**
	 * The entity ID of this SP.
	 *
	 * @var string
	 */
	private $entityId;


	/**
	 * The metadata of this SP.
	 *
	 * @var SimpleSAML_Configuration.
	 */
	private $metadata;


	/**
	 * The IdP the user is allowed to log into.
	 *
	 * @var string|NULL  The IdP the user can log into, or NULL if the user can log into all IdPs.
	 */
	private $idp;


	/**
	 * URL to discovery service.
	 *
	 * @var string|NULL
	 */
	private $discoURL;


	/**
	 * Constructor for SAML SP authentication source.
	 *
	 * @param array $info  Information about this authentication source.
	 * @param array $config  Configuration.
	 */
	public function __construct($info, $config) {
		assert('is_array($info)');
		assert('is_array($config)');

		/* Call the parent constructor first, as required by the interface. */
		parent::__construct($info, $config);

		if (!isset($config['entityID'])) {
			$config['entityID'] = $this->getMetadataURL();
		}

		/* For compatibility with code that assumes that $metadata->getString('entityid') gives the entity id. */
		$config['entityid'] = $config['entityID'];

		$this->metadata = SimpleSAML_Configuration::loadFromArray($config, 'authsources[' . var_export($this->authId, TRUE) . ']');
		$this->entityId = $this->metadata->getString('entityID');
		$this->idp = $this->metadata->getString('idp', NULL);
		$this->discoURL = $this->metadata->getString('discoURL', NULL);
		
		if (empty($this->discoURL) && SimpleSAML_Module::isModuleEnabled('discojuice')) {
			$this->discoURL = SimpleSAML_Module::getModuleURL('discojuice/central.php');
		}
		if (!is_string($config['dsn'])) {
	            throw new Exception('Missing or invalid dsn option in config.');
        	}

	        $this->dsn = $config['dsn'];
	        
	    if (!is_string($config['idp_name'])) {
	            throw new Exception('Missing or invalid idp_name option in config.');
        }
	    $this->idp_name = $config['idp_name'];
	}


	/**
	 * Retrieve the URL to the metadata of this SP.
	 *
	 * @return string  The metadata URL.
	 */
	public function getMetadataURL() {

		return SimpleSAML_Module::getModuleURL('saml/sp/metadata.php/' . urlencode($this->authId));
	}


	/**
	 * Retrieve the entity id of this SP.
	 *
	 * @return string  The entity id of this SP.
	 */
	public function getEntityId() {

		return $this->entityId;
	}


	/**
	 * Retrieve the metadata of this SP.
	 *
	 * @return SimpleSAML_Configuration  The metadata of this SP.
	 */
	public function getMetadata() {

		return $this->metadata;

	}


	/**
	 * Retrieve the metadata of an IdP.
	 *
	 * @param string $entityId  The entity id of the IdP.
	 * @return SimpleSAML_Configuration  The metadata of the IdP.
	 */
	public function getIdPMetadata($entityId) {
		assert('is_string($entityId)');

		if ($this->idp !== NULL && $this->idp !== $entityId) {
			throw new SimpleSAML_Error_Exception('Cannot retrieve metadata for IdP ' . var_export($entityId, TRUE) .
				' because it isn\'t a valid IdP for this SP.');
		}

		$metadataHandler = SimpleSAML_Metadata_MetaDataStorageHandler::getMetadataHandler();

		/* First, look in saml20-idp-remote. */
		try {
			return $metadataHandler->getMetaDataConfig($entityId, 'saml20-idp-remote');
		} catch (Exception $e) {
			/* Metadata wasn't found. */
		}


		/* Not found in saml20-idp-remote, look in shib13-idp-remote. */
		try {
			return $metadataHandler->getMetaDataConfig($entityId, 'shib13-idp-remote');
		} catch (Exception $e) {
			/* Metadata wasn't found. */
		}

		/* Not found. */
		throw new SimpleSAML_Error_Exception('Could not find the metadata of an IdP with entity ID ' . var_export($entityId, TRUE));
	}


	/**
	 * Send a SAML1 SSO request to an IdP.
	 *
	 * @param SimpleSAML_Configuration $idpMetadata  The metadata of the IdP.
	 * @param array $state  The state array for the current authentication.
	 */
	private function startSSO1(SimpleSAML_Configuration $idpMetadata, array $state) {

		$idpEntityId = $idpMetadata->getString('entityid');

		$state['saml:idp'] = $idpEntityId;

		$ar = new SimpleSAML_XML_Shib13_AuthnRequest();
		$ar->setIssuer($this->entityId);

		$id = SimpleSAML_Auth_State::saveState($state, 'saml:sp:sso');
		$ar->setRelayState($id);

		$useArtifact = $idpMetadata->getBoolean('saml1.useartifact', NULL);
		if ($useArtifact === NULL) {
			$useArtifact = $this->metadata->getBoolean('saml1.useartifact', FALSE);
		}

		if ($useArtifact) {
			$shire = SimpleSAML_Module::getModuleURL('saml/sp/saml1-acs.php/' . $this->authId . '/artifact');
		} else {
			$shire = SimpleSAML_Module::getModuleURL('saml/sp/saml1-acs.php/' . $this->authId);
		}

		$url = $ar->createRedirect($idpEntityId, $shire);

		SimpleSAML_Logger::debug('Starting SAML 1 SSO to ' . var_export($idpEntityId, TRUE) .
			' from ' . var_export($this->entityId, TRUE) . '.');
		SimpleSAML_Utilities::redirect($url);
	}


	/**
	 * Send a SAML2 SSO request to an IdP.
	 *
	 * @param SimpleSAML_Configuration $idpMetadata  The metadata of the IdP.
	 * @param array $state  The state array for the current authentication.
	 */
	private function startSSO2(SimpleSAML_Configuration $idpMetadata, array $state) {
	
		if (isset($state['saml:ProxyCount']) && $state['saml:ProxyCount'] < 0) {
			SimpleSAML_Auth_State::throwException($state, new SimpleSAML_Error_ProxyCountExceeded("ProxyCountExceeded"));
		}

		$ar = sspmod_saml_Message::buildAuthnRequest($this->metadata, $idpMetadata);

		$ar->setAssertionConsumerServiceURL(SimpleSAML_Module::getModuleURL('saml/sp/saml2-acs.php/' . $this->authId));

		if (isset($state['SimpleSAML_Auth_Default.ReturnURL'])) {
			$ar->setRelayState($state['SimpleSAML_Auth_Default.ReturnURL']);
		}

		if (isset($state['saml:AuthnContextClassRef'])) {
			$accr = SimpleSAML_Utilities::arrayize($state['saml:AuthnContextClassRef']);
			$ar->setRequestedAuthnContext(array('AuthnContextClassRef' => $accr));
		}

		if (isset($state['ForceAuthn'])) {
			$ar->setForceAuthn((bool)$state['ForceAuthn']);
		}

		if (isset($state['isPassive'])) {
			$ar->setIsPassive((bool)$state['isPassive']);
		}

		if (isset($state['saml:NameIDPolicy'])) {
			if (is_string($state['saml:NameIDPolicy'])) {
				$policy = array(
					'Format' => (string)$state['saml:NameIDPolicy'],
					'AllowCreate' => TRUE,
				);
			} elseif (is_array($state['saml:NameIDPolicy'])) {
				$policy = $state['saml:NameIDPolicy'];
			} else {
				throw new SimpleSAML_Error_Exception('Invalid value of $state[\'saml:NameIDPolicy\'].');
			}
			$ar->setNameIdPolicy($policy);
		}

		if (isset($state['saml:IDPList'])) {
			$IDPList = $state['saml:IDPList'];
		} else {
            $IDPList = array();
        }
		
		$ar->setIDPList(array_unique(array_merge($this->metadata->getArray('IDPList', array()), 
												$idpMetadata->getArray('IDPList', array()),
												(array) $IDPList)));
		
		if (isset($state['saml:ProxyCount']) && $state['saml:ProxyCount'] !== null) {
			$ar->setProxyCount($state['saml:ProxyCount']);
		} elseif ($idpMetadata->getInteger('ProxyCount', null) !== null) {
			$ar->setProxyCount($idpMetadata->getInteger('ProxyCount', null));
		} elseif ($this->metadata->getInteger('ProxyCount', null) !== null) {
			$ar->setProxyCount($this->metadata->getInteger('ProxyCount', null));
		}
		
		$requesterID = array();
		if (isset($state['saml:RequesterID'])) {
			$requesterID = $state['saml:RequesterID'];
		}
		
		if (isset($state['core:SP'])) {
			$requesterID[] = $state['core:SP'];
		}
		
		$ar->setRequesterID($requesterID);
		
		if (isset($state['saml:Extensions'])) {
			$ar->setExtensions($state['saml:Extensions']);
		}

		$id = SimpleSAML_Auth_State::saveState($state, 'saml:sp:sso', TRUE);
		$ar->setId($id);

		SimpleSAML_Logger::debug('Sending SAML 2 AuthnRequest to ' . var_export($idpMetadata->getString('entityid'), TRUE));
		$b = new SAML2_HTTPRedirect();
		$b->send($ar);

		assert('FALSE');
	}


	/**
	 * Send a SSO request to an IdP.
	 *
	 * @param string $idp  The entity ID of the IdP.
	 * @param array $state  The state array for the current authentication.
	 */
	public function startSSO($idp, array $state) {
		assert('is_string($idp)');

		$idpMetadata = $this->getIdPMetadata($idp);

		$type = $idpMetadata->getString('metadata-set');
		switch ($type) {
		case 'shib13-idp-remote':
			$this->startSSO1($idpMetadata, $state);
			assert('FALSE'); /* Should not return. */
		case 'saml20-idp-remote':
			$this->startSSO2($idpMetadata, $state);
			assert('FALSE'); /* Should not return. */
		default:
			/* Should only be one of the known types. */
			assert('FALSE');
		}
	}


	/**
	 * Start an IdP discovery service operation.
	 *
	 * @param array $state  The state array.
	 */
	private function startDisco(array $state) {

		$id = SimpleSAML_Auth_State::saveState($state, 'saml:sp:sso');

		$config = SimpleSAML_Configuration::getInstance();

		$discoURL = $this->discoURL;
		if ($discoURL === NULL) {
			/* Fallback to internal discovery service. */
			$discoURL = SimpleSAML_Module::getModuleURL('saml/disco.php');
		}

		$returnTo = SimpleSAML_Module::getModuleURL('saml/sp/discoresp.php', array('AuthID' => $id));
        
        $params = array(
            'entityID' => $this->entityId,
            'return' => $returnTo,
            'returnIDParam' => 'idpentityid'
        );
        
        if(isset($state['saml:IDPList'])) {
            $params['IDPList'] = $state['saml:IDPList'];
        }

		SimpleSAML_Utilities::redirect($discoURL, $params);
	}


	/**
	 * Start login.
	 *
	 * This function saves the information about the login, and redirects to the IdP.
	 *
	 * @param array &$state  Information about the current authentication.
	 */
	public function authenticate(&$state) {
		assert('is_array($state)');

		/* We are going to need the authId in order to retrieve this authentication source later. */
		$state['saml:sp:AuthId'] = $this->authId;

		$idp = $this->idp;

		if (isset($state['saml:idp'])) {
			$idp = (string)$state['saml:idp'];
		}

		if ($idp === NULL && isset($state['saml:IDPList']) && sizeof($state['saml:IDPList']) == 1) {
			$idp = $state['saml:IDPList'][0];
		}

		if ($idp === NULL) {
			$this->startDisco($state);
			assert('FALSE');
		}

		$this->startSSO($idp, $state);
		assert('FALSE');
	}


	/**
	 * Start a SAML 2 logout operation.
	 *
	 * @param array $state  The logout state.
	 */
	public function startSLO2(&$state) {
		assert('is_array($state)');
		assert('array_key_exists("saml:logout:IdP", $state)');
		assert('array_key_exists("saml:logout:NameID", $state)');
		assert('array_key_exists("saml:logout:SessionIndex", $state)');

		$id = SimpleSAML_Auth_State::saveState($state, 'saml:slosent');

		$idp = $state['saml:logout:IdP'];
		$nameId = $state['saml:logout:NameID'];
		$sessionIndex = $state['saml:logout:SessionIndex'];

		$idpMetadata = $this->getIdPMetadata($idp);

		$endpoint = $idpMetadata->getDefaultEndpoint('SingleLogoutService', array(SAML2_Const::BINDING_HTTP_REDIRECT), FALSE);
		if ($endpoint === FALSE) {
			SimpleSAML_Logger::info('No logout endpoint for IdP ' . var_export($idp, TRUE) . '.');
			return;
		}

		$lr = sspmod_saml_Message::buildLogoutRequest($this->metadata, $idpMetadata);
		$lr->setNameId($nameId);
		$lr->setSessionIndex($sessionIndex);
		$lr->setRelayState($id);

		$encryptNameId = $idpMetadata->getBoolean('nameid.encryption', NULL);
		if ($encryptNameId === NULL) {
			$encryptNameId = $this->metadata->getBoolean('nameid.encryption', FALSE);
		}
		if ($encryptNameId) {
			$lr->encryptNameId(sspmod_saml_Message::getEncryptionKey($idpMetadata));
		}

		$b = new SAML2_HTTPRedirect();
		$b->send($lr);

		assert('FALSE');
	}


	/**
	 * Start logout operation.
	 *
	 * @param array $state  The logout state.
	 */
	public function logout(&$state) {
		assert('is_array($state)');
		assert('array_key_exists("saml:logout:Type", $state)');

		$logoutType = $state['saml:logout:Type'];
		switch ($logoutType) {
		case 'saml1':
			/* Nothing to do. */
			return;
		case 'saml2':
			$this->startSLO2($state);
			return;
		default:
			/* Should never happen. */
			assert('FALSE');
		}
	}


	/**
	 * Handle a response from a SSO operation.
	 *
	 * @param array $state  The authentication state.
	 * @param string $idp  The entity id of the IdP.
	 * @param array $attributes  The attributes.
	 */
	public function handleResponse(array $state, $idp, array $attributes) {
		assert('is_string($idp)');
		assert('array_key_exists("LogoutState", $state)');
		assert('array_key_exists("saml:logout:Type", $state["LogoutState"])');
		
		$idpMetadata = $this->getIdpMetadata($idp);

		$spMetadataArray = $this->metadata->toArray();
		$idpMetadataArray = $idpMetadata->toArray();

		$authProcState = array(
			'saml:sp:IdP' => $idp,
			'saml:sp:State' => $state,
			'ReturnCall' => array('sspmod_saml_Auth_Source_ContrailSP', 'onProcessingCompleted'),

			'Attributes' => $attributes,
			'Destination' => $spMetadataArray,
			'Source' => $idpMetadataArray,
		);

		if (isset($state['saml:sp:NameID'])) {
			$authProcState['saml:sp:NameID'] = $state['saml:sp:NameID'];
		}
		if (isset($state['saml:sp:SessionIndex'])) {
			$authProcState['saml:sp:SessionIndex'] = $state['saml:sp:SessionIndex'];
		}

		$pc = new SimpleSAML_Auth_ProcessingChain($idpMetadataArray, $spMetadataArray, 'sp');
		$pc->processState($authProcState);

		self::onProcessingCompleted($authProcState);
	}


	/**
	 * Handle a logout request from an IdP.
	 *
	 * @param string $idpEntityId  The entity ID of the IdP.
	 */
	public function handleLogout($idpEntityId) {
		assert('is_string($idpEntityId)');

		/* Call the logout callback we registered in onProcessingCompleted(). */
		$this->callLogoutCallback($idpEntityId);
	}

	/** 
	* Method: POST, PUT, GET etc
	* Data: array("param" => "value") ==> index.php?param=value
	**/

	private function callAPI($method, $url, $header, $data = false)
	{
	    $curl = curl_init();
	    curl_setopt($curl, CURLOPT_HEADER, true);
	    curl_setopt($curl, CURLOPT_RETURNTRANSFER, true);
	    curl_setopt($curl, CURLOPT_HTTPHEADER, $header);
	    switch ($method)
	    {
		case "POST":
		    curl_setopt($curl, CURLOPT_POST, 1);

		    if ($data)
		        curl_setopt($curl, CURLOPT_POSTFIELDS, $data);
		    break;
		case "PUT":
		    curl_setopt($curl, CURLOPT_PUT, 1);
		    break;
		default:
		    if ($data)
		        $url = sprintf("%s?%s", $url, http_build_query($data));
	    }

	    curl_setopt($curl, CURLOPT_URL, $url);
	    curl_setopt($curl, CURLOPT_RETURNTRANSFER, 1);
	    $resulta = curl_exec($curl);
	    if (curl_errno($curl)) {
		print curl_error($curl);
	    }
		$start = stripos($resulta, "{");
		$end = stripos($resulta, "}");
	
		$startLB = stripos($resulta, "[");
		$endRB = stripos($resulta, "]");
		if( $startLB == false ){
			// We can not find left bracket (LB)
			$body = substr($resulta,$start,$end-$start+1);
		}
	     	elseif ( $startLB < $start ){ 
			// If we have LB, start must be less than first {
			$body = substr($resulta,$startLB,$endRB-$startLB+1);
		}
		else{
			// If not, do the ordinary parsing
			$body = substr($resulta,$start,$end-$start+1);
		}

		$http_status = curl_getinfo($curl, CURLINFO_HTTP_CODE);
		curl_close($curl);
		$result = array('result' => $resulta, "code" => $http_status, "body" => $body);

	    return $result;
	}

	private function getUserRoleId($server){
		$header=array('Accept: application/json', 'Content-Type: application/json');
		$ret_data=$this->callAPI("GET", $server."/roles", $header);
		$decoded = json_decode($ret_data["body"]);
		foreach($decoded as $role){
			if($role->name == "FederationUser"){
				$start = strrpos($role->uri, "/", -1);
				return substr($role->uri, $start+1);
			}
		}
		return $decoded;
	}


	private function getRoles($server, $uuid){
		$header=array('Accept: application/json', 'Content-Type: application/json');
		$ret_data=$this->callAPI("GET", $server."/users/".$uuid."/roles", $header);
		$decoded = json_decode($ret_data["body"]);
		return $decoded;
	}


	private function setRoleToUser($server, $uuid, $roleId){
		$data=json_encode(array('roleId'=> "roles/".$roleId));
		$header=array('Accept: application/json', 'Content-Type: application/json');
		$ret_data=$this->callAPI("POST", $server."/users/".$uuid."/roles", $header, $data);
		return $ret_data;
	}

	private function getUser($server, $email){
		$data=json_encode(array('user:email' => $email));
		$header=array('Accept: application/json', 'Content-Type: application/json');
		$ret_data=$this->callAPI("POST", $server."/usersutils/filter", $header, $data);
		$decoded = json_decode($ret_data["body"]);
		return $decoded;
	}

	/**
	* This fetches user's external ID if present
	*/
	private function getUserExtId($server, $id){
		$data=json_encode(array('user:idp:identity' => $id));
		$header=array('Accept: application/json', 'Content-Type: application/json');
		$ret_data=$this->callAPI("POST", $server."/usersutils/filter", $header, $data);
		$decoded = json_decode($ret_data["body"]);
		return $decoded;
	}

	private	function getUserDetails($server, $uuid){
		$header=array('Accept: application/json', 'Content-Type: application/json');
		$ret_data=$this->callAPI("GET", $server."/users/".$uuid, $header);
		$decoded = json_decode($ret_data["body"]);
		return $decoded;
	}

	private function getUserAttributes($server, $uuid){
		$header=array('Accept: application/json', 'Content-Type: application/json');
		$ret_data=$this->callAPI("GET", $server."/users/".$uuid."/attributes", $header);
		$decoded = json_decode($ret_data["body"]);
		return $decoded;
	}


	private function getGroups($server, $uuid){
		$header=array('Accept: application/json', 'Content-Type: application/json');
		$ret_data=$this->callAPI("GET", $server."/users/".$uuid."/groups", $header);
		$decoded = json_decode($ret_data["body"]);
		return $decoded;
	}

	private function getIdPLoA($server, $idp_name){
		$header=array('Accept: application/json', 'Content-Type: application/json');
		$ret_data=$this->callAPI("GET", $server."/idps", $header);
		$decoded = json_decode($ret_data["body"]);
		$idp_uuid = "";
		$value = "";
		foreach($decoded as $idp){
			if($idp->name == $idp_name){
				$start = strrpos($idp->uri, "/", -1);
				$idp_uuid = substr($idp->uri, $start+1);
			}
		}
		$ret_data=$this->callAPI("GET", $server."/idps/".$idp_uuid."/attributes", $header);
		$decoded = json_decode($ret_data["body"]);
		foreach($decoded as $attribute){
			if($attribute->name == "urn:contrail:names:federation:subject:current-loa"){
				$value = $attribute->value;
			}
		}
		return $value;
	}

	/**
	* Method creates a user, fetches its uuid and sets FederationUser role for the user.
	*/
	private function createUser($server, $username, $password, $email, $fname, $lname ){
		$data=json_encode(array('username'=> $username, 'email' => $email, 'password' => $password, 'firstName' => $fname, 'lastName' => $lname));
		$header=array('Accept: application/json', 'Content-Type: application/json');
		$ret_data=$this->callAPI("POST", $server."/users", $header, $data);
		return $ret_data;
	}

	// We need php5-curl
	private function authenticateUser($server, $username, $password)
	{
		$data=json_encode(array('username' => $username, 'password' => $password));
		$header=array('Accept: application/json', 'Content-Type: application/json');
		$ret_data=$this->callAPI("POST", $server."/usersutils/authenticate", $header, $data);
		$decoded = json_decode($ret_data["body"]);
		return $decoded;
		return strcmp ( $username ,  $decoded->username );
	}

	/**
	 * Returns Random String of custom length.
	 */ 
	private function generateRandomString($length = 8) {
	    $chars = 'abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789';
	    $count = mb_strlen($chars);

	    for ($i = 0, $result = ''; $i < $length; $i++) {
		$index = rand(0, $count - 1);
		$result .= mb_substr($chars, $index, 1);
	    }

	    return $result;
	}

	/* 
	* Contrail's extension to read SAML attributes.
	*/
	public function contrailRESTExtSAML($attributes){
		$server_uri = $this->dsn;
		SimpleSAML_Logger::debug('User attributes from SAML:' . var_export($attributes, TRUE));
		$user_saml_email = NULL;
		if(array_key_exists('email',$attributes)){
			$user_saml_email = $attributes['email'][0];
			SimpleSAML_Logger::debug('Extracted user email:' . var_export($user_saml_email, TRUE));
		}
		if ( $user_saml_email == NULL){
			SimpleSAML_Logger::debug('Users email is NULL, using default');
			$user_saml_email = $this->generateRandomString(16) . '@contrail.eu';
			SimpleSAML_Logger::debug('Users email is ' . $user_saml_email );
		}
		SimpleSAML_Logger::debug('Server URI:' . var_export($server_uri, TRUE));	

		$ret=$this->getUser($server_uri, $user_saml_email);
		SimpleSAML_Logger::debug('ret from getUser: ' . var_export($ret, TRUE));
		if($ret == NULL){
			$ret=$this->getUserExtId($server_uri, $user_saml_email);
			SimpleSAML_Logger::debug('ret from getUserExtId: ' . var_export($ret, TRUE));
			if($ret == NULL){
				// Create the profile
				$password = $this->generateRandomString(16);
				$rnd_name = $this->generateRandomString(4);
				$this->createUser($server_uri, $user_saml_email, $password, $user_saml_email,'User'.$rnd_name, 'Surname');
				$fedUserRoleId = $this->getUserRoleId ( $server_uri );
				$ret = $this->getUser( $server_uri, $user_saml_email );
				$this->setRoleToUser($server_uri, $ret->uuid, $fedUserRoleId);
			}
		}
		$user_details = $this->getUserDetails( $server_uri, $ret->uuid );
		$ret_roles = $this->getRoles( $server_uri, $ret->uuid );
		$roles_arr=array();
		foreach($ret_roles as $role){
			array_push($roles_arr, $role->name);
		}
		SimpleSAML_Logger::debug('User roles:' . var_export($roles_arr, TRUE));
		$ret_groups = $this->getGroups( $server_uri, $ret->uuid );
		$groups_arr=array();		
		if ($ret_groups != NULL){
			foreach($ret_groups as $group){
		        array_push($groups_arr, $group->name);
			}
		}
		else{
			SimpleSAML_Logger::debug('groups_arr: NULL');
		}
		SimpleSAML_Logger::debug('User groups:' . var_export($groups_arr, TRUE));
		$ret_attributes = $this->getUserAttributes( $server_uri, $ret->uuid );
		$attr_arr=array();
		if ($ret_attributes != NULL){
			foreach($ret_attributes as $attribute){
					array_push($attr_arr, $attribute);
			}
		}
		else{
			SimpleSAML_Logger::debug('ret_attributes: NULL');
		}
		SimpleSAML_Logger::debug('User attributes:' . var_export($attr_arr, TRUE));
		$ret_user = $this->getUserDetails( $server_uri, $ret->uuid );
		$current_loa = $this->getIdPLoA($server_uri, $this->idp_name);
		/* Construct an array about info on attributes. */
		$attributes = array(
		    	'uid' => array($ret->username),
		    	'displayName' => array($user_details->firstName, $user_details->lastName),
		   	'roles' => array_values($roles_arr),
		   	'groups' => array_values($groups_arr),
			'contrailUser' => array( true ),
			'uuid' => array($ret->uuid),
			'current_loa' => array($current_loa),
		);
		SimpleSAML_Logger::debug('Complete returned attributes:' . var_export($attributes, TRUE));
		return $attributes;		
	}

	/**
	 * Called when we have completed the procssing chain.
	 *
	 * @param array $authProcState  The processing chain state.
	 */
	public static function onProcessingCompleted(array $authProcState) {
		assert('array_key_exists("saml:sp:IdP", $authProcState)');
		assert('array_key_exists("saml:sp:State", $authProcState)');
		assert('array_key_exists("Attributes", $authProcState)');

		$idp = $authProcState['saml:sp:IdP'];
		$state = $authProcState['saml:sp:State'];

		$sourceId = $state['saml:sp:AuthId'];
		$source = SimpleSAML_Auth_Source::getById($sourceId);
		if ($source === NULL) {
			throw new Exception('Could not find authentication source with id ' . $sourceId);
		}

		/* Register a callback that we can call if we receive a logout request from the IdP. */
		$source->addLogoutCallback($idp, $state);
		SimpleSAML_Logger::debug('SAML SP Returned Attributes: '. implode(", ",array_keys($authProcState['Attributes'])));
		//$state['Attributes'] = $authProcState['Attributes'];
		$attributes = $authProcState['Attributes'];
		SimpleSAML_Logger::debug('Attributes before contrail mapping: '. var_export($attributes, TRUE));
		try {
			$state['Attributes'] = $source->contrailRESTExtSAML($attributes);
			SimpleSAML_Logger::debug('Attributes returned to the SP: '. var_export($state['Attributes'], TRUE));
		}catch (Exception $e){
		    	SimpleSAML_Logger::warning('Contrail SAML SP: Something went weird during the authentication ');
		    	throw new SimpleSAML_Error_Error('WRONGUSERPASS');
		}

		if (isset($state['saml:sp:isUnsolicited']) && (bool)$state['saml:sp:isUnsolicited']) {
			if (!empty($state['saml:sp:RelayState'])) {
				$redirectTo = $state['saml:sp:RelayState'];
			} else {
				$redirectTo = $source->getMetadata()->getString('RelayState', '/');
			}
			SimpleSAML_Auth_Default::handleUnsolicitedAuth($sourceId, $state, $redirectTo);
		}

		SimpleSAML_Auth_Source::completeAuth($state);
	}

}
