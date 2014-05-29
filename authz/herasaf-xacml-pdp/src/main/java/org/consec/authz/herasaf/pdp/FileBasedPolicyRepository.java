package org.consec.authz.herasaf.pdp;

import org.apache.log4j.Logger;
import org.herasaf.xacml.core.SyntaxException;
import org.herasaf.xacml.core.api.PolicyRetrievalPoint;
import org.herasaf.xacml.core.api.UnorderedPolicyRepository;
import org.herasaf.xacml.core.policy.Evaluatable;
import org.herasaf.xacml.core.policy.EvaluatableID;
import org.herasaf.xacml.core.policy.PolicyMarshaller;
import org.herasaf.xacml.core.simplePDP.MapBasedSimplePolicyRepository;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;

public class FileBasedPolicyRepository extends MapBasedSimplePolicyRepository implements UnorderedPolicyRepository, PolicyRetrievalPoint {
    private static Logger log = Logger.getLogger(FileBasedPolicyRepository.class);
    private File persistentRepository;
    private static final String policyFilePrefix = "HERASAFPOLICY-";

    public FileBasedPolicyRepository(File persistentRepository) throws SyntaxException {
        super();
        this.persistentRepository = persistentRepository;
    }

    @Override
    public void deploy(Evaluatable evaluatable) {
        // store policy to the file
        StringWriter writer = new StringWriter();
        try {
            PolicyMarshaller.marshal(evaluatable, writer);
            String content = writer.toString();

            File policyFile = getPolicyFile(evaluatable.getId());
            PrintWriter out = new PrintWriter(policyFile);
            out.print(content);
            out.close();
        }
        catch (Exception e) {
            throw new RuntimeException("Failed to store the policy, marshal failed: " + e.getMessage());
        }

        super.deploy(evaluatable);
    }

    @Override
    public void undeploy(EvaluatableID id) {
        File policyFile = getPolicyFile(id);
        policyFile.delete();

        super.undeploy(id);
    }

    public void restoreRepository() throws SyntaxException {
        log.trace("Reloading policies from the persistent storage " + persistentRepository.getAbsolutePath());
        File[] files = persistentRepository.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.getName().startsWith(policyFilePrefix)) {
                    Evaluatable evaluatable = PolicyMarshaller.unmarshal(file);
                    super.deploy(evaluatable);
                    log.trace("Policy has been deployed successfully: " + evaluatable.getId());
                }
            }
        }
        log.trace("Policies have been deployed successfully.");
    }

    public void clearRepository() {
        File[] files = persistentRepository.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.getName().startsWith(policyFilePrefix)) {
                    file.delete();
                }
            }
        }
    }

    private File getPolicyFile(EvaluatableID id) {
        String fileName = policyFilePrefix + id.getId().replaceAll("\\W", "-");
        File policyFile = new File(persistentRepository, fileName);
        return policyFile;
    }
}