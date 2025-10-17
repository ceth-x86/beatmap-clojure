#!/usr/bin/env python3
"""
Spotify Access Token Generator

This script helps you generate a Spotify access token for use with the Beatmap application.
It uses the Authorization Code Flow with PKCE for enhanced security.
"""

import base64
import hashlib
import json
import secrets
import webbrowser
from http.server import BaseHTTPRequestHandler
from http.server import HTTPServer
from urllib.parse import parse_qs
from urllib.parse import urlencode
from urllib.parse import urlparse

# Configuration
CLIENT_ID = ""  # Fill this with your Spotify Client ID
REDIRECT_URI = "http://127.0.0.1:8888/callback"  # Spotify allows http for 127.0.0.1
SCOPES = "user-follow-read user-top-read"

# Global variable to store the authorization code
auth_code = None
access_token = None


def generate_code_verifier():
    """Generate a code verifier for PKCE."""
    return base64.urlsafe_b64encode(secrets.token_bytes(32)).decode("utf-8").rstrip("=")


def generate_code_challenge(verifier):
    """Generate a code challenge from the verifier."""
    digest = hashlib.sha256(verifier.encode("utf-8")).digest()
    return base64.urlsafe_b64encode(digest).decode("utf-8").rstrip("=")


class CallbackHandler(BaseHTTPRequestHandler):
    """HTTP request handler for OAuth callback."""

    def do_GET(self):
        global auth_code

        # Parse the authorization code from the callback URL
        query_components = parse_qs(urlparse(self.path).query)

        if "code" in query_components:
            auth_code = query_components["code"][0]

            # Send success response
            self.send_response(200)
            self.send_header("Content-type", "text/html")
            self.end_headers()
            self.wfile.write(b"""
                <html>
                <head><title>Success!</title></head>
                <body>
                    <h1>Authorization successful!</h1>
                    <p>You can close this window and return to the terminal.</p>
                </body>
                </html>
            """)
        else:
            # Send error response
            error = query_components.get("error", ["Unknown error"])[0]
            self.send_response(400)
            self.send_header("Content-type", "text/html")
            self.end_headers()
            self.wfile.write(
                f"""
                <html>
                <head><title>Error</title></head>
                <body>
                    <h1>Authorization failed!</h1>
                    <p>Error: {error}</p>
                </body>
                </html>
            """.encode()
            )

    def log_message(self, format, *args):
        """Suppress log messages."""


def get_authorization_code(client_id, redirect_uri, scopes, code_challenge):
    """Step 1: Get authorization code by opening browser."""
    auth_url = "https://accounts.spotify.com/authorize?" + urlencode(
        {
            "client_id": client_id,
            "response_type": "code",
            "redirect_uri": redirect_uri,
            "scope": scopes,
            "code_challenge_method": "S256",
            "code_challenge": code_challenge,
        }
    )

    print("Opening browser for authorization...")
    print("If browser doesn't open, visit this URL manually:")
    print(f"\n{auth_url}\n")

    webbrowser.open(auth_url)

    # Start local server to receive callback
    server = HTTPServer(("127.0.0.1", 8888), CallbackHandler)
    print("Waiting for authorization... (listening on http://127.0.0.1:8888)")

    # Handle one request (the callback)
    server.handle_request()
    server.server_close()

    return auth_code


def exchange_code_for_token(client_id, code, code_verifier, redirect_uri):
    """Step 2: Exchange authorization code for access token."""
    import urllib.request

    token_url = "https://accounts.spotify.com/api/token"

    data = urlencode(
        {
            "client_id": client_id,
            "grant_type": "authorization_code",
            "code": code,
            "redirect_uri": redirect_uri,
            "code_verifier": code_verifier,
        }
    ).encode()

    req = urllib.request.Request(token_url, data=data, method="POST")
    req.add_header("Content-Type", "application/x-www-form-urlencoded")

    try:
        with urllib.request.urlopen(req) as response:
            result = json.loads(response.read().decode())
            return result.get("access_token"), result.get("refresh_token"), result.get("expires_in")
    except urllib.error.HTTPError as e:
        print(f"Error exchanging code for token: {e.code}")
        print(e.read().decode())
        return None, None, None


def main():
    """Main function to run the token generation flow."""
    global CLIENT_ID

    print("=" * 60)
    print("Spotify Access Token Generator for Beatmap")
    print("=" * 60)
    print()

    # Get Client ID if not set
    if not CLIENT_ID:
        print("First, create a Spotify app at:")
        print("https://developer.spotify.com/dashboard")
        print()
        CLIENT_ID = input("Enter your Spotify Client ID: ").strip()

    if not CLIENT_ID:
        print("Error: Client ID is required!")
        return

    print()
    print("Starting OAuth flow...")
    print()

    # Generate PKCE codes
    code_verifier = generate_code_verifier()
    code_challenge = generate_code_challenge(code_verifier)

    # Step 1: Get authorization code
    auth_code = get_authorization_code(CLIENT_ID, REDIRECT_URI, SCOPES, code_challenge)

    if not auth_code:
        print("Error: Failed to get authorization code!")
        return

    print()
    print("✓ Authorization successful!")
    print()

    # Step 2: Exchange code for token
    print("Exchanging authorization code for access token...")
    access_token, refresh_token, expires_in = exchange_code_for_token(CLIENT_ID, auth_code, code_verifier, REDIRECT_URI)

    if not access_token:
        print("Error: Failed to get access token!")
        return

    print()
    print("=" * 60)
    print("SUCCESS! Your Spotify Access Token:")
    print("=" * 60)
    print()
    print(access_token)
    print()
    print("=" * 60)
    print()
    print("This token expires in:", expires_in, "seconds (approximately", expires_in // 3600, "hours)")
    print()
    print("Add this token to your config/local.edn file:")
    print()
    print("{:secrets")
    print(' {:spotify-token "' + access_token + '"}}')
    print()

    if refresh_token:
        print("Refresh token (save this for later):")
        print(refresh_token)
        print()

    print("=" * 60)


if __name__ == "__main__":
    main()
