import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;


public class ReadJson {

    public ReadJson() {
    }

    public String[] getJson() throws IOException, ParseException {
        String fileName = "sample.json";
        JSONParser jsonParser = new JSONParser();
        FileReader reader = new FileReader(fileName);
        Object obj = jsonParser.parse(reader);
        JSONObject file =  (JSONObject)obj;
        String status = (String) file.get("status");
        String move = (String) file.get("move");
        return new String[] {status, move};
    }

}