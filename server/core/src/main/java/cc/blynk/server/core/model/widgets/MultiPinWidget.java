package cc.blynk.server.core.model.widgets;

import cc.blynk.server.core.model.DataStream;
import cc.blynk.server.core.model.enums.PinType;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Arrays;
import java.util.StringJoiner;

import static cc.blynk.utils.StringUtils.BODY_SEPARATOR;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 02.11.15.
 */
public abstract class MultiPinWidget extends Widget implements AppSyncWidget {

    public int deviceId;

    @JsonProperty("pins") //todo "pins" for back compatibility
    public DataStream[] dataStreams;

    @Override
    public boolean updateIfSame(int deviceId, byte pinIn, PinType type, String value) {
        boolean isSame = false;
        if (this.dataStreams != null && this.deviceId == deviceId) {
            for (DataStream dataStream : this.dataStreams) {
                if (dataStream.isSame(pinIn, type)) {
                    dataStream.value = value;
                    isSame = true;
                }
            }
        }
        return isSame;
    }

    @Override
    public void updateIfSame(Widget widget) {
        if (widget instanceof MultiPinWidget) {
            MultiPinWidget multiPinWidget = (MultiPinWidget) widget;
            if (multiPinWidget.dataStreams != null && multiPinWidget.deviceId == this.deviceId) {
                for (DataStream dataStream : multiPinWidget.dataStreams) {
                    updateIfSame(multiPinWidget.deviceId, dataStream.pin, dataStream.pinType, dataStream.value);
                }
            }
        }
    }

    @Override
    public boolean isSame(int deviceId, byte pinIn, PinType pinType) {
        if (dataStreams != null && this.deviceId == deviceId) {
            for (DataStream dataStream : dataStreams) {
                if (dataStream.isSame(pinIn, pinType)) {
                    return true;
                }
            }
        }
        return false;
    }

    public abstract boolean isSplitMode();

    public String makeHardwareBody(byte pinIn, PinType pinType) {
        if (dataStreams == null) {
            return null;
        }
        if (isSplitMode()) {
            for (DataStream dataStream : dataStreams) {
                if (dataStream.isSame(pinIn, pinType)) {
                    return dataStream.makeHardwareBody();
                }
            }
        } else {
            if (dataStreams[0].notEmpty()) {
                StringBuilder sb = new StringBuilder(dataStreams[0].makeHardwareBody());
                for (int i = 1; i < dataStreams.length; i++) {
                    sb.append(BODY_SEPARATOR).append(dataStreams[i].value);
                }
                return sb.toString();
            }
        }
        return null;
    }

    @Override
    public String getValue(byte pinIn, PinType pinType) {
        if (dataStreams != null) {
            for (DataStream dataStream : dataStreams) {
                if (dataStream.isSame(pinIn, pinType)) {
                    return dataStream.value;
                }
            }
        }
        return null;
    }

    @Override
    public void append(StringBuilder sb, int deviceId) {
        if (dataStreams != null && this.deviceId == deviceId) {
            for (DataStream dataStream : dataStreams) {
                append(sb, dataStream.pin, dataStream.pinType, getModeType());
            }
        }
    }

    @Override
    public String getJsonValue() {
        if (dataStreams == null) {
            return "[]";
        }
        StringJoiner sj = new StringJoiner(",", "[", "]");
        for (DataStream dataStream : dataStreams) {
            if (dataStream.value == null) {
                sj.add("\"\"");
            } else {
                sj.add("\"" + dataStream.value + "\"");
            }
        }
        return sj.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MultiPinWidget)) return false;
        if (!super.equals(o)) return false;

        MultiPinWidget that = (MultiPinWidget) o;

        if (deviceId != that.deviceId) return false;
        return Arrays.equals(pins, that.pins);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + deviceId;
        result = 31 * result + Arrays.hashCode(pins);
        return result;
    }
}
