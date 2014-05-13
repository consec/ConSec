package org.ow2.contrail.federation.auditmanager.scheduler;

public class SchedulerFactory {
    private static Scheduler scheduler;

    public static void initScheduler() {
        scheduler = new Scheduler();
    }

    public static Scheduler getScheduler() {
        return scheduler;
    }
}
