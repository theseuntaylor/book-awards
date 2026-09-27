"""Serves one file as /awards.json with ETag support, logging each request, for e2e/data-refresh.sh.

Usage: python3 serve_awards.py <port> <file-to-serve> <request-log>
The file is re-read on every request, so the E2E script can swap its contents between steps.
"""
import hashlib
import sys
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer

port, served_file, request_log = int(sys.argv[1]), sys.argv[2], sys.argv[3]


class Handler(BaseHTTPRequestHandler):
    def do_GET(self):
        if_none_match = self.headers.get("If-None-Match")
        try:
            with open(served_file, "rb") as f:
                body = f.read()
        except FileNotFoundError:
            return self.respond(404, if_none_match)

        etag = '"%s"' % hashlib.sha1(body).hexdigest()
        if self.path != "/awards.json":
            return self.respond(404, if_none_match)
        if if_none_match == etag:
            return self.respond(304, if_none_match, etag=etag)
        self.respond(200, if_none_match, etag=etag, body=body)

    def respond(self, status, if_none_match, etag=None, body=b""):
        self.send_response(status)
        if etag:
            self.send_header("ETag", etag)
        self.send_header("Content-Length", str(len(body)))
        self.end_headers()
        self.wfile.write(body)
        with open(request_log, "a") as log:
            log.write("%s status=%d if-none-match=%s\n" % (self.path, status, if_none_match or "-"))

    def log_message(self, *args):
        pass


ThreadingHTTPServer(("127.0.0.1", port), Handler).serve_forever()
