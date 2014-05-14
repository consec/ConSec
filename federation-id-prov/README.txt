				-------------------------------------------
					Federation Identity DB
				-------------------------------------------
				ales.cernivec@xlab.si
				-------------------------------------------
						2012-01-16


Development

  The easiest way to import the project into Eclipse is to use m2eclipse. 

Service configuration

  src/main/webapp/WEB-INF/web.xml contains context parameter with the name of <<<properties-file>>>. By default it points
  to the configuration file on the system <<</etc/contrail/federation-api/federation-id-prov.conf>>>. The configuration file
  is created when the service is installed from debian package. 
  
  Currently there are three parameters inside the configuration:  
  * <<<authz-enabled>>> by default set to <<<false>>>
  * <<<authz-file>>>
  
  <<<authz-enabled>>> is set to <<<true>>> when authorization is enabled. When authorization is enabled type of authz engine
  is defined by other attribute <<<authz-engine>>>.@Damjan, please update :)
  
  <<<authz-file>>> is used when <<<authz-engine>>> defines simple authorization. Only users with CNs in the file are alllowed
  to execute API commands.

Database

  At this point only users and their attributes can be managed with the use of the database.

* Database installation (debian)

  Note that at this point <<</users>>>, <<</attributes>>>, <<</idps>>>, <</accounts>>, <</groups>>, <</roles>> resources use the database backend. 

--------------------------------------------------
# apt-get install mysql-server
--------------------------------------------------
  
  Allow access to a user <<<contrail>>> with password <<<contrail>>> 
  to manipulate the database <<<contrail>>>:
 
----------------------------------------------------
$ mysql -u root -p 		// Note: your root account can be different

mysql> CREATE USER 'contrail'@'localhost' IDENTIFIED BY 'contrail';
mysql> GRANT SELECT,INSERT,UPDATE,DELETE,CREATE,DROP ON contrail.* TO 'contrail'@'localhost';
----------------------------------------------------

* Database schema

  Database schema is provided by the project federation-db.
  Database has been created with <<<MySQL Workbench>>>. 
  Under the directory <<<src/main/resources/db>>> you can find file <<<Provider-db.mwb>>> 
  wich is <<<MySQL Workbench>>> file. <<<Provider-db.sql>>> is an exported script of the 
  which can be used to import the database scheme into the <<<mysql>>>. Note: user account on 
  your machine can differ to bellow:
 
----------------------------------------------------- 
$ mysql -u root -p < Provider-db.sql
-----------------------------------------------------

* Database configuration

  Resides under <<<src/main/resources/persistence.xml>>>. There are several sections in the configuration file:
  
      * <<<javax.persistence.jdbc.url>>> describes listening port of the API server
      
      * <<<javax.persistence.jdbc.driver>>> defines which driver is used for the database
                   
      * <<<javax.persistence.jdbc.user>>> username of the database
      
      * <<<javax.persistence.jdbc.password>>> password of the database

      []
      
  Here we provide an example of the configuration file.
  
-----
...
        <properties>
            <property name="javax.persistence.jdbc.url" value="jdbc:mysql://localhost/contrail"/>
            <property name="javax.persistence.jdbc.password" value="contrail"/>
            <property name="javax.persistence.jdbc.driver" value="com.mysql.jdbc.Driver"/>
            <property name="javax.persistence.jdbc.user" value="contrail"/>
        </properties>
...
-----

Compiling

-------------
$ mvn clean compile package
-------------

 Under <<<{$PWD}/target/federation-id-prov.war>>> <<<war>>> file is created which should be 
 copied under <<<webapps>>> directory of <<<tomcat>>> installation (e.g. <<</var/lib/tomcat6/webapps/>>>).
 This way application is deployed as a servlet. And is up and running

* Testing

  For testing the API, <<<curl>>> can be used. In following subsections present examples how to test HTTP commands of each of the resource provided by the <<Federation API>>.
  See section <<<feder-db-examples>>> to see some examples. 
  
Running unit tests

Generating documentation

----------
$ mvn site
----------

Deploying

-------------
$ mvn clean compile site package
-------------

 Creates <<<tar.gz>>> under <<<target>>> directory. It can be used to deploy the application to the 
 new server.  