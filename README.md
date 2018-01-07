# lc-core
a Captcha framework

Currently, the following example Captchas are provided by LibreCaptcha:

`BlurCaptcha`
An image of a word is blurred before being shown to the user.

`LabelCaptcha`
An image that has a pair of words is created. The answer to one of the words is known and to that of the other is unknown. The user is tested on the known word, and their answer to the unknown word is recorded. If a sufficient number of users agree on their answer to the unknown word, it is transferred to the list of known words.

`FilterCaptcha`
An image of a random string of alphabets is created. Then a series of image filters that add effecs such as Smear, Diffuse, and Ripple are applied to the image to make it less readable.
