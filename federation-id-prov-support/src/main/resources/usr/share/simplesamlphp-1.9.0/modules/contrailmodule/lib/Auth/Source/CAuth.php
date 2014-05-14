<?php
class sspmod_contrailmodule_Auth_Source_CAuth extends sspmod_core_Auth_UserPassBase {

    /* The database DSN.
     * See the documentation for the various database drivers for information about the syntax:
     *     http://www.php.net/manual/en/pdo.drivers.php
     */
    private $dsn;

	private $idp_name;

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
		$ret_data=$this->callAPI("POST", $server."/users/filter", $header, $data);
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
	private function createUser($server, $username, $email, $fname, $lname ){
		$password = generatePassword(16);
		$data=json_encode(array('username'=> $username, 'email' => $email, 'password' => $password, 'firstName' => $fname, 'lastName' => $lname));
		$header=array('Accept: application/json', 'Content-Type: application/json');
		$ret_data=$this->callAPI("POST", $server."/users", $header, $data);
		$fedUserRoleId = getUserRoleId ( $server );
		$user = getUser( $server, $email );
		setRoleToUser($server, $user->uuid, $fedUserRoleId);
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

    public function __construct($info, $config) {
        parent::__construct($info, $config);

        if (!is_string($config['dsn'])) {
            throw new Exception('Missing or invalid dsn option in config.');
        }
        $this->dsn = $config['dsn'];
        
        if (!is_string($config['idp_name'])) {
	            throw new Exception('Missing or invalid idp_name option in config.');
        }
	    $this->idp_name = $config['idp_name'];
    }


    protected function login($username, $password) {
	$server_uri = $this->dsn;
	SimpleSAML_Logger::debug('Server URI:' . var_export($server_uri, TRUE));	

	$ret=$this->authenticateUser($server_uri, $username, $password);
	if ( strcmp ( $username ,  $ret->username ) != 0 ){
 	    	/* User not found. */
            SimpleSAML_Logger::warning('ContrailAuth: Could not find user ' . var_export($username, TRUE) . '.');
            throw new SimpleSAML_Error_Error('WRONGUSERPASS');
	}

	$ret_roles = $this->getRoles( $server_uri, $ret->uuid );
	$roles_arr=array();
	foreach($ret_roles as $role){
		array_push($roles_arr, $role->name);
	}
	SimpleSAML_Logger::debug('User roles:' . var_export($roles_arr, TRUE));

	$ret_groups = $this->getGroups( $server_uri, $ret->uuid );
	$groups_arr=array();
        foreach($ret_groups as $group){
                array_push($groups_arr, $group->name);
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
            	'uid' => array($username),
		'uuid' => array($ret->uuid),
            	'displayName' => array($ret_user->firstName, $ret_user->lastName),
	   	'roles' => array_values($roles_arr),
	   	'groups' => array_values($groups_arr),
	   	'current_loa' => array($current_loa),
	   	//'attributes'  => array_values($attr_arr),
        );
/*
	foreach($attributes_arr as $attribute){
		$attributes[$attribute->name] = array($attribute->value);
        }
*/
	SimpleSAML_Logger::debug('Complete returned attributes:' . var_export($attributes, TRUE));

        return $attributes;
    }

}

