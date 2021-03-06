INSERT INTO `attribute` VALUES ('d9e6f630-5835-11e3-949a-0800200c9a66', 'urn:contrail:names:provider:subject:slasoi-id',
                                'http://www.w3.org/2001/XMLSchema#integer', '0', '/users',
                                'Designates SLA@SOI id generated by the federation');
INSERT INTO `attribute` VALUES
  ('ec8b9b10-5835-11e3-949a-0800200c9a66', 'urn:contrail:names:provider:subject:num-vm-owns',
   'http://www.w3.org/2001/XMLSchema#integer', '0', '/users', 'Number of VMs owned by the user');
INSERT INTO `attribute` VALUES
  ('f5e01060-5835-11e3-949a-0800200c9a66', 'urn:contrail:names:federation:subject:active-vep',
   'http://www.w3.org/2001/XMLSchema#string', '0', '/users', 'Encodes an id of a vep service');
INSERT INTO `attribute` VALUES
  ('f6e21165-5835-11e3-949a-0800200c9a66', 'urn:contrail:names:federation:subject:current-loa',
   'http://www.w3.org/2001/XMLSchema#string', '4', '/users', 'Current LoA used by the user.');
INSERT INTO `attribute` VALUES
  ('f5e11263-5835-11e3-949a-0800200c9a66', 'urn:contrail:names:provider:subject:minimum-loa',
   'http://www.w3.org/2001/XMLSchema#string', '4', '/providers', 'Minimum LoA available for specific provider.');
INSERT INTO `identity_provider` VALUES ('b2a2a840-5910-11e3-949a-0800200c9a66', 'https://www.google.com/accounts/o8/id',
                                        'Google over Contrail Identity Provider', 'google',
                                        '{\"f6e21165-5835-11e3-949a-0800200c9a66\":\"2\"}');
INSERT INTO `identity_provider` VALUES
  ('ccd33241-abc1-1223-9492-0800210c9456', 'https://multi.contrail.xlab.si/simplesaml/saml2/idp/metadata.php',
   'Contrail IdP XLAB', 'contrail-idp-xlab', '{\"f6e21165-5835-11e3-949a-0800200c9a66\":\"1\"}');
INSERT INTO `group` (group_id, name, description)
VALUES (1, 'bronze', 'Users with an estimation of low resource usage - 5VMs');
INSERT INTO `group` (group_id, name, description)
VALUES (2, 'silver', 'Users with an estimation of medium resource usage - 10VMs');
INSERT INTO `group` (group_id, name, description)
VALUES (3, 'gold', 'Users with an estimation of high resource usage - 20VMs');
INSERT INTO `role` (role_id, name, description) VALUES (1, 'FederationUser', 'Ordinary federation user');
INSERT INTO `role` (role_id, name, description) VALUES (2, 'FederationCoordinator', 'User coordinating federation');
INSERT INTO `role` (role_id, name, description)
VALUES (3, 'CloudAdministrator', 'Administrators of provider infrastructure');
INSERT INTO `user` (user_id, username, attributes, first_name, last_name, email, password)
VALUES ('5a947f8c-83d3-4da0-a52c-d9436ae77bb5', 'coordinator',
        NULL, 'Coordinator', '', 'coordinator@contrail.eu',
        '$2a$06$DCq7YPn5Rq63x1Lad4cll.kD3zZ845LsvMekyowTQk2VNmbDdQsWO');
INSERT INTO `user` (user_id, username, attributes, first_name, last_name, email, password)
VALUES ('cb96e102-82f7-4c5f-a8f0-23149aa6a936', 'admin', NULL,
        'Administrator', '', 'admin@contrail.eu', '$2a$06$DCq7YPn5Rq63x1Lad4c11.P4fII8YTYrgf3o7Jq9Q9i..OjKvLrFm');
INSERT INTO `user` (user_id, username, attributes, first_name, last_name, email,
                    password) VALUES ('caa6e102-8ff0-400f-a120-23149326a936', 'contrailuser',
                                      NULL, 'Demo', 'User', 'demo.user@contrail.eu',
                                      '$2a$06$DCq7YPn5Rq63x1Lad4c11.P4fII8YTYrgf3o7Jq9Q9i..OjKvLrFm');
INSERT INTO `user_has_role` (user_id, role_id) VALUES (1, 2);
INSERT INTO `user_has_role` (user_id, role_id) VALUES (2, 3);
INSERT INTO `user_has_role` (user_id, role_id) VALUES (3, 1);
INSERT INTO `user_has_group` (user_id, group_id) VALUES (1, 1);
INSERT INTO `user_has_group` (user_id, group_id) VALUES (3, 1);
INSERT INTO `user_has_attribute` (user_id, attribute_id, value, referenceId) VALUES (3,
                                                                                     'ec8b9b10-5835-11e3-949a-0800200c9a66',
                                                                                     '0', 3);
INSERT INTO `user_has_attribute` (user_id, attribute_id, value, referenceId) VALUES (3,
                                                                                     'd9e6f630-5835-11e3-949a-0800200c9a66',
                                                                                     '3', 3);
