import time

import jwt

TEAM_ID = "...."
KEY_ID = "...."
PRIVATE_KEY_PATH = "***.p8"

with open(PRIVATE_KEY_PATH) as f:
    private_key = f.read()

token = jwt.encode(
    {
        "iss": TEAM_ID,
        "iat": int(time.time()),
        "exp": int(time.time()) + 3600 * 12,
    },
    private_key,
    algorithm="ES256",
    headers={"alg": "ES256", "kid": KEY_ID},
)

print("✅ Developer Token:\n", token)
