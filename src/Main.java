import java.util.Scanner;

public class Main {

    static String api = "";


    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        boolean correctURL = false;
        YouTubeExtractor youTubeExtractor = new YouTubeExtractor();

        System.out.print("Введите URL: ");
        while (correctURL == false){
            String url = sc.next();

            if (url != null && (url.contains("://youtu.be/") || url.contains("youtube.com/watch?v="))) {
                correctURL = true;

                youTubeExtractor.doInBackground(url);

                //System.out.println("True");
            } else {
                System.out.println("Введите URL видео с ютуба");// https://www.youtube.com/watch?v=DIOjHmig5S4
            }
        }
    }
}
