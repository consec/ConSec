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

	
    /* The database DSN.
     * See the documentation for the various database drivers for information about the syntax:
     *     http://www.php.net/manual/en/pdo.drivers.php
     */
    private $dsn;

    /* The database username & password. */
    private $username;
    private $password;



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
	        if (!is_string($config['username'])) {
	            throw new Exception('Missing or invalid username option in config.');
	        }
	        $this->username = $config['username'];
	        if (!is_string($config['password'])) {
	            throw new Exception('Missing or invalid password option in config.');
	        }
	        $this->password = $config['password'];
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

	/* 
	* Contrail's extension to read OpenID attributes.
	*/
	public function contrailExtOpenID($attributes){

		SimpleSAML_Logger::debug('User attributes from openID:' . var_export($attributes, TRUE));
		$user_openid_email = $attributes['http://axschema.org/contact/email'][0];
		SimpleSAML_Logger::debug('Extracted user email:' . var_export($user_openid_email, TRUE));
		
		//$userIdentity = 
		$dsn = 'mysql:host=localhost;port=3306;dbname=contrail';

 	        // Connect to the database. 
	        $db = new PDO($this->dsn, $this->username, $this->password);
	        $db->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);

	        // Ensure that we are operating with UTF-8 encoding.
       		// This command is for MySQL. Other databases may need different commands.
	        
	        $db->exec("SET NAMES 'utf8'");

	        // With PDO we use prepared statements. This saves us from having to escape
	        // the username in the database query.	         
	        $user_data = $db->prepare('SELECT u.userId, u.username, u.firstName, u.lastName FROM User u, User_has_identityProvider uidp WHERE uidp.identity=:identity AND uidp.userId=u.userId');

		if (!$user_data->execute(array('identity' => $user_openid_email))) {
	            throw new Exception('Failed to query database for user.');
	        }
		
		$row_user_data = $user_data->fetchAll();
		if(!$row_user_data){
		    throw new Exception('Failed to query database for user.');
		}

		// Extract roles
		$roles = $db->prepare('SELECT ur.name FROM URole ur, User u, User_has_Role uhr WHERE u.username=:username AND uhr.userId=u.userId AND ur.roleId=uhr.roleId');
	
		if (!$roles->execute(array('username' => $row_user_data[0]['username']))) {
			throw new Exception('Failed to query database for user roles.');
		}		
		$row_roles = $roles->fetchAll();
		$roles_arr=array();
		if (!$row_roles) {
			// Problems
			SimpleSAML_Logger::warning('Could not fetch user roles. Possibly no info about roles. ');
		}
		else{
			SimpleSAML_Logger::debug('User roles:' . var_export($row_roles, TRUE));
			$count = count($row_roles);
			for ($i = 0; $i < $count; $i++) {
				$roles_arr[$i]=$row_roles[$i]['name'];
			}
		}
		// \ Extract roles

		// Groups
		$groups = $db->prepare('SELECT ug.name FROM UGroup ug, User u, User_has_Group uhg WHERE u.username=:username AND uhg.userId=u.userId AND ug.groupId=uhg.groupId');
		if (!$groups->execute(array('username' => $row_user_data[0]['username']))) {
			throw new Exception('Failed to query database for user groups.');
		}		
		$row_groups = $groups->fetchAll();
		$groups_arr=array();
		if (!$row_groups) {
		      	// Problems 
			SimpleSAML_Logger::warning('Could not fetch user groups. Possibly no info about groups.');
		}
		else{
			SimpleSAML_Logger::debug('User groups:' . var_export($row_groups, TRUE));
	       		$count = count($row_groups);
			for ($i = 0; $i < $count; $i++) {
				$groups_arr[$i]=$row_groups[$i]['name'];
			}
		}
		// \ Groups
		
		// Ext attributes
		$con_attrs = $db->prepare('SELECT attr.name, uha.value FROM Attribute attr, User u, User_has_Attribute uha WHERE u.username=:username AND uha.userId=u.userId AND attr.attributeId=uha.attributeId');
		if (!$con_attrs->execute(array('username' => $row_user_data[0]['username']))) {
			throw new Exception('Failed to query database for user contrail attributes.');
		}

		// Retrieve the rows from the database.
		$row_con_attrs = $con_attrs->fetchAll();
		if (!$row_con_attrs) {
			// Problems
		      	SimpleSAML_Logger::warning('User does not have extended contrail attributes.');
		}
		else{
			SimpleSAML_Logger::debug('User contrail attributes:' . var_export($row_con_attrs, TRUE));
			$count = count($row_con_attrs);
			$con_attrs_arr=array();
			for ($i = 0; $i < $count; $i++) {
				$con_attrs_arr[$row_con_attrs[$i]['name']]=$row_con_attrs[$i]['value'];
			}
			// Create the attribute array of the user.
			$attrs_keys=array_keys($con_attrs_arr);
			$attrs_values=array_values($con_attrs_arr);
			for($i=0;$i<count($con_attrs_arr);$i++){
				$attributes[$attrs_keys[$i]]=array($attrs_values[$i]);
			}
		}

		// \ Ext attributes
		$user_data_array=array();
		if (!$row_user_data) {
			// Problems
			SimpleSAML_Logger::warning('Could not fetch user data. Possibly no info about user data? ');
		}
		else{
			SimpleSAML_Logger::debug('User data:' . var_export($row_user_data, TRUE));

			$user_attributes = array(
				   'uid' => array($row_user_data[0]['username']),
				   'displayName' => array($row_user_data[0]['firstName'], $row_user_data[0]['lastName']),
				   'roles' => array_values($roles_arr),
				   'groups' => array_values($groups_arr),
				);
			$attributes=array_merge($attributes, $user_attributes);
			SimpleSAML_Logger::debug('User data:' . var_export($user_attributes, TRUE));
		}

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
		$state['Attributes'] = $this->contrailExtOpenID($attributes);
		SimpleSAML_Auth_Source::completeAuth($state);
	}

}
