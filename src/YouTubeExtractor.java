import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;



public class YouTubeExtractor {
    private String videoID;

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
        //Log.i(LOG_TAG, "getUrl " + String.valueOf(getUrl));
        System.out.println("getUrl: \n" + getUrl);
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
            //System.out.println("pageHtml: \n" + pageHtml);
            //Log.i(LOG_TAG, "pageHtml" + String.valueOf(pageHtml));
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

            JSONArray formats = streamingData.getJSONArray("formats");
            for (int i = 0; i < formats.length(); i++) {

                JSONObject format = ((JSONArray) formats).getJSONObject(i);


                String type = format.optString("type");
                //System.out.println("type: " + type);
                if (type != null && type.equals("FORMAT_STREAM_TYPE_OTF"))
                    continue;

                int itag = format.getInt("itag");
                System.out.println("itag: " + itag);
                //System.out.println("FORMAT_MAP.get(itag): " + FORMAT_MAP.get(itag));
                heightVideo.add(FORMAT_MAP.get(itag).getHeight());
                if (FORMAT_MAP.get(itag) != null) {
                    if (format.has("url")) {

                        String url = format.getString("url").replace("\\u0026", "&");
                        ytFiles.put(itag, new YtFile(FORMAT_MAP.get(itag), url));
                    } else if (format.has("signatureCipher")) {

                        mat = patSigEncUrl.matcher(format.getString("signatureCipher"));
                        Matcher matSig = patSignature.matcher(format.getString("signatureCipher"));
                        if (mat.find() && matSig.find()) {
                            String url = URLDecoder.decode(mat.group(1), "UTF-8");
                            String signature = URLDecoder.decode(matSig.group(1), "UTF-8");
                            ytFiles.put(itag, new YtFile(FORMAT_MAP.get(itag), url));
                            //System.out.println("url ytFiles1" + ytFile.toString());
                            encSignatures.put(itag, signature);
                        }

                    }
                }
            }

