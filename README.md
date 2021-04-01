# LibreCaptcha
LibreCaptcha is a framework that allows developers to create their own [CAPTCHA](https://en.wikipedia.org/wiki/CAPTCHA)s.
It allows developers to easily create new types of CAPTCHAs by defining a structure for them. Mundane details are handled by the
framework itself. Details such as:
  * Background workers to render CAPTCHAs and to store them in a database
  * Providing an HTTP interface for serving CAPTCHAs
  * Managing secrets for the CAPTCHAs (tokens, expected answers, etc)
  * Safe re-impressions of CAPTCHA images (by creating unique tokens for every impression)
  * Sandboxed plugin architecture (To be done)

Some sample CAPTCHA generators are included in the distribution. We will continue adding more samples to the list. For quick
deployments the samples themselves might be sufficient. Projects with more resources could create their own CAPTCHAs
and use the samples as inspiration.

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

## Configuration
If a `config.json` file is not present in the `data/` folder, the app creates one, and this can be modified
to customize the app features, such as which CAPTCHAs are enabled and their difficulty settings.

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

An image of a random string of alphabets is created. Then a series of image filters that add effecs such as Smear, Diffuse, and Ripple are applied to the image to make it less readable.

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
       - click (choose)
    - `media`: `String` - 
      The type of media of a captcha
       - image
       - audio
       - gif
    - `size`: `dict` - 
      The dimensions of a captcha (Optional). It needs two more fields nested in this parameter
       - `height`: `Int`
       - `width`: `Int`

  - Return type:
    - `id`: `String` - The uuid of the captcha generated


### - `/v1/media`: `POST`,`GET` 
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
