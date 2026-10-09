package oap.mail.message;

import lombok.ToString;
import oap.json.AbstractProperties;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

@ToString
public abstract class AbstractEvent extends AbstractProperties implements Serializable {
    public static final String TEMPLATE_NAME = "templateName";

    @Serial
    private static final long serialVersionUID = -8524075652105735488L;

    public final int version = 1;

    public final String name;
    public String templateName;
    public String fromAddress;
    public String fromPersonal;
    public String toAddress;
    public List<String> toAddresses;
    public String toPersonal;

    protected AbstractEvent( String name ) {
        this.name = name;
    }
}
