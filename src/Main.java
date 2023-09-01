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

//    String fileName = "src/photosAndVideos/dog.jpg";
//
//    FileOutputStream fout = null;
//    BufferedInputStream in = null;
//
//        try {
//                in = new BufferedInputStream(new URL(url).openStream());
//                fout = new FileOutputStream(fileName);
//                byte data[] = new byte[1024];
//                int count;
//                while ((count = in.read(data, 0, 1024)) != -1)
//                {
//                fout.write(data, 0, count);
//                fout.flush();
//                }
//                } catch (MalformedURLException e) {
//                e.printStackTrace();
//                } catch (IOException e) {
//                e.printStackTrace();
//                } finally {
//                {
//                try {
//                in.close();
//                } catch (IOException e) {
//                throw new RuntimeException(e);
//                } finally {
//                try {
//                fout.close();
//                } catch (IOException e) {
//                throw new RuntimeException(e);
//                }
//                }
//                }
//                }