            JSONArray adaptiveFormats = streamingData.getJSONArray("adaptiveFormats");
            //System.out.println("adaptiveFormats: " + adaptiveFormats);
            for (int i = 0; i < adaptiveFormats.length(); i++) {

                JSONObject adaptiveFormat = adaptiveFormats.getJSONObject(i);

                String type = adaptiveFormat.optString("type");
                if (type != null && type.equals("FORMAT_STREAM_TYPE_OTF"))
                    continue;

                int itag = adaptiveFormat.getInt("itag");

                if (FORMAT_MAP.get(itag) != null) {
                    if (adaptiveFormat.has("url")) {
                        String url = adaptiveFormat.getString("url").replace("\\u0026", "&");
                        ytFiles.put(itag, new YtFile(FORMAT_MAP.get(itag), url));
                    } else if (adaptiveFormat.has("signatureCipher")) {

                        mat = patSigEncUrl.matcher(adaptiveFormat.getString("signatureCipher"));
                        Matcher matSig = patSignature.matcher(adaptiveFormat.getString("signatureCipher"));
                        if (mat.find() && matSig.find()) {
                            String url = URLDecoder.decode(mat.group(1), "UTF-8");
                            String signature = URLDecoder.decode(matSig.group(1), "UTF-8");
                            ytFiles.put(itag, new YtFile(FORMAT_MAP.get(itag), url));
                            //System.out.println("url ytFiles2" + ytFile.toString());
                            encSignatures.put(itag, signature);
                        }
                    }
                }
                System.out.println(adaptiveFormats);
            }
            JSONObject videoDetails = ytPlayerResponse.getJSONObject("videoDetails");
            addButtonToMainLayout(videoDetails.getString("title"), videoDetails);

        }


    }
    private void addButtonToMainLayout(String videoTitle, YtFragmentedVideo ytFrVideo){
        String btnText;
        if (ytFrVideo.height == -1)
            btnText = "Audio " + ytFrVideo.audioFile.getFormat().getAudioBitrate() + " kbit/s";
        else
            btnText = (ytFrVideo.videoFile.getFormat().getFps() == 60) ? ytFrVideo.height + "p60" :
                    ytFrVideo.height + "p";
        String filename;
        if (videoTitle.length() > 55) {
            filename = videoTitle.substring(0, 55);
        } else {
            filename = videoTitle;
        }
        filename = filename.replaceAll("[\\\\><\"|*?%:#/]", "");
        filename += (ytFrVideo.height == -1) ? "" : "-" + ytFrVideo.height + "p";
        String downloadIds = "";
        boolean hideAudioDownloadNotification = false;
        if (ytFrVideo.videoFile != null) {
//            downloadIds += downloadFromUrl(ytFrVideo.videoFile.getUrl(), videoTitle,
//                    filename + "." + ytFrVideo.videoFile.getFormat().getExt(), false);
//            downloadIds += "-";
//            hideAudioDownloadNotification = true;
        }
        if (ytFrVideo.audioFile != null) {
//            downloadIds += downloadFromUrl(ytFrVideo.audioFile.getUrl(), videoTitle,
//                    filename + "." + ytFrVideo.audioFile.getFormat().getExt(), hideAudioDownloadNotification);
        }



//    boolean formatSelected = false;
//    Scanner sc = new Scanner(System.in);
//        while (formatSelected == false){
//        for (int i = 0; i < heightVideo.size(); i++){
//            System.out.println(heightVideo.get(i));
//        }
//
//        int format = Integer.parseInt(sc.next());
//        for(int i = 0; i < heightVideo.size(); i++){
//            if (format == heightVideo.get(i)){
//                formatSelected = true;
//                System.out.println("Вы выбрали формат: " + format);
//
//
//
//                YtFile ytFile = new YtFile(format, url);
//
//
//                break;
//            }
//        }
//        if (formatSelected == false){
//            System.out.println("Не верный формат. Выбирете нужный.");
//        }
//
//
//
//
//
//
//    List<Integer> itag = new ArrayList<>();
//    JSONObject ytPlayerResponse = new JSONObject(mat.group(1));
//    JSONObject streamingData = ytPlayerResponse.getJSONObject("streamingData");
//    JSONArray adaptiveFormats = streamingData.getJSONArray("adaptiveFormats");
//                    for (int i = 0; i < adaptiveFormats.length(); i++){
//        itag.add(FORMAT_MAP.)
//    }
//
//                    for (int i = 0; i < FORMAT_MAP.size(); i++){
//
//        if (format == FORMAT_MAP.get(i))
//
//    }
//
//
//
//    private void addButtonToMainLayout(final String videoTitle, final YtFragmentedVideo ytFrVideo){
//        String btnText;
//        if (ytFrVideo.height == -1)
//            btnText = "Audio " + ytFrVideo.audioFile.getFormat().getAudioBitrate() + " kbit/s";
//        else
//            btnText = (ytFrVideo.videoFile.getFormat().getFps() == 60) ? ytFrVideo.height + "p60" :
//                    ytFrVideo.height + "p";
//        String filename;
//        if (videoTitle.length() > 55) {
//            filename = videoTitle.substring(0, 55);
//        } else {
//            filename = videoTitle;
//        }
//        filename = filename.replaceAll("[\\\\><\"|*?%:#/]", "");
//        filename += (ytFrVideo.height == -1) ? "" : "-" + ytFrVideo.height + "p";
//        String downloadIds = "";
//        boolean hideAudioDownloadNotification = false;
//        if (ytFrVideo.videoFile != null) {
//            downloadIds += downloadFromUrl(ytFrVideo.videoFile.getUrl(), videoTitle,
//                    filename + "." + ytFrVideo.videoFile.getFormat().getExt(), false);
//            downloadIds += "-";
//            hideAudioDownloadNotification = true;
//        }
//        if (ytFrVideo.audioFile != null) {
//            downloadIds += downloadFromUrl(ytFrVideo.audioFile.getUrl(), videoTitle,
//                    filename + "." + ytFrVideo.audioFile.getFormat().getExt(), hideAudioDownloadNotification);
//        }
//    }
//
//    private long downloadFromUrl(String youtubeDlUrl, String downloadTitle, String fileName, boolean hide) {
//        Uri uri = Uri.parse(youtubeDlUrl);
//        DownloadManager.Request request = new DownloadManager.Request(uri);
//        request.setTitle(downloadTitle);
//        if (hide) {
//            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_HIDDEN);
//            request.setVisibleInDownloadsUi(false);
//        } else
//            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
//
//        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName);
//
//        DownloadManager manager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
//        return manager.enqueue(request);
//    }
//
    private static class YtFragmentedVideo {
        int height;
        YtFile audioFile;
        YtFile videoFile;
    }
}
