from django.core.management.base import NoArgsCommand

from federweb.base.models import federation

def initial():
    r_fc = federation.roles.new()
    r_fc.name = 'FederationCoordinator'
    r_fc.description = 'FederationCoordinator'
    r_fc.acl = ''
    r_fc.save()
    
    f_ca = federation.roles.new()
    f_ca.name = 'CloudAdministrator'
    f_ca.description = 'CloudAdministrator'
    f_ca.acl = ''
    f_ca.save()
    
    r_fu = federation.roles.new()
    r_fu.name = 'FederationUser'
    r_fu.description = 'FederationUser'
    r_fu.acl = ''
    r_fu.save()
    
    u_c = federation.users.new()
    u_c.username = 'coordinator'
    u_c.password = 'password'
    u_c.email = 'foo@bar.baz'
    u_c.firstName = 'Coordinator'
    u_c.lastName = 'of Federation'
    u_c.save()
    
    ur = u_c.roles.new()
    ur.roleId = r_fc.id
    ur.save()

class Command(NoArgsCommand):
    help = 'POSTs initial data to Federation API'
    
    def handle_noargs(self, **options):
        initial()
        
        self.stdout.write('Done.\n')
