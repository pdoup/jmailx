/** This class represents a FetchQuote that can fetch quotes from an API. */
package iti.mail;

import org.apache.commons.cli.*;
import org.apache.http.HttpException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

public class FetchQuote {
    /** The URL of the API. */
    public static final String API_URL = "https://api.quotable.io/random";

    /**
     * Fetches a quote from the API.
     *
     * @return the fetched quote and its author, or an error message if an exception occurs
     */
    public static String getQuote() {
        try {
            URL url = new URL(API_URL);
            HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
            int responseCode = connection.getResponseCode();

            if (responseCode == HttpsURLConnection.HTTP_OK) {
                BufferedReader in =
                        new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String inputLine;
                StringBuffer response = new StringBuffer();

                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();

                // Parse the JSON response to get the quote and author
                JSONObject jsonResponse = new JSONObject(response.toString());

                String quote = jsonResponse.getString("content");
                String author = jsonResponse.getString("author");

                return quote + " - " + author;

            } else {
                throw new HttpException(
                        "Failed to fetch quote from " + API_URL + " [" + responseCode + " Code]");
            }
        } catch (Exception e) {
            return "Exception: " + e.getMessage();
        }
    }
}
