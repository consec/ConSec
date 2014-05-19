Install
=======

.. code-block:: bash

    ## BUILD ##

    mvn package

    ## INSTALL ##

    apt-get update
    apt-get install python-zookeeper python-chardet apache2 libapache2-mod-wsgi

    useradd -m contrail

    mkdir -p /var/lib/contrail/federation/federation-web/static
    mkdir -p /usr/share/contrail/federation/
    mkdir -p /etc/contrail/federation

    tar xvzpf federation-web.tar.gz

    mv federation-web /usr/share/contrail/federation/

    pushd /usr/share/contrail/federation/federation-web

    python offline_bootstrap.py
    ./bin/buildout -No

    popd

    /usr/share/contrail/federation/federation-web/bin/django default_settings > /etc/contrail/federation/federation-web.conf

    # Copy pages
    cp /usr/share/contrail/federation/federation-web/extra/apache-vhost /etc/apache2/sites-available/contrail-federation-web
	cp /usr/share/contrail/federation/federation-web/extra/contrail-federation-web-ssl /etc/apache2/sites-available/contrail-federation-web-ssl    

	# Add contrail-federation-web.contrail.eu into /etc/hosts (pointing to localhost)
    echo "127.0.0.1	contrail-federation-web.contrail.eu" >> /etc/hosts

	# Add ports	
 	cat /usr/share/contrail/federation/federation-web/extra/ports-to-add.conf >> /etc/apache2/ports.conf

    sudo a2dissite default
    sudo a2ensite contrail-federation-web
    sudo a2ensite contrail-federation-web-ssl
    
    /usr/share/contrail/federation/federation-web/bin/django collectstatic --noinput

    chown contrail:contrail /var/lib/contrail -R
    chown contrail:contrail /usr/share/contrail -R
    chown contrail:contrail /etc/contrail/federation -R

    /etc/init.d/apache2 restart

    ## REMOVE ##

    a2dissite contrail-federation-web

    /etc/init.d/apache2 reload

    rm -Rf /var/lib/contrail/federation/federation-web/static
    rm -Rf /usr/share/contrail/federation/federation-web/
    rm /etc/apache2/sites-available/contrail-federation-web

