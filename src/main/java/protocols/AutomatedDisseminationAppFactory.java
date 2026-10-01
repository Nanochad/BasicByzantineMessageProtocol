package protocols;

import java.util.Properties;

import pt.unl.fct.di.novasys.babel2.core.BabelProtocolFactory;

public class AutomatedDisseminationAppFactory implements BabelProtocolFactory<AutomatedDisseminationApp> {

    @Override
    public AutomatedDisseminationApp createInstance(Properties properties) {
        int payloadSize = Integer.parseInt(properties.getProperty("payload_size"));
        long prepareTime = Long.parseLong(properties.getProperty("prepare_time"));
        long cooldownTime = Long.parseLong(properties.getProperty("cooldown_time"));
        long runTime = Long.parseLong(properties.getProperty("run_time"));
        long disseminationInterval = Long.parseLong(properties.getProperty("broadcast_interval"));

        return new AutomatedDisseminationApp(payloadSize, prepareTime, runTime, cooldownTime, disseminationInterval);
    }
}
