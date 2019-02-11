package cc.blynk.server.core.model;

import cc.blynk.server.core.model.device.Device;
import cc.blynk.server.core.model.enums.PinType;
import cc.blynk.server.core.model.enums.Theme;
import cc.blynk.server.core.model.enums.WidgetProperty;
import cc.blynk.server.core.model.serialization.JsonParser;
import cc.blynk.server.core.model.serialization.View;
import cc.blynk.server.core.model.storage.key.DeviceStorageKey;
import cc.blynk.server.core.model.storage.value.PinStorageValue;
import cc.blynk.server.core.model.widgets.DeviceCleaner;
import cc.blynk.server.core.model.widgets.MultiPinWidget;
import cc.blynk.server.core.model.widgets.OnePinWidget;
import cc.blynk.server.core.model.widgets.Widget;
import cc.blynk.server.core.model.widgets.controls.Timer;
import cc.blynk.server.core.model.widgets.notifications.Mail;
import cc.blynk.server.core.model.widgets.notifications.Twitter;
import cc.blynk.server.core.model.widgets.others.eventor.Eventor;
import cc.blynk.server.core.model.widgets.others.webhook.WebHook;
import cc.blynk.server.core.model.widgets.outputs.graph.GraphDataStream;
import cc.blynk.server.core.model.widgets.outputs.graph.Superchart;
import cc.blynk.server.core.model.widgets.ui.reporting.ReportingWidget;
import cc.blynk.server.core.model.widgets.ui.tiles.DeviceTiles;
import cc.blynk.server.core.model.widgets.ui.tiles.TileTemplate;
import cc.blynk.server.core.protocol.exceptions.IllegalCommandException;
import cc.blynk.server.core.protocol.exceptions.JsonException;
import cc.blynk.server.workers.timer.TimerWorker;
import com.fasterxml.jackson.annotation.JsonView;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static cc.blynk.server.core.model.widgets.Widget.EMPTY_WIDGETS;

/**
 * User: ddumanskiy
 * Date: 21.11.13
 * Time: 13:04
 */
public class DashBoard {

    private static final Logger log = LogManager.getLogger(DashBoard.class);

    //-1 means this is not child project
    private static final int IS_PARENT_DASH = -1;
    private static final String DEFAULT_NAME = "New Project";

    public int id;

    public int parentId = IS_PARENT_DASH;

    public boolean isPreview;

    public volatile String name;

    public long createdAt;

    public volatile long updatedAt;

    public volatile Widget[] widgets = EMPTY_WIDGETS;

    public volatile Theme theme = Theme.Blynk;

    public volatile boolean keepScreenOn;

    public volatile boolean isAppConnectedOn;

    public volatile boolean isNotificationsOff;

    public volatile boolean isShared;

    public volatile boolean isActive;

    public volatile boolean widgetBackgroundOn;

    public int color = -1;

    public boolean isDefaultColor = true;

    @JsonView(View.Private.class)
    public volatile String sharedToken;

    public DashBoard() {
        this.name = "New Project";
    }

    public boolean isChild() {
        return parentId != IS_PARENT_DASH;
    }

    public String getNameOrEmpty() {
        return name == null ? "" : name;
    }

    public String getNameOrDefault() {
        return name == null ? DEFAULT_NAME : name;
    }

    public void activate() {
        isActive = true;
        updatedAt = System.currentTimeMillis();
    }

    public void deactivate() {
        isActive = false;
        updatedAt = System.currentTimeMillis();
    }

    public Widget findWidgetByPin(int deviceId, short pin, PinType pinType) {
        for (Widget widget : widgets) {
            if (widget.isSame(deviceId, pin, pinType)) {
                return widget;
            }
        }
        return null;
    }

    public WebHook findWebhookByPin(int deviceId, short pin, PinType pinType) {
        for (Widget widget : widgets) {
            if (widget instanceof WebHook) {
                WebHook webHook = (WebHook) widget;
                if (webHook.isSameWebHook(deviceId, pin, pinType)) {
                    return webHook;
                }
            }
        }
        return null;
    }

    public int getWidgetIndexByIdOrThrow(long id) {
        return Widget.getWidgetIndexByIdOrThrow(widgets, id);
    }

    public boolean hasWidgetsByDeviceId(int deviceId) {
        for (Widget widget : widgets) {
            if (widget.isAssignedToDevice(deviceId)) {
                return true;
            }
        }
        return false;
    }

