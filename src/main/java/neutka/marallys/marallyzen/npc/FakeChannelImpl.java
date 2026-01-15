package neutka.marallys.marallyzen.npc;

import io.netty.channel.*;

import java.net.SocketAddress;

/**
 * Fake channel implementation for fake player entities.
 * Based on Denizen's FakeChannelImpl but adapted for NeoForge.
 */
public class FakeChannelImpl extends AbstractChannel {

    private final ChannelConfig config = new DefaultChannelConfig(this);

    protected FakeChannelImpl(Channel parent) {
        super(parent);
    }

    @Override
    public ChannelConfig config() {
        config.setAutoRead(true);
        return config;
    }

    @Override
    protected AbstractUnsafe newUnsafe() {
        return null;
    }

    @Override
    protected boolean isCompatible(EventLoop eventLoop) {
        return false;
    }

    @Override
    protected SocketAddress localAddress0() {
        return null;
    }

    @Override
    protected SocketAddress remoteAddress0() {
        return null;
    }

    @Override
    protected void doBind(SocketAddress socketAddress) throws Exception {
        // Do nothing
    }

    @Override
    protected void doDisconnect() throws Exception {
        // Do nothing
    }

    @Override
    protected void doClose() throws Exception {
        // Do nothing
    }

    @Override
    protected void doBeginRead() throws Exception {
        // Do nothing
    }

    @Override
    protected void doWrite(ChannelOutboundBuffer channelOutboundBuffer) throws Exception {
        // Do nothing
    }

    @Override
    public boolean isOpen() {
        return false;
    }

    @Override
    public boolean isActive() {
        return false;
    }

    @Override
    public ChannelMetadata metadata() {
        return new ChannelMetadata(true);
    }
}



