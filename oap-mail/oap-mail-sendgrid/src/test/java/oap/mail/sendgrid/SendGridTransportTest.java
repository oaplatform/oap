package oap.mail.sendgrid;

import oap.json.Binder;
import oap.mail.Attachment;
import oap.mail.MailAddress;
import oap.mail.Message;
import org.testng.annotations.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class SendGridTransportTest {
    @Test
    public void mailPayloadPreservesSendGridContract() {
        var message = new Message( "Subject", "<b>HTML</b>", List.of(
            new Attachment( "text/plain", "aGVsbG8=", "cid", "test.txt", "Test" ) ) );
        message.from = new MailAddress( "from@example.org" );
        var mapper = Binder.json.getMapper();
        var actual = mapper.valueToTree( SendGridTransport.payload( message, new MailAddress( "to@example.org" ) ) );
        assertThat( actual ).isEqualTo( mapper.readTree( """
            {"from":{"email":"from@example.org"},"subject":"Subject",
             "personalizations":[{"to":[{"email":"to@example.org"}]}],
             "content":[{"type":"text/html","value":"<b>HTML</b>"}],
             "attachments":[{"content":"aGVsbG8=","content_id":"cid","disposition":"attachment",
                             "filename":"test.txt","type":"text/plain"}]}
            """ ) );
    }

    @Test
    public void absentAttachmentFieldsAreOmitted() {
        var message = new Message( "Subject", "Body", List.of( new Attachment( "text/plain", "aGVsbG8=" ) ) );
        message.from = new MailAddress( "from@example.org" );
        var tree = Binder.json.getMapper().valueToTree( SendGridTransport.payload( message, new MailAddress( "to@example.org" ) ) );
        assertThat( tree.get( "attachments" ).get( 0 ).has( "content_id" ) ).isFalse();
        assertThat( tree.get( "attachments" ).get( 0 ).has( "filename" ) ).isFalse();
        message.attachments.clear();
        assertThat( SendGridTransport.payload( message, new MailAddress( "to@example.org" ) ) ).doesNotContainKey( "attachments" );
    }
}
