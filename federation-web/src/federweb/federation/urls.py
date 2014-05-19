from django.conf.urls.defaults import patterns, url

from federweb.federation.views.dashboard import Dashboard
from federweb.federation.views.users import UsersList, UsersCreate, UsersEdit,\
    UsersRemove
from federweb.federation.views.providers import ProvidersList, ProvidersCreate,\
    ProvidersEdit, ProvidersRemove
from federweb.federation.views.attributes import AttributesList,\
    AttributesCreate, AttributesEdit, AttributesRemove
from federweb.federation.views.idps import IdpsList, IdpsCreate, IdpsEdit,\
    IdpsRemove
from federweb.federation.views.roles import RolesList, RolesCreate, RolesEdit,\
    RolesRemove
from federweb.federation.views.groups import GroupsList, GroupsCreate,\
    GroupsEdit, GroupsRemove

from federweb.federation.views.authorization import AuthorizationOrganizationsList, \
    AuthorizationOrganizationsEdit, AuthorizationOrganizationsCreate, \
    AuthorizationOrganizationsRemove, AuthorizationOrganizationsClientsList, \
    AuthorizationOrganizationsClientsCreate, AuthorizationOrganizationsClientsRemove, \
    AuthorizationOrganizationsClientsEdit, AuthorizationTrustList, authorizationTrustModify

urlpatterns = patterns('',
    url(r'^$', Dashboard.as_view(), name='federation'),
            
    url(r'^/users$', UsersList.as_view(), name='federation_users'),
    url(r'^/users/create$', UsersCreate.as_view(), name='federation_users_create'),
    url(r'^/users/(?P<id>FEDERATION)/edit$', UsersEdit.as_view(), name='federation_users_edit'),
    url(r'^/users/(?P<id>FEDERATION)/remove$', UsersRemove.as_view(), name='federation_users_remove'),
    url(r'^/users/(?P<id>[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12})/edit$', UsersEdit.as_view(), name='federation_users_edit'),
    url(r'^/users/(?P<id>[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12})/remove$', UsersRemove.as_view(), name='federation_users_remove'),

    url(r'^/providers$', ProvidersList.as_view(), name='federation_providers'),
    url(r'^/providers/create$', ProvidersCreate.as_view(), name='federation_providers_create'),
    url(r'^/providers/(?P<id>[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12})/edit$', ProvidersEdit.as_view(), name='federation_providers_edit'),
    url(r'^/providers/(?P<id>[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12})/remove$', ProvidersRemove.as_view(), name='federation_providers_remove'),
    
    url(r'^/attributes$', AttributesList.as_view(), name='federation_attributes'),
    url(r'^/attributes/create$', AttributesCreate.as_view(), name='federation_attributes_create'),
    url(r'^/attributes/(?P<id>[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12})/edit$', AttributesEdit.as_view(), name='federation_attributes_edit'),
    url(r'^/attributes/(?P<id>[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12})/remove$', AttributesRemove.as_view(), name='federation_attributes_remove'),
    
    url(r'^/idps$', IdpsList.as_view(), name='federation_idps'),
    url(r'^/idps/create$', IdpsCreate.as_view(), name='federation_idps_create'),
    url(r'^/idps/(?P<id>[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12})/edit$', IdpsEdit.as_view(), name='federation_idps_edit'),
    url(r'^/idps/(?P<id>[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12})/remove$', IdpsRemove.as_view(), name='federation_idps_remove'),
    
    url(r'^/roles$', RolesList.as_view(), name='federation_roles'),
    url(r'^/roles/create$', RolesCreate.as_view(), name='federation_roles_create'),
    url(r'^/roles/(?P<id>\d+)/edit$', RolesEdit.as_view(), name='federation_roles_edit'),
    url(r'^/roles/(?P<id>\d+)/remove$', RolesRemove.as_view(), name='federation_roles_remove'),
    
    url(r'^/groups$', GroupsList.as_view(), name='federation_groups'),
    url(r'^/groups/create$', GroupsCreate.as_view(), name='federation_groups_create'),
    url(r'^/groups/(?P<id>\d+)/edit$', GroupsEdit.as_view(), name='federation_groups_edit'),
    url(r'^/groups/(?P<id>\d+)/remove$', GroupsRemove.as_view(), name='federation_groups_remove'),


    #authorization
    ####################################### url(r'^/authorization$', AuthorizationList.as_view(), name='federation_authorization'),

    url(r'^/authorization/organizations$', AuthorizationOrganizationsList.as_view(), name='federation_authorization_organizations'),
    url(r'^/authorization/organizations/create$', AuthorizationOrganizationsCreate.as_view(), name='federation_authorization_organizations_create'),
    url(r'^/authorization/organizations/(?P<id>\d+)/edit$', AuthorizationOrganizationsEdit.as_view(), name='federation_authorization_organizations_edit'),
    url(r'^/authorization/organizations/(?P<id>\d+)/remove$', AuthorizationOrganizationsRemove.as_view(), name='federation_authorization_organizations_remove'),

    url(r'^/authorization/organizations/(?P<organization_id>\d+)/clients$', AuthorizationOrganizationsClientsList.as_view(), name='federation_authorization_organizations_clients'),
    url(r'^/authorization/organizations/(?P<organization_id>\d+)/clients/create$', AuthorizationOrganizationsClientsCreate.as_view(), name='federation_authorization_organizations_clients_create'),
    url(r'^/authorization/organizations/(?P<organization_id>\d+)/clients/(?P<id>\d+)/edit$', AuthorizationOrganizationsClientsEdit.as_view(), name='federation_authorization_organizations_clients_edit'),
    url(r'^/authorization/organizations/(?P<organization_id>\d+)/clients/(?P<id>\d+)/remove$', AuthorizationOrganizationsClientsRemove.as_view(), name='federation_authorization_organizations_clients_remove'),

    url(r'^/authorization/trust$', AuthorizationTrustList.as_view(), name='federation_authorization_trust'),
    url(r'^/authorization/trust/modify/(?P<organization_id>\d+)$', authorizationTrustModify, name='federation_authorization_trust_modify_organizations'),
    url(r'^/authorization/trust/modify/(?P<organization_id>\d+)/clients/(?P<client_id>\d+)$', authorizationTrustModify, name='federation_authorization_trust_modify_organizations_clients'),

    # Auditing
    url(r'^/auditing$', 'federweb.federation.views.auditing.auditing_show_tab', name='federation_auditing'),
)
