# LibreCaptcha
LibreCaptcha is a framework that allows developers to create their own [CAPTCHA](https://en.wikipedia.org/wiki/CAPTCHA)s.
The framework defines the API for a CAPTCHA generator and takes care of mundane details
such as:
  * An HTTP interface for serving CAPTCHAs
  * Background workers to pre-compute CAPTCHAs and to store them in a database
  * Managing secrets for the CAPTCHAs (tokens, expected answers, etc)
  * Safe re-impressions of CAPTCHA images (by creating unique tokens for every impression)
  * Garbage collection of stale CAPTCHAs
  * Sandboxed plugin architecture (TBD)

Some sample CAPTCHA generators are included in the distribution (see below). We will continue adding more samples to the list. For quick
deployments the samples themselves might be sufficient. Projects with more resources might want create their own CAPTCHAs
and use the samples as inspiration. See the [CAPTCHA creation guide](https://github.com/librecaptcha/lc-core/wiki/Creating-your-own-CAPTCHA-provider).

## Quick start with Docker
Using `docker-compose`:

```
git clone https://github.com/librecaptcha/lc-core.git
docker-compose up
```

Using `docker`:

```
docker run -v lcdata:/lc-core/data librecaptcha/lc-core:latest
```

A default `config.json` is automatically created in the mounted volume.

To test the installation, try:

```
curl -d '{"media":"image/png","level":"easy","input_type":"text"}' localhost:8888/v1/captcha
```
This should return an id that can be used in further API calls. The API endpoints are described below.

## Configuration
If a `config.json` file is not present in the `data/` folder, the app creates one, and this can be modified
to customize the app features, such as which CAPTCHAs are enabled and their difficulty settings.

More details can be found [in the wiki](https://github.com/librecaptcha/lc-core/wiki/Configuration)

## Why LibreCaptcha?

### Eliminate dependency on a third-party
An open-source CAPTCHA framework will allow anyone to host their own CAPTCHA service and thus avoid dependencies on
third-parties.

### Respecting user privacy
A self-hosted service prevents user information from leaking to other parties.

### More variety of CAPTCHAs
Ain't it boring to identify photos of buses, store-fronts and traffic signals? With LibreCaptcha, developers can
create CAPTCHAs that suit their application and audience, with matching themes and looks.

And, the more the variety of CAPTCHAS, the harder it is for bots to crack CAPTCHAs.

## Sample CAPTCHAs

### FilterCaptcha

![FilterCaptcha Sample](./samples/FilterChallenge.png)

An image of a random string of alphabets is created. Then a series of image filters that add effects such as Smear, Diffuse, and Ripple are applied to the image to make it less readable.

### RainDropsCaptcha
![RaindDrops Sample](./samples/RainDropsCaptcha.gif)

### BlurCaptcha
An image of a word is blurred before being shown to the user.

### LabelCaptcha
An image that has a pair of words is created. The answer to one of the words is known and to that of the other is unknown. The user is tested on the known word, and their answer to the unknown word is recorded. If a sufficient number of users agree on their answer to the unknown word, it is transferred to the list of known words.

***

## HTTP API 
### - `/v1/captcha`: `POST`
  - Parameters:
    - `level`: `String` - 
      The difficulty level of a captcha
       - easy
       - medium
       - hard
    - `input_type`: `String` - 
      The type of input option for a captcha
       - text
       - (More to come)
    - `media`: `String` - 
      The type of media of a captcha
       - image/png
       - image/gif
       - (More to come)
    - `size`: `Map` - 
      The dimensions of a captcha (Optional). It needs two more fields nested in this parameter
       - `height`: `Int`
       - `width`: `Int`

  - Return type:
    - `id`: `String` - The uuid of the captcha generated


### - `/v1/media`: `GET` 
  - Parameters:
    - `id`: `String` - The uuid of the captcha

  - Return type:
    - `image`: `Array[Byte]` - The requested media as bytes


### - `/v1/answer`: `POST`
  - Parameter:
    - `id`: `String` - The uuid of the captcha that needs to be solved
    - `answer`: `String` - The answer to the captcha that needs to be validated

  - Return Type:
    - `result`: `String` - The result after validation/checking of the answer
      - True - If the answer is correct
      - False - If the answer is incorrect
      - Expired - If the time limit to solve the captcha exceeds

***

## Roadmap

Things to do in the future:
* Sandboxed plugin architecture
* Audio CAPTCHA samples
* Interactive CAPTCHA samples
