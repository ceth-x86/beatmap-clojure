
1. Create dev key (https://developer.apple.com/account/resources/authkeys/list), download it.
2. Use `token_generation.py` to generate developer token.
3. Подставляем токен в html и вот этот код ниже.
4. `python3 -m http.server 8080` and open `http://localhost:8080`
5. In browser console:


```
const music = MusicKit.getInstance();
MusicKit.configure({
  developerToken: 'eyJhbGciOiJFUzI1NiIsImtpZCI6IjVTUUQ3MkRHR1oiLCJ0eXAiOiJKV1QifQ.eyJpc3MiOiI2ODlCNlUzQVRXIiwiaWF0IjoxNzcwNjU3MTkwLCJleHAiOjE3NzQ5NzcxOTB9.mcFQ66-6qchUVH69Dzpqxlfx_Xpi2WV3uKRnkygOT_njNPvkuztMPHqdfMwq0OVQveGwk8r_hN_g0jYt12o-Cg',
  app: { name: 'Test', build: '1.0' }
});
music.authorize().then(token => console.log("userToken:", token));
```

6. 