    public Widget getWidgetByIdOrThrow(long id) {
        return widgets[getWidgetIndexByIdOrThrow(id)];
    }

    public DeviceTiles getDeviceTilesByIdOrThrow(long id) {
        Widget widget = widgets[getWidgetIndexByIdOrThrow(id)];
        if (!(widget instanceof DeviceTiles)) {
            throw new JsonException("Income widget id is not DeviceTiles.");
        }
        return (DeviceTiles) widget;
    }

    public Widget getWidgetById(long id) {
        return getWidgetById(widgets, id);
    }

    public Widget getWidgetByIdInDeviceTilesOrThrow(long id) {
        for (Widget widget : widgets) {
            if (widget instanceof DeviceTiles) {
                DeviceTiles deviceTiles = (DeviceTiles) widget;
                Widget widgetInDeviceTiles = deviceTiles.getWidgetById(id);
                if (widgetInDeviceTiles != null) {
                    return widgetInDeviceTiles;
                }
            }
        }
        throw new IllegalCommandException("Widget with passed id not found.");
    }

    private static Widget getWidgetById(Widget[] widgets, long id) {
        for (Widget widget : widgets) {
            if (widget.id == id) {
                return widget;
            }
        }
        return null;
    }

    public Eventor getEventorWidget() {
        return getWidgetByType(Eventor.class);
    }

    public Twitter getTwitterWidget() {
        return getWidgetByType(Twitter.class);
    }

    public Mail getMailWidget() {
        return getWidgetByType(Mail.class);
    }

    public ReportingWidget getReportingWidget() {
        return getWidgetByType(ReportingWidget.class);
    }

    @SuppressWarnings("unchecked")
    public <T> T getWidgetByType(Class<T> clazz) {
        for (Widget widget : widgets) {
            if (clazz.isInstance(widget)) {
                return (T) widget;
            }
        }
        return null;
    }

    public String buildPMMessage(int deviceId) {
        StringBuilder sb = new StringBuilder("pm");
        for (Widget widget : widgets) {
            widget.append(sb, deviceId);
        }
        if (sb.length() == 2) {
            return null;
        }
        return sb.toString();
    }

    public void eraseWidgetValues() {
        for (Widget widget : widgets) {
            widget.erase();
        }
    }

    public void eraseWidgetValuesForDevice(int deviceId) {
        for (Widget widget : widgets) {
            if (widget.isAssignedToDevice(deviceId)) {
                widget.erase();
            }
            if (widget instanceof DeviceCleaner) {
                ((DeviceCleaner) widget).deleteDevice(deviceId);
            }
        }
    }

    public void addTimers(TimerWorker timerWorker, int orgId, String email) {
        for (Widget widget : widgets) {
            if (widget instanceof DeviceTiles) {
                timerWorker.add(orgId, email, (DeviceTiles) widget, id);
            } else if (widget instanceof Timer) {
                timerWorker.add(orgId, email, (Timer) widget, id, -1L, -1L);
            } else if (widget instanceof Eventor) {
                timerWorker.add(orgId, email, (Eventor) widget, id);
            }
        }
    }

    //todo add DashboardSettings as Dashboard field
    public void updateSettings(DashboardSettings settings) {
        this.name = settings.name;
        this.isShared = settings.isShared;
        this.theme = settings.theme;
        this.keepScreenOn = settings.keepScreenOn;
        this.isAppConnectedOn = settings.isAppConnectedOn;
        this.isNotificationsOff = settings.isNotificationsOff;
        this.widgetBackgroundOn = settings.widgetBackgroundOn;
        this.color = settings.color;
        this.isDefaultColor = settings.isDefaultColor;
        this.updatedAt = System.currentTimeMillis();
    }

    public void updateFields(DashBoard updatedDashboard) {
        this.name = updatedDashboard.name;
        this.isShared = updatedDashboard.isShared;
        this.theme = updatedDashboard.theme;
        this.keepScreenOn = updatedDashboard.keepScreenOn;
        this.isAppConnectedOn = updatedDashboard.isAppConnectedOn;
        this.isNotificationsOff = updatedDashboard.isNotificationsOff;
        this.widgetBackgroundOn = updatedDashboard.widgetBackgroundOn;
        this.color = updatedDashboard.color;
        this.isDefaultColor = updatedDashboard.isDefaultColor;
        this.widgets = updatedDashboard.widgets;
    }

