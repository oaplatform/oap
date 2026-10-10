package oap.mail.message;

import oap.mail.Template;

public enum NotificationType {
    EMAIL( Template.Type.XML ),
    SLACK( Template.Type.TEXT );

    private final Template.Type type;

    NotificationType( Template.Type type ) {
        this.type = type;
    }

    public Template.Type getTemplateType() {
        return type;
    }
}
