package no.jckf.dhsupport.core.handler;

import no.jckf.dhsupport.core.DhSupport;
import no.jckf.dhsupport.core.bytestream.Decoder;
import no.jckf.dhsupport.core.bytestream.Encoder;
import no.jckf.dhsupport.core.message.plugin.CloseReasonMessage;
import no.jckf.dhsupport.core.message.plugin.LevelInitMessage;
import no.jckf.dhsupport.core.message.plugin.PluginMessage;
import no.jckf.dhsupport.core.message.plugin.RemotePlayerConfigMessage;
import org.junit.Before;
import org.junit.Test;

import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.Assert.*;

public class PluginMessageHandlerTest
{
    private DhSupport dhSupport;
    private PluginMessageHandler messageHandler;
    private byte[] lastSentData;

    @Before
    public void setUp()
    {
        dhSupport = new DhSupport("0.16.0-SNAPSHOT", "1.16.5");
        dhSupport.setLogger(Logger.getLogger("TestLogger"));
        dhSupport.setPluginMessageSender((recipientUuid, channel, message) -> {
            lastSentData = message;
        });

        messageHandler = dhSupport.getPluginMessageHandler();
    }

    @Test
    public void testProtocolVersionMatching()
    {
        assertEquals(16, messageHandler.protocolVersion);
    }

    @Test
    public void testRejectOutdatedClientProtocol()
    {
        UUID senderUuid = UUID.randomUUID();

        Encoder encoder = new Encoder(16);
        encoder.writeShort((short) 15); // Outdated client protocol version
        encoder.writeShort((short) 3);  // Message type

        PluginMessage response = messageHandler.readPluginMessage(senderUuid, encoder.toByteArray());

        assertTrue(response instanceof CloseReasonMessage);
        CloseReasonMessage closeReason = (CloseReasonMessage) response;
        assertEquals("Client is outdated.", closeReason.getReason());
    }

    @Test
    public void testRejectFutureClientProtocol()
    {
        UUID senderUuid = UUID.randomUUID();

        Encoder encoder = new Encoder(16);
        encoder.writeShort((short) 17); // Newer client protocol version
        encoder.writeShort((short) 3);  // Message type

        PluginMessage response = messageHandler.readPluginMessage(senderUuid, encoder.toByteArray());

        assertTrue(response instanceof CloseReasonMessage);
        CloseReasonMessage closeReason = (CloseReasonMessage) response;
        assertEquals("Server is outdated.", closeReason.getReason());
    }

    @Test
    public void testLevelInitMessageEncodingDecoding()
    {
        LevelInitMessage message = new LevelInitMessage();
        message.setNamespacedKey("minecraft:overworld");
        message.setServerKey("my-server");
        message.setLevelKey("world");
        message.setTime(123456789L);

        Encoder encoder = new Encoder(128);
        message.encode(encoder);

        Decoder decoder = new Decoder(encoder.toByteArray());
        LevelInitMessage decodedMessage = new LevelInitMessage();
        decodedMessage.decode(decoder);

        assertEquals("minecraft:overworld", decodedMessage.getNamespacedKey());
        assertEquals("my-server", decodedMessage.getServerKey());
        assertEquals("world", decodedMessage.getLevelKey());
        assertEquals(123456789L, decodedMessage.getTime());
    }

    @Test
    public void testRemotePlayerConfigMessageEncodingDecoding()
    {
        RemotePlayerConfigMessage configMessage = new RemotePlayerConfigMessage();
        configMessage.setDistantGenerationEnabled(true);
        configMessage.setRenderDistance(32);
        configMessage.setBorderCenterX(0);
        configMessage.setBorderCenterZ(0);
        configMessage.setBorderRadius(1000);
        configMessage.setClientLodRequestConcurrencyLimit(4);
        configMessage.setRealTimeUpdatesEnabled(true);
        configMessage.setRealTimeUpdateRadius(16);
        configMessage.setLoginDataSyncEnabled(true);
        configMessage.setLoginDataSyncRadius(8);
        configMessage.setLoginDataSyncRcLimit(2);
        configMessage.setMaxDataTransferSpeed(100);

        Encoder encoder = new Encoder(256);
        configMessage.encode(encoder);

        Decoder decoder = new Decoder(encoder.toByteArray());
        RemotePlayerConfigMessage decodedMessage = new RemotePlayerConfigMessage();
        decodedMessage.decode(decoder);

        assertTrue(decodedMessage.getDistantGenerationEnabled());
        assertEquals(32, decodedMessage.getRenderDistance());
        assertEquals(0, decodedMessage.getBorderCenterX());
        assertEquals(0, decodedMessage.getBorderCenterZ());
        assertEquals(1000, decodedMessage.getBorderRadius());
        assertEquals(4, decodedMessage.getClientLodRequestConcurrencyLimit());
        assertEquals(16, decodedMessage.getRealTimeUpdateRadius());
        assertEquals(8, decodedMessage.getLoginDataSyncRadius());
        assertEquals(2, decodedMessage.getLoginDataSyncRcLimit());
        assertEquals(100, decodedMessage.getMaxDataTransferSpeed());
    }
}