    public void updateFaceFields(DashBoard parent) {
        this.name = parent.name;
        this.isShared = parent.isShared;
        this.theme = parent.theme;
        this.keepScreenOn = parent.keepScreenOn;
        this.isAppConnectedOn = parent.isAppConnectedOn;
        this.isNotificationsOff = parent.isNotificationsOff;
        this.widgetBackgroundOn = parent.widgetBackgroundOn;
        this.color = parent.color;
        this.isDefaultColor = parent.isDefaultColor;
        //do not update devices by purpose
        //this.devices = parent.devices;
        this.widgets = copyWidgetsAndPreservePrevValues(this.widgets, parent.widgets);
        //export app specific requirement
        for (Widget widget : widgets) {
            widget.isDefaultColor = false;
        }
    }

    public static Widget[] copyWidgetsAndPreservePrevValues(Widget[] oldWidgets, Widget[] newWidgets) {
        ArrayList<Widget> copy = new ArrayList<>(newWidgets.length);
        for (Widget newWidget : newWidgets) {
            Widget oldWidget = getWidgetById(oldWidgets, newWidget.id);

            Widget copyWidget = newWidget.copy();

            //for now erasing only for this types, not sure about DeviceTiles
            if (copyWidget instanceof OnePinWidget
                    || copyWidget instanceof MultiPinWidget
                    || copyWidget instanceof ReportingWidget) {
                copyWidget.erase();
            }

            if (oldWidget != null) {
                copyWidget.updateValue(oldWidget);
            }
            copy.add(copyWidget);
        }

        return copy.toArray(new Widget[newWidgets.length]);
    }

    public TileTemplate getTileTemplate(String templateId) {
        DeviceTiles deviceTiles = getWidgetByType(DeviceTiles.class);
        if (deviceTiles == null) {
            log.warn("Device tiles widget not found.");
            return null;
        }

        TileTemplate tileTemplate = deviceTiles.getTileTemplateByTemplateId(templateId);
        if (tileTemplate == null) {
            log.warn("Could not find templateId {}.", templateId);
            return null;
        }

        return tileTemplate;
    }

    public void fillValues(List<Device> devices) {
        for (Widget widget : widgets) {
            if (widget instanceof DeviceTiles) {
                DeviceTiles deviceTiles = (DeviceTiles) widget;
                deviceTiles.recreateTiles(devices);
                deviceTiles.updateAllValues(devices);
            }
        }
        for (Device device : devices) {
            for (var entry : device.pinStorage.values.entrySet()) {
                DeviceStorageKey key = entry.getKey();
                PinStorageValue value = entry.getValue();
                updateWidgetsValue(device, key.pin, key.pinType, value.lastValue());
            }
        }
    }

    private void updateWidgetsValue(Device device, short pin, PinType type, String value) {
        for (Widget widget : widgets) {
            widget.updateIfSame(device.id, pin, type, value);
        }
    }

    public Widget updateProperty(int deviceId, short pin, WidgetProperty widgetProperty, String propertyValue) {
        Widget widget = null;
        for (Widget dashWidget : widgets) {
            if (dashWidget.isSame(deviceId, pin, PinType.VIRTUAL)) {
                if (dashWidget.setProperty(widgetProperty, propertyValue)) {
                    widget = dashWidget;
                }
            }
        }
        return widget;
    }

    public boolean needRawDataForGraph(short pin, PinType pinType) {
        for (Widget widget : widgets) {
            if (widget instanceof Superchart) {
                Superchart superchart = (Superchart) widget;
                if (superchart.hasLivePeriodsSelected()) {
                    //todo check targetId?
                    for (GraphDataStream graphDataStream : superchart.dataStreams) {
                        if (graphDataStream.dataStream != null && graphDataStream.dataStream.isSame(pin, pinType)) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        DashBoard dashBoard = (DashBoard) o;

        if (id != dashBoard.id) {
            return false;
        }
        if (name != null ? !name.equals(dashBoard.name) : dashBoard.name != null) {
            return false;
        }
        // Probably incorrect - comparing Object[] arrays with Arrays.equals
        return Arrays.equals(widgets, dashBoard.widgets);
    }

    @Override
    public int hashCode() {
        int result = id;
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + Arrays.hashCode(widgets);
        return result;
    }

    @Override
    public String toString() {
        return JsonParser.toJson(this);
    }

}
