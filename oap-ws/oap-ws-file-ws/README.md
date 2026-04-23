# oap-ws-file-ws

HTTP file upload and download service with optional multi-bucket storage. Files are stored on the local filesystem; buckets map logical names to directories.

Depends on: `oap-ws`

## Endpoints

```
POST /file/          Upload a file; returns the stored filename as plain text
GET  /file/?path=… Download a file by its stored path
```

Both endpoints accept an optional `?bucket=name` query parameter to target a specific bucket. The default bucket uses `/tmp` if no bucket path is configured.

### Upload

```bash
# upload to the default bucket
curl -X POST http://localhost:8080/file/ \
     -H "Content-Type: application/json" \
     -d '{"name":"report.csv","content":"base64encodedcontent"}'

# upload to a named bucket
curl -X POST "http://localhost:8080/file/?bucket=reports" \
     -H "Content-Type: application/json" \
     -d '{"content":"base64encodedcontent"}'
```

Response: the stored filename (plain text), e.g. `report.csv` or a generated unique ID if no name was provided.

The request body is validated against the built-in `data.conf` JSON schema (`/oap/ws/file/schema/data.conf`).

### Download

```bash
# download from the default bucket
curl "http://localhost:8080/file/?path=report.csv" -o report.csv

# download from a named bucket
curl "http://localhost:8080/file/?path=report.csv&bucket=reports" -o report.csv
```

Content-Type is inferred from the file extension. Returns 404 if the file does not exist.

## OAP Module Integration

```hocon
name = my-app
dependsOn = [oap-ws-file-ws]
```

Configure bucket directories in `application.conf`:

```hocon
services {
  oap-ws-file.oap-ws-file-bucket-manager.parameters.buckets {
    reports = /var/data/reports
    uploads = /var/data/uploads
  }
}
```

If no buckets are configured, all files go to `/tmp`.
