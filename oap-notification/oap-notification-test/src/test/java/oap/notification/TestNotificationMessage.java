package oap.notification;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.io.Serial;
import java.io.Serializable;

@EqualsAndHashCode
@ToString
@AllArgsConstructor
public class TestNotificationMessage implements Serializable {
    @Serial
    private static final long serialVersionUID = -4166315788080834194L;

    public final String value;
}
