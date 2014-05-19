Contribute
==========

Install python>=2.6.

.. code-block:: bash

	# apt-get update
	# apt-get install python-zookeeper python-chardet build-essential python-setuptools python-dev 

.. code-block:: bash

    $ make build
    $ bin/django default_settings > local_settings.py
    $ ./bin/django collectstatic

Now you can edit your local_settings.py to resemble something like this:

.. code-block:: python

	#STATIC_ROOT = '/var/lib/contrail/federation/federation-web/static'
	DEBUG=True
	
	FEDERATION_API_URL = 'http://localhost:8080/federation-api'
	SLA_EXTRACTOR_BASE = 'http://localhost:8080/rest-monitoring/sla/slaextractor'
	MONITORING_BASE = 'http://localhost:8080/rest-monitoring/monitoring'
	ZOOKEEPER_BASE = '127.0.0.1:2181'
	
	ONLINE_CA_USE=False
	ONLINE_CA_URI='https://one-test.contrail.rl.ac.uk:8443/ca/portaluser'
	
	FEDERATION_WEB='http://contrail-federation-web.contrail.eu'
	FEDERATION_WEB_LOCAL_METADATA='/usr/lib/contrail/federation-web/extra/remote_metadata.xml'
	FEDERATION_WEB_CERT='/usr/lib/contrail/federation-web/extra/contrail-federation-web.cert'
	FEDERATION_WEB_KEY='/usr/lib/contrail/federation-web/extra/contrail-federation-web.key'
	FEDERATION_WEB_CA_FILE='/usr/lib/contrail/federation-web/extra/ca.crt'
	TRUSTSTORE_DIR = '/etc/contrail/truststore'
	
	SSL_USE_DELEGATED_USER_CERT=False
	
	FEDERATION_IDP_GOOGLE='https://google.contrail-idp.contrail.eu'
	FEDERATION_IDP_FEDERATION='https://federation.contrail-idp.contrail.eu'


Next, execute following:

.. code-block:: bash

    $ bin/django runserver 8000

Open http://localhost:8000 in your browser and login with u: coordinator and
p: password
