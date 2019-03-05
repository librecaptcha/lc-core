package lc;

public class MainCaptcha implements ChallengeProvider{

    public String getId(){
        return "SomeText";
    }

    public Challenge returnChallenge(){
        CaptchaTypes captchaTypes = new CaptchaTypes();
        return new Challenge(captchaTypes.type_1("Hello"),"png","qwert");
    }

    public boolean checkAnswer(String secret, String answer){
        return true;
    }

    public static void main(String[] args){
        MainCaptcha mainCaptcha = new MainCaptcha();
        Challenge challenge = mainCaptcha.returnChallenge();
        System.out.println("Content type: " + challenge.contentType);
    }
}
