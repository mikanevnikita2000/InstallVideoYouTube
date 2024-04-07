import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;



public class YouTubeExtractor {
    private String videoID;
    private static HashMap<Integer, String> formatUrl = new HashMap<>();
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 6.1; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/97.0.4692.98 Safari/537.36";

    private static final Pattern patYouTubePageLink = Pattern.compile("(http|https)://(www\\.|m.|)youtube\\.com/watch\\?v=(.+?)( |\\z|&)");
    private static final Pattern patYouTubeShortLink = Pattern.compile("(http|https)://(www\\.|)youtu.be/(.+?)( |\\z|&)");

    private static final Pattern patPlayerResponse = Pattern.compile("var ytInitialPlayerResponse\\s*=\\s*(\\{.+?\\})\\s*;");
    private static final Pattern patSigEncUrl = Pattern.compile("url=(.+?)(\\u0026|$)");
    private static final Pattern patSignature = Pattern.compile("s=(.+?)(\\u0026|$)");

    private static final Pattern patVariableFunction = Pattern.compile("([{; =])([a-zA-Z$][a-zA-Z0-9$]{0,2})\\.([a-zA-Z$][a-zA-Z0-9$]{0,2})\\(");
    private static final Pattern patFunction = Pattern.compile("([{; =])([a-zA-Z$_][a-zA-Z0-9$]{0,2})\\(");

    private static final Pattern patDecryptionJsFile = Pattern.compile("\\\\/s\\\\/player\\\\/([^\"]+?)\\.js");
    private static final Pattern patDecryptionJsFileWithoutSlash = Pattern.compile("/s/player/([^\"]+?).js");
    private static final Pattern patSignatureDecFunction = Pattern.compile("(?:\\b|[^a-zA-Z0-9$])([a-zA-Z0-9$]{1,4})\\s*=\\s*function\\(\\s*a\\s*\\)\\s*\\{\\s*a\\s*=\\s*a\\.split\\(\\s*\"\"\\s*\\)");

    private static final HashMap<Integer, Format> FORMAT_MAP = new HashMap<Integer, Format>();

