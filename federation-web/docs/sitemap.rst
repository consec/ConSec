Sitemap
=======

.. code-block:: yaml
    
    Federation (role=federation_coordinator) /federation
    -Dashboard /federation
    -Users /federation/users
    -Providers /federation/providers
    -Attributes /federation/attributes
    -IDPs /federation/idps
    -Roles /federation/roles
    -Groups /federation/groups
    
    
    User (role=cloud_user) /user
    -Dashboard /user
    -Applications /user/apps
    -OVFs /user/ovfs
    -SLA Templates /user/slats
    -SLAs /user/slas
    -My Profile /user/profile
    #--Identity providers /user/profile/ids
    #--Groups
    #--Roles
    
    
    Provider (role=provider_admin) /provider
    -Dashboard /provider
    -OVFs /provider/ovfs
    -SLATs /provider/slats
    -Virtual Machines /provider/vms
    -Servers /provider/servers
    -Clusters /provider/clusters
    -Virtual Organizations /provider/vos
