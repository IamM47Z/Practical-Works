package pt.isec.cmi.rest.server.shared.data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

public record CodeData(int id, String code, int lifespanMinutes, EventData eventData, UserData ownerData, LocalDateTime startTime) implements Serializable
{
    @Serial
    private static final long serialVersionUID = 2L;

    @Override
    public String toString()
    {
        return "CodeData{" +
                "id=" + id +
                ", code='" + code + '\'' +
                ", lifespanMinutes=" + lifespanMinutes +
                ", eventData=" + eventData +
                ", ownerData=" + ownerData +
                ", startTime=" + startTime +
                '}';
    }
}