    static {
        // http://en.wikipedia.org/wiki/YouTube#Quality_and_formats

        // Video and Audio
        FORMAT_MAP.put(17, new Format(17, "3gp", 144, Format.VCodec.MPEG4, Format.ACodec.AAC, 24, false));
        FORMAT_MAP.put(36, new Format(36, "3gp", 240, Format.VCodec.MPEG4, Format.ACodec.AAC, 32, false));
        FORMAT_MAP.put(5, new Format(5, "flv", 240, Format.VCodec.H263, Format.ACodec.MP3, 64, false));
        FORMAT_MAP.put(43, new Format(43, "webm", 360, Format.VCodec.VP8, Format.ACodec.VORBIS, 128, false));
        FORMAT_MAP.put(18, new Format(18, "mp4", 360, Format.VCodec.H264, Format.ACodec.AAC, 96, false));
        FORMAT_MAP.put(22, new Format(22, "mp4", 720, Format.VCodec.H264, Format.ACodec.AAC, 192, false));

        // Dash Video
        FORMAT_MAP.put(160, new Format(160, "mp4", 144, Format.VCodec.H264, Format.ACodec.NONE, true));
        FORMAT_MAP.put(133, new Format(133, "mp4", 240, Format.VCodec.H264, Format.ACodec.NONE, true));
        FORMAT_MAP.put(134, new Format(134, "mp4", 360, Format.VCodec.H264, Format.ACodec.NONE, true));
        FORMAT_MAP.put(135, new Format(135, "mp4", 480, Format.VCodec.H264, Format.ACodec.NONE, true));
        FORMAT_MAP.put(136, new Format(136, "mp4", 720, Format.VCodec.H264, Format.ACodec.NONE, true));
        FORMAT_MAP.put(137, new Format(137, "mp4", 1080, Format.VCodec.H264, Format.ACodec.NONE, true));
        FORMAT_MAP.put(264, new Format(264, "mp4", 1440, Format.VCodec.H264, Format.ACodec.NONE, true));
        FORMAT_MAP.put(266, new Format(266, "mp4", 2160, Format.VCodec.H264, Format.ACodec.NONE, true));

        FORMAT_MAP.put(298, new Format(298, "mp4", 720, Format.VCodec.H264, 60, Format.ACodec.NONE, true));
        FORMAT_MAP.put(299, new Format(299, "mp4", 1080, Format.VCodec.H264, 60, Format.ACodec.NONE, true));

        // Dash Audio
        FORMAT_MAP.put(140, new Format(140, "m4a", Format.VCodec.NONE, Format.ACodec.AAC, 128, true));
        FORMAT_MAP.put(141, new Format(141, "m4a", Format.VCodec.NONE, Format.ACodec.AAC, 256, true));
        FORMAT_MAP.put(256, new Format(256, "m4a", Format.VCodec.NONE, Format.ACodec.AAC, 192, true));
        FORMAT_MAP.put(258, new Format(258, "m4a", Format.VCodec.NONE, Format.ACodec.AAC, 384, true));

        // WEBM Dash Video
        FORMAT_MAP.put(278, new Format(278, "webm", 144, Format.VCodec.VP9, Format.ACodec.NONE, true));
        FORMAT_MAP.put(242, new Format(242, "webm", 240, Format.VCodec.VP9, Format.ACodec.NONE, true));
        FORMAT_MAP.put(243, new Format(243, "webm", 360, Format.VCodec.VP9, Format.ACodec.NONE, true));
        FORMAT_MAP.put(244, new Format(244, "webm", 480, Format.VCodec.VP9, Format.ACodec.NONE, true));
        FORMAT_MAP.put(247, new Format(247, "webm", 720, Format.VCodec.VP9, Format.ACodec.NONE, true));
        FORMAT_MAP.put(248, new Format(248, "webm", 1080, Format.VCodec.VP9, Format.ACodec.NONE, true));
        FORMAT_MAP.put(271, new Format(271, "webm", 1440, Format.VCodec.VP9, Format.ACodec.NONE, true));
        FORMAT_MAP.put(313, new Format(313, "webm", 2160, Format.VCodec.VP9, Format.ACodec.NONE, true));

        FORMAT_MAP.put(302, new Format(302, "webm", 720, Format.VCodec.VP9, 60, Format.ACodec.NONE, true));
        FORMAT_MAP.put(308, new Format(308, "webm", 1440, Format.VCodec.VP9, 60, Format.ACodec.NONE, true));
        FORMAT_MAP.put(303, new Format(303, "webm", 1080, Format.VCodec.VP9, 60, Format.ACodec.NONE, true));
        FORMAT_MAP.put(315, new Format(315, "webm", 2160, Format.VCodec.VP9, 60, Format.ACodec.NONE, true));

        // WEBM Dash Audio
        FORMAT_MAP.put(171, new Format(171, "webm", Format.VCodec.NONE, Format.ACodec.VORBIS, 128, true));

        FORMAT_MAP.put(249, new Format(249, "webm", Format.VCodec.NONE, Format.ACodec.OPUS, 48, true));
        FORMAT_MAP.put(250, new Format(250, "webm", Format.VCodec.NONE, Format.ACodec.OPUS, 64, true));
        FORMAT_MAP.put(251, new Format(251, "webm", Format.VCodec.NONE, Format.ACodec.OPUS, 160, true));

        // HLS Live Stream
        FORMAT_MAP.put(91, new Format(91, "mp4", 144, Format.VCodec.H264, Format.ACodec.AAC, 48, false, true));
        FORMAT_MAP.put(92, new Format(92, "mp4", 240, Format.VCodec.H264, Format.ACodec.AAC, 48, false, true));
        FORMAT_MAP.put(93, new Format(93, "mp4", 360, Format.VCodec.H264, Format.ACodec.AAC, 128, false, true));
        FORMAT_MAP.put(94, new Format(94, "mp4", 480, Format.VCodec.H264, Format.ACodec.AAC, 128, false, true));
        FORMAT_MAP.put(95, new Format(95, "mp4", 720, Format.VCodec.H264, Format.ACodec.AAC, 256, false, true));
        FORMAT_MAP.put(96, new Format(96, "mp4", 1080, Format.VCodec.H264, Format.ACodec.AAC, 256, false, true));
    }

