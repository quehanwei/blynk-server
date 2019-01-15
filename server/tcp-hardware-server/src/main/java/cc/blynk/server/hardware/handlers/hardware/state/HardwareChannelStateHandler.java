package cc.blynk.server.hardware.handlers.hardware.state;

import cc.blynk.server.Holder;
import cc.blynk.server.core.dao.SessionDao;
import cc.blynk.server.core.model.auth.Session;
import cc.blynk.server.core.model.device.Device;
import cc.blynk.server.core.model.web.product.EventType;
import cc.blynk.server.core.model.web.product.events.system.OfflineEvent;
import cc.blynk.server.core.session.HardwareStateHolder;
import cc.blynk.server.db.ReportingDBManager;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleStateEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.TimeUnit;

import static cc.blynk.server.core.protocol.enums.Command.HARDWARE_LOG_EVENT;
import static cc.blynk.server.internal.StateHolderUtil.getHardState;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 2/20/2015.
 *
 * Removes channel from session in case it became inactive (closed from client side).
 */
@ChannelHandler.Sharable
public class HardwareChannelStateHandler extends ChannelInboundHandlerAdapter {

    private static final Logger log = LogManager.getLogger(HardwareChannelStateHandler.class);

    private final SessionDao sessionDao;
    private final ReportingDBManager reportingDBManager;

    public HardwareChannelStateHandler(Holder holder) {
        this.sessionDao = holder.sessionDao;
        this.reportingDBManager = holder.reportingDBManager;
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        Channel hardwareChannel = ctx.channel();
        HardwareStateHolder state = getHardState(hardwareChannel);
        if (state != null) {
            Session session = sessionDao.getOrgSession(state.orgId);
            if (session != null) {
                Device device = state.device;
                log.trace("Hardware channel disconnect for deviceId {}, token {}.", device.id, device.token);
                sentOfflineMessage(ctx, session, state, device);
            }
        }
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) {
        if (evt instanceof IdleStateEvent) {
            log.trace("State handler. Hardware timeout disconnect. Event : {}. Closing.",
                    ((IdleStateEvent) evt).state());
            ctx.close();
        } else {
            ctx.fireUserEventTriggered(evt);
        }
    }

    private void sentOfflineMessage(ChannelHandlerContext ctx,
                                    Session session, HardwareStateHolder state, Device device) {
        //this is special case.
        //in case hardware quickly reconnects we do not mark it as disconnected
        //as it is already online after quick disconnect.
        //https://github.com/blynkkk/blynk-server/issues/403
        boolean isHardwareConnected = session.isHardwareConnected(device.id);
        if (!isHardwareConnected) {
            log.trace("Changing device status. Device {}", device.id);
            device.disconnected();

            OfflineEvent offlineEvent = state.product.getEventByType(OfflineEvent.class);
            int ignoreOfflineEventPeriod = offlineEvent == null ? 0 : offlineEvent.ignorePeriod;
            if (ignoreOfflineEventPeriod > 0) {
                log.trace("Ignore period is {} for deviceId {}. Delaying event.",
                        ignoreOfflineEventPeriod, device.id);
                ctx.executor().schedule(new DelayedOfflineEvent(device, session),
                        ignoreOfflineEventPeriod, TimeUnit.MILLISECONDS);
            } else {
                reportingDBManager.insertSystemEvent(device.id, EventType.OFFLINE);
                session.sendToSelectedDeviceOnWeb(HARDWARE_LOG_EVENT, 0, EventType.OFFLINE.name(), device.id);
            }
        }

        session.sendOfflineMessageToApps(device.id);
        session.sendOfflineMessageToWeb(device.id);
    }

    private final class DelayedOfflineEvent implements Runnable {

        private final Device device;
        private final Session session;

        DelayedOfflineEvent(Device device, Session session) {
            this.device = device;
            this.session = session;
        }

        @Override
        public void run() {
            log.trace("Wake up for offline delayed event and deviceId {}.");
            //if after wake up we still don't see the device as online, this means we can report the disconnect
            int deviceId = device.id;
            if (device.isOffline()) {
                reportingDBManager.insertSystemEvent(deviceId, EventType.OFFLINE);
                session.sendToSelectedDeviceOnWeb(HARDWARE_LOG_EVENT, 0, EventType.OFFLINE.name(), deviceId);
            } else {
                log.trace("Device {} is already online. No need to report offline event.", deviceId);
            }

        }
    }

}
