
1. Create dev key (https://developer.apple.com/account/resources/authkeys/list), download it.
2. Use `token_generation.py` to generate developer token.
3. `python3 -m http.server 8080` and open `http://localhost:8080`
4. In browser console:


```
const music = MusicKit.getInstance();
MusicKit.configure({
  developerToken: '...',
  app: { name: 'Test', build: '1.0' }
});
music.authorize().then(token => console.log("userToken:", token));
```