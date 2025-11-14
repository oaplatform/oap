# OAP Alert Slack

Slack integration for the OAP alerting system, enabling delivery of system alerts and notifications to Slack channels.

## Overview

OAP Alert Slack provides Slack channel integration for the OAP alerting framework. It allows alerts to be automatically delivered to Slack channels with:
- Alert state change notifications
- Color-coded alert severity
- Channel and mention customization
- Message formatting with details
- Attachment support for rich formatting
- Integration with alert monitoring systems

## Maven Coordinates

```xml
<dependency>
    <groupId>oap</groupId>
    <artifactId>oap-alert-slack</artifactId>
    <version>${oap.version}</version>
</dependency>
```

## Key Features

- **Channel Integration** - Send alerts to configured Slack channels
- **Formatted Messages** - Rich message formatting with attachments
- **Color Coding** - Visual severity indicators (red for alert, green for resolved)
- **State Tracking** - Only send notifications on state changes
- **Webhook Support** - HTTP webhook-based integration
- **Configuration** - Flexible channel and user mention configuration
- **Error Handling** - Graceful error handling with retries

## Key Classes

- `SlackMessenger` - Main messenger implementation for Slack
- `SlackMessageTransport` - Transport layer for Slack webhook delivery
- `Messenger` - Base interface for message delivery
- `Alert` - Alert object containing alert information
- `MessageStream` - Stream for async message delivery

## Quick Example

```java
import oap.alert.slack.SlackMessenger;
import oap.alert.slack.SlackMessageTransport;
import oap.alert.Alert;
import oap.alert.MessageStream;

// Create Slack transport
SlackMessageTransport transport = new SlackMessageTransport(
    "https://hooks.slack.com/services/YOUR/WEBHOOK/URL"
);

// Create message stream
MessageStream<Payload> stream = new MessageStream<>();

// Create messenger
SlackMessenger messenger = new SlackMessenger(
    "#alerts",           // Channel
    "AlertBot",          // Username
    stream               // Message stream
);

// Send alert
Alert alert = new Alert()
    .setName("database_connection")
    .setStatus("CRITICAL")
    .setMessage("Database connection lost");

messenger.send("prod-server", "db-monitor", alert, true);

// Message sent to Slack: @alerts Database connection lost
```

## Configuration

Enable OAP Alert Slack in `oap-module.yaml`:

```yaml
dependsOn:
  - oap-alert-slack
  - oap-alert  # Parent module required
```

Configure in `application.conf`:

```hocon
oap-alert-slack {
    # Slack webhook URL
    webhookUrl = "https://hooks.slack.com/services/YOUR/WEBHOOK/URL"
    
    # Default channel
    channel = "#alerts"
    
    # Bot username in Slack
    username = "OAP-AlertBot"
    
    # Enable message batching
    batchMessages = true
    
    # Batch timeout (ms)
    batchTimeoutMs = 5000
    
    # Retry configuration
    maxRetries = 3
    retryDelayMs = 1000
}
```

## Alert Severity Levels

### Critical (Red)
```
Alert Status: CRITICAL
Slack Color: #FF0000 (Red)
Example: Database offline, memory critical
```

### Warning (Yellow/Orange)
```
Alert Status: WARNING
Slack Color: #FFA500 (Orange)
Example: High memory usage, slow response
```

### Resolved (Green)
```
Alert Status: OK / RESOLVED
Slack Color: #00FF00 (Green)
Example: Service back online
```

## Message Format

Alerts are formatted as Slack attachments with:

```
Channel: #alerts
User: OAP-AlertBot

[Attachment]
Title: Database Monitor
Color: Red (Critical)
Fields:
  - Host: prod-server
  - Component: db-monitor
  - Alert: database_connection
  - Status: CRITICAL
  - Message: Database connection lost
  - Timestamp: 2024-11-14 10:30:00
```

## Integration with Alert Framework

```java
import oap.alert.Alert;
import oap.alert.AlertManager;
import oap.alert.slack.SlackMessenger;

// Create alert manager
AlertManager manager = new AlertManager();

// Register Slack messenger
SlackMessenger slack = createSlackMessenger();
manager.register(slack);

// Post alert
manager.post("api-server", "rate-limiter", new Alert()
    .setName("high_request_rate")
    .setStatus("WARNING")
    .setMessage("Request rate exceeded threshold"));
```

