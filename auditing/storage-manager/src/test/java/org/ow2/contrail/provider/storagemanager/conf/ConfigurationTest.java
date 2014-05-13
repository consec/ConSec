package org.ow2.contrail.provider.storagemanager.conf;

import org.junit.Test;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.Writer;
import java.util.ArrayList;

import static junit.framework.Assert.assertEquals;

public class ConfigurationTest {

    private static final String CONFIG_FILE_TEST = "./config-test.xml";

    @Test
    public void testConfigurationMarshalling() throws Exception {

        Configuration config = new Configuration();
        ArrayList<Metric> metrics = new ArrayList<Metric>();
        config.setMetrics(metrics);

        Metric cores = new Metric();
        metrics.add(cores);
        cores.setName("cores");
        cores.setTargetInterval(new Interval(1, "day"));
        ValuesData valuesDataCores = new ValuesData(true, false, ValuesData.Type.INTERVALS_OF_CONST_VALUE);
        cores.setValuesData(valuesDataCores);

        Metric idle = new Metric();
        metrics.add(idle);
        idle.setName("idle");
        idle.setTargetInterval(new Interval(1, "h"));
        ValuesData valuesDataIdle = new ValuesData(true, true, ValuesData.Type.ARRAY_OF_VALUES);
        valuesDataIdle.setInterval(new Interval(1, "min"));
        idle.setValuesData(valuesDataIdle);
        SummaryData summaryDataIdle = new SummaryData();
        summaryDataIdle.setEnabled(true);
        ArrayList<SummaryData.Statistic> statistics = new ArrayList<SummaryData.Statistic>();
        statistics.add(SummaryData.Statistic.MIN);
        statistics.add(SummaryData.Statistic.MAX);
        statistics.add(SummaryData.Statistic.AVERAGE);
        summaryDataIdle.setStatistics(statistics);
        idle.setSummaryData(summaryDataIdle);


        // create JAXB context and instantiate marshaller
        JAXBContext context = JAXBContext.newInstance(Configuration.class);
        Marshaller m = context.createMarshaller();
        m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
        m.marshal(config, System.out);

        Writer w = null;
        w = new FileWriter(CONFIG_FILE_TEST);
        m.marshal(config, w);

        // get variables from our xml file, created before
        System.out.println();
        System.out.println("Output from our XML File: ");
        Unmarshaller um = context.createUnmarshaller();
        Configuration config1 = (Configuration) um.unmarshal(new FileReader(CONFIG_FILE_TEST));

        assertEquals(config1.getMetrics().size(), 2);
    }
}