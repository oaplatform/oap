## OAP SendGrid Mail
Add module to `oap-module.yaml`

    dependsOn:
        - oap-mail-sendgrid

Go to https://app.sendgrid.com/settings/api_keys and click
`Create API key`.

Enable and configure SendGrid in `application.conf`

    profiles = [
        oap-mail-sendgrid
    ]
 
    oap-mail-smtp-transport.parameters.sendGridKey = [NEW KEY]
    oap-mail-queue.paramaters.transport = "@service:oap-mail-sendgrid-trasport"