## Webhook Setup

### 1. Create Slack App
1. Go to https://api.slack.com/apps
2. Click "Create New App"
3. Select "From scratch"
4. Fill in app name and workspace

### 2. Enable Incoming Webhooks
1. In app settings, go to "Incoming Webhooks"
2. Enable Incoming Webhooks
3. Click "Add New Webhook to Workspace"
4. Select target channel (#alerts)
5. Authorize

### 3. Copy Webhook URL
The webhook URL will look like:
```
https://hooks.slack.com/services/T00000000/B00000000/XXXXXXXXXXXXXXXXXXXX
```

### 4. Configure in Application
```hocon
oap-alert-slack.webhookUrl = "https://hooks.slack.com/services/T00000000/B00000000/XXXXXXXXXXXXXXXXXXXX"
```

## Advanced Configuration

### Multiple Channels
```hocon
oap-alert-slack {
    channels = [
        {
            name = "critical"
            webhook = "https://hooks.slack.com/services/.../critical"
            severity = "CRITICAL"
        },
        {
            name = "warnings"
            webhook = "https://hooks.slack.com/services/.../warnings"
            severity = "WARNING"
        }
    ]
}
```

### Custom Message Templates
```java
class CustomSlackMessenger extends SlackMessenger {
    @Override
    protected Payload formatMessage(Alert alert) {
        // Custom formatting logic
        return super.formatMessage(alert);
    }
}
```

### Mention Users/Groups
```java
// Configuration to mention specific users on critical alerts
messenger.setMentionOnCritical("@channel");
messenger.setMentionOnWarning("@team-ops");
```

## Error Handling

Slack integration includes:
- Automatic retry with exponential backoff
- Fallback channels for failed delivery
- Error logging for debugging
- Network timeout handling
- Webhook URL validation

## Performance Considerations

- **Async Delivery** - Messages sent asynchronously
- **Batching** - Multiple alerts can be batched
- **Rate Limiting** - Respects Slack rate limits
- **Queue Depth** - Configurable message queue
- **Memory** - Minimal memory overhead

## Best Practices

1. **Webhook Security** - Keep webhook URL secret
2. **Channel Naming** - Use descriptive channel names
3. **Severity Levels** - Organize channels by severity
4. **Testing** - Test alert delivery before production
5. **Monitoring** - Monitor for delivery failures
6. **Frequency** - Avoid alert spam with proper thresholds

## Integration Example

```java
public class AlertingSystem {
    private final AlertManager alertManager;
    
    public AlertingSystem() {
        alertManager = new AlertManager();
        
        // Add Slack integration
        SlackMessenger slack = new SlackMessenger(
            "#alerts",
            "AlertBot",
            createMessageStream()
        );
        alertManager.register(slack);
    }
    
    public void monitorService(String serviceName) {
        // Service monitoring logic
        if (serviceDown) {
            alertManager.post("monitoring", serviceName, 
                new Alert()
                    .setName(serviceName + "_down")
                    .setStatus("CRITICAL")
                    .setMessage("Service " + serviceName + " is down"));
        }
    }
}
```

## Dependencies

- **Slack API Library** - flowctrl/slack-api for Slack integration
- **OAP Alert Framework** - Core alerting system
- **OAP Stdlib** - Core utilities

## Related Modules

- `oap-alert` - Core alerting framework
- `oap-alertmanager` - Alert management and routing
- `oap-prometheus` - Prometheus metrics integration
- `oap-application` - Application framework

## Testing

See `oap-alert-slack/src/test/` for integration tests and examples.

## Troubleshooting

### Webhook Not Delivering
- Verify webhook URL is correct
- Check Slack workspace permissions
- Monitor network connectivity
- Check logs for error messages

### Too Many Alerts
- Adjust alert thresholds
- Implement alert deduplication
- Use alert batching
- Filter non-critical alerts

### Message Formatting Issues
- Verify message content is UTF-8
- Check special characters handling
- Review attachment structure
- Check Slack API limits
