<?php
class sspmod_contrailmodule_Auth_Source_CAuth extends sspmod_core_Auth_UserPassBase {

    /* The database DSN.
     * See the documentation for the various database drivers for information about the syntax:
     *     http://www.php.net/manual/en/pdo.drivers.php
     */
    private $dsn;

    /* The database username & password. */
    private $username;
    private $password;

	// We need php5-curl
	private function authenticate_crypt($username, $password)
	{
	        // create curl resource
        	$ch = curl_init();
	        // set url
	        curl_setopt($ch, CURLOPT_URL, "http://localhost:8080/federation-id-prov/users/authenticate");
		$headers = array(
		    'Accept: application/json',
		    'Content-Type: application/json',
		);
		curl_setopt($ch, CURLOPT_HTTPHEADER, $headers);

	        //return the transfer as a string
		$query = '{ "username" : "'.$username.'", "password":"'.$password.'" }';
		curl_setopt($ch, CURLOPT_POST, 1);
		curl_setopt($ch, CURLOPT_POSTFIELDS, $query);
		#curl_setopt($ch, CURLOPT_POSTFIELDSIZE, strlen($query));
	        curl_setopt($ch, CURLOPT_RETURNTRANSFER, 1);

	        // $output contains the output string
        	$output = curl_exec($ch);
	        // close curl resource to free up system resources
	        curl_close($ch);
		$returned_arr = json_decode($output);
		return strcmp ( $username ,  $returned_arr->{'username'} );
	}

    public function __construct($info, $config) {
        parent::__construct($info, $config);

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
     * A helper function for validating a password hash.
     *
     * In this example we check a SSHA-password, where the database
     * contains a base64 encoded byte string, where the first 20 bytes
     * from the byte string is the SHA1 sum, and the remaining bytes is
     * the salt.
     */
    private function checkPassword($passwordHash, $password) {
        /**
        * $passwordHash = base64_decode($passwordHash);
        * $digest = substr($passwordHash, 0, 20);
        * $salt = substr($passwordHash, 20);
        *
        * $checkDigest = sha1($password . $salt, TRUE);
        * return $digest === $checkDigest;
        */
        return $passwordHash == $password;
    }

    protected function login($username, $password) {

        $dsn = 'mysql:host=localhost;port=3306;dbname=contrail';
        /* Connect to the database. */
        $db = new PDO($this->dsn, $this->username, $this->password);
        $db->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);

        /* Ensure that we are operating with UTF-8 encoding.
         * This command is for MySQL. Other databases may need different commands.
         */
        $db->exec("SET NAMES 'utf8'");

        /* With PDO we use prepared statements. This saves us from having to escape
         * the username in the database query.
         */
        $st = $db->prepare('SELECT username, password, firstName, lastName FROM User WHERE username=:username');

	if (!$st->execute(array('username' => $username))) {
            throw new Exception('Failed to query database for user.');
        }

        /* Retrieve the row from the database. */
        $row = $st->fetch(PDO::FETCH_ASSOC);
        if (!$row) {
            /* User not found. */
            SimpleSAML_Logger::warning('MyAuth: Could not find user ' . var_export($username, TRUE) . '.');
            throw new SimpleSAML_Error_Error('WRONGUSERPASS');
        }
	$ret=$this->authenticate_crypt($username, $password);
        /* Check the password. */
	/*       if (!$this->checkPassword($row['password'], $password)) {*/
	if($ret != 0){
            /* Invalid password. */
            SimpleSAML_Logger::warning('MyAuth: Wrong password for user ' . var_export($username, TRUE) . '.');
            throw new SimpleSAML_Error_Error('WRONGUSERPASS');
        }

	$roles = $db->prepare('SELECT ur.name FROM URole ur, User u, User_has_Role uhr WHERE u.username=:username AND uhr.userId=u.userId AND ur.roleId=uhr.roleId');
	
	if (!$roles->execute(array('username' => $username))) {
		throw new Exception('Failed to query database for user roles.');
	}

	/* Retrieve the rows from the database. */
        $row_roles = $roles->fetchAll();
	$roles_arr=array();
        if (!$row_roles) {
	        /* User not found. */
		SimpleSAML_Logger::warning('Could not fetch user roles. Possibly no info about roles. ');
	}
	else{
		SimpleSAML_Logger::debug('User roles:' . var_export($row_roles, TRUE));
		$count = count($row_roles);
		for ($i = 0; $i < $count; $i++) {
			$roles_arr[$i]=$row_roles[$i]['name'];
		}
	}
	/* Construct an array with info about groups */
        $groups = $db->prepare('SELECT ug.name FROM UGroup ug, User u, User_has_Group uhg WHERE u.username=:username AND uhg.userId=u.userId AND ug.groupId=uhg.groupId');
	if (!$groups->execute(array('username' => $username))) {
		throw new Exception('Failed to query database for user groups.');
        }

	/* Retrieve the rows from the database. */
        $row_groups = $groups->fetchAll();
        $groups_arr=array();
        if (!$row_groups) {
              	/* User not found. */
		SimpleSAML_Logger::warning('Could not fetch user groups. Possibly no info about groups.');
	}
	else{
		SimpleSAML_Logger::debug('User groups:' . var_export($row_groups, TRUE));
       		$count = count($row_groups);
		for ($i = 0; $i < $count; $i++) {
			$groups_arr[$i]=$row_groups[$i]['name'];
		}
	}
	/* End of groups */
	/* Construct an array about info on attributes. */
	$con_attrs = $db->prepare('SELECT attr.name, uha.value FROM Attribute attr, User u, User_has_Attribute uha WHERE u.username=:username AND uha.userId=u.userId AND attr.attributeId=uha.attributeId');
	if (!$con_attrs->execute(array('username' => $username))) {
		throw new Exception('Failed to query database for user contrail attributes.');
	}
        $attributes = array(
            'uid' => array($username),
            'displayName' => array($row['firstName'], $row['lastName']),
	   'roles' => array_values($roles_arr),
	   'groups' => array_values($groups_arr),
        );

	/* Retrieve the rows from the database. */
	$row_con_attrs = $con_attrs->fetchAll();
	if (!$row_con_attrs) {
        	/* User not found. */
              	SimpleSAML_Logger::warning('User does not have extended contrail attributes.');
	}
	else{
		SimpleSAML_Logger::debug('User contrail attributes:' . var_export($row_con_attrs, TRUE));
		$count = count($row_con_attrs);
		$con_attrs_arr=array();
		for ($i = 0; $i < $count; $i++) {
			$con_attrs_arr[$row_con_attrs[$i]['name']]=$row_con_attrs[$i]['value'];
        	}
	        /* Create the attribute array of the user. */
		$attrs_keys=array_keys($con_attrs_arr);
		$attrs_values=array_values($con_attrs_arr);
		for($i=0;$i<count($con_attrs_arr);$i++){
			$attributes[$attrs_keys[$i]]=array($attrs_values[$i]);
		}
	}
	/* End of attributes */
        /* Return the attributes. */
        return $attributes;
    }

}

