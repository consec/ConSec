<?php

/*
 * Disable strict error reporting, since the OpenID library
 * used is PHP4-compatible, and not PHP5 strict-standards compatible.
 */
SimpleSAML_Utilities::maskErrors(E_STRICT);
if (defined('E_DEPRECATED')) {
	/* PHP 5.3 also has E_DEPRECATED. */
	SimpleSAML_Utilities::maskErrors(constant('E_DEPRECATED'));
}

/* Add the OpenID library search path. */
set_include_path(get_include_path() . PATH_SEPARATOR . dirname(dirname(dirname(dirname(dirname(dirname(__FILE__)))))) . '/lib');

require_once('Auth/OpenID/AX.php');
require_once('Auth/OpenID/SReg.php');
require_once('Auth/OpenID/Server.php');
require_once('Auth/OpenID/ServerRequest.php');


/**
 * Authentication module which acts as an OpenID Consumer
 *
 * @author Andreas Åkre Solberg, <andreas.solberg@uninett.no>, UNINETT AS., ales.cernivec@xlab.si, XLAB d.o.o.
 * @package simpleSAMLphp
 * @version $Id$
 */
class sspmod_openid_Auth_Source_OpenIDContrailConsumer extends SimpleSAML_Auth_Source {
	
	private $idp_name;
	
	/* The database DSN.
	* See the documentation for the various database drivers for information about the syntax:
	*     http://www.php.net/manual/en/pdo.drivers.php
	*/
	private $dsn;

	/**
	 * Static openid target to use.
	 *
	 * @var string|NULL
	 */
	private $target;

	/**
	 * Custom realm to use.
	 *
	 * @var string|NULL
	 */
	private $realm;

	/**
	 * List of optional attributes.
	 */
	private $optionalAttributes;
	private $optionalAXAttributes;


	/**
	 * List of required attributes.
	 */
	private $requiredAttributes;
	private $requiredAXAttributes;

	/**
	 * Validate SReg responses.
	 */
	private $validateSReg;

	/**
	 * List of custom extension args
	 */
	private $extensionArgs;

	/**
	 * Prefer HTTP Redirect over HTML Form Redirection (POST)
	 */
	private $preferHttpRedirect;

