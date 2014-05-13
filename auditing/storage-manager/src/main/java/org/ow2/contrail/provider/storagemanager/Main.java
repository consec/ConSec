package org.ow2.contrail.provider.storagemanager;

import org.apache.log4j.Logger;
import org.ow2.contrail.provider.storagemanager.utils.MongoDBConnection;
import org.quartz.CronExpression;
import org.quartz.Scheduler;
import org.quartz.impl.JobDetailImpl;
import org.quartz.impl.StdSchedulerFactory;
import org.quartz.impl.triggers.CronTriggerImpl;

import java.io.IOException;

public class Main {
    private static Logger log = Logger.getLogger(Main.class);

    public static void main(String[] args) throws IOException {
        if (args.length != 2 ||
                !args[0].equals("--config")) {
            System.out.println("Usage: Archiver --config <file>");
            System.exit(1);
        }

        String confFile = args[1];

        Conf.getInstance().load(confFile);
        MongoDBConnection.init();

        try {
            log.trace("Storage-manager is starting...");

            log.trace("Starting MetricsDataListener...");
            MetricsDataListener metricsDataListener = new MetricsDataListener();
            Thread metricsDataListenerThread = new Thread(metricsDataListener);
            metricsDataListenerThread.start();

            log.trace("Starting AuditEventsListener...");
            AuditEventsListener auditEventsListener = new AuditEventsListener();
            auditEventsListener.start();

            log.trace("Scheduling Archiver...");
            Scheduler scheduler = StdSchedulerFactory.getDefaultScheduler();
            scheduler.start();

            JobDetailImpl job = new JobDetailImpl();
            job.setJobClass(Archiver.class);
            job.setName("Archiver Job");
            job.setGroup(Scheduler.DEFAULT_GROUP);

            // Initiate CronTrigger with its name and group name
            CronTriggerImpl cronTrigger = new CronTriggerImpl();
            cronTrigger.setName("CronTrigger for Archiver Job");
            cronTrigger.setGroup(Scheduler.DEFAULT_GROUP);

            // setup CronExpression
            String cronExpression = Conf.getInstance().getArchiverSchedule();
            CronExpression cronExp = new CronExpression(cronExpression);

            // Assign the CronExpression to CronTrigger
            cronTrigger.setCronExpression(cronExp);

            // schedule a job with JobDetail and Trigger
            scheduler.scheduleJob(job, cronTrigger);
            log.trace("Archiver scheduled at " + cronExpression);

            log.trace("Storage-manager started successfully.");
        }
        catch (Exception e) {
            log.error("Storage-manager failed to start: ", e);
        }
    }
}