    protected void doInBackground(String url) {
        videoID = null;

        Matcher mat = patYouTubePageLink.matcher(url);//надо почитать что такое Matcher
        if (mat.find()) {
            videoID = mat.group(3);
        } else {
            mat = patYouTubeShortLink.matcher(url);
            if (mat.find()) {
                videoID = mat.group(3);
            } else if (url.matches("\\p{Graph}+?")) {
                videoID = url;
            }
        }
        if (videoID != null) {
            try {
                System.out.println("videoID: " + videoID);
                getStreamUrls(videoID);
            } catch (Exception e) {
                //Log.e(LOG_TAG, "Extraction failed", e);
            }
        } else {
            //Log.e(LOG_TAG, "Wrong YouTube link format");
        }
        String endUrl = "";
        Scanner scanner = new Scanner(System.in);
        int vFormat = scanner.nextInt();
        switch (vFormat){
                case 1080:
                endUrl = formatUrl.get(1080);
                case 720:
                endUrl = formatUrl.get(720);
                case 480:
                endUrl = formatUrl.get(480);
                case 360:
                endUrl = formatUrl.get(360);
                case 240:
                endUrl = formatUrl.get(240);
                case 144:
                endUrl = formatUrl.get(144);
        }
        Dounl(endUrl);
        System.out.println("endUrl: " + endUrl);
    }

    private void getStreamUrls(String videoID) throws IOException, InterruptedException, JSONException
    {
        String pageHtml;
        YtFile ytFile = null;
        HashMap<Object, Object> encSignatures = new HashMap<>();
        HashMap<Object, Object> ytFiles = new HashMap<>();
        BufferedReader reader = null;
        HttpURLConnection urlConnection = null;
        URL getUrl = new URL("https://youtube.com/watch?v=" + videoID);
        try {
            urlConnection = (HttpURLConnection) getUrl.openConnection();
            urlConnection.setRequestProperty("User-Agent", USER_AGENT);
            reader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
            StringBuilder sbPageHtml = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sbPageHtml.append(line);
            }
            pageHtml = sbPageHtml.toString();
        } finally {
            if (reader != null) {
                reader.close();
            }
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
        }

        System.out.println("Выбирите формат: ");
        List<Integer> heightVideo = new ArrayList<>();

        Matcher mat = patPlayerResponse.matcher(pageHtml);
        if (mat.find()) {
            JSONObject ytPlayerResponse = new JSONObject(mat.group(1));
            JSONObject streamingData = ytPlayerResponse.getJSONObject("streamingData");

            JSONArray adaptiveFormats = streamingData.getJSONArray("adaptiveFormats");

            //System.out.println("adaptiveFormats: " + adaptiveFormats);

            for (int i = 0; i < adaptiveFormats.length(); i++) {

                JSONObject adaptiveFormat = adaptiveFormats.getJSONObject(i);
                //System.out.println("adaptiveFormats : " + adaptiveFormats);
                int form = adaptiveFormat.getInt("height");
                if (!(formatUrl.containsKey(form))){
                    String urlD = adaptiveFormat.getString("url");
                    formatUrl.put(form, urlD);
                    System.out.println(form);
                }
            }

        }

    }


    public void Dounl(String endUrl){
        URL url = null;
        try {
            url = URI.create(endUrl).toURL();
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
        String fileName = "Video1";
        Path filePath = Paths.get("C:\\Users\\Nikita\\IdeaProjects\\TestYouTubeVideo\\video", fileName);
        //Files.createDirectories(filePath.getParent());
        try (BufferedInputStream inputStream = new BufferedInputStream(url.openStream());
             FileOutputStream outputStream = new FileOutputStream(filePath.toFile())) {

            // Буферизованное считывание данных из URL
            byte[] buffer = new byte[1024];
            int bytesRead;

            // Запись данных в файл
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // Вывести сообщение об успешном скачивании
        System.out.println("Видео успешно скачано в файл: " + filePath);
    }

}

