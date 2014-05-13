package org.ow2.contrail.federation.auditmanager.scheduler;

import org.ow2.contrail.federation.auditmanager.jobs.Job;
import org.ow2.contrail.federation.auditmanager.jobs.JobStatus;
import org.ow2.contrail.federation.auditmanager.utils.Conf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class Scheduler {
    private static Logger log = LoggerFactory.getLogger(Scheduler.class);
    private ThreadPoolExecutor executor;
    private BlockingQueue<Runnable> workQueue;
    Map<String, Job> jobs = Collections.synchronizedMap(new HashMap<String, Job>());

    public Scheduler() {
        workQueue = new LinkedBlockingQueue<Runnable>();
        int poolSize = Conf.getInstance().getSchedulerPoolSize();
        executor = new ThreadPoolExecutor(poolSize, poolSize, 0, TimeUnit.SECONDS, workQueue) {

            @Override
            public void execute(Runnable r) {
                super.execute(r);
                Job job = (Job) r;
                jobs.put(job.getJobId(), job);
                if (log.isDebugEnabled()) {
                    log.debug("Job {} has been scheduled.", job.getJobId());
                    log.debug("jobs collection size is {}.", jobs.size());
                }
            }

            @Override
            protected void afterExecute(Runnable r, Throwable t) {
                super.afterExecute(r, t);
                Job job = (Job) r;
                jobs.remove(job.getJobId());
                if (log.isDebugEnabled()) {
                    log.debug("Job {} removed.", job.getJobId());
                }
            }
        };

        log.debug("Scheduler initialized successfully. Pool size set to {}.", poolSize);
    }

    public void addJob(Job job) throws Exception {
        log.debug("Adding new job with id {} to the queue.", job.getJobId());
        try {
            job.setJobStatus(JobStatus.QUEUED);
            job.persist();
            executor.execute(job);
            log.debug("ThreadPoolExecutor status: Active tasks: {}, Completed tasks: {}, Total tasks: {}",
                    executor.getActiveCount(), executor.getCompletedTaskCount(), executor.getTaskCount());
        }
        catch (Exception e) {
            throw new Exception("Failed to schedule job: " + e.getMessage(), e);
        }
    }

    public <E> E getJob(String jobId, Class<E> clazz) {
        Job job = jobs.get(jobId);
        if (job == null || !clazz.isInstance(job)) {
            return null;
        }
        else {
            return (E) job;
        }
    }
}
