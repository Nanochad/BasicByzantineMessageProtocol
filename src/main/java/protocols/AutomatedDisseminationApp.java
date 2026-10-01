package protocols;

import java.util.Random;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import messages.SuperImportantMessage;
import pt.unl.fct.di.novasys.babel2.annotations.UponBabelEvent;
import pt.unl.fct.di.novasys.babel2.annotations.UponBroadcastDeliver;
import pt.unl.fct.di.novasys.babel2.channels.ComposedBabelChannel;
import pt.unl.fct.di.novasys.babel2.core.Babel;
import pt.unl.fct.di.novasys.babel2.core.BabelNode;
import pt.unl.fct.di.novasys.babel2.core.broadcast.BroadcastConsumerBabelProtocol;
import timers.DisseminationTimer;
import timers.ExitTimer;
import timers.StartTimer;
import timers.StopTimer;

public class AutomatedDisseminationApp implements BroadcastConsumerBabelProtocol {
    private static final Logger LOGGER = LogManager.getLogger(AutomatedDisseminationApp.class);
    private static final int LEFT_LIMIT = 'A';
    private static final int RIGHT_LIMIT = 'Z';

    /**
     * Relevant objects to communicate with the Babel 2 framework.
     * The runtime object is for intra machine protocol communication. The channel
     * object is for inter machine communication.
     * The Babel object can only have one implementation. while BabelChannel can
     * have many. Ideally, the protocol code should not know what is the specific
     * BabelChannel implementation being used, only what guarantees it provides.
     */
    public Babel runtime;

    private final Random random;

    // Size of the payload of each message (in bytes)
    private final int payloadSize;

    // Time to wait until starting sending messages
    private final long prepareTime;

    // Time to run before shutting down
    private final long runTime;

    // Time to wait until starting sending messages
    private final long cooldownTime;

    // Interval between each broadcast
    private final long disseminationInterval;

    private int messageIndex;

    private ScheduledFuture<?> broadcastTimer;

    public AutomatedDisseminationApp(int payloadSize, long prepareTime, long runTime, long cooldownTime,
            long disseminationInterval) {
        if (payloadSize < 0) {
            throw new IllegalArgumentException("payloadSize must be higher than 0");
        }
        this.payloadSize = payloadSize;
        this.prepareTime = prepareTime;
        this.runTime = runTime;
        this.cooldownTime = cooldownTime;
        this.disseminationInterval = disseminationInterval;
        this.messageIndex = 0;
        this.random = new Random();
    }

    // This method is called by the Babel runtime itself. Once this method is
    // called, the protocol is informed of the Babel object and BabelChannel
    // associated with them. This one doesn't need any of the provided methods of
    // the channel. The protocol can start its normal operation; in this
    // case, setup a timer.
    @Override
    public void init(Babel runtime, ComposedBabelChannel channel) {
        this.runtime = runtime;
        LOGGER.info("Waiting for {} ms", prepareTime);
        runtime.setUpTimer(new StartTimer(), prepareTime, TimeUnit.MILLISECONDS);
    }

    /**
     * Starts broadcasting periodically. The period is set by the
     * {@code this.isseminationInterval} and will stop after {@code this.runTime}
     * 
     * @param startTimer
     */
    @UponBabelEvent
    public void uponStartTimer(StartTimer startTimer) {
        LOGGER.info("Starting Broadcasting Messages... (every {} ms)", disseminationInterval);
        this.broadcastTimer = runtime.setUpTimer(new DisseminationTimer(), 0, disseminationInterval,
                TimeUnit.MILLISECONDS);

        LOGGER.info("Will stop in {} ms...", runTime);
        runtime.setUpTimer(new StopTimer(), runTime, TimeUnit.MILLISECONDS);
    }

    /**
     * Create a new message to be broadcast by the broadcast protocol registerd in
     * this Babel instance.
     * 
     * @param broadcastTimer
     */
    @UponBabelEvent
    public void uponBroadcastTimer(DisseminationTimer broadcastTimer) {
        String newMessage = runtime.getMyself() + " MSG " + messageIndex++ + " "
                + randomCapitalLetters();
        var superDuperImportantMessage = new SuperImportantMessage(newMessage);
        this.broadcast(runtime, superDuperImportantMessage);
        LOGGER.info("Sending: {})", superDuperImportantMessage);
    }

    /**
     * Print the message delivered by the broadcast protocol. We don't need to worry
     * about verifying signatures, because the underlying channels already verify
     * signatures.
     * 
     * @param message
     * @param sender
     */
    @UponBroadcastDeliver
    public void uponDeliverSuperImportantMessage(SuperImportantMessage message, BabelNode sender) {
        LOGGER.info("Received broadcast delivery from: {} from {}", message, sender);
    }

    /**
     * When this timer is triggered, stop sendind messages after
     * {@code this.cooldownTime}.
     * 
     * @param stopTimer
     */
    @UponBabelEvent
    public void uponStopTimer(StopTimer stopTimer) {
        LOGGER.info("Stopping publications");
        broadcastTimer.cancel(true);

        LOGGER.info("Stopping sending messages... will terminate in {} ms", cooldownTime);
        runtime.setUpTimer(new ExitTimer(), cooldownTime, TimeUnit.MILLISECONDS);
    }

    /**
     * When this timer is triggered, exit the program.
     * 
     * @param exitTimer
     */
    @UponBabelEvent
    public void uponExitTimer(ExitTimer exitTimer) {
        LOGGER.info("Exiting...");
        System.exit(0);
    }

    private String randomCapitalLetters() {
        return random.ints(LEFT_LIMIT, RIGHT_LIMIT + 1).limit(this.payloadSize)
                .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append).toString();
    }
}
