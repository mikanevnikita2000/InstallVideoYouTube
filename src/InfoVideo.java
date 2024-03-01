import org.json.JSONObject;

import java.util.List;

public class InfoVideo {
    public void InfoVideo(JSONObject videoDetails){
        //String format =
    }

    public List<Integer> formats;
    public void Formats(Format format){
        Format format1 = format;
        formats.add(Integer.getInteger(String.valueOf(format1.getHeight())));
    }

    @Override
    public String toString() {

        return formats.toString();
    }
}
