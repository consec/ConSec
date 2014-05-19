from django.core.management.base import NoArgsCommand

from federweb.base.models import federation

def initial():
    p = federation.providers.new()
    p.name = 'Provider'
    p.typeId = 42
    p.providerUri = 'http://10.31.1.10:10500'
    p.save()
    
    ps = p.slats.new()
    ps.name = 'XLAB SLA'
    ps.url = 'http://contrail.xlab.si/test-files/ubuntu-test-xlab-SLA.xml'
    ps.save()
    
    pvm = p.vms.new()
    pvm.name = 'New VM'
    pvm.save()
    
    pserv = p.servers.new()
    pserv.name = 'n0010'
    pserv.ram_total = '3915'
    pserv.ram_used = '1152'
    pserv.ram_free = '2763'
    pserv.cpu_cores = '4'
    pserv.cpu_speed = '2494.276'
    pserv.cpu_load_one = '0.09'
    pserv.cpu_load_five = '0.04'
    pserv.save()
    
    pc = p.clusters.new()
    pc.name = 'cluster1'
    pc.save()
    
    pcs = pc.servers.new()
    pcs.raw = [pserv.get_relative_uri()]
    pcs.save(force_put=True)
    
    pcvm = pc.vms.new()
    pcvm.raw = [pvm.get_relative_uri()]
    pcvm.save(force_put=True)
    
    pvo = p.vos.new()
    pvo.name = 'Contrail organization'
    pvo.save()
    
    pvoc = pvo.clusters.new()
    pvoc.raw = [pc.get_relative_uri()]
    pvoc.save(force_put=True)
    
    a = federation.attributes.new()
    a.name = 'org.ow2.contrail.user.registration-date'
    a.defaultValue = '2012-12-12'
    a.save()
    
    group1 = federation.groups.new()
    group1.name = 'Our Group'
    group1.save()
    
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
    
    ip = federation.idps.new()
    ip.providerName = 'Google'
    ip.providerURI = 'http://auth.google.com'
    ip.description = 'description'
    ip.save()
    
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
    
    u_p = federation.users.new()
    u_p.username = 'provider'
    u_p.password = 'password'
    u_p.email = 'foo@bar.baz'
    u_p.firstName = 'Provider'
    u_p.lastName = 'Admin'
    u_p.save()
    
    ur = u_p.roles.new()
    ur.roleId = f_ca.id
    ur.save()
    
    u_u = federation.users.new()
    u_u.username = 'user'
    u_u.password = 'password'
    u_u.email = 'foo@bar.baz'
    u_u.firstName = 'User'
    u_u.lastName = 'of Foo'
    u_u.save()
    
    ur = u_u.roles.new()
    ur.roleId = r_fu.id
    ur.save()
    
    ug = u_u.groups.new()
    ug.groupId = group1.id
    ug.save()
    
    ua = u_u.attributes.new()
    ua.attributeId = a.id
    ua.value = '2012-02-13'
    ua.save()
    
    ui = u_u.ids.new()
    ui.identityProviderId = ip.id
    ui.identity = 'user@gmail.com'
    ui.save()
    
#    slat = u_u.slats.new()
#    slat.url = 'http://www.contrail-project.eu/slats/123456'
#    slat.content = '<sla/>'
#    slat.slatId = ps.id
#    slat.save()
    
#    sla = u_u.slas.new()
#    sla.sla = '<sla/>'
#    sla.content = '<sla/>'
##    sla.templateurl = slat.url
##    sla.slatId = slat.get_relative_uri()
#    sla.save()
    
#    uo = u_u.ovfs.new()
#    uo.providerOvfId = po.id
#    uo.name = 'My new OVF'
#    uo.content = '<ovf/>'
#    #uo.attributes = None
#    uo.save()
    
#    ua = u_u.applications.new()
#    ua.name = 'New Application'
#    ua.ovf = uo.content
#    ua.save()

class Command(NoArgsCommand):
    help = 'POSTs text data to Federation API for testing data display'
    
    def handle_noargs(self, **options):
        initial()
        
        self.stdout.write('Done.\n')
