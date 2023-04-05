import express from 'express'
import helmet from 'helmet'
const app = express();
import fetch from 'node-fetch';
import * as dotenv from 'dotenv'
dotenv.config()

app.use(helmet.contentSecurityPolicy({
  directives: {
    defaultSrc: ["'self'", "player.twitch.tv"],
  }
}));

app.get('/app_token', async (req, res) => {
  const params = new URLSearchParams();
  params.append('client_id', process.env.CLIENT_ID);
  params.append('client_secret', process.env.CLIENT_SECRET);
  params.append('grant_type', 'client_credentials');

  const options = {
    method: 'POST',
    body: params
  };

  const response = await fetch("https://id.twitch.tv/oauth2/token", options);
  const data = await response.json();

  console.log(data)

  res.send(data['access_token'])
});

app.get('/stream', (req, res) => {

  const channel = req.query.channel;
  const height = req.query.height;
  const width = req.query.width;

  console.log("https://gcttv.samste-vault.net/stream : channel:" + channel + ", height:" + height + ", width:" + width);

  res.send(`
  <body style="margin: 0;">
    <iframe src="https://player.twitch.tv/?channel=${channel}&parent=gcttv.samste-vault.net" frameborder="0" height="${height}" width="${width}" allowfullscreen="false" scrolling="no"></iframe>
  </body>
  `);
});

app.get('/twitch_auth_redirect', async (req, res) => {
  const code = req.query.code;
  const scope = req.query.scope;
  const error = req.query.error;

  console.log("https://gcttv.samste-vault.net/twitch_auth_redirect : code: " + code + ", scope :" + scope);

  console.log("CLIENT_ID : " + process.env.CLIENT_ID)
  console.log("CLIENT_SECRET : " + process.env.CLIENT_SECRET)

  const params = new URLSearchParams();
  params.append('client_id', process.env.CLIENT_ID);
  params.append('client_secret', process.env.CLIENT_SECRET);
  params.append('code', code);
  params.append('grant_type', 'authorization_code');
  params.append('redirect_uri', 'https://gcttv.samste-vault.net/twitch_token_redirect');

  const options = {
    method: 'POST',
    body: params
  };

  const response = await fetch("https://id.twitch.tv/oauth2/token", options);
  const data = await response.json();

  if (code) {
    // Redirect to the app with the access token
    res.redirect(`gcttv://auth?access_token=${data['access_token']}&scope=${data['scope']}`);
  } else if (error) {
    // Redirect to the app with the error
    res.redirect(`gcttv://auth?error=${error}`);
  } else {
    res.status(400).send('Invalid request');
  }
});

/*
app.get('/twitch_token_redirect', (req, res) => {
  const access_token = req.query.access_token;
  const scope = req.query.scope;
  const error = req.query.error;

  console.log("https://gcttv.samste-vault.net/twitch_token_redirect : access_token: " + access_token + ", scope :" + scope);

  if (code) {
    // Redirect to the app with the access token
    res.redirect(`gcttv://auth?access_token=${access_token}&scope=${scope}`);
  } else if (error) {
    // Redirect to the app with the error
    res.redirect(`gcttv://auth?error=${error}`);
  } else {
    res.status(400).send('Invalid request');
  }
});
*/

app.listen(3000, () => {
  console.log('Server started on port 3000');
});