	/**
	 * Constructor for this authentication source.
	 *
	 * @param array $info  Information about this authentication source.
	 * @param array $config  Configuration.
	 */
	public function __construct($info, $config) {

		/* Call the parent constructor first, as required by the interface. */
		parent::__construct($info, $config);

		$cfgParse = SimpleSAML_Configuration::loadFromArray($config,
			'Authentication source ' . var_export($this->authId, TRUE));

		$this->target = $cfgParse->getString('target', NULL);
		$this->realm = $cfgParse->getString('realm', NULL);

		$this->optionalAttributes = $cfgParse->getArray('attributes.optional', array());
		$this->requiredAttributes = $cfgParse->getArray('attributes.required', array());

		$this->optionalAXAttributes = $cfgParse->getArray('attributes.ax_optional', array());
		$this->requiredAXAttributes = $cfgParse->getArray('attributes.ax_required', array());

		$this->validateSReg = $cfgParse->getBoolean('sreg.validate',TRUE);

		$this->extensionArgs = $cfgParse->getArray('extension.args', array());

		$this->preferHttpRedirect = $cfgParse->getBoolean('prefer_http_redirect', FALSE);

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
	 * Initiate authentication. Redirecting the user to the consumer endpoint 
	 * with a state Auth ID.
	 *
	 * @param array &$state  Information about the current authentication.
	 */
	public function authenticate(&$state) {
		assert('is_array($state)');

		$state['openid:AuthId'] = $this->authId;

		if ($this->target !== NULL) {
			/* We know our OpenID target URL. Skip the page where we ask for it. */
			$this->doAuth($state, $this->target);

			/* doAuth() never returns. */
			assert('FALSE');
		}

		$id = SimpleSAML_Auth_State::saveState($state, 'openid:init');

		$url = SimpleSAML_Module::getModuleURL('openid/consumer.php');
		SimpleSAML_Utilities::redirect($url, array('AuthState' => $id));
	}


	/**
	 * Retrieve the Auth_OpenID_Consumer instance.
	 *
	 * @param array &$state  The state array we are currently working with.
	 * @return Auth_OpenID_Consumer  The Auth_OpenID_Consumer instance.
	 */
	private function getConsumer(array &$state) {
		$store = new sspmod_openid_StateStore($state);
		$session = new sspmod_openid_SessionStore();
		return new Auth_OpenID_Consumer($store, $session);
	}


	/**
	 * Retrieve the URL we should return to after successful authentication.
	 *
	 * @return string  The URL we should return to after successful authentication.
	 */
	private function getReturnTo($stateId) {
		assert('is_string($stateId)');

		return SimpleSAML_Module::getModuleURL('openid/linkback.php', array(
			'AuthState' => $stateId,
		));
	}


	/**
	 * Retrieve the trust root for this openid site.
	 *
	 * @return string  The trust root.
	 */
	private function getTrustRoot() {
		if (!empty($this->realm)) {
			return $this->realm;
		} else {
			return SimpleSAML_Utilities::selfURLhost();
		}
	}


	/**
	 * Send an authentication request to the OpenID provider.
	 *
	 * @param array &$state  The state array.
	 * @param string $openid  The OpenID we should try to authenticate with.
	 */
	public function doAuth(array &$state, $openid) {
		assert('is_string($openid)');

		$stateId = SimpleSAML_Auth_State::saveState($state, 'openid:auth');

		$consumer = $this->getConsumer($state);

		// Begin the OpenID authentication process.
		$auth_request = $consumer->begin($openid);

		// No auth request means we can't begin OpenID.
		if (!$auth_request) {
			throw new SimpleSAML_Error_BadRequest('Not a valid OpenID: ' . var_export($openid, TRUE));
		}

		$sreg_request = Auth_OpenID_SRegRequest::build(
			$this->requiredAttributes,
			$this->optionalAttributes
		);

		if ($sreg_request) {
			$auth_request->addExtension($sreg_request);
		}

		// Create attribute request object
		$ax_attribute = array();

		foreach($this->requiredAXAttributes as $attr) {
			$ax_attribute[] = Auth_OpenID_AX_AttrInfo::make($attr,1,true);
		}

		foreach($this->optionalAXAttributes as $attr) {
			$ax_attribute[] = Auth_OpenID_AX_AttrInfo::make($attr,1,false);
		}

		if (count($ax_attribute) > 0) {

			// Create AX fetch request
			$ax_request = new Auth_OpenID_AX_FetchRequest;

			// Add attributes to AX fetch request
			foreach($ax_attribute as $attr){
				$ax_request->add($attr);
			}

			// Add AX fetch request to authentication request
			$auth_request->addExtension($ax_request);

		}

		foreach($this->extensionArgs as $ext_ns => $ext_arg) {
			if (is_array($ext_arg)) {
				foreach($ext_arg as $ext_key => $ext_value) {
					$auth_request->addExtensionArg($ext_ns, $ext_key, $ext_value);
				}
			}
		}

		// Redirect the user to the OpenID server for authentication.
		// Store the token for this authentication so we can verify the
		// response.

		// For OpenID 1, send a redirect.  For OpenID 2, use a Javascript form
		// to send a POST request to the server or use redirect if
		// prefer_http_redirect is enabled and redirect URL size
		// is less than 2049
		$should_send_redirect = $auth_request->shouldSendRedirect();
		if ($this->preferHttpRedirect || $should_send_redirect) {
			$redirect_url = $auth_request->redirectURL($this->getTrustRoot(), $this->getReturnTo($stateId));

			// If the redirect URL can't be built, display an error message.
			if (Auth_OpenID::isFailure($redirect_url)) {
				throw new SimpleSAML_Error_AuthSource($this->authId, 'Could not redirect to server: ' . var_export($redirect_url->message, TRUE));
			}

			// For OpenID 2 failover to POST if redirect URL is longer than 2048
			if ($should_send_redirect || strlen($redirect_url) <= 2048) {
				SimpleSAML_Utilities::redirect($redirect_url);
				assert('FALSE');
			}
		}

		// Generate form markup and render it.
		$form_id = 'openid_message';
		$form_html = $auth_request->formMarkup($this->getTrustRoot(), $this->getReturnTo($stateId), FALSE, array('id' => $form_id));

		// Display an error if the form markup couldn't be generated; otherwise, render the HTML.
		if (Auth_OpenID::isFailure($form_html)) {
			throw new SimpleSAML_Error_AuthSource($this->authId, 'Could not redirect to server: ' . var_export($form_html->message, TRUE));
		} else {
			echo '<html><head><title>OpenID transaction in progress</title></head>
				<body onload=\'document.getElementById("' . $form_id . '").submit()\'>' .
				$form_html . '</body></html>';
			exit;
		}
	}

	private function generatePassword($length = 8) {
	    $chars = 'abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789';
	    $count = mb_strlen($chars);

	    for ($i = 0, $result = ''; $i < $length; $i++) {
		$index = rand(0, $count - 1);
		$result .= mb_substr($chars, $index, 1);
	    }

	    return $result;
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

	    // Optional Authentication:
	    //curl_setopt($curl, CURLOPT_HTTPAUTH, CURLAUTH_BASIC);
	    //curl_setopt($curl, CURLOPT_USERPWD, "username:password");
	    SimpleSAML_Logger::debug('callAPI: method ' . $method . ', url ' . $url . ', header ' . var_export($header, TRUE) . ', data ' . var_export($data, TRUE));
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

	/**
	* Method creates a user, fetches its uuid and sets FederationUser role for the user.
	*/
	private function createUser($server, $username, $password, $email, $fname, $lname ){
		#$password = generatePassword(16);
		$data=json_encode(array('username'=> $username, 'email' => $email, 'password' => $password, 'firstName' => $fname, 'lastName' => $lname));
		$header=array('Accept: application/json', 'Content-Type: application/json');
		$ret_data=$this->callAPI("POST", $server."/users", $header, $data);
		#$fedUserRoleId = getUserRoleId ( $server );
		#$user = getUser( $server, $email );
		#setRoleToUser($server, $user->uuid, $fedUserRoleId);
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

	/* 
	* Contrail's extension to read OpenID attributes.
	*/
	public function contrailRESTExtOpenID($attributes){
		$server_uri = $this->dsn;
		SimpleSAML_Logger::debug('User attributes from openID:' . var_export($attributes, TRUE));
		$user_openid_email = $attributes['http://axschema.org/contact/email'][0];
		SimpleSAML_Logger::debug('Extracted user email:' . var_export($user_openid_email, TRUE));
		
		SimpleSAML_Logger::debug('Server URI:' . var_export($server_uri, TRUE));	

		$ret=$this->getUser($server_uri, $user_openid_email);
		SimpleSAML_Logger::debug('ret from getUser: ' . var_export($ret, TRUE));
		if($ret == NULL){
			$ret=$this->getUserExtId($server_uri, $user_openid_email);
			SimpleSAML_Logger::debug('ret from getUserExtId: ' . var_export($ret, TRUE));
			if($ret == NULL){
				// Create the profile
				$password = $this->generatePassword(16);
				$this->createUser($server_uri, $user_openid_email, $password, $user_openid_email,$attributes['http://axschema.org/namePerson/first'][0], $attributes['http://axschema.org/namePerson/last'][0]);
				$fedUserRoleId = $this->getUserRoleId ( $server_uri );
				$ret = $this->getUser( $server_uri, $user_openid_email );
				$this->setRoleToUser($server_uri, $ret->uuid, $fedUserRoleId);

				//$attributes = array(
			    	//'uid' => array($user_openid_email),
			    	//'displayName' => array($attributes['http://axschema.org/namePerson/first'][0], $attributes['http://axschema.org/namePerson/last'][0]),
				//'contrailUser' => array( false ),
				////'create_new_profile'  => array( true ),
				//);
				//return $attributes;
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
	 * Process an authentication response.
	 *
	 * @param array &$state  The state array.
	 */
	public function postAuth(array &$state) {

		$consumer = $this->getConsumer($state);

		$return_to = SimpleSAML_Utilities::selfURL();

		// Complete the authentication process using the server's
		// response.
		$response = $consumer->complete($return_to);

		// Check the response status.
		if ($response->status == Auth_OpenID_CANCEL) {
			// This means the authentication was cancelled.
			throw new SimpleSAML_Error_UserAborted();
		} else if ($response->status == Auth_OpenID_FAILURE) {
			// Authentication failed; display the error message.
			throw new SimpleSAML_Error_AuthSource($this->authId, 'Authentication failed: ' . var_export($response->message, TRUE));
		} else if ($response->status != Auth_OpenID_SUCCESS) {
			throw new SimpleSAML_Error_AuthSource($this->authId, 'General error. Try again.');
		}

		// This means the authentication succeeded; extract the
		// identity URL and Simple Registration data (if it was
		// returned).
		$openid = $response->identity_url;

		$attributes = array('openid' => array($openid));
		$attributes['openid.server_url'] = array($response->endpoint->server_url);

		if ($response->endpoint->canonicalID) {
			$attributes['openid.canonicalID'] = array($response->endpoint->canonicalID);
		}

		if ($response->endpoint->local_id) {
				$attributes['openid.local_id'] = array($response->endpoint->local_id);
		}

		$sreg_resp = Auth_OpenID_SRegResponse::fromSuccessResponse($response, $this->validateSReg);
		$sregresponse = $sreg_resp->contents();

		if (is_array($sregresponse) && count($sregresponse) > 0) {
			$attributes['openid.sregkeys'] = array_keys($sregresponse);
			foreach ($sregresponse AS $sregkey => $sregvalue) {
				$attributes['openid.sreg.' . $sregkey] = array($sregvalue);
			}
		}

		// Get AX response information
		$ax = new Auth_OpenID_AX_FetchResponse();
		$ax_resp = $ax->fromSuccessResponse($response);

		if (($ax_resp instanceof Auth_OpenID_AX_FetchResponse) && (!empty($ax_resp->data))) {
			$axresponse = $ax_resp->data;

			$attributes['openid.axkeys'] = array_keys($axresponse);
			foreach ($axresponse AS $axkey => $axvalue) {
				if (preg_match("/^\w+:/",$axkey)) {
					$attributes[$axkey] = (is_array($axvalue)) ? $axvalue : array($axvalue);
				} else {
					SimpleSAML_Logger::warning('Invalid attribute name in AX response: ' . var_export($axkey, TRUE));
				}
			}
		}

		SimpleSAML_Logger::debug('OpenID Returned Attributes: '. implode(", ",array_keys($attributes)));

//		$this->contrailExtOpenID($attributes);
//		$state['Attributes'] = $attributes;
		try {
			$state['Attributes'] = $this->contrailRESTExtOpenID($attributes);
			SimpleSAML_Logger::debug('Attributes returned to the SP: '. var_export($state['Attributes'], TRUE));
		}catch (Exception $e){
		    	SimpleSAML_Logger::warning('Contrail Google OpenID: Something went weird during the authentication ');
		    	throw new SimpleSAML_Error_Error('WRONGUSERPASS');
		}
		SimpleSAML_Auth_Source::completeAuth($state);
	}

}
