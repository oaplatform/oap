package oap.mail.message;

import lombok.ToString;
import oap.json.AbstractProperties;

import java.io.Serial;
import java.io.Serializable;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@ToString
public abstract class AbstractEvent extends AbstractProperties implements Serializable {
    public static final String TEMPLATE_NAME = "templateName";

    @Serial
    private static final long serialVersionUID = -8524075652105735488L;

    public final int version = 1;

    public final String name;
    public final Set<NotificationType> types = new HashSet<>();
    public String templateName;
    public String fromAddress;
    public String fromPersonal;
    public String toAddress;
    public List<String> toAddresses;
    public String toPersonal;

    protected AbstractEvent( Set<NotificationType> types, String name ) {
        this.name = name;

        if( types != null ) {
            this.types.addAll( types );
        }
    }
}
