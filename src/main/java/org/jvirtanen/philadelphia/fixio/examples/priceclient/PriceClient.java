package org.jvirtanen.philadelphia.fixio.examples.priceclient;

import static com.paritytrading.philadelphia.fix44.FIX44Enumerations.*;
import static com.paritytrading.philadelphia.fix44.FIX44MsgTypes.*;
import static com.paritytrading.philadelphia.fix44.FIX44Tags.*;

import com.paritytrading.philadelphia.FIXConfig;
import com.paritytrading.philadelphia.FIXMessage;
import com.paritytrading.philadelphia.FIXMessageListener;
import com.paritytrading.philadelphia.FIXSession;
import com.paritytrading.philadelphia.FIXStatusListener;
import com.paritytrading.philadelphia.FIXValue;
import com.paritytrading.philadelphia.FIXVersion;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.StandardSocketOptions;
import java.nio.channels.SocketChannel;

class PriceClient implements FIXMessageListener, FIXStatusListener {

    private static final int MAX_QUOTE_COUNT = 100_000;

    private static final FIXConfig CONFIG = new FIXConfig.Builder()
        .setVersion(FIXVersion.FIX_4_4)
        .setSenderCompID("Client.CompID")
        .setTargetCompID("Server.CompID")
        .setHeartBtInt(30)
        .build();

    private FIXSession session;

    private FIXMessage message;

    private String quoteReqId;

    private int counter;

    private long startTimeNanos;

    private boolean finished;

    PriceClient(SocketChannel channel) {
        session = new FIXSession(channel, CONFIG, this, this);

        message = session.create();

        quoteReqId = Long.toHexString(System.currentTimeMillis());

        counter = 0;

        finished = false;
    }

    @Override
    public void message(FIXMessage message) throws IOException {
        FIXValue msgType = message.getMsgType();

        if (msgType.length() != 1 || msgType.asChar() != Quote)
            return;

        if (startTimeNanos == 0)
            startTimeNanos = System.nanoTime();

        counter++;

        if (counter % 10_000 == 0)
            System.err.println("Read " + counter + " Quotes");

        if (counter > MAX_QUOTE_COUNT && !finished) {
            finished = true;

            long timeMillis = (System.nanoTime() - startTimeNanos) / 1_000_000;
            System.err.printf("Read %s Quotes in %s ms, ~%s Quotes/sec\n", counter, timeMillis,
                    counter * 1000.0 / timeMillis);

            sendQuoteCancel();
        }
    }

    @Override
    public void close(FIXSession session, String message) {
    }

    @Override
    public void sequenceReset(FIXSession session) {
    }

    @Override
    public void tooLowMsgSeqNum(FIXSession session, long receivedMsgSeqNum, long expectedMsgSeqNum) {
    }

    @Override
    public void heartbeatTimeout(FIXSession session) {
    }

    @Override
    public void reject(FIXSession session, FIXMessage message) {
    }

    @Override
    public void logon(FIXSession session, FIXMessage message) throws IOException {
        sendQuoteRequest();
    }

    @Override
    public void logout(FIXSession session, FIXMessage message) {
    }

    void run() throws IOException {
        int i = 0;

        session.sendLogon(true);

        while (!finished) {
            if (session.receive() < 0)
                break;

            if (i % 100_000 == 0) {
                session.updateCurrentTimestamp();
                session.keepAlive();
            }

            i++;
        }
    }

    private void sendQuoteRequest() throws IOException {
        session.prepare(message, QuoteRequest);

        message.addField(QuoteReqID).setString(quoteReqId);
        message.addField(ClOrdID).setString(quoteReqId + counter);
        message.addField(NoRelatedSym).setInt(2);
        message.addField(Symbol).setString("EUR/USD");
        message.addField(SecurityType).setString(SecurityTypeValues.ForeignExchangeContract);
        message.addField(Symbol).setString("EUR/CHF");
        message.addField(SecurityType).setString(SecurityTypeValues.ForeignExchangeContract);
        message.addField(QuoteRequestType).setInt(QuoteRequestTypeValues.Automatic);

        session.send(message);
    }

    private void sendQuoteCancel() throws IOException {
        session.prepare(message, QuoteCancel);

        message.addField(QuoteID).setString("*");
        message.addField(QuoteCancelType).setInt(QuoteCancelTypeValues.CancelAllQuotes);
        message.addField(QuoteReqID).setString(quoteReqId);

        session.send(message);
    }

    public static void main(String[] args) throws IOException {
        SocketChannel channel = SocketChannel.open();

        channel.connect(new InetSocketAddress("127.0.0.1", 10101));
        channel.setOption(StandardSocketOptions.TCP_NODELAY, true);
        channel.configureBlocking(false);

        new PriceClient(channel).run();
    }

}
