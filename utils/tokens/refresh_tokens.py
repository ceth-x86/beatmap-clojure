"""
Automates Apple Music token refresh:
1. Generates developer token
2. Serves HTML page with token embedded
3. Opens browser for user authorization
4. Receives user token via POST callback
5. Updates config/local.edn with both tokens
"""

import os
import re
import sys
import time
import webbrowser
from functools import partial
from http.server import HTTPServer, BaseHTTPRequestHandler
from pathlib import Path
from threading import Timer

import jwt

SCRIPT_DIR = Path(__file__).parent
HTML_PATH = SCRIPT_DIR / "index.html"
CONFIG_PATH = SCRIPT_DIR / "../../config/local.edn"

REQUIRED_ENV_VARS = {
    "APPLE_TEAM_ID": "Apple Developer Team ID",
    "APPLE_KEY_ID": "Apple Music key ID",
    "APPLE_PRIVATE_KEY_PATH": "Path to .p8 private key file",
}


def load_env():
    missing = [
        f"  {var} — {desc}"
        for var, desc in REQUIRED_ENV_VARS.items()
        if not os.environ.get(var)
    ]
    if missing:
        print("Missing environment variables:\n" + "\n".join(missing))
        print("\nSet them or add to utils/tokens/.env (see .env.example)")
        sys.exit(1)

    return (
        os.environ["APPLE_TEAM_ID"],
        os.environ["APPLE_KEY_ID"],
        os.environ["APPLE_PRIVATE_KEY_PATH"],
    )


def generate_developer_token(team_id, key_id, private_key_path):
    with open(private_key_path) as f:
        private_key = f.read()

    return jwt.encode(
        {
            "iss": team_id,
            "iat": int(time.time()),
            "exp": int(time.time()) + 3600 * 12,
        },
        private_key,
        algorithm="ES256",
        headers={"alg": "ES256", "kid": key_id},
    )


def update_config(developer_token, user_token):
    config_path = CONFIG_PATH.resolve()
    content = config_path.read_text()

    content = re.sub(
        r'(:developer-token\s+)"[^"]*"',
        rf'\1"{developer_token}"',
        content,
    )
    content = re.sub(
        r'(:user-token\s+)"[^"]*"',
        rf'\1"{user_token}"',
        content,
    )

    config_path.write_text(content)


class TokenHandler(BaseHTTPRequestHandler):
    def __init__(self, developer_token, html_template, *args, **kwargs):
        self.developer_token = developer_token
        self.html_template = html_template
        super().__init__(*args, **kwargs)

    def do_GET(self):
        if self.path != "/":
            self.send_error(404)
            return

        html = self.html_template.replace("{{DEVELOPER_TOKEN}}", self.developer_token)
        self.send_response(200)
        self.send_header("Content-Type", "text/html")
        self.end_headers()
        self.wfile.write(html.encode())

    def do_POST(self):
        if self.path != "/callback":
            self.send_error(404)
            return

        length = int(self.headers.get("Content-Length", 0))
        user_token = self.rfile.read(length).decode()

        self.send_response(200)
        self.send_header("Content-Type", "text/plain")
        self.end_headers()
        self.wfile.write(b"OK")

        print(f"\nUser token: {user_token[:20]}...{user_token[-20:]}")
        update_config(self.developer_token, user_token)
        print(f"Tokens saved to {CONFIG_PATH.resolve()}")
        print("Done.")

        # Shut down server after response is sent
        Timer(0.5, self.server.shutdown).start()

    def log_message(self, format, *args):
        pass  # suppress default HTTP logs


def main():
    env_file = SCRIPT_DIR / ".env"
    print(f"Config: {env_file} ({'found' if env_file.exists() else 'not found'})")

    team_id, key_id, private_key_path = load_env()
    print(f"Team ID: {team_id}")
    print(f"Key ID: {key_id}")
    print(f"Private key: {private_key_path}")

    developer_token = generate_developer_token(team_id, key_id, private_key_path)
    print(f"Developer token: {developer_token[:20]}...{developer_token[-20:]}")

    html_template = HTML_PATH.read_text()
    handler = partial(TokenHandler, developer_token, html_template)

    server = HTTPServer(("localhost", 8080), handler)
    print(f"\nOpening browser at http://localhost:8080 ...")
    webbrowser.open("http://localhost:8080")
    print("Waiting for authorization...")

    try:
        server.serve_forever()
    except KeyboardInterrupt:
        print("\nAborted.")
        sys.exit(1)
    finally:
        server.server_close()


if __name__ == "__main__":
    main